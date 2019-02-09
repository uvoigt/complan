package org.planner.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class EmTestUtil {

	private static EntityManagerFactory emf;

	public static void startup() {
		try {
			emf = Persistence.createEntityManagerFactory("PLANNER_PU");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void shutdown() {
		if (emf != null)
			emf.close();
	}

	public static EntityManager createEntityManager() {
		return emf.createEntityManager();
	}
}
