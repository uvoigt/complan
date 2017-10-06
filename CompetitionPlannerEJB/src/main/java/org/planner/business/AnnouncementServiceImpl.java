package org.planner.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.planner.business.CommonImpl.Operation;
import org.planner.dao.PlannerDao;
import org.planner.ejb.CallerProvider;
import org.planner.eo.Announcement;
import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Announcement_;
import org.planner.eo.Category;
import org.planner.eo.Location;
import org.planner.eo.Participant;
import org.planner.eo.Race;
import org.planner.eo.Race_;
import org.planner.eo.RegEntry;
import org.planner.eo.RegEntry_;
import org.planner.eo.Registration;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Registration_;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Gender;
import org.planner.model.Suchkriterien;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.Messages;

@Named
public class AnnouncementServiceImpl {

	@Inject
	private CommonImpl common;

	@Inject
	private CallerProvider caller;

	@Inject
	private PlannerDao dao;

	@Inject
	private Messages messages;

	public Announcement saveAnnouncement(Announcement announcement) {

		Announcement existing = null;
		if (announcement.getId() != null)
			existing = common.getById(Announcement.class, announcement.getId(), 0);

		common.checkWriteAccess(existing != null ? existing : announcement, Operation.save);

		if (existing != null) {
			existing.setName(announcement.getName());
			existing.setCategory(announcement.getCategory());
			existing.setText(announcement.getText());
			// TODO existing.getLocation().;
			announcement = existing;
		} else {
			announcement.setStatus(AnnouncementStatus.created);
			announcement.setClub(common.getCallingUser().getClub());
		}
		Category category = announcement.getCategory();
		if (category.getId() == null)
			common.save(category);
		// Location announcer = announcement.getAnnouncer();
		// if (announcer.getClub() != null)
		// announcer.setAddress(null);
		// if (announcer.getId() == null)
		// common.save(announcer);
		// Location juryLocation = announcement.getJuryLocation();
		// if (juryLocation.getClub() != null)
		// juryLocation.setAddress(null);
		// if (juryLocation.getId() == null)
		// common.save(juryLocation);
		Location location = announcement.getLocation();
		if (location.getClub() != null)
			location.setAddress(null);
		if (location.getId() == null)
			common.save(location);
		// Location openingLocation = announcement.getOpeningLocation();
		// if (openingLocation.getClub() != null)
		// openingLocation.setAddress(null);
		// if (openingLocation.getId() == null)
		// common.save(openingLocation);

		return common.save(announcement);
	}

	public List<Race> getRaces(Long announcementId) {
		Suchkriterien krit = new Suchkriterien();
		krit.addFilter(Race_.announcement.getName(), announcementId);
		return dao.search(Race.class, krit, null).getListe();
	}

	public void createRaces(Long announcementId, String[] selectedAgeTypes, String[] selectedBoatClasses,
			String[] selectedGenders, String[] selectedDistances, Integer dayOffset) {
		// erstelle für jede Kombination ein Race
		Announcement announcement = dao.getById(Announcement.class, announcementId);
		common.checkWriteAccess(announcement, Operation.save);

		List<Race> newRaces = new ArrayList<>();
		for (String ageTypeString : selectedAgeTypes) {
			AgeType ageType = AgeType.valueOf(ageTypeString);
			for (String boatClassString : selectedBoatClasses) {
				BoatClass boatClass = BoatClass.valueOf(boatClassString);
				for (String genderString : selectedGenders) {
					Gender gender = Gender.valueOf(genderString);
					if ((boatClass == BoatClass.c1 || boatClass == BoatClass.k1) && gender == Gender.mixed)
						continue;
					for (String distance : selectedDistances) {
						Race race = new Race();
						// nicht sonderlich effizient, aber vertretbar
						// race.setAgeType(common.getEnumByName(ageType,
						// AgeType.class));
						// race.setBoatClass(common.getEnumByName(boatClass,
						// BoatClass.class));
						race.setAgeType(ageType);
						race.setBoatClass(boatClass);
						race.setDistance(Integer.valueOf(distance));
						race.setGender(gender);
						race.setAnnouncement(announcement);
						race.setDay(dayOffset);
						newRaces.add(race);
					}
				}
			}
		}
		// da es sich um meist nicht mehr als 200 Rennen handelt, ist das OK
		// dauert ca. 0,5 Sekunden
		int highestRaceNumber = 0;
		for (Iterator<Race> it = newRaces.iterator(); it.hasNext();) {
			Race newRace = it.next();
			for (Race race : announcement.getRaces()) {
				boolean exists = newRace.getAgeType().name().equals(race.getAgeType().name());
				exists &= newRace.getBoatClass().name().equals(race.getBoatClass().name());
				exists &= newRace.getDistance() == race.getDistance();
				exists &= newRace.getGender().equals(race.getGender());
				if (exists)
					it.remove();
				if (race.getNumber() > highestRaceNumber)
					highestRaceNumber = race.getNumber();
			}
		}
		String loginName = caller.getLoginName();
		for (Race race : newRaces) {
			race.setNumber(++highestRaceNumber);
			dao.save(race, loginName);
		}
	}

	public void deleteRaces(Long announcementId, List<Long> raceIds) {
		Announcement announcement = dao.getById(Announcement.class, announcementId);
		common.checkWriteAccess(announcement, Operation.save);
		dao.delete(Race.class, raceIds);
	}

	public void saveRace(Race race) {
		common.checkWriteAccess(race, Operation.save);
		common.save(race);
	}

	public List<Announcement> getOpenAnnouncements() {
		Suchkriterien krit = new Suchkriterien();
		krit.addFilter(Announcement_.status.getName(), AnnouncementStatus.announced);
		return dao.search(Announcement.class, krit, null).getListe();
	}

	public Long createRegistration(Registration registration) {

		Registration existing = null;
		if (registration.getId() != null)
			existing = common.getById(Registration.class, registration.getId(), 0);
		else {
			// gibt es bereits eine Meldung für diese Ausschreibung, dann nimm
			// diese!
			Suchkriterien krit = new Suchkriterien();
			krit.addFilter(Registration_.announcement.getName(), registration.getAnnouncement().getId());
			List<Registration> registrations = common.search(Registration.class, krit).getListe();
			// theoretisch können mehrere existieren, wenn zwei User wirklich
			// gleichzeitig speichern
			if (registrations.size() > 0)
				existing = registrations.get(0);
		}

		common.checkWriteAccess(existing != null ? existing : registration, Operation.save);

		if (existing != null) {
			return existing.getId();
		} else {
			if (registration.getId() != null)
				throw new FachlicheException(messages.getResourceBundle(), "registration.exists");
			else {
				User user = common.getCallingUser();
				registration.setClub(user.getClub());
				registration.setStatus(RegistrationStatus.created);
			}
			common.save(registration);
			return registration.getId();
		}
	}

	public void announce(Long announcementId) {
		Announcement announcement = common.getById(Announcement.class, announcementId, 0);
		common.checkWriteAccess(announcement, Operation.save);
		announcement.setStatus(AnnouncementStatus.announced);
		common.save(announcement);
	}

	public void saveRegEntries(Long registrationId, List<RegEntry> entries) {
		Registration registration = common.getById(Registration.class, registrationId, 0);
		common.checkWriteAccess(registration, Operation.save);

		// es gibt zwei Usecases:

		RegEntry firstEntry = entries.size() > 0 ? entries.get(0) : null;
		if (firstEntry != null && firstEntry.getId() != null) {
			// 1. es wird zu einem existierenden Entry hinzugefügt

			List<Participant> newParticipants = firstEntry.getParticipants();
			RegEntry existingEntry = common.getById(RegEntry.class, firstEntry.getId(), 0);
			List<Participant> existingParticipants = existingEntry.getParticipants();
			int maxPos = determineMaxPos(existingParticipants);
			boolean added = false;
			OuterLoop: for (Participant newParticipant : newParticipants) {
				// Hibernate kümmert sich zwar ohnehin um eine unique Liste
				// aber um die position würde immer weiter hochgezählt werden
				for (Participant existingParticipant : existingParticipants) {
					if (existingParticipant.getUser().getId().equals(newParticipant.getUser().getId()))
						continue OuterLoop;
				}
				newParticipant.setPos(++maxPos);
				existingParticipants.add(newParticipant);
				added = true;
			}
			if (added) {
				checkMaximalTeamSize(firstEntry.getRace().getBoatClass(), existingParticipants.size());
				checkGender(firstEntry.getRace().getGender(), existingParticipants);

				common.save(existingEntry);
			}
		} else {
			// 2. es werden neue Entries angelegt

			for (RegEntry newEntry : entries) {

				checkMaximalTeamSize(newEntry.getRace().getBoatClass(), newEntry.getParticipants().size());
				checkGender(newEntry.getRace().getGender(), newEntry.getParticipants());

				// außerdem prüfen, ob die Besatzung bzw. ein Teil daraus! bereits in diesem Rennen gemeldet wurde
				for (Participant participant : newEntry.getParticipants()) {
					Suchkriterien criteria = new Suchkriterien();
					criteria.addFilter(RegEntry_.race.getName(), newEntry.getRace().getId());
					criteria.addFilter(RegEntry_.participants.getName() + ".user", participant.getUser().getId());
					if (common.search(RegEntry.class, criteria).getGesamtgroesse() > 0)
						throw new FachlicheException(messages.getResourceBundle(), "registration.alreadyRegistered",
								participant.getUser().getFirstName() + " " + participant.getUser().getLastName(),
								newEntry.getRace().getNumber());
				}

				common.save(newEntry);
				registration.getEntries().add(newEntry);
			}

			common.save(registration);
		}
	}

	public void deleteFromRegEntry(Long registrationId, RegEntry entry) {
		Registration registration = common.getById(Registration.class, registrationId, 0);
		common.checkWriteAccess(registration, Operation.delete);

		List<Participant> participants = entry.getParticipants();
		int maxPos = determineMaxPos(participants);
		for (Iterator<Participant> it = participants.iterator(); it.hasNext();) {
			if (it.next().getPos() == maxPos) {
				it.remove();
				break;
			}
		}
		if (participants.isEmpty()) {
			common.delete(RegEntry.class, entry.getId());
		} else {
			common.save(entry);
		}
	}

	private int determineMaxPos(List<Participant> participants) {
		int maxPos = 0;
		for (Participant participant : participants) {
			if (participant.getPos() > maxPos)
				maxPos = participant.getPos();
		}
		return maxPos;
	}

	private void checkMaximalTeamSize(BoatClass boatClass, int numberOfParticipants) {
		int[] maximalTeamSize = getMaximalTeamSize(boatClass);
		if (numberOfParticipants > maximalTeamSize[0] + maximalTeamSize[1])
			throw new FachlicheException(messages.getResourceBundle(), "registration.maximalTimesizeExceeded",
					maximalTeamSize[0], maximalTeamSize[1], boatClass.getText());
	}

	private void checkGender(Gender gender, Collection<Participant> participants) {
		for (Participant user : participants) {
			if (user.getUser().getGender() != gender)
				throw new FachlicheException(messages.getResourceBundle(), "registration.illegalGender", gender,
						user.getUser().getGender());
		}
	}

	// inklusive Ersatz
	private int[] getMaximalTeamSize(BoatClass boatClass) {
		switch (boatClass) {
		case c1:
		case k1:
			return new int[] { 1, 0 };
		case c2:
		case k2:
			return new int[] { 2, 1 };
		case c4:
		case k4:
			return new int[] { 4, 1 };
		case c8:
			return new int[] { 8, 2 };
		default:
			throw new IllegalArgumentException(boatClass.name());
		}
	}

	public void submitRegistration(Long registrationId) {
		Registration registration = common.getById(Registration.class, registrationId, 0);
		common.checkWriteAccess(registration, Operation.save);
		registration.setStatus(RegistrationStatus.submitted);
		common.save(registration);
	}
}
