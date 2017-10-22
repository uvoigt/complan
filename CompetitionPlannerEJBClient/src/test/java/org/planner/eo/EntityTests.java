package org.planner.eo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EntityTests {

	private static EntityManagerFactory emf;
	private EntityManager em;

	@BeforeClass
	public static void create() {
		Map<String, String> properties = new HashMap<>();
		properties.put("hibernate.connection.username", "sa");
		properties.put("hibernate.connection.password", "sa");
		properties.put("hibernate.connection.url", "jdbc:h2:unittestdb");
		try {
			emf = Persistence.createEntityManagerFactory("PLANNER_TEST_PU", properties);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void close() {
		if (emf != null)
			emf.close();
	}

	@Before
	public void init() {
		em = emf.createEntityManager();
	}

	@After
	public void finish() {
		if (em.isOpen())
			em.close();
	}

	@Test
	public void queryUserAlreadyRegisteredForRace() {
		TypedQuery<Long> typedQuery = em
				.createQuery("select r.id from RegEntry r join r.participants p where p.user.id = :userId", Long.class);
		typedQuery.setParameter("userId", 2445L);
		List<Long> typedList = typedQuery.getResultList();
		System.out.println(typedList);
	}

	@Test
	public void createUser() {
		em.getTransaction().begin();
		User user = new User();
		user.setFirstName("Unit");
		user.setLastName("Test");
		String userId = UUID.randomUUID().toString().replace("-", "");
		if (userId.length() > 32)
			userId = userId.substring(0, 32);
		user.setUserId(userId);
		user.setClub(null);
		em.persist(user);
		em.getTransaction().commit();
	}

	@Test
	public void testRegistration() {
		System.out.println("EntityTests.testRegistration()");
		// em.getTransaction().begin();

		// Announcement announcement = em.find(Announcement.class, 2505L);
		// Club club = em.find(Club.class, 16L);
		// Registration registration = new Registration();
		// registration.setAnnouncement(announcement);
		// registration.setClub(club);
		// registration.setStatus(RegistrationStatus.created);
		// Map<Race, Participants> map = new HashMap<>();
		// Participants participants = new Participants();
		// User u1 = em.find(User.class, 6L);
		// User u2 = em.find(User.class, 1853L);
		// participants.setUsers(new HashSet<>(Arrays.asList(u1, u2)));
		// map.put(em.find(Race.class, 2557L), participants);
		// registration.setParticipants(map);
		// em.persist(participants);
		// em.persist(registration);
		// em.getTransaction().commit();
	}
}
