package org.planner.ui.beans.masterdata;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.planner.eo.Role;
import org.planner.ui.beans.AbstractEditBean;

@Named
@RequestScoped
public class RoleBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Role role;

	@PostConstruct
	public void init() {
		Long id = getIdFromRequestParameters();
		if (id != null)
			role = service.getObject(Role.class, id, 0);
		else
			role = new Role();
	}

	@Override
	public void setItem(Object item) {
		this.role = (Role) item;
	}

	public Role getRole() {
		return role;
	}

	@Override
	protected void doSave() {
		service.saveRole(role);
	}

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
