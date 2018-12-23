package org.planner.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class EmTestUtil {

	private static EntityManagerFactory emf;

	public static void startup() {
		Map<String, String> properties = new HashMap<>();
		properties.put("hibernate.connection.username", "sa");
		properties.put("hibernate.connection.password", "sa");
		properties.put("hibernate.connection.url", "jdbc:h2:unittestdb");
		try {
			emf = Persistence.createEntityManagerFactory("PLANNER_PU", properties);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void shutdown() {
		if (emf != null)
			emf.close();
		deleteDB();
	}

	private static void deleteDB() {
		File dir = new File(".");
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("unittestdb");
			}
		};
		for (File file : dir.listFiles(filter)) {
			file.delete();
		}
	}

	public static EntityManager createEntityManager() {
		return emf.createEntityManager();
	}
}
