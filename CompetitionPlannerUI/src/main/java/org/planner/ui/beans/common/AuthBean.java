package org.planner.ui.beans.common;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;

@Named
@ApplicationScoped
public class AuthBean {

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

	public boolean notInRole(String role) {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		return !ctx.isUserInRole(role);
	}

	public boolean notInRole(String role1, String role2) {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		return !ctx.isUserInRole(role1) && !ctx.isUserInRole(role2);
	}
}
