package org.planner.ui.beans.common;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.User;
import org.planner.remote.ServiceFacade;

@Named
@SessionScoped
public class AuthBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private ServiceFacade service;

	private User loggedInUser;

	public boolean inRole(String role) {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		return ctx.isUserInRole(role);
	}

	public boolean inRole(String role1, String role2) {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		return ctx.isUserInRole(role1) || ctx.isUserInRole(role2);
	}

	public boolean inRole(String role1, String role2, String role3) {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		return ctx.isUserInRole(role1) || ctx.isUserInRole(role2) || ctx.isUserInRole(role3);
	}

	public boolean inRoles(String roles) {
		String[] split = roles.split(",");
		for (String s : split) {
			if (inRole(s))
				return true;
		}
		return false;
	}

	public boolean notInRole(String role) {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		return !ctx.isUserInRole(role);
	}

	public boolean notInRole(String role1, String role2) {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		return !ctx.isUserInRole(role1) && !ctx.isUserInRole(role2);
	}

	public User getLoggedInUser() {
		if (loggedInUser == null)
			loggedInUser = service.getLoggedInUser();
		return loggedInUser;
	}

}
