package org.planner.ejb;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.enterprise.inject.Produces;

public class ResourceFactory {

	@Produces
	@Resource
	private EJBContext context;
}