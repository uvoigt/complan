package org.planner.dao;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class EntityManagerFactory {

	@Produces
	@PersistenceContext(unitName = "PLANNER_PU")
	private EntityManager em;
}
