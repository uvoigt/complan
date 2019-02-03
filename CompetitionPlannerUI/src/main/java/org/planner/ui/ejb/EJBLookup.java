package org.planner.ui.ejb;

import javax.ejb.EJB;
import javax.enterprise.inject.Produces;

import org.planner.remote.ServiceFacade;

public class EJBLookup {

	@EJB
	@Produces
	private ServiceFacade service;
}
