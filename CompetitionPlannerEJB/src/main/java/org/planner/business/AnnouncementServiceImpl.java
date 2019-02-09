package org.planner.business;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;

import org.joda.time.LocalDate;
import org.planner.business.CommonImpl.Operation;
import org.planner.dao.IOperation;
import org.planner.dao.PlannerDao;
import org.planner.dao.QueryModifier;
import org.planner.ejb.CallerProvider;
import org.planner.eo.Announcement;
import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Category;
import org.planner.eo.Club_;
import org.planner.eo.Location;
import org.planner.eo.Participant;
import org.planner.eo.Placement;
import org.planner.eo.Race;
import org.planner.eo.Race_;
import org.planner.eo.RegEntry;
import org.planner.eo.RegEntry_;
import org.planner.eo.Registration;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Registration_;
import org.planner.eo.Role_;
import org.planner.eo.User;
import org.planner.eo.User_;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Gender;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.model.Suchkriterien.Filter;
import org.planner.model.Suchkriterien.Filter.Comparison;
import org.planner.model.Suchkriterien.Property;
import org.planner.model.Suchkriterien.SortField;
import org.planner.util.CurrentTime;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.Messages;

@Named
public class AnnouncementServiceImpl {

	private class AthletesFilterer extends QueryModifier {
		private final Suchkriterien criteria;
		private int[] ages;
		boolean sortFieldModified;

		private AthletesFilterer(Suchkriterien criteria) {
			this.criteria = criteria;
		}

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Predicate createPredicate(Root root, CriteriaBuilder builder) {
			SetJoin roles = root.join(User_.roles);
			Predicate isSportler = builder.equal(roles.get(Role_.role), nextParam(builder, "Sportler"));
			Predicate isMasterSportler = builder.equal(roles.get(Role_.role), nextParam(builder, "Mastersportler"));
			Predicate result = builder.or(isSportler, isMasterSportler);

			if (ages == null)
				extractAgeFilter();
			if (ages != null) {
				int year = LocalDate.now().getYear();
				LocalDate fromD = new LocalDate(year - ages[1], 1, 1);
				LocalDate toD = new LocalDate(year - ages[0], 12, 31);
				result = builder.and(result,
						builder.between(root.get(User_.birthDate),
								(Expression<Date>) nextParam(builder, fromD.toDate()),
								(Expression<Date>) nextParam(builder, toD.toDate())));
			}
			List<SortField> sortierung = criteria.getSortierung();
			if (!sortFieldModified && sortierung != null) {
				for (Iterator<SortField> it = sortierung.iterator(); it.hasNext();) {
					SortField sortField = it.next();
					if ("birthDate".equals(sortField.getSortierFeld())) {
						sortField.setAsc(!sortField.isAsc());
						sortFieldModified = true;
					}
				}
			}
			return result;
		}

		private void extractAgeFilter() {
			Map<String, Filter> filter = criteria.getFilter();
			Filter ageTypeFilter = filter != null ? filter.remove("ageType") : null;
			if (ageTypeFilter == null)
				return;
			List<AgeType> matchingAgeTypes = new ArrayList<>();
			String filterValue = ((String) ageTypeFilter.getValue()).toLowerCase();
			for (AgeType ageType : AgeType.values()) {
				if (ageType.getText().toLowerCase().contains(filterValue))
					matchingAgeTypes.add(ageType);
			}
			ages = AgeType.getAgesForAgeType(matchingAgeTypes);
		}
	}

	@Inject
	private CommonImpl common;

	@Inject
	private CallerProvider caller;

	@Inject
	private PlannerDao dao;

	@Inject
	private Messages messages;

	@Inject
	private CurrentTime time;

	public Announcement saveAnnouncement(Announcement announcement, boolean copy) {

		Announcement existing = null;
		if (announcement.getId() != null)
			existing = dao.getById(Announcement.class, announcement.getId());

		common.checkWriteAccess(existing != null ? existing : announcement, Operation.save);

		if (existing == null || copy) {
			announcement.setStatus(AnnouncementStatus.created);
			announcement.setClub(common.getCallingUser().getClub());
		}

		Category category = announcement.getCategory();
		common.handleEnum(category);
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

		// location ist cascade=ALL
		Location location = announcement.getLocation();
		if (location.getClub() != null)
			location.setAddress(null);

		// Location openingLocation = announcement.getOpeningLocation();
		// if (openingLocation.getClub() != null)
		// openingLocation.setAddress(null);
		// if (openingLocation.getId() == null)
		// common.save(openingLocation);

		if (copy) {
			announcement.setId(null);
			announcement.getLocation().setId(null);
		}

		announcement = common.save(announcement);
		if (copy) {
			String loginName = caller.getLoginName();
			Set<Race> races = announcement.getRaces();
			if (races != null) {
				for (Race race : races) {
					race.setId(null);
					dao.save(race, loginName);
				}
			}
		}
		return announcement;
	}

	public List<Race> getRaces(Long announcementId) {
		Suchkriterien krit = new Suchkriterien();
		krit.addFilter(Race_.announcement.getName(), announcementId);
		return dao.search(Race.class, krit, null).getListe();
	}

	public void createRaces(Long announcementId, AgeType[] selectedAgeTypes, BoatClass[] selectedBoatClasses,
			Gender[] selectedGenders, int[] selectedDistances, Integer dayOffset) {
		// erstelle für jede Kombination ein Race
		Announcement announcement = dao.getById(Announcement.class, announcementId);
		common.checkWriteAccess(announcement, Operation.save);

		List<Race> newRaces = new ArrayList<>();
		for (AgeType ageType : selectedAgeTypes) {
			for (BoatClass boatClass : selectedBoatClasses) {
				for (Gender gender : selectedGenders) {
					if ((boatClass == BoatClass.c1 || boatClass == BoatClass.k1) && gender == Gender.mixed)
						continue;
					for (int distance : selectedDistances) {
						Race race = new Race();
						race.setAgeType(ageType);
						race.setBoatClass(boatClass);
						race.setDistance(distance);
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
				boolean exists = newRace.getAgeType() == race.getAgeType();
				exists &= newRace.getBoatClass() == race.getBoatClass();
				exists &= newRace.getDistance() == race.getDistance();
				exists &= newRace.getGender() == race.getGender();
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
		// Prüfung auf eindeutige Renn-Nr.
		Suchkriterien criteria = new Suchkriterien(true);
		criteria.addFilter(Race_.announcementId.getName(), race.getAnnouncementId());
		criteria.addFilter(Race_.number.getName(), race.getNumber());
		criteria.addFilter(new Filter(Comparison.ne, Race_.id.getName(), race.getId()));
		if (common.search(Race.class, criteria).getGesamtgroesse() > 0)
			throw new FachlicheException(messages.getResourceBundle(), "announcement.raceNoExists", race.getNumber());

		common.save(race);
	}

	public Long createRegistration(Registration registration) {

		Registration existing = null;
		if (registration.getId() != null)
			existing = dao.getById(Registration.class, registration.getId());
		else {
			// gibt es bereits eine Meldung für diese Ausschreibung, dann nimm
			// diese!
			Suchkriterien krit = new Suchkriterien();
			krit.addFilter(Registration_.announcement.getName(), registration.getAnnouncement().getId());
			User callingUser = common.getCallingUser();
			krit.addFilter(Registration_.club.getName(), callingUser.getClub().getId());
			// TODO das Meldedatum darf nicht überschritten sein!
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

	public void setAnnouncementStatus(Long announcementId, AnnouncementStatus status) {
		Announcement announcement = dao.getById(Announcement.class, announcementId);
		common.checkWriteAccess(announcement, Operation.save);
		if (!caller.isInRole("Tester") && status != AnnouncementStatus.announced)
			throw new FachlicheException(messages.getResourceBundle(), "accessSetStatus");
		announcement.setStatus(status);
		common.save(announcement);
	}

	public Suchergebnis<User> getAthletes(final Suchkriterien criteria) {
		criteria.setProperties(
				// group by user.id, um keine doppelten Einträge zu haben, wenn Sportler und Mastersportler
				Arrays.asList(new Property(User_.club.getName() + "." + Club_.name.getName(), User_.id.getName()),
						new Property(User_.club.getName() + "." + Club_.shortName.getName()),
						new Property(User_.firstName.getName()), new Property(User_.lastName.getName()),
						new Property(User_.birthDate.getName()), new Property(User_.gender.getName())));
		return common.internalSearch(User.class, criteria, new AthletesFilterer(criteria));
	}

	public void saveRegEntries(Long registrationId, List<RegEntry> entries) {
		Registration registration = dao.getById(Registration.class, registrationId);
		common.checkWriteAccess(registration, Operation.save);

		// es gibt zwei Usecases:

		RegEntry firstEntry = entries.size() > 0 ? entries.get(0) : null;
		if (firstEntry != null && firstEntry.getId() != null) {
			// 1. es wird zu einem existierenden Entry hinzugefügt

			List<Participant> newParticipants = firstEntry.getParticipants();
			RegEntry existingEntry = dao.getById(RegEntry.class, firstEntry.getId());
			List<Participant> existingParticipants = existingEntry.getParticipants();
			int maxPos = determineMaxPos(existingParticipants);
			boolean added = false;
			boolean isRequest = false;
			OuterLoop: for (Participant newParticipant : newParticipants) {
				// Sonderfall: es wird ein Teilnehmer gesucht
				if (newParticipant.getRemark() != null) {
					isRequest = true;
				} else {
					// Hibernate kümmert sich zwar ohnehin um eine unique Liste
					// aber die Position würde immer weiter hochgezählt werden
					for (Participant existingParticipant : existingParticipants) {
						User existingUser = existingParticipant.getUser();
						User newUser = newParticipant.getUser();
						if (existingUser != null && newUser != null && existingUser.getId().equals(newUser.getId()))
							continue OuterLoop;
					}
				}
				newParticipant.setPos(++maxPos);
				existingParticipants.add(newParticipant);
				added = true;
			}
			if (added) {
				checkMaximalTeamSize(firstEntry.getRace().getBoatClass(), existingParticipants.size());
				if (!isRequest) {
					checkGender(firstEntry.getRace(), existingParticipants);
					checkIfAlreadyRegistered(firstEntry);
					checkRaceAgeTypeAgainstParticipant(firstEntry);
				}

				common.save(existingEntry);
			}
		} else {
			// 2. es werden neue Entries angelegt

			for (RegEntry newEntry : entries) {

				checkMaximalTeamSize(newEntry.getRace().getBoatClass(), newEntry.getParticipants().size());
				checkGender(newEntry.getRace(), newEntry.getParticipants());

				// außerdem prüfen, ob die Besatzung bzw. ein Teil daraus! bereits in diesem Rennen gemeldet wurde
				checkIfAlreadyRegistered(newEntry);
				checkRaceAgeTypeAgainstParticipant(newEntry);

				common.save(newEntry);
				registration.getEntries().add(newEntry);
			}

			common.save(registration);
		}
	}

	public void deleteFromRegEntry(Long registrationId, RegEntry entry) {
		Registration registration = dao.getById(Registration.class, registrationId);
		common.checkWriteAccess(registration, Operation.delete);

		List<Participant> participants = entry.getParticipants();
		int maxPos = determineMaxPos(participants);
		for (Iterator<Participant> it = participants.iterator(); it.hasNext();) {
			if (it.next().getPos() == maxPos) {
				it.remove();
				break;
			}
		}
		if (participants.isEmpty())
			common.delete(RegEntry.class, entry.getId());
		else
			common.save(entry);
		// damit update timestamp und user geschrieben werden
		common.save(registration);
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
		int maximalTeamSize = boatClass.getMaximalTeamSize();
		int allowedSubstitutes = boatClass.getAllowedSubstitutes();
		if (numberOfParticipants > maximalTeamSize + allowedSubstitutes)
			throw new FachlicheException(messages.getResourceBundle(), "registration.maximalTimesizeExceeded",
					maximalTeamSize, allowedSubstitutes, boatClass.getText());
	}

	private void checkGender(Race race, Collection<Participant> participants) {
		if (race.getGender() == Gender.mixed) {
			if (participants.size() >= race.getBoatClass().getMinimalTeamSize()) {
				int females = 0;
				int males = 0;
				for (Participant participant : participants) {
					if (participant.getUser().getGender() == Gender.f)
						females++;
					else if (participant.getUser().getGender() == Gender.m)
						males++;
				}
				int half = race.getBoatClass().getMinimalTeamSize() / 2;
				if (females < half || males < half)
					throw new FachlicheException(messages.getResourceBundle(), "registration.illegalMixed");
			}
		} else {
			for (Participant participant : participants) {
				User user = participant.getUser();
				if (user != null && user.getGender() != race.getGender()) {
					throw new FachlicheException(messages.getResourceBundle(), "registration.illegalGender",
							race.getGender().getAgeFriendlyText(race.getAgeType()),
							user.getGender().getAgeFriendlyText(user.getAgeType()));
				}
			}
		}
	}

	private void checkIfAlreadyRegistered(RegEntry entry) {
		for (Participant participant : entry.getParticipants()) {
			Suchkriterien criteria = new Suchkriterien(true);
			criteria.addFilter(RegEntry_.race.getName(), entry.getRace().getId());
			criteria.addFilter(RegEntry_.participants.getName() + ".user", participant.getUser().getId());
			if (entry.getId() != null)
				criteria.addFilter(new Filter(Comparison.ne, RegEntry_.id.getName(), entry.getId()));
			if (common.search(RegEntry.class, criteria).getGesamtgroesse() > 0)
				throw new FachlicheException(messages.getResourceBundle(), "registration.alreadyRegistered",
						participant.getUser().getFirstName() + " " + participant.getUser().getLastName(),
						entry.getRace().getNumber());
		}
	}

	private void checkRaceAgeTypeAgainstParticipant(RegEntry entry) {
		// hoch- runter melden
		AgeType raceAgeType = entry.getRace().getAgeType();
		for (Participant participant : entry.getParticipants()) {
			if (participant.getRemark() != null)
				continue;
			AgeType userAgeType = participant.getUser().getAgeType();
			switch (userAgeType) {
			case schuelerA:
			case schuelerB:
			case schuelerC:
			case jugend:
			case junioren:
			case lk:
				// diese Altersklassen dürfen nicht gegen Jüngere fahren
				if (raceAgeType.compareTo(userAgeType) < 0 ||
				// Spezialfall: die Jüngeren dürfen, LK-Fahrer aber nicht: gegen Ältere fahren
						userAgeType == AgeType.lk && raceAgeType.compareTo(userAgeType) > 0)
					throw new FachlicheException(messages.getResourceBundle(), "registration.illegalAgeType",
							participant.getUser().getName(), entry.getRace().getNumber());
				break;
			case akA:
			case akB:
			case akC:
			case akD:
				// TODO diese Prüfungen anpassen!
				// AK-Fahrer dürfen generell nicht gegen Ältere fahren
				if (raceAgeType.compareTo(userAgeType) > 0 /* && raceAgeType != AgeType.ak */ ||
				// aber gegen Jüngere nur nicht unterhalb LK
						raceAgeType.compareTo(userAgeType) < 0 && raceAgeType.compareTo(AgeType.lk) < 0)
					throw new FachlicheException(messages.getResourceBundle(), "registration.illegalAgeType",
							participant.getUser().getName(), entry.getRace().getNumber());
				break;
			}
			switch (raceAgeType) {
			default:
				break;
			case akA:
			case akB:
			case akC:
			case akD:
				// Spezialfall AK, da sind alle AKs zusammengefasst
				// Der Fahrer muss lediglich in der AK sein
				if (userAgeType.compareTo(AgeType.akA) < 0)
					throw new FachlicheException(messages.getResourceBundle(), "registration.illegalAgeType",
							participant.getUser().getName(), entry.getRace().getNumber());
				break;
			}
		}
	}

	public void setRegistrationStatus(Long registrationId, RegistrationStatus status) {
		Registration registration = dao.getById(Registration.class, registrationId);
		common.checkWriteAccess(registration, Operation.save);

		if (!caller.isInRole("Tester") && status != RegistrationStatus.submitted)
			throw new FachlicheException(messages.getResourceBundle(), "accessSetStatus");

		if (status == RegistrationStatus.submitted) {
			List<RegEntry> entries = registration.getEntries();
			if (entries.isEmpty())
				throw new FachlicheException(messages.getResourceBundle(), "registration.empty");

			Set<Integer> erroredRaces = new TreeSet<>();
			for (RegEntry entry : entries) {
				int minimalTeamSize = entry.getRace().getBoatClass().getMinimalTeamSize();
				if (entry.getParticipants().size() < minimalTeamSize)
					erroredRaces.add(entry.getRace().getNumber());
				checkRaceAgeTypeAgainstParticipant(entry);
			}
			if (!erroredRaces.isEmpty())
				throw new FachlicheException(messages.getResourceBundle(), "registration.incompleteEntries",
						erroredRaces);
		}
		registration.setStatus(status);
		common.save(registration);
	}

	public List<RegEntry> getMyUpcomingRegistrations() {
		if (!caller.isInRole("Sportler") && !caller.isInRole("Mastersportler"))
			return new ArrayList<>();
		return dao.executeOperation(new IOperation<List<RegEntry>>() {
			@Override
			public List<RegEntry> execute(EntityManager em) {
				return em.createNamedQuery("upcomingRegistrations", RegEntry.class).setParameter("today", time.now())
						.setParameter("userId", caller.getLoginName()).getResultList();
			}
		});
	}

	public List<Placement> getMyLatestResults(final int months) {
		if (!caller.isInRole("Sportler") && !caller.isInRole("Mastersportler"))
			return new ArrayList<>();
		return dao.executeOperation(new IOperation<List<Placement>>() {
			@Override
			public List<Placement> execute(EntityManager em) {
				Calendar date = Calendar.getInstance();
				date.add(Calendar.MONTH, -months);
				List<Tuple> tuples = em.createNamedQuery("latestResults", Tuple.class)
						.setParameter("date", date.getTime()).setParameter("userId", caller.getLoginName())
						.getResultList();
				List<Placement> result = new ArrayList<>();
				for (Tuple tuple : tuples) {
					result.add((Placement) tuple.get(0));
				}
				return result;
			}
		});
	}
}
