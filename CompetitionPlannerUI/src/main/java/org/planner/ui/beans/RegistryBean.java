package org.planner.ui.beans;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.planner.eo.User;
import org.planner.remote.ServiceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@RequestScoped
public class RegistryBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(RegistryBean.class);

	@Inject
	private ServiceFacade service;

	public void resetPassword(HttpServletRequest request, String password) {
		String token = getToken(request);
		if (token == null)
			return;
		try {
			String logonName = service.resetPassword(token, password);
			request.logout();
			request.login(logonName, password);
		} catch (Exception e) {
			LOG.error("Fehler beim Passwort-Reset", e);
		}
	}

	public User authenticate(HttpServletRequest request) {
		String token = getToken(request);
		if (token == null)
			return null;
		return service.authenticate(token);
	}

	private String getToken(HttpServletRequest request) {
		return request.getParameter("t");
	}

	public String sendRegister(String email, HttpServletRequest request) {
		return service.sendRegister(email, getResetUrl(request));
	}

	public String sendPasswortReset(String logonName, HttpServletRequest request) {
		return service.sendPasswortReset(logonName, getResetUrl(request));
	}

	public void sendPasswortReset(Long userId) {
		if (userId == null)
			return;
		HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
				.getRequest();
		String success = service.sendPasswortReset(userId, getResetUrl(request));
		if (success != null)
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(null, success));
	}

	private String getResetUrl(HttpServletRequest request) {
		return request.getRequestURL().toString().replace(request.getServletPath(), "/passwordreset");
	}
}
