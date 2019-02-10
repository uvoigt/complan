package org.planner.ejb;

import javax.ejb.EJBContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class CallerProvider {

	@Inject
	private EJBContext context;

	public String getLoginName() {
		return context.getCallerPrincipal().getName();
	}

	public boolean isInRole(String role) {
		return context.isCallerInRole(role);
	}
}
