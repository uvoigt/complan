package org.planner.eo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Gender;
import org.planner.model.ResultExtra;
import org.planner.util.BaseTestWithEm;

import junit.framework.Assert;

public class EntityTest extends BaseTestWithEm {

	@Test
	public void queryUserAlreadyRegisteredForRace() {
		Club announcementClub = getClub("AnnouncementClub");
		Club registrationClub = getClub("RegistrationClub");
		Announcement announcement = getAnnouncement("test-announcement", announcementClub);
		Race race = addRace(announcement, AgeType.lk, BoatClass.k1, Gender.m, 500);
		Registration registration = getRegistration(announcement, registrationClub);
		User user = getUser("Starter1", registrationClub);
		addToRegistration(user, registration, race);
		List<Long> list = em
				.createQuery("select r.id from RegEntry r join r.participants p where p.user.id = :userId", Long.class)
				.setParameter("userId", user.getId()).getResultList();
		Assert.assertEquals(1, list.size());
	}

	private User getUser(String userId, Club club) {
		List<User> list = em.createQuery("select u from User u where u.userId = :userId", User.class)
				.setParameter("userId", userId).getResultList();
		if (list.size() == 1)
			return list.get(0);
		User user = new User();
		user.setUserId(userId);
		user.setFirstName(userId);
		user.setLastName(userId);
		user.setClub(club);
		em.getTransaction().begin();
		em.persist(user);
		em.getTransaction().commit();
		return user;
	}

	private Club getClub(String name) {
		List<Club> list = em.createQuery("select c from Club c where c.name = :name", Club.class)
				.setParameter("name", name).getResultList();
		if (list.size() == 1)
			return list.get(0);
		Club c = new Club();
		c.setName(name);
		em.getTransaction().begin();
		em.persist(c);
		em.getTransaction().commit();
		return c;
	}

	private Announcement getAnnouncement(String name, Club club) {
		List<Announcement> list = em
				.createQuery("select a from Announcement a where a.name = :name", Announcement.class)
				.setParameter("name", name).getResultList();
		if (list.size() == 1)
			return list.get(0);
		Announcement announcement = new Announcement();
		announcement.setName(name);
		announcement.setClub(club);
		announcement.setStartDate(new Date());
		announcement.setEndDate(new Date());
		announcement.setStatus(AnnouncementStatus.created);
		Location location = new Location();
		location.setClub(club);
		location.setAddress(null);
		announcement.setLocation(location);
		Category category = new Category();
		category.setName("Meisterschaft");
		announcement.setCategory(category);
		em.getTransaction().begin();
		em.persist(category);
		em.persist(location);
		em.persist(announcement);
		em.getTransaction().commit();
		return announcement;
	}

	private Race addRace(Announcement announcement, AgeType ageType, BoatClass boatClass, Gender gender, int distance) {
		em.refresh(announcement);
		Race race = new Race();
		race.setAnnouncement(announcement);
		race.setAgeType(ageType);
		race.setBoatClass(boatClass);
		race.setGender(gender);
		race.setDistance(distance);
		race.setNumber(announcement.getRaces().size() + 1);
		announcement.getRaces().add(race);
		em.getTransaction().begin();
		em.persist(race);
		em.merge(announcement);
		em.getTransaction().commit();
		return race;
	}

	private Registration getRegistration(Announcement announcement, Club club) {
		List<Registration> list = em
				.createQuery("select r from Registration r where r.announcement = :announcement", Registration.class)
				.setParameter("announcement", announcement).getResultList();
		if (list.size() == 1)
			return list.get(0);
		Registration registration = new Registration();
		registration.setAnnouncement(announcement);
		registration.setClub(club);
		registration.setStatus(RegistrationStatus.created);
		em.getTransaction().begin();
		em.persist(registration);
		em.getTransaction().commit();
		return registration;
	}

	private void addToRegistration(User user, Registration registration, Race race) {
		em.refresh(registration);
		RegEntry entry = new RegEntry();
		entry.setRace(race);
		if (entry.getParticipants() == null)
			entry.setParticipants(new ArrayList<Participant>());
		Participant participant = new Participant();
		participant.setUser(user);
		entry.getParticipants().add(participant);
		registration.getEntries().add(entry);
		em.getTransaction().begin();
		em.merge(registration);
		em.getTransaction().commit();
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
	public void createResultAndPlacement() {
		Club club = getClub("for program");
		Announcement announcement = getAnnouncement("for program", club);
		addRace(announcement, AgeType.akA, BoatClass.c1, Gender.m, 100);
		Long programId = getProgram(announcement, club, getUser("mem1", club), getUser("mem2", club));
		em.clear();
		Program program = em.find(Program.class, programId);

		// Placements
		List<ProgramRaceTeam> participants = program.getRaces().get(0).getParticipants();
		Placement p1 = new Placement(participants.get(0), 1123L, null);
		Placement p2 = new Placement(participants.get(1), null, ResultExtra.dnf);
		program.getRaces().get(0).setPlacements(new ArrayList<>(Arrays.asList(p1, p2)));
		em.getTransaction().begin();
		em.merge(program.getRaces().get(0));
		em.getTransaction().commit();
		em.clear();
		program = em.find(Program.class, programId);
		participants = program.getRaces().get(0).getParticipants();
		List<Placement> placements = program.getRaces().get(0).getPlacements();
		Assert.assertEquals(2, participants.size());
		Assert.assertEquals(2, placements.size());
		Assert.assertEquals(1123L, placements.get(0).getTime().longValue());
		Assert.assertEquals(ResultExtra.dnf, placements.get(1).getExtra());
		p1 = new Placement(participants.get(0), 22L, null);
		p2 = new Placement(participants.get(1), null, ResultExtra.dq);
		program.getRaces().get(0).setPlacements(new ArrayList<>(Arrays.asList(p1, p2)));
		em.getTransaction().begin();
		em.merge(program.getRaces().get(0));
		em.getTransaction().commit();
		em.clear();
		program = em.find(Program.class, programId);
		participants = program.getRaces().get(0).getParticipants();
		placements = program.getRaces().get(0).getPlacements();
		Assert.assertEquals(2, participants.size());
		Assert.assertEquals(2, placements.size());
	}

	private Long getProgram(Announcement announcement, Club club, User user1, User user2) {
		Program program = new Program();
		program.setAnnouncement(announcement);
		program.setOptions(new ProgramOptions());
		Race race = announcement.getRaces().iterator().next();
		List<ProgramRace> races = new ArrayList<>();
		ProgramRace programRace = new ProgramRace();
		Team team1 = new Team();
		team1.setClub(club);
		TeamMember member1 = new TeamMember();
		member1.setUser(user1);
		team1.addMember(member1);
		programRace.setRace(race);
		programRace.addParticipant(new ProgramRaceTeam(programRace, team1));
		Team team2 = new Team();
		team2.setClub(club);
		TeamMember member2 = new TeamMember();
		member2.setUser(user2);
		team2.addMember(member2);
		programRace.addParticipant(new ProgramRaceTeam(programRace, team2));
		races.add(programRace);
		program.setRaces(races);
		em.getTransaction().begin();
		em.persist(program);
		em.getTransaction().commit();
		return program.getId();
	}
}
