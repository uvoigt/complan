package org.planner.business;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.LogManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;

import org.planner.eo.AbstractEnum;
import org.planner.eo.Announcement;
import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Category;
import org.planner.eo.Club;
import org.planner.eo.HasId;
import org.planner.eo.Participant;
import org.planner.eo.Race;
import org.planner.eo.RegEntry;
import org.planner.eo.Registration;
import org.planner.eo.Role;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Gender;

public class TestUtils {
	public static interface CreateOp<T> {
		T create();
	}

	public TestUtils() throws Exception {
		// logging
		LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"));
	}

	static public String random(int length) {
		String string = UUID.randomUUID().toString();
		return length > 0 ? string.substring(0, length) : string;
	}

	public static <T extends AbstractEnum> T getEnum(EntityManager em, Class<T> enumType, String name) {
		T theEnum = null;
		try {
			theEnum = em.createQuery("from " + enumType.getSimpleName() + " where name=?1", enumType)
					.setParameter(1, name).getSingleResult();
		} catch (NoResultException e) {
			try {
				theEnum = enumType.newInstance();
			} catch (InstantiationException | IllegalAccessException e1) {
				fail(e);
			}
			em.persist(theEnum);
			em.flush();
		}
		return theEnum;
	}

	public static <T extends HasId> T getEntity(EntityManager em, Class<T> type, String query, String key,
			boolean clearEm, CreateOp<T> op) {
		T t = null;
		try {
			if (query == null)
				throw new NoResultException();
			t = em.createQuery(query, type).setParameter(1, key).getSingleResult();
		} catch (NoResultException e) {
			EntityTransaction transaction = em.getTransaction();
			boolean wasActive = transaction.isActive();
			if (!wasActive)
				transaction.begin();

			t = op.create();
			em.persist(t);
			em.flush();
			if (!wasActive)
				transaction.commit();
		} finally {
			if (clearEm && t != null) {
				em.detach(t);
			}
		}
		return t;
	}

	public static User getUserByUserId(EntityManager em, String userId, String clubName) {
		return getUserByUserId(em, userId, Gender.m, new Date(), clubName);
	}

	public static User getUserByUserId(final EntityManager em, final String userId, final Gender gender,
			final Date birthDate, final String clubName, final String... roles) {
		return getEntity(em, User.class, "from User where userId=?1", userId, true, new CreateOp<User>() {
			@Override
			public User create() {
				User user = new User();
				user.setUserId(userId);
				user.setFirstName(userId + " First");
				user.setLastName(userId + " Last");
				user.setGender(gender); // Gender is nullable - Erstanmeldungen erhalten kein Gender
				user.setBirthDate(birthDate);
				user.setClub(getClub(em, clubName, false));
				for (String role : roles) {
					Role r = getRole(em, role);
					user.getRoles().add(r);
				}
				return user;
			}
		});
	}

	public static Club getClub(EntityManager em, final String name, boolean clearEm) {
		return getEntity(em, Club.class, "from Club where name=?1", name, clearEm, new CreateOp<Club>() {
			@Override
			public Club create() {
				Club club = new Club();
				club.setName(name);
				return club;
			}
		});
	}

	public static Role getRole(EntityManager em, final String name) {
		return getEntity(em, Role.class, "from Role where role=?1", name, true, new CreateOp<Role>() {
			@Override
			public Role create() {
				Role role = new Role();
				role.setRole(name);
				return role;
			}
		});
	}

	public static Race createRace(EntityManager em, final Announcement announcement, final int number,
			final AgeType ageType, final BoatClass boatClass, final Gender gender, final int distance,
			boolean clearEm) {
		return getEntity(em, Race.class, null, null, clearEm, new CreateOp<Race>() {
			@Override
			public Race create() {
				Race race = new Race();
				race.setNumber(number);
				race.setAnnouncement(announcement);
				race.setAgeType(ageType);
				race.setBoatClass(boatClass);
				race.setGender(gender);
				race.setDistance(distance);
				return race;
			}
		});
	}

	public static Announcement createAnnouncement(final EntityManager em, final Club club,
			final AnnouncementStatus status, boolean clearEm) {
		return getEntity(em, Announcement.class, null, null, clearEm, new CreateOp<Announcement>() {
			@Override
			public Announcement create() {
				Announcement announcement = new Announcement();
				announcement.setName(random(0));
				announcement.getLocation().setClub(club);
				announcement.getLocation().setAddress(null);
				Calendar calendar = Calendar.getInstance();
				announcement.setStartDate(calendar.getTime());
				calendar.add(Calendar.DATE, 1);
				announcement.setEndDate(calendar.getTime());
				announcement.setStatus(status);
				Category category = getEnum(em, Category.class, "test");
				announcement.setCategory(category);
				announcement.setClub(club);
				return announcement;
			}
		});
	}

	public static RegEntry createEntry(final EntityManager em, Registration registration, final Race race,
			final int numParticipants, boolean clearEm) {
		RegEntry entry = getEntity(em, RegEntry.class, null, null, clearEm, new CreateOp<RegEntry>() {
			@Override
			public RegEntry create() {
				RegEntry entry = new RegEntry();
				entry.setRace(race);
				List<Participant> participants = new ArrayList<>();
				for (int i = 0; i < numParticipants; i++) {
					Participant participant = new Participant();
					participant.setUser(getUserByUserId(em, random(32), random(0)));
					participants.add(participant);
				}
				entry.setParticipants(participants);
				return entry;
			}
		});
		Registration r = em.find(Registration.class, registration.getId());
		r.getEntries().add(entry);
		em.merge(r);
		em.flush();
		if (clearEm)
			em.clear();
		return entry;
	}
}
