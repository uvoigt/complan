package org.planner.business;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Message.RecipientType;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringUtils;
import org.planner.dao.PlannerDao;
import org.planner.eo.Role;
import org.planner.eo.Role_;
import org.planner.eo.Token;
import org.planner.eo.Token.TokenType;
import org.planner.eo.User;
import org.planner.model.Suchkriterien;
import org.planner.util.CommonMessages;
import org.planner.util.LogUtil;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.LogUtil.TechnischeException;
import org.planner.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RegistryImpl {

	private static final Logger LOG = LoggerFactory.getLogger(RegistryImpl.class);

	static String encodePw(String password) {
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-512");
			byte[] digest = sha.digest(password.getBytes("UTF8"));
			// mit crlf ist JBoss etwas pissig!
			return new String(Base64.getMimeEncoder(76, new byte[] { '\n' }).encode(digest), "UTF8");
		} catch (Exception e) {
			LogUtil.handleException(e, LOG, "Fehler beim Kodieren des Passworts");
			return null;
		}
	}

	@Inject
	private Messages messages;

	@Inject
	private PlannerDao plannerDao;

	@Inject
	private MasterDataServiceImpl masterData;

	@Inject
	private CommonImpl common;

	@Resource(lookup = "java:jboss/mail/Default")
	private Session session;

	public String resetPassword(String token, String password) throws TechnischeException {
		User user = authenticate(token, true);
		if (user == null)
			return null;
		user.setPassword(encodePw(password));
		// lösche alle Tokens
		user.getTokens().clear();
		if (LOG.isInfoEnabled())
			LOG.info("Neues Passwort für " + user + " gesetzt.");
		// der Caller ist "anonymous", deshalb hier der Benutzer
		plannerDao.save(user, user.getUserId());

		String emailText = getFormattedMessage("email.passwordchanged.html", user.getFirstName(), user.getLastName());

		String subject = messages.getMessage("email.passwordchanged.subject");
		sendEmail(user, subject, emailText);

		return user.getUserId();
	}

	public User authenticate(String token, boolean email) {
		byte[] hashFromToken = new byte[64]; // digest length sha-512
		String userId = extractHashFromToken(token, hashFromToken);

		User user = plannerDao.getUserByUserId(userId);
		if (user == null)
			return null;
		Token storedToken = null;
		if (email) {
			storedToken = getStoredEmailToken(user);
			if (!isTokenValid(user.getUserId(), storedToken, hashFromToken))
				return null;
		} else {
			for (Token t : getStoredLoginTokens(user)) {
				if (!isTokenValid(user.getUserId(), t, hashFromToken))
					continue;
				storedToken = t;
				break;
			}
		}
		if (storedToken == null)
			return null;

		if (System.currentTimeMillis() > storedToken.getTokenExpires()) {
			if (LOG.isInfoEnabled())
				LOG.info("Die Gültigkeit des Login- oder E-Mail-Tokens für " + user + " ist abgelaufen.");
			user.getTokens().remove(storedToken);
			plannerDao.saveToken(user);
			return null;
		}
		return user;
	}

	private String extractHashFromToken(String token, byte[] hashFromToken) {
		byte[] decoded = null;
		try {
			token = URLDecoder.decode(token, "UTF8");
			decoded = Base64.getDecoder().decode(token.getBytes("UTF8"));
		} catch (Exception e) {
			LOG.error("Fehler beim Dekodieren des Sicherheitstokens", e);
			return null;
		}

		int paddingLength = decoded[0];
		int idLength = decoded[paddingLength + 1];
		System.arraycopy(decoded, paddingLength + idLength + 2, hashFromToken, 0, hashFromToken.length);
		return new String(decoded, paddingLength + 2, idLength, Charset.forName("UTF8"));
	}

	private boolean isTokenValid(String userId, Token token, byte[] hashFromToken) {
		if (token == null)
			return false;
		byte[] hash = shaHash(userId, token);
		return Arrays.equals(hash, hashFromToken);
	}

	public String rememberMe(String currentToken) {
		User user = common.getCallingUser();
		removeToken(user, currentToken);
		Token token = createAndStoreToken(user, TokenType.login, 60 * 24 * 31); // ein Monat
		return createEncodedToken(user, token);
	}

	public void forgetMe(String currentToken) {
		User user = common.getCallingUser();
		removeToken(user, currentToken);
		plannerDao.saveToken(user);
	}

	private void removeToken(User user, String encodedToken) {
		if (encodedToken == null)
			return;
		List<Token> storedTokens = getStoredLoginTokens(user);
		byte[] hashFromToken = new byte[64]; // digest length sha-512
		String userId = extractHashFromToken(encodedToken, hashFromToken);
		for (Token token : storedTokens) {
			if (isTokenValid(userId, token, hashFromToken)) {
				user.getTokens().remove(token);
				if (LOG.isDebugEnabled())
					LOG.debug("Removed authentication token: " + token);
				break;
			}
		}
	}

	public String sendRegister(String email, String resetUrl) {
		User user = masterData.getUserByUserId(email, false);
		if (user != null) {
			// Ein Benutzer, der sich bereits angemeldet hatte, kann sich nicht
			// noch einmal registrieren
			if (user.getPassword() != null)
				return getFormattedMessage("user.exists", email);

			Token emailToken = getStoredEmailToken(user);
			if (emailToken != null) {
				if (emailToken.getTokenExpires() - System.currentTimeMillis() > 0)
					return getFormattedMessage("email.register.alreadysent", CommonMessages.niceTimeString(
							(int) ((emailToken.getTokenExpires() - System.currentTimeMillis()) / 1000 / 60)));
			}
		} else {
			user = new User();
			user.setUserId(email);
			user.setEmail(email);
			user.setFirstName("");
			user.setLastName("");
			Suchkriterien krit = new Suchkriterien();
			krit.addFilter(Role_.role.getName(), "Sportwart");
			krit.setExact(true);
			krit.setIgnoreCase(false);
			List<Role> liste = plannerDao.search(Role.class, krit, null).getListe();
			// falls die Rolle nicht existiert, kann der Benutzer nichts machen
			if (liste.size() > 0) {
				Role role = liste.get(0);
				user.getRoles().add(role);
			}
			user.setClub(null);
			// Der Club muss nach der Erstanmeldung im Profil gesetzt werden
			// für den Sportwart gilt, dass er sich
			// auch seinen Verein anlegen muss
			plannerDao.save(user, email);
		}

		Token emailToken = createAndStoreToken(user, TokenType.email, 120);
		String encodedToken = createEncodedToken(user, emailToken);

		String subject = messages.getMessage("email.register.subject");
		String emailText = getFormattedMessage("email.register.html",
				DateFormat.getDateTimeInstance().format(emailToken.getTokenExpires()), resetUrl + "?" + encodedToken);

		sendEmail(user, subject, emailText);
		return getFormattedMessage("email.register.sent", email);
	}

	public String sendPasswortReset(String userId, String resetUrl) {
		User user = masterData.getUserByUserId(userId, false);
		return user != null ? internalSendPasswordReset(user, resetUrl) : messages.getMessage("user.unknown");
	}

	public String sendPasswortReset(Long id, String resetUrl) {
		if (id == null)
			return null;
		User user = plannerDao.getById(User.class, id);
		if (user == null)
			return null;
		return internalSendPasswordReset(user, resetUrl);
	}

	private String internalSendPasswordReset(User user, String resetUrl) {
		Token emailToken = getStoredEmailToken(user);
		if (emailToken != null) {
			// es existiert bereits ein noch gültiges Sicherheitstoken.
			if (emailToken.getTokenExpires() - System.currentTimeMillis() > 0)
				return getFormattedMessage("email.passwordreset.alreadysent", CommonMessages.niceTimeString(
						(int) ((emailToken.getTokenExpires() - System.currentTimeMillis()) / 1000 / 60)));
		}
		emailToken = createAndStoreToken(user, TokenType.email, 120);
		String encodedToken = createEncodedToken(user, emailToken);

		String emailText = getFormattedMessage("email.passwordreset.html", user.getFirstName(), user.getLastName(),
				DateFormat.getDateTimeInstance().format(emailToken.getTokenExpires()), resetUrl + "?" + encodedToken);

		String subject = messages.getMessage("email.passwordreset.subject");
		sendEmail(user, subject, emailText);
		return getFormattedMessage("email.passwordreset.sent", user.getEmail());
	}

	private Token createAndStoreToken(User user, TokenType type, int expiryMinutes) {
		Token token = new Token();
		token.setType(type);
		token.setValue(UUID.randomUUID().toString());
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, expiryMinutes);
		token.setTokenExpires(cal.getTime().getTime());
		user.getTokens().add(token);
		plannerDao.saveToken(user);
		if (LOG.isDebugEnabled())
			LOG.debug("Saved authentication token: " + token);
		return token;
	}

	private String createEncodedToken(User user, Token token) {
		String encodedToken = null;
		try {
			/*
			 * Das Token hat folgende Zusammensetzung: - Länge der Random-Padding-Section (1 byte) - Random-Padding
			 * Section - Länge der User-ID in Bytes (1 byte) - UserId (bis hierhin war es nur obfuscation) - SHA-Hash
			 * (64 byte)
			 */
			byte[] id = user.getUserId().getBytes(Charset.forName("UTF8"));
			// hänge die User-ID an das Token, um eine Identifikation zu
			// erleichtern
			// man könnte sonst auch alle gespeicherten Tokens durchprobieren
			byte[] hash = shaHash(user.getUserId(), token);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int paddingLength = (int) (50 * Math.random());
			for (int i = 0; i < paddingLength; i++) {
				out.write((byte) (256 * Math.random()));
			}
			byte[] padding = out.toByteArray();
			byte[] tokenBytes = new byte[1 + padding.length + 1 + id.length + hash.length];
			tokenBytes[0] = (byte) paddingLength;
			System.arraycopy(padding, 0, tokenBytes, 1, padding.length);
			tokenBytes[padding.length + 1] = (byte) id.length;
			System.arraycopy(id, 0, tokenBytes, padding.length + 2, id.length);
			System.arraycopy(hash, 0, tokenBytes, padding.length + 2 + id.length, hash.length);
			encodedToken = new String(Base64.getEncoder().encode(tokenBytes), "UTF8");
			encodedToken = URLEncoder.encode(encodedToken, "UTF8");
		} catch (Exception e) {
			LogUtil.handleException(e, LOG, "Fehler beim Erzeugen des Sicherheitstokens", user);
		}
		return encodedToken;
	}

	private String getFormattedMessage(String key, Object... arguments) {
		if (key.endsWith(".html")) {
			InputStream in = getClass().getClassLoader().getResourceAsStream("/" + key);
			if (in == null)
				throw new MissingResourceException("Can't find resource for key " + key, null, key);
			StringBuilder sb = new StringBuilder();
			try {
				BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				char buf[] = new char[500];
				for (int c; (c = r.read(buf)) != -1;) {
					sb.append(new String(buf, 0, c));
				}
			} catch (IOException e) {
				LOG.error("Cannot read html ressource " + key, e);
			}
			return MessageFormat.format(sb.toString(), arguments);
		} else {
			return messages.getFormattedMessage(key, arguments);
		}
	}

	private void sendEmail(User recipient, String subject, String msg) throws TechnischeException, FachlicheException {
		if (recipient.getEmail() == null)
			throw new FachlicheException(messages.getResourceBundle(), "no.email.stored", recipient.getUserId());
		try {
			MimeMessage message = new MimeMessage(session);
			message.setSubject(subject);
			MimeMultipart multipart = new MimeMultipart();
			MimeBodyPart bodyPart = new MimeBodyPart();
			bodyPart.setText(msg, "UTF-8", "html");
			multipart.addBodyPart(bodyPart);
			message.setContent(multipart);
			if (Boolean.getBoolean("devmode"))
				message.setFrom(new InternetAddress("uwe_ewald@yahoo.com"));
			else
				message.setFrom(new InternetAddress("noreply@planner.com", "Noreply Planner"));
			if (StringUtils.isNotBlank(recipient.getFirstName()) || StringUtils.isNotBlank(recipient.getLastName())) {
				message.addRecipient(RecipientType.TO, new InternetAddress(recipient.getEmail(),
						recipient.getFirstName() + " " + recipient.getLastName()));
			} else {
				message.addRecipient(RecipientType.TO, new InternetAddress(recipient.getEmail()));
			}
			Transport.send(message);
			if (LOG.isInfoEnabled())
				LOG.info("E-Mail mit Subject: " + subject + " an " + recipient.getEmail() + " versandt.");
		} catch (SendFailedException e) {
			throw new FachlicheException(messages.getResourceBundle(), "email.send.error", recipient.getEmail());
		} catch (Exception e) {
			LogUtil.handleException(e, LOG, "Fehler beim E-Mail-Versand", recipient);
		}
	}

	private Token getStoredEmailToken(User user) {
		Set<Token> tokens = user.getTokens();
		if (tokens.isEmpty())
			return null;
		for (Token t : tokens) {
			if (t.getType() == TokenType.email)
				return t;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<Token> getStoredLoginTokens(User user) {
		Set<Token> tokens = user.getTokens();
		if (tokens.isEmpty())
			return Collections.EMPTY_LIST;
		List<Token> result = new ArrayList<>();
		for (Token t : tokens) {
			if (t.getType() == TokenType.login)
				result.add(t);
		}
		return result;
	}

	private byte[] shaHash(String userId, Token token) {
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-512");
			// erzeuge eine Hashwert über ein Random-Token, den Anmeldenamen
			// sowie den Ablaufzeitpunkt
			return sha.digest((token.getValue() + userId + token.getTokenExpires()).getBytes("UTF8"));
		} catch (Exception e) {
			throw new TechnischeException("Fehler beim Erstellen des SHA-Digest", e);
		}
	}
}
