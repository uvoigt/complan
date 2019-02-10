package org.planner.business;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.planner.ejb.Init;

public class TestEntityManagerFactory {

	private static EntityManagerFactory emf;

	public TestEntityManagerFactory() {
		if (emf == null) {
			try {
				emf = Persistence.createEntityManagerFactory("TEST_PU");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void shutdown() {
		if (emf != null)
			emf.close();
		emf = null;
	}

	@Produces
	@ApplicationScoped
	public EntityManager getEntityManager() {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Init init = new Init();
		init.createResultView(em);
		em.getTransaction().commit();
		return em;
	}
}
