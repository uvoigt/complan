package org.planner.business;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.planner.dao.PlannerDao;
import org.planner.ejb.CallerProvider;
import org.planner.eo.Announcement;
import org.planner.eo.Category;
import org.planner.eo.Location;
import org.planner.eo.Race;
import org.planner.eo.Race_;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Gender;
import org.planner.model.Suchkriterien;

@Named
public class AnnouncementServiceImpl {

	@Inject
	private CommonImpl common;

	@Inject
	private CallerProvider caller;

	@Inject
	private PlannerDao dao;

	// @Inject
	// private Messages messages;

	public Announcement saveAnnouncement(Announcement announcement) {

		Category category = announcement.getCategory();
		if (category.getId() == null)
			common.save(category);
		Location announcer = announcement.getAnnouncer();
		if (announcer.getClub() != null)
			announcer.setAddress(null);
		if (announcer.getId() == null)
			common.save(announcer);
		Location juryLocation = announcement.getJuryLocation();
		if (juryLocation.getClub() != null)
			juryLocation.setAddress(null);
		if (juryLocation.getId() == null)
			common.save(juryLocation);
		Location location = announcement.getLocation();
		if (location.getClub() != null)
			location.setAddress(null);
		if (location.getId() == null)
			common.save(location);
		Location openingLocation = announcement.getOpeningLocation();
		if (openingLocation.getClub() != null)
			openingLocation.setAddress(null);
		if (openingLocation.getId() == null)
			common.save(openingLocation);

		return dao.save(announcement, caller.getLoginName());
	}

	public List<Race> getRaces(Long announcementId) {
		Suchkriterien krit = new Suchkriterien();
		krit.addFilter(Race_.announcement.getName(), announcementId);
		return dao.findEntities(Race.class, krit).getListe();
	}

	public void createRaces(Long announcementId, String[] selectedAgeTypes, String[] selectedBoatClasses,
			String[] selectedGenders, String[] selectedDistances, Integer dayOffset) {
		// erstelle für jede Kombination ein Race
		Announcement announcement = dao.find(Announcement.class, announcementId);
		List<Race> newRaces = new ArrayList<>();
		for (String ageType : selectedAgeTypes) {
			for (String boatClass : selectedBoatClasses) {
				for (String gender : selectedGenders) {
					for (String distance : selectedDistances) {
						Race race = new Race();
						// nicht sonderlich effizient, aber vertretbar
						// race.setAgeType(common.getEnumByName(ageType,
						// AgeType.class));
						// race.setBoatClass(common.getEnumByName(boatClass,
						// BoatClass.class));
						race.setAgeType(AgeType.valueOf(ageType));
						race.setBoatClass(BoatClass.valueOf(boatClass));
						race.setDistance(Long.valueOf(distance));
						race.setGender(Gender.valueOf(gender));
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

	public void saveRace(Race race) {
		common.checkWriteAccess(race);
		dao.save(race, caller.getLoginName());
	}
}
