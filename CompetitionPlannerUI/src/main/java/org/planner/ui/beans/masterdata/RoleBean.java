package org.planner.ui.beans.masterdata;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.planner.eo.Role;
import org.planner.eo.Role_;
import org.planner.eo.User;
import org.planner.eo.User_;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.util.JsfUtil;
import org.primefaces.model.DualListModel;

@Named
@RequestScoped
public class RoleBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Role role;

	private DualListModel<Role> model;

	@Override
	@PostConstruct
	public void init() {
		super.init();

		Long id = getIdFromRequestParameters();
		if (id == null)
			id = (Long) JsfUtil.getViewVariable("id");
		if (id != null && !isCancelPressed()) {
			role = service.getObject(Role.class, id, 1);
			JsfUtil.setViewVariable("id", role.getId());
		} else {
			role = new Role();
			role.setId(0L);
		}

		if (!isCancelPressed()) {
			model = new DualListModel<>(service.getAllRoles(), null);
			initModel();
		}
	}

	@Override
	public void setItem(Object item) {
		this.role = (Role) item;
		initModel();
	}

	public boolean canDelete(@SuppressWarnings("unused") Map<String, String> item) {
		return true;
	}

	private void initModel() {
		List<Role> target = role.getRoles();
		List<Role> source = model.getSource();
		for (Iterator<Role> it = source.iterator(); it.hasNext();) {
			Role sourceRole = it.next();
			// die eigene Rolle wird generell entfernt
			if (sourceRole.getRole().equals(role.getRole()))
				it.remove();
			else {
				for (Role targetRole : target) {
					if (sourceRole.getRole().equals(targetRole.getRole())) {
						it.remove();
						break;
					}
				}
			}
		}
		model.setTarget(target);
	}

	public Role getRole() {
		return role;
	}

	public DualListModel<Role> getModel() {
		return model;
	}

	public void setModel(DualListModel<Role> model) {
		this.model = model;
	}

	public List<User> getUsers() {
		Suchkriterien criteria = new Suchkriterien();
		criteria.addFilter(User_.roles.getName() + "." + Role_.role.getName(), role.getRole());
		criteria.setExact(true);
		criteria.setIgnoreCase(false);
		criteria.addSortierung(User_.lastName.getName(), true);
		criteria.addSortierung(User_.firstName.getName(), true);
		Suchergebnis<User> result = service.search(User.class, criteria);
		return result.getListe();
	}

	@Override
	protected void doSave() {
		role.getRoles().clear();
		role.getRoles().addAll(model.getTarget());
		service.saveRole(role);
	}
}
