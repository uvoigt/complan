package org.planner.ui.util.converter;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.Role;
import org.planner.remote.ServiceFacade;
import org.planner.ui.util.JsfUtil;

@Named
@RequestScoped
public class RoleConverter implements Converter<Object> {

	@Inject
	private ServiceFacade service;

	private Map<Long, Role> roles;

	@PostConstruct
	public void init() {
		if (!JsfUtil.isFromSource("btnCancel")) {
			roles = new HashMap<>();
			for (Role role : service.getAllRoles()) {
				roles.put(role.getId(), role);
			}
		}
	}

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		Long id = Long.valueOf(value);
		return roles.get(id);
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		return ((Long) value).toString();
	}
}
