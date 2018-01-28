package org.planner.ui.beans.masterdata;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.planner.eo.Role;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.util.JsfUtil;

@Named
@RequestScoped
public class RoleBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Role role;

	@Override
	@PostConstruct
	public void init() {
		super.init();

		Long id = getIdFromRequestParameters();
		if (id == null)
			id = (Long) JsfUtil.getViewVariable("id");
		if (id != null && !isCancelPressed()) {
			role = service.getObject(Role.class, id, 0);
			JsfUtil.setViewVariable("id", role.getId());
		} else {
			role = new Role();
		}
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
}
