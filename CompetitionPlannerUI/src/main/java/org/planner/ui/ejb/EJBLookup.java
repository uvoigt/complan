package org.planner.ui.ejb;

import javax.ejb.EJB;
import javax.enterprise.inject.Produces;

import org.planner.remote.ServiceFacade;

public class EJBLookup {

	@EJB(lookup = "java:global/ear-1.0.0-SNAPSHOT/ejb-1.0.0-SNAPSHOT/ServiceFacadeBean")
	@Produces
	private ServiceFacade admin;
}
