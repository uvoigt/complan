package org.planner.util;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class BaseTestWithEm {

	protected EntityManager em;

	@BeforeClass
	public static void create() {
		EmTestUtil.startup();
	}

	@AfterClass
	public static void close() {
		EmTestUtil.shutdown();
	}

	@Before
	public void init() {
		em = EmTestUtil.createEntityManager();
	}

	@After
	public void finish() {
		if (em.isOpen())
			em.close();
	}

}
