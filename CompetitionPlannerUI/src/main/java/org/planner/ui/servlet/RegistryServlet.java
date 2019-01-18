package org.planner.ui.servlet;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.mutable.MutableInt;
import org.planner.eo.User;
import org.planner.remote.ServiceFacade;
import org.planner.ui.beans.RegistryBean;
import org.planner.ui.util.ImageCreator;
import org.planner.ui.util.KeepLoggedInAuthenticationMechanism;
import org.planner.util.CommonMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryServlet extends HttpServlet {
	private class Status {
		private long first = System.currentTimeMillis();
		private MutableInt count = new MutableInt();
		private boolean locked;
	}

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(RegistryServlet.class);

	@Inject
	private RegistryBean registry;

	@Inject
	private ServiceFacade service;

	private ImageCreator imageCreator = new ImageCreator();

	private Map<String, Status> requests = new HashMap<>();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if ("/userimg".equals(request.getServletPath())) {
			userimg(response);
			return;
		}
		if (isPotentialAttack(request, response))
			return;
		// Aufruf des E-Mail-Links
		User user = null;
		try {
			user = registry.authenticate(request);
		} catch (Exception e) {
			LOG.error("Cannot authenticate link", e);
		}
		if (user != null) {
			request.getRequestDispatcher("/WEB-INF/jsp/passwordchange.jsp").forward(request, response);
		} else {
			request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (isPotentialAttack(request, response))
			return;
		String servletPath = request.getServletPath();
		if ("/register".equals(servletPath))
			register(request, response);
		else if ("/passwordreset".equals(servletPath))
			passwordreset(request, response);
		else if ("/passwordchange".equals(servletPath))
			passwordchange(request, response);
		else if ("/logout".equals(servletPath))
			logout(request, response);
	}

	private boolean isPotentialAttack(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final int lockMinutes = 15;
		clearOld(lockMinutes);

		// Client-Adresse oder letzter Proxy, egal
		String addr = request.getRemoteAddr();
		Status status = requests.get(addr);
		if (status == null)
			requests.put(addr, status = new Status());
		if (!status.locked) {
			status.count.increment();
			int count = status.count.intValue();
			long frequency = (System.currentTimeMillis() - status.first) / count;
			if (count > 10 && frequency < 10000) // 10 Sekunden
				status.locked = true;
		}
		if (status.locked) {
			String minutesString = CommonMessages.niceTimeString(lockMinutes);
			String message = getMessages(request.getLocale()).getString("locked");
			message = MessageFormat.format(message, minutesString);
			writeResult(response, message);
		}
		return status.locked;
	}

	private ResourceBundle getMessages(Locale locale) {
		return ResourceBundle.getBundle("MessagesBundle", locale);
	}

	private void clearOld(int lockMinutes) {
		final int threshold = 1000 * 60 * lockMinutes;
		long now = System.currentTimeMillis();
		synchronized (requests) {
			for (Iterator<Status> it = requests.values().iterator(); it.hasNext();) {
				Status status = it.next();
				long distance = now - status.first;
				if (distance > threshold)
					it.remove();
			}
		}
	}

	private void register(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String email = request.getParameter("email");
		if (email != null) {
			String success = registry.sendRegister(email, request);
			writeResult(response, success);
		}
	}

	private void passwordreset(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String logonName = request.getParameter("user");
		if (logonName != null) {
			String success = registry.sendPasswortReset(logonName, request);
			writeResult(response, success);
		}
	}

	private void writeResult(HttpServletResponse response, String success) throws IOException {
		response.setCharacterEncoding("UTF-8");
		response.getWriter().print("<xml>" + success + "</xml>");
	}

	private void passwordchange(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String p1 = request.getParameter("p1");
		String p2 = request.getParameter("p2");
		if (p1 != null && p2 != null && p1.equals(p2)) {
			registry.resetPassword(request, p1);
			String url = request.getRequestURL().toString().replace(request.getServletPath(), "");
			response.sendRedirect(url);
		} else {
			request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
		}
	}

	private void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Cookie[] cookies = request.getCookies();
		for (Cookie cookie : cookies) {
			if (KeepLoggedInAuthenticationMechanism.COOKIE_TOKEN.equals(cookie.getName())) {
				service.forgetMe(cookie.getValue());
				if (LOG.isDebugEnabled())
					LOG.debug("Deleting authentication cookie: " + cookie.getValue());
				cookie = new Cookie(KeepLoggedInAuthenticationMechanism.COOKIE_TOKEN, null);
				cookie.setPath(request.getContextPath());
				cookie.setMaxAge(0);
				response.addCookie(cookie);
				break;
			}
		}
		request.logout();
		request.getSession().invalidate();
		writeResult(response, "ok");
	}

	private void userimg(HttpServletResponse response) throws IOException {
		User user = service.getLoggedInUser();
		String firstName = user.getFirstName();
		String lastName = user.getLastName();
		response.setContentType("image/png");
		byte[] image = imageCreator.createCCAbbreviation(firstName, lastName);
		response.getOutputStream().write(image);
	}
}