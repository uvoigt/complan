package org.planner.dao;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class EntityManagerFactory {

	@PersistenceContext(unitName = "PLANNER_PU")
	private EntityManager em;

	@Produces
	@PlannerDB
	protected EntityManager createPlannerEM() {
		return this.em;
	}

}
