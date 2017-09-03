package org.planner.ui.beans.masterdata;

import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.planner.eo.Role;
import org.planner.ui.beans.AbstractEditBean;

@Named
@SessionScoped
public class RoleBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Role role = new Role();

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

	public List<Role> getRoles() {
		return service.getRoles();
	}

	public boolean inOneOf(String role) {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		return ctx.isUserInRole(role);
	}

	public boolean inOneOf(String role1, String role2) {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		return ctx.isUserInRole(role1) || ctx.isUserInRole(role2);
	}

	public boolean inOneOf(String role1, String role2, String role3) {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		return ctx.isUserInRole(role1) || ctx.isUserInRole(role2) || ctx.isUserInRole(role3);
	}
}
