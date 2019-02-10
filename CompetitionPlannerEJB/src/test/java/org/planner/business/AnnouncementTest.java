package org.planner.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.planner.business.TestUtils.random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.spi.DirStateFactory.Result;

import org.jboss.arquillian.junit.InSequence;
import org.joda.time.DateTime;
import org.junit.Test;
import org.planner.business.CommonImpl.Operation;
import org.planner.eo.Announcement;
import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Category;
import org.planner.eo.Location;
import org.planner.eo.Participant;
import org.planner.eo.Placement;
import org.planner.eo.Race;
import org.planner.eo.RegEntry;
import org.planner.eo.Registration;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Gender;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.util.CommonMessages;
import org.planner.util.LogUtil.FachlicheException;

public class AnnouncementTest extends BaseTest {

	@Inject
	private AnnouncementServiceImpl announcementService;

	private static Long announcementId;

	private static Long registrationId;

	@Test(expected = IllegalStateException.class)
	public void failCheckWriteAccessNoSuchMethod() {
		common.checkWriteAccess(Result.class, Operation.save);
	}

	@Test
	public void getMyLatestResultsNoSportler() {
		setTestRoles("Sportwart");
		List<Placement> results = announcementService.getMyLatestResults(12);
		assertEquals(0, results.size());
	}

	@Test
	public void getMyLatestResults() {
		setTestRoles("Mastersportler", "Sportler");
		announcementService.getMyLatestResults(12);
	}

	@Test
	public void getMyUpcomingRegistrationsNoSportler() {
		List<RegEntry> registrations = announcementService.getMyUpcomingRegistrations();
		assertEquals(0, registrations.size());
	}

	@Test
	public void getMyUpcomingRegistrations() {
		setTestRoles("Mastersportler", "Sportler");
		announcementService.getMyUpcomingRegistrations();
	}

	@Test(expected = FachlicheException.class)
	public void failCreateAnnouncementNotAllowed() {
		User callingUser = getCallingUser();
		Announcement announcement = new Announcement();
		announcement.setName("test");
		Category category = new Category();
		category.setName("test");
		announcement.setCategory(category);
		announcement.setStartDate(new Date());
		announcement.setEndDate(new Date());
		announcement.setClub(callingUser.getClub());
		announcement.getLocation().setClub(callingUser.getClub());
		setTestRoles("Sportler");
		announcementService.saveAnnouncement(announcement, false);
	}

	@Test
	@InSequence(100)
	public void saveAnnouncement() {
		User callingUser = getCallingUser();
		Announcement announcement = new Announcement();
		announcement.setName("test");
		Category category = new Category();
		category.setName("test");
		announcement.setCategory(category);
		announcement.setStartDate(new Date());
		announcement.setEndDate(new Date());
		announcement.setClub(callingUser.getClub());
		announcement.getLocation().setClub(callingUser.getClub());
		setTestRoles("Sportwart");
		announcementService.saveAnnouncement(announcement, false);
		announcementId = announcement.getId();
	}

	@Test
	@InSequence(110)
	public void saveRaces() {
		setTestRoles("Sportwart");
		announcementService.createRaces(announcementId, new AgeType[] { AgeType.schuelerC },
				new BoatClass[] { BoatClass.k2 }, new Gender[] { Gender.mixed }, new int[] { 200, 500 }, 0);
		flushAndClear();
		List<Race> races = announcementService.getRaces(announcementId);
		assertEquals(2, races.size());
		assertEquals(Integer.valueOf(1), races.get(0).getNumber());
		assertEquals(Integer.valueOf(2), races.get(1).getNumber());
	}

	@Test
	@InSequence(120)
	public void saveMoreRaces() {
		setTestRoles("Sportwart");
		announcementService.createRaces(announcementId, new AgeType[] { AgeType.lk },
				new BoatClass[] { BoatClass.k1, BoatClass.c1 }, new Gender[] { Gender.m }, new int[] { 200, 500 }, 0);
		flushAndClear();
		List<Race> races = announcementService.getRaces(announcementId);
		assertEquals(6, races.size());
		assertEquals(Integer.valueOf(1), races.get(0).getNumber());
		assertEquals(Integer.valueOf(2), races.get(1).getNumber());
		assertEquals(Integer.valueOf(3), races.get(2).getNumber());
		assertEquals(Integer.valueOf(4), races.get(3).getNumber());
		assertEquals(Integer.valueOf(5), races.get(4).getNumber());
		assertEquals(Integer.valueOf(6), races.get(5).getNumber());
	}

	@Test
	@InSequence(130)
	public void saveMoreRacesAndExisting() {
		setTestRoles("Sportwart");
		announcementService.createRaces(announcementId, new AgeType[] { AgeType.lk }, new BoatClass[] { BoatClass.k1 },
				new Gender[] { Gender.m }, new int[] { 500, 1000 }, 0);
		List<Race> races = announcementService.getRaces(announcementId);
		assertEquals(7, races.size());
	}

	@Test
	@InSequence(140)
	public void saveRace() {
		Announcement announcement = getEm().find(Announcement.class, announcementId);
		Race race = getRace(announcement.getRaces(), 2);
		race.setDay(2);
		setTestRoles("Sportwart");
		announcementService.saveRace(race);
		flushAndClear();
		List<Race> races = announcementService.getRaces(announcementId);
		race = getRace(races, 2);
		assertEquals(Integer.valueOf(2), race.getDay());
	}

	@Test
	@InSequence(150)
	public void deleteRaces() {
		Announcement announcement = getEm().find(Announcement.class, announcementId);
		List<Long> toDelete = new ArrayList<>();
		for (Race race : announcement.getRaces()) {
			if (race.getBoatClass() == BoatClass.k1)
				toDelete.add(race.getId());
		}

		flushAndClear();
		setTestRoles("Sportwart");
		announcementService.deleteRaces(announcementId, toDelete);
		List<Race> races = announcementService.getRaces(announcementId);
		assertEquals(4, races.size());
	}

	@Test
	@InSequence(160)
	public void getRaces() {
		List<Race> races = announcementService.getRaces(announcementId);
		assertEquals(4, races.size());
	}

	@Test
	@InSequence(170)
	public void saveAnnouncementAsCopy() {
		Announcement announcement = getEm().find(Announcement.class, announcementId);
		// fetch
		announcement.getLocation().getClub().getId();
		announcement.getCategory().getName();
		// for (Race race : announcement.getRaces()) {
		// race.getId();
		// }
		// //
		flushAndClear();
		Announcement copy = new Announcement();
		copy.setId(announcementId);
		copy.setName("test-Kopie");
		copy.setStartDate(new Date());
		copy.setEndDate(new Date());
		copy.setCategory(new Category());
		copy.getCategory().setName(announcement.getCategory().getName());
		copy.setLocation(new Location());
		copy.getLocation().setClub(announcement.getLocation().getClub());
		setTestRoles("Sportwart");
		announcementService.saveAnnouncement(copy, true);
		getEm().flush();
		Suchergebnis<Announcement> result = common.search(Announcement.class, new Suchkriterien(true));
		assertEquals(2, result.getGesamtgroesse());
	}

	@Test
	@InSequence(180)
	public void createRegistration() {
		Registration registration = new Registration();
		registration.setAnnouncement(getEm().find(Announcement.class, announcementId));
		registration.setClub(registration.getAnnouncement().getClub());
		setTestRoles("Trainer");
		registrationId = announcementService.createRegistration(registration);
	}

	@Test
	@InSequence(180)
	public void failSetAnnouncementStatusWrongStatus() {
		thrown.expect(FachlicheException.class);
		thrown.expectMessage(messages.getMessage("accessSetStatus"));
		setTestRoles("Sportwart");
		announcementService.setAnnouncementStatus(announcementId, AnnouncementStatus.created);
	}

	@Test
	@InSequence(190) // TODO test wrong status
	public void setAnnouncementStatus() {
		setTestRoles("Sportwart");
		announcementService.setAnnouncementStatus(announcementId, AnnouncementStatus.announced);
	}

	@Test
	public void getAthletes() {
		DateTime date = DateTime.parse("2018-07-06");
		User user = TestUtils.getUserByUserId(getEm(), random(32), Gender.m, date.toDate(), random(0), "Sportler");
		TestUtils.getUserByUserId(getEm(), random(32), Gender.m, new Date(), random(0), "Sportler");
		TestUtils.getUserByUserId(getEm(), random(32), Gender.m, new Date(), random(0), "Sportler");
		Suchkriterien criteria = new Suchkriterien();
		criteria.addFilter("userId", user.getUserId());
		Suchergebnis<User> result = announcementService.getAthletes(criteria);
		assertNotNull(result);
		assertEquals(1, result.getGesamtgroesse());
		assertEquals(1, result.getListe().size());
		@SuppressWarnings("rawtypes")
		Map map = (Map) result.getListe().get(0);
		assertEquals(user.getBirthDate(), map.get("birthDate"));
	}

	@Test
	@InSequence(200)
	public void saveRegEntries() {
		Announcement announcement = getEm().find(Announcement.class, announcementId);
		Race race = getRace(announcement.getRaces(), 2); // k2 500 mixed
		RegEntry entry = new RegEntry();
		entry.setRace(race);
		List<Participant> participants = new ArrayList<>();
		Participant p1 = new Participant();
		p1.setUser(TestUtils.getUserByUserId(getEm(), random(32), Gender.m, new Date(), "newClub"));
		participants.add(p1);
		Participant p2 = new Participant();
		p2.setUser(TestUtils.getUserByUserId(getEm(), random(32), Gender.f, new Date(), "newClub"));
		participants.add(p2);
		entry.setParticipants(participants);

		flushAndClear();
		setTestRoles("Trainer");
		announcementService.saveRegEntries(registrationId, Arrays.asList(entry));
	}

	@Test
	@InSequence(205)
	public void saveRegEntriesOnExisting() {
		Registration registration = getEm().find(Registration.class, registrationId);
		RegEntry entry = registration.getEntries().get(0);
		entry.getRace().getBoatClass(); // fetch
		flushAndClear();

		List<Participant> participants = new ArrayList<>();
		Participant p1 = new Participant();
		p1.setUser(TestUtils.getUserByUserId(getEm(), random(32), "newClub"));
		participants.add(p1);
		Participant p2 = new Participant();
		p2.setUser(TestUtils.getUserByUserId(getEm(), random(32), "newClub"));
		participants.add(p2);
		entry.setParticipants(participants);

		setTestRoles("Trainer");
		announcementService.saveRegEntries(registrationId, Arrays.asList(entry));
	}

	@Test
	@InSequence(206)
	public void failSaveRegEntriesTooManyParticipants() {
		thrown.expect(FachlicheException.class);
		thrown.expectMessage(messages.getFormattedMessage("registration.maximalTimesizeExceeded", 1, 1, "C1"));

		Announcement announcement = getEm().find(Announcement.class, announcementId);
		Race race = getRace(announcement.getRaces(), 5); // c1
		RegEntry entry = new RegEntry();
		entry.setRace(race);
		List<Participant> participants = new ArrayList<>();
		Participant p1 = new Participant();
		p1.setUser(TestUtils.getUserByUserId(getEm(), random(32), "newClub"));
		participants.add(p1);
		Participant p2 = new Participant();
		p2.setUser(TestUtils.getUserByUserId(getEm(), random(32), "newClub"));
		participants.add(p2);
		Participant p3 = new Participant();
		p3.setUser(TestUtils.getUserByUserId(getEm(), random(32), "newClub"));
		participants.add(p3);
		entry.setParticipants(participants);

		flushAndClear();
		setTestRoles("Trainer");
		announcementService.saveRegEntries(registrationId, Arrays.asList(entry));
	}

	@Test
	@InSequence(207)
	public void failSaveRegEntriesGenderMismatch() {
		Announcement announcement = getEm().find(Announcement.class, announcementId);
		Race race = getRace(announcement.getRaces(), 5); // c1

		RegEntry entry = new RegEntry();
		entry.setRace(race);
		List<Participant> participants = new ArrayList<>();
		Participant p = new Participant();
		p.setUser(TestUtils.getUserByUserId(getEm(), random(32), Gender.f, new Date(), "newClub"));
		participants.add(p);
		entry.setParticipants(participants);

		thrown.expect(FachlicheException.class);
		thrown.expectMessage(messages.getFormattedMessage("registration.illegalGender",
				race.getGender().getAgeFriendlyText(race.getAgeType()),
				p.getUser().getGender().getAgeFriendlyText(p.getUser().getAgeType())));

		flushAndClear();
		setTestRoles("Trainer");
		announcementService.saveRegEntries(registrationId, Arrays.asList(entry));
	}

	@Test
	@InSequence(208)
	public void failSaveRegEntriesWrongMixed() {
		thrown.expect(FachlicheException.class);
		thrown.expectMessage(messages.getFormattedMessage("registration.illegalMixed"));

		Announcement announcement = getEm().find(Announcement.class, announcementId);
		Race race = getRace(announcement.getRaces(), 2); // k2 mixed

		RegEntry entry = new RegEntry();
		entry.setRace(race);
		List<Participant> participants = new ArrayList<>();
		Participant p1 = new Participant();
		p1.setUser(TestUtils.getUserByUserId(getEm(), random(32), Gender.f, new Date(), "newClub"));
		participants.add(p1);
		Participant p2 = new Participant();
		p2.setUser(TestUtils.getUserByUserId(getEm(), random(32), Gender.f, new Date(), "newClub"));
		participants.add(p2);
		entry.setParticipants(participants);

		flushAndClear();
		setTestRoles("Trainer");
		announcementService.saveRegEntries(registrationId, Arrays.asList(entry));
	}

	private Race getRace(Collection<Race> races, int raceNumber) {
		for (Race race : races) {
			if (race.getNumber() == raceNumber)
				return race;
		}
		return null;
	}

	@Test
	@InSequence(210)
	public void setRegistrationStatus() {
		setTestRoles("Trainer");
		announcementService.setRegistrationStatus(registrationId, RegistrationStatus.submitted);
	}

	@Test
	@InSequence(220)
	public void resetRegistrationStatus() {
		setTestRoles("Tester", "Trainer");
		announcementService.setRegistrationStatus(registrationId, RegistrationStatus.created);
	}

	@Test
	@InSequence(230)
	public void delete3FromRegEntry() {
		Registration registration = getEm().find(Registration.class, registrationId);
		RegEntry entry = registration.getEntries().get(0);
		entry.getParticipants().size();

		flushAndClear();
		setTestRoles("Trainer");
		announcementService.deleteFromRegEntry(registrationId, entry);
		announcementService.deleteFromRegEntry(registrationId, entry);
		announcementService.deleteFromRegEntry(registrationId, entry);
	}

	@Test
	@InSequence(240)
	public void failSetRegistrationStatusIncorrectRegistration() {
		Registration registration = getEm().find(Registration.class, registrationId);
		Integer raceNumber = registration.getEntries().iterator().next().getRace().getNumber();
		thrown.expect(FachlicheException.class);
		thrown.expectMessage(CommonMessages.formatMessage(messages.getMessage("registration.incompleteEntries"),
				Arrays.asList(raceNumber)));

		flushAndClear();
		setTestRoles("Trainer");
		announcementService.setRegistrationStatus(registrationId, RegistrationStatus.submitted);
	}

	@Test
	@InSequence(250)
	public void deleteLastRegEntry() {
		Registration registration = getEm().find(Registration.class, registrationId);
		Iterator<RegEntry> it = registration.getEntries().iterator();
		RegEntry entry = it.next();
		entry.getParticipants().size(); // fetch

		flushAndClear();
		setTestRoles("Trainer");
		announcementService.deleteFromRegEntry(registrationId, entry);
	}

	@Test
	@InSequence(260)
	public void failSetRegistrationStatusEmptyRegistration() {
		thrown.expect(FachlicheException.class);
		thrown.expectMessage(messages.getMessage("registration.empty"));
		setTestRoles("Trainer");
		announcementService.setRegistrationStatus(registrationId, RegistrationStatus.submitted);
	}
}
