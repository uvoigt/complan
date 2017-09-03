package org.planner.ejb;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.inject.Named;

@Named
public class CallerProvider {

	@Resource
	private EJBContext context;

	public String getLoginName() {
		return context.getCallerPrincipal().getName();
	}

	public boolean isInRole(String role) {
		return context.isCallerInRole(role);
	}
}
