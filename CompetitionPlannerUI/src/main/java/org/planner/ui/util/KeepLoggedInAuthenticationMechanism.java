package org.planner.ui.util;

import static io.undertow.UndertowMessages.MESSAGES;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.xml.bind.DatatypeConverter;

import org.planner.eo.User;
import org.planner.remote.ServiceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.servlet.handlers.security.ServletFormAuthenticationMechanism;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

public class KeepLoggedInAuthenticationMechanism extends ServletFormAuthenticationMechanism {

	public static class Factory implements AuthenticationMechanismFactory {
		@Override
		public AuthenticationMechanism create(String mechanismName, FormParserFactory formParserFactory,
				Map<String, String> properties) {
			return new KeepLoggedInAuthenticationMechanism(formParserFactory, mechanismName, properties.get(LOGIN_PAGE),
					properties.get(ERROR_PAGE));
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(KeepLoggedInAuthenticationMechanism.class);

	public static final String COOKIE_TOKEN = "utok";

	private static final ThreadLocal<User> authenticatedUser = new ThreadLocal<>();

	static User getAuthenticatedUser() {
		return authenticatedUser.get();
	}

	public static String getIdentity(User user) {
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-512");
			byte[] digest = sha
					.digest((user.getId().toString() + Long.toString(user.getCreateTime().getTime())).getBytes("UTF8"));
			return DatatypeConverter.printHexBinary(digest);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public KeepLoggedInAuthenticationMechanism(FormParserFactory formParserFactory, String name, String loginPage,
			String errorPage) {
		super(formParserFactory, name, loginPage, errorPage, null, false);
	}

	@Override
	@SuppressWarnings("deprecation")
	public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
		if (!(exchange.getRequestMethod().equals(Methods.POST)
				&& exchange.getRequestPath().endsWith(DEFAULT_POST_LOCATION))) {
			Map<String, Cookie> cookies = exchange.getRequestCookies();
			Cookie cookie = cookies.get(COOKIE_TOKEN);
			if (cookie != null) {
				String cookieValue = cookie.getValue();

				Instance<ServiceFacade> instance = getServiceInstance();
				ServiceFacade service = instance.get();
				try {
					User user = service.authenticate(cookieValue, false);

					if (user != null) {
						if (LOG.isDebugEnabled())
							LOG.debug("Authenticated user " + user.getUserId() + " by cookie: " + cookieValue);

						authenticatedUser.set(user);
						AuthenticationMechanismOutcome outcome = null;
						Account account = securityContext.getIdentityManager().verify(user.getUserId(), null);
						if (account != null) {
							securityContext.authenticationComplete(account, securityContext.getMechanismName(), true);
							outcome = AuthenticationMechanismOutcome.AUTHENTICATED;
							// im Moment keine Idee, wie der mehrfache Aufruf beim Hereinkommen mehrerer
							// nicht authentisierter Requests verhindert werden kann:
							// die HTTP-Session existiert noch nicht (aber auch wenn man sie neu erzeugt,
							// hilft das nichts, da alle Requests ohne Session-Cookie kommen)
							// und an den Security-Context, der in den Attachments steckt,
							// kommt man aus Classloading-Gründen nicht heran
							service.saveLastLogonTime();
						} else {
							securityContext.authenticationFailed(MESSAGES.authenticationFailed(user.getUserId()),
									securityContext.getMechanismName());
						}

						authenticatedUser.remove();
						return outcome != null ? outcome : AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
					}
				} finally {
					instance.destroy(service);
				}
			}
		}
		return super.authenticate(exchange, securityContext);
	}

	@Override
	protected void handleRedirectBack(HttpServerExchange exchange) {

		Instance<ServiceFacade> instance = getServiceInstance();
		ServiceFacade service = instance.get();
		try {
			if (exchange.getRequestHeaders().getFirst("Stay-Logged-In") != null) {

				// Authentisierung erfolgt, Speichern der Re-Auth-Info
				Map<String, Cookie> cookies = exchange.getRequestCookies();
				Cookie cookie = cookies.get(COOKIE_TOKEN);
				String newCookieValue = service.rememberMe(cookie != null ? cookie.getValue() : null);
				addNewTokenCookie(exchange, newCookieValue);

				if (LOG.isDebugEnabled())
					LOG.debug("Added new authentication cookie " + newCookieValue);
			}
			service.saveLastLogonTime();
			String identity = getIdentity(service.getLoggedInUser());

			// da wir durch saveOriginalRequest=false keinen Redirect senden,
			// wird für den Ajax-Caller eine leere XML-Struktur zurückgegeben
			// das ist hauptsächlich ein FF-Problem
			try {
				exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/plain");
				exchange.getOutputStream().write(identity.getBytes());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			instance.destroy(service);
		}
		super.handleRedirectBack(exchange);
	}

	private void addNewTokenCookie(HttpServerExchange exchange, String value) {
		CookieImpl cookie = new CookieImpl(COOKIE_TOKEN, value);
		cookie.setHttpOnly(true);
		cookie.setPath(exchange.getResolvedPath());
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, 1);
		cookie.setExpires(calendar.getTime());
		exchange.setResponseCookie(cookie);
	}

	private Instance<ServiceFacade> getServiceInstance() {
		return CDI.current().select(ServiceFacade.class);
	}
}
