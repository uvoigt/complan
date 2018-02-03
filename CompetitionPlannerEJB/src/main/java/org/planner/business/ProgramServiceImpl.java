package org.planner.business;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.joda.time.Days;
import org.joda.time.Instant;
import org.joda.time.MutableDateTime;
import org.planner.business.CommonImpl.Operation;
import org.planner.business.program.Change;
import org.planner.business.program.Checks;
import org.planner.business.program.Problem;
import org.planner.dao.IOperation;
import org.planner.dao.PlannerDao;
import org.planner.eo.Announcement;
import org.planner.eo.Club;
import org.planner.eo.Participant;
import org.planner.eo.Program;
import org.planner.eo.ProgramOptions;
import org.planner.eo.ProgramOptions.Break;
import org.planner.eo.ProgramOptions.DayTimes;
import org.planner.eo.ProgramRace;
import org.planner.eo.ProgramRace.RaceType;
import org.planner.eo.Program_;
import org.planner.eo.Race;
import org.planner.eo.RegEntry;
import org.planner.eo.Registration;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Registration_;
import org.planner.eo.Team;
import org.planner.eo.TeamMember;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Gender;
import org.planner.model.Suchkriterien;
import org.planner.util.ExpressionParser;
import org.planner.util.ExpressionParser.ExpressionException;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.LogUtil.TechnischeException;
import org.planner.util.Messages;
import org.planner.util.ParserMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ProgramServiceImpl {

	private class InitialOrder implements Comparator<Team> {
		@Override
		public int compare(Team t1, Team t2) {
			return (int) (Math.random() * 3) - 2;
		}
	}

	private class EvalContext {
		private Announcement announcement;
		private ProgramOptions options;
		private int day;
		private MutableDateTime time;
		private int nextBreakIndex = -1;

		private EvalContext(Announcement announcement, ProgramOptions options) {
			this.announcement = announcement;
			this.options = options;
		}

		public Date nextTime() {
			List<DayTimes> dayTimes = options.getDayTimes();
			if (time == null) {
				if (dayTimes.size() > day) {
					DayTimes times = dayTimes.get(day);
					time = new MutableDateTime();
					time.setDate(announcement.getStartDate().getTime());
					Calendar c = Calendar.getInstance();
					c.setTime(times.getStart());
					time.setTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), 0, 0);
					if (times.getBreaks().size() > 0)
						nextBreakIndex = 0;
				} else
					throw new IllegalStateException();
			} else {
				time.addMinutes(options.getTimeLag());
				DayTimes times = dayTimes.get(day);
				List<Break> breaks = times.getBreaks();
				Break nextBreak = nextBreakIndex != -1 && breaks.size() > nextBreakIndex ? breaks.get(nextBreakIndex)
						: null;
				if (nextBreak != null && getTimeOfDay(time) >= getTimeOfDay(nextBreak.getTime())) {
					time.addMinutes(nextBreak.getDuration());
					nextBreakIndex++;
				} else if (getTimeOfDay(time) >= getTimeOfDay(times.getEnd())) {
					day++;
					Days days = Days.daysBetween(new Instant(announcement.getStartDate()),
							new Instant(announcement.getEndDate()));
					if (day > days.getDays()) {
						throw new FachlicheException(messages.getResourceBundle(), "program.daysExceeded",
								time.toDate(), announcement.getEndDate());
					}
					times = dayTimes.get(day);
					Calendar c = Calendar.getInstance();
					c.setTime(times.getStart());
					time.setTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), 0, 0);
					time.addDays(1);
					nextBreakIndex = 0;
				}
			}
			return time.toDate();
		}

		private int getTimeOfDay(Date d) {
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			return getTimeOfDay(c);
		}

		private int getTimeOfDay(Calendar c) {
			return 60 * c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE);
		}

		private int getTimeOfDay(MutableDateTime d) {
			return 60 * d.getHourOfDay() + d.getMinuteOfHour();
		}
	}

	private class HeatCalculator {
		// in
		private int numLanes;
		private int numTeams;
		private ProgramOptions options;

		// out
		private int numRaces;
		private int remainder;
		private int numLanesPerRace;
		private int directlyIntoFinal; // 0 keiner, ansonsten die Platzierungen
		private int intoSemiFinal; // 0 keiner, ansonsten die Platzierungen
		private int fromSemiIntoFinal; // 0 keiner, ansonsten die Platzierungen

		private HeatCalculator(int numLanes, int numTeams, ProgramOptions options) {
			this.numLanes = numLanes;
			this.numTeams = numTeams;
			this.options = options;
		}

		private void run() {
			numRaces = numTeams / numLanes;
			remainder = numTeams % numLanes;
			if (remainder > 0)
				numRaces++;
			// "normale" Operation ohne weitere Optionen
			if (numTeams <= numLanes)
				return;

			ExpressionParser parser = new ExpressionParser(ParserMessages.INSTANCE);
			try {
				parser.evaluateExpression(options.getExpr(), numTeams, numLanes);
			} catch (ExpressionException e) {
				throw new FachlicheException(messages.getResourceBundle(), "program.parseError", e.getMessage());
			}
			directlyIntoFinal = parser.getDirectlyIntoFinal();
			intoSemiFinal = parser.getIntoSemiFinal();
			fromSemiIntoFinal = parser.getIntoFinal();

			// if (numTeams <= 2 * numLanes) {
			// // 2 Vorläufe, 1-4 in Endlauf
			// intoFinal = 4;
			// } else if (numTeams <= 3 * numLanes) {
			// // 3 Vorläufe, 1-3 in Endlauf
			// intoFinal = 3;
			// } else if (numTeams <= 4 * numLanes) {
			// // 4 Vorläufe, 1-4 in Zwischenlauf
			// numRaces = 4;
			// intoSemiFinal = 4;
			// } else if (numTeams <= 5 * numLanes) {
			// // 5 Vorläufe
			// numRaces = 5;
			// intoSemiFinal = 3;
			// // TODO was ist, wenn es noch mehr sind?
			// }
			numLanesPerRace = numTeams / numRaces;

			// übrig bleibender Rest
			remainder = numTeams - numLanesPerRace * numRaces;
		}
	}

	private class FollowUpRaces {
		private final List<ProgramRace> heats = new ArrayList<>();
		private final List<ProgramRace> semiFinals = new ArrayList<>();
		private final List<ProgramRace> finals = new ArrayList<>();
	}

	private static final Logger LOG = LoggerFactory.getLogger(ProgramServiceImpl.class);

	@Inject
	private CommonImpl common;

	@Inject
	private PlannerDao dao;

	@Inject
	private Messages messages;

	@Inject
	private Checks checks;

	private String getDefaultHeatModeExpression() {
		Reader in = new InputStreamReader(getClass().getResourceAsStream("/defaultExpression"),
				Charset.forName("UTF8"));
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[500];
		try {
			for (int c; (c = in.read(buf)) != -1;)
				sb.append(new String(buf, 0, c));
			in.close();
		} catch (IOException e) {
			LOG.error("Cannot read default expression", e);
		}
		return sb.toString();
	}

	public Long createProgram(Program program) {
		// gibt es bereits ein Programm für diese Ausschreibung? dann nimm dieses
		Suchkriterien krit = new Suchkriterien();
		krit.addFilter(Program_.announcement.getName(), program.getAnnouncement().getId());
		List<Program> programs = common.search(Program.class, krit).getListe();
		// theoretisch können mehrere existieren, wenn zwei User wirklich
		// gleichzeitig speichern
		if (programs.size() > 0)
			return programs.get(0).getId();

		common.checkWriteAccess(program, Operation.create);

		program.getOptions().setExpr(getDefaultHeatModeExpression());
		common.save(program);

		return program.getId();
	}

	public void generateProgram(Program program) {
		common.checkWriteAccess(program, Operation.save);

		// lösche zuerst das vorher generierte Programm
		Program savedProgram = dao.getById(Program.class, program.getId());
		savedProgram.getRaces().clear();
		common.save(savedProgram);
		dao.executeOperation(new IOperation<Void>() {
			@Override
			public Void execute(EntityManager em) {
				em.flush();
				return null;
			}
		});
		program.setVersion(savedProgram.getVersion());

		// lade alle Meldungen
		Suchkriterien crit = new Suchkriterien();
		crit.addFilter(Registration_.announcement.getName(), program.getAnnouncement().getId());
		crit.addFilter(Registration_.status.getName(), RegistrationStatus.submitted);
		List<Registration> registrations = dao.search(Registration.class, crit, null).getListe();
		Map<Long, List<Team>> races = new HashMap<>();

		if (LOG.isDebugEnabled())
			LOG.debug("Erzeuge Programm fuer " + registrations.size() + " Meldungen.");

		// hier werden erst einmal alle Meldungen der Map hinzugefügt
		// eine Startbahn (lane) ist noch nicht zugeordnet!
		for (Registration registration : registrations) {
			if (LOG.isDebugEnabled())
				LOG.debug(registration.getClub().getName() + " meldet " + registration.getEntries().size()
						+ " Einzelmeldungen");
			for (RegEntry entry : registration.getEntries()) {
				addEntryToProgram(registration.getClub(), entry, races);
			}
		}
		Announcement announcement = common.getById(Announcement.class, program.getAnnouncement().getId(), 0);
		if (LOG.isDebugEnabled())
			LOG.debug("Wir haben insgesamt Meldungen fuer " + races.size() + " von " + announcement.getRaces().size()
					+ " ausgeschriebenen Rennen");

		int numberOfLanes = 9; // TODO announcement.get.. tracksSprint
		program.setRaces(new ArrayList<ProgramRace>());

		EvalContext context = new EvalContext(announcement, program.getOptions());

		// simples Hinzufügen eines Laufs ... das wird erweitert!
		List<ProgramRace> allHeats = new ArrayList<>();
		List<ProgramRace> instantFinals = new ArrayList<>();
		List<ProgramRace> semiFinals = new ArrayList<>();
		List<ProgramRace> finals = new ArrayList<>();

		List<ProgramRace> heats = new ArrayList<>();
		for (Race race : announcement.getRaces()) {
			Long raceId = race.getId();
			// das sind alle Teams, die für das Rennen gemeldet haben
			List<Team> teams = races.get(raceId);
			// die Teams werden nun auf die verfügbaren Startbahnen verteilt

			// upps.. hat niemand für dieses Rennen gemeldet?
			if (teams == null)
				continue;

			int fromSemiIntoFinal = calculateHeats(numberOfLanes, race, teams, context, heats, instantFinals);

			// füge Semifinale hinzu und Finale hinzu
			int teamsInSemiFinals = 0;
			int teamsInFinals = 0;
			for (ProgramRace heat : heats) {
				teamsInSemiFinals += heat.getIntoSemiFinal();
				teamsInFinals += heat.getIntoFinal();
			}
			int numSemiFinals = teamsInSemiFinals / numberOfLanes;
			if (teamsInSemiFinals % numberOfLanes > 0)
				numSemiFinals++;
			for (int i = 0; i < numSemiFinals; i++) {
				ProgramRace semiFinal = new ProgramRace();
				semiFinal.setRace(race);
				semiFinal.setRaceType(RaceType.semiFinal);
				semiFinal.setNumber(i + 1); // TODO die Nummer muss irgendwie anders sein
				semiFinal.setIntoFinal(fromSemiIntoFinal);
				semiFinal.setIntoSemiFinal(0);

				semiFinals.add(semiFinal);
			}
			// falls instant final.. dann braucht es kein weiteres Finale
			if (!heats.isEmpty()) {
				ProgramRace theFinal = new ProgramRace();
				theFinal.setRace(race);
				theFinal.setRaceType(RaceType.finalA);

				finals.add(theFinal);
			}

			allHeats.addAll(heats);
			heats.clear();
		}

		// lege die Semifinale für den Anfang hinter die Vorläufe,
		// das muss später umgeordnet werden
		for (ProgramRace semiFinal : semiFinals) {
			semiFinal.setStartTime(context.nextTime());
		}

		// nun aktualisiere die Startzeiten der sofortigen Finale
		for (ProgramRace instantFinal : instantFinals) {
			instantFinal.setStartTime(context.nextTime());
		}
		// und nun die Finale
		for (ProgramRace aFinal : finals) {
			aFinal.setStartTime(context.nextTime());
		}

		program.getRaces().addAll(allHeats);
		program.getRaces().addAll(semiFinals);
		program.getRaces().addAll(instantFinals);
		program.getRaces().addAll(finals);

		// jetzt haben wir alle Rennen beisammen
		checkProgram(program.getRaces(), program);

		common.save(program);
	}

	private int calculateHeats(int numberOfLanes, Race race, List<Team> teams, EvalContext context,
			List<ProgramRace> heats, List<ProgramRace> instantFinals) {

		if (LOG.isDebugEnabled())
			LOG.debug("Ermittle die Vorlaeufe fuer Rennen " + race.getNumber() + " fuer " + teams.size()
					+ " Einzelmeldungen");

		HeatCalculator calc = new HeatCalculator(numberOfLanes, teams.size(), context.options);
		calc.run();

		Collections.shuffle(teams);
		// Collections.sort(teams, new InitialOrder());

		// wenn so wenige Einzelmeldungen da sind, gibt es keine Vorläufe
		if (calc.numRaces == 1) {

			if (LOG.isDebugEnabled())
				LOG.debug("Rennen " + race.getNumber() + " hat einen Endlauf");

			ProgramRace instantFinal = new ProgramRace();
			instantFinal.setRace(race);
			instantFinal.setRaceType(RaceType.finalA);
			// die Zeit wird erst nach den Endläufen eingetragen
			List<Team> participants = new ArrayList<>(teams);
			int lane = 1;
			for (Team team : participants) {
				team.setLane(lane++);
			}
			instantFinal.setParticipants(participants);
			instantFinals.add(instantFinal);

			return 0;
		} else {

			if (LOG.isDebugEnabled())
				LOG.debug("Rennen " + race.getNumber() + " hat " + calc.numRaces + " Vorlaeufe, Modus ist "
						+ (calc.directlyIntoFinal > 0 ? (calc.directlyIntoFinal > 1 ? "1 bis " : "")
								+ calc.directlyIntoFinal + " in den Endlauf " : " ")
						+ (calc.intoSemiFinal > 0 ? (calc.directlyIntoFinal > 0 ? (calc.directlyIntoFinal + 1) : "1")
								+ " bis " + calc.intoSemiFinal + " in den Zwischenlauf" : ""));

			int addToSingleRace = 0;
			if (calc.remainder > 0) {
				if (calc.numLanesPerRace < numberOfLanes)
					addToSingleRace = calc.remainder;
				else
					throw new TechnischeException("Mist!", null); // TODO
			}

			for (int i = 0, teamOffset = 0; i < calc.numRaces; i++) {
				ProgramRace heat = new ProgramRace();
				heat.setRace(race);
				heat.setRaceType(RaceType.heat);
				heat.setNumber(i + 1);
				heat.setStartTime(context.nextTime());
				heat.setIntoFinal(calc.directlyIntoFinal);
				heat.setIntoSemiFinal(calc.intoSemiFinal);
				int numLanesPerRace = calc.numLanesPerRace;
				if (addToSingleRace > 0) {
					numLanesPerRace++;
					addToSingleRace--;
				}
				int endOffset = Math.min(teamOffset + numLanesPerRace, teams.size());
				List<Team> participants = teams.subList(teamOffset, endOffset);
				int lane = 1;
				for (Team team : participants) {
					team.setLane(lane++);
				}
				heat.setParticipants(participants);
				heats.add(heat);
				teamOffset = endOffset;
			}

			return calc.fromSemiIntoFinal;

			// int raceIntoFinal = 0;
			// int raceIntoSemiFinal = 0;
			// // versuche zuerst, ohne Semifinale auszukommen
			// // dabei wird angenommen, dass es fair ist, der Hälfte eines Vorlaufs.. also z.B. 1-4
			// // ein Chance zum Weiterkommen zu geben
			// int fairAmount = numberOfLanes / 2;
			// int potentialLaneNumbersInFinal = heats.size() * fairAmount;
			// if (potentialLaneNumbersInFinal <= numberOfLanes) {
			// raceIntoFinal = fairAmount;
			// raceIntoSemiFinal = 0;
			// }
			// for (ProgramRace programRace : heats) {
			// programRace.setIntoFinal(raceIntoFinal);
			// programRace.setIntoSemiFinal(raceIntoSemiFinal);
			// }
		}
	}

	public void swapRaces(final ProgramRace r1, final ProgramRace r2) {
		common.checkWriteAccess(r1, Operation.save);
		common.checkWriteAccess(r2, Operation.save);

		Date st1 = r1.getStartTime();
		r1.setStartTime(r2.getStartTime());
		r2.setStartTime(st1);
		// man könnte natürlich auch save anpassem...
		dao.executeOperation(new IOperation<Void>() {
			@Override
			public Void execute(EntityManager em) {
				em.merge(r1);
				em.merge(r2);
				return null;
			}
		});
	}

	public Program getProgram(final Long id) {
		return dao.executeOperation(new IOperation<Program>() {
			@Override
			public Program execute(EntityManager em) {
				return doGetProgram(em, id);
			}
		});
	}

	private Program doGetProgram(EntityManager em, Long id) {
		// in JPQL gibt es eine MultipleBagException
		String sql = "select " //
				+ "pr.id RaceId, pr.number, pr.startTime, pr.raceType, pr.heatMode, " //
				+ "r.number RaceNumber, r.gender, r.distance, r.boatClass, r.ageType, " //
				+ "t.id TeamId, t.lane, " //
				+ "tc.id TeamClubId, tc.shortName TCShort, tc.name TCName, " //
				+ "tm.pos, tm.remark, " //
				+ "tu.firstName, tu.lastName, tu.birthDate, " //
				+ "uc.id UserClubId, uc.shortName UCShort, uc.name UCName " //
				+ "from Program p " //
				+ "inner join Announcement a on p.announcement_id=a.id "
				+ "inner join ProgramRace pr on pr.program_id=p.id " //
				+ "left outer join Race r on pr.race_id=r.id " //
				+ "left outer join Team t on t.programrace_id=pr.id " //
				+ "left outer join Club tc on t.club_id=tc.id " //
				+ "left outer join TeamMember tm on tm.team_id=t.id " //
				+ "left outer join User tu on tm.user_id=tu.id " //
				+ "left outer join Club uc on tu.club_id=uc.id " + "where p.id = :id";
		Query query = em.createNativeQuery(sql);
		query.setParameter("id", id);
		@SuppressWarnings("unchecked")
		List<Object[]> result = query.getResultList();
		Map<BigInteger, ProgramRace> races = new HashMap<>();
		Map<Integer, FollowUpRaces> racesByNumber = new HashMap<>();
		Map<BigInteger, Team> teams = new HashMap<>();
		Map<BigInteger, Club> clubs = new HashMap<>();
		// so werden die Options mitgeladen
		Program program = em.find(Program.class, id);
		em.detach(program);
		program.setRaces(new ArrayList<ProgramRace>());
		for (Object[] row : result) {
			BigInteger raceId = (BigInteger) row[0];
			ProgramRace race = races.get(raceId);
			if (race == null) {
				race = new ProgramRace();
				race.setId(raceId.longValue());
				race.setNumber((Integer) row[1]);
				race.setStartTime((Date) row[2]);
				race.setRaceType(byOrdinal(RaceType.class, (Integer) row[3]));
				race.setHeatMode((String) row[4]);
				race.setParticipants(new ArrayList<Team>());
				races.put(raceId, race);

				Race r = new Race();
				r.setNumber((Integer) row[5]);
				r.setGender(byOrdinal(Gender.class, (Integer) row[6]));
				r.setDistance(((Number) row[7]).intValue());
				r.setBoatClass(byOrdinal(BoatClass.class, (Integer) row[8]));
				r.setAgeType(byOrdinal(AgeType.class, (Integer) row[9]));
				race.setRace(r);

				program.getRaces().add(race);

				FollowUpRaces followUpRaces = racesByNumber.get(r.getNumber());
				if (followUpRaces == null && !race.getRaceType().isFinal()) {
					followUpRaces = new FollowUpRaces();
					racesByNumber.put(r.getNumber(), followUpRaces);
				}
				if (followUpRaces != null) {
					if (race.getRaceType() == RaceType.heat)
						followUpRaces.heats.add(race);
					else if (race.getRaceType() == RaceType.semiFinal)
						followUpRaces.semiFinals.add(race);
					else
						followUpRaces.finals.add(race);
				}
			}
			BigInteger teamId = (BigInteger) row[10];
			if (teamId != null) {
				Team team = teams.get(teamId);
				if (team == null) {
					team = new Team();
					team.setLane((int) row[11]);
					team.setClub(getClub(clubs, row, 12));
					team.setMembers(new ArrayList<TeamMember>());
					teams.put(teamId, team);
					race.getParticipants().add(team);
				}
				TeamMember member = new TeamMember();
				member.setPos((int) row[15]);
				member.setRemark((String) row[16]);
				if (member.getRemark() == null) {
					User user = new User();
					user.setFirstName((String) row[17]);
					user.setLastName((String) row[18]);
					user.setBirthDate((Date) row[19]);
					user.setClub(getClub(clubs, row, 20));
					member.setUser(user);
				}
				team.getMembers().add(member);
			}
		}

		for (FollowUpRaces followUpRaces : racesByNumber.values()) {
			if (followUpRaces.semiFinals.size() > 0) {
				ProgramRace firstSemiFinal = followUpRaces.semiFinals.get(0);
				for (ProgramRace heat : followUpRaces.heats) {
					heat.setFollowUpRace(firstSemiFinal);
				}
			}
			if (followUpRaces.finals.size() > 0) {
				ProgramRace firstFinal = followUpRaces.finals.get(0);
				if (followUpRaces.semiFinals.size() > 0) {
					for (ProgramRace semiFinal : followUpRaces.semiFinals) {
						semiFinal.setFollowUpRace(firstFinal);
					}
				} else if (followUpRaces.heats.size() > 0) {
					for (ProgramRace heat : followUpRaces.heats) {
						heat.setFollowUpRace(firstFinal);
					}
				}
			}
		}
		return program;
	}

	private Club getClub(Map<BigInteger, Club> clubs, Object[] row, int offset) {
		BigInteger clubId = (BigInteger) row[offset++];
		Club club = clubs.get(clubId);
		if (club == null) {
			club = new Club();
			club.setId(clubId.longValue());
			club.setShortName((String) row[offset++]);
			club.setName((String) row[offset]);
			clubs.put(clubId, club);
		}
		return club;
	}

	private <T extends Enum<T>> T byOrdinal(Class<T> type, int ordinal) {
		for (T t : type.getEnumConstants()) {
			if (t.ordinal() == ordinal)
				return t;
		}
		throw new IllegalArgumentException();
	}

	public void checkProgram(Program program) {
		checkProgram(program.getRaces(), program);
	}

	private List<Change> checkProgram(List<ProgramRace> races, Program program) {
		// Altersklassen trennen, so dass die Rennen erreichbar sind!
		// Endläufe nach hinten
		// die einzelnen Prüfungen müssen separate Module sein.
		// Rennen Austausch muss gemerkt und nicht wiederholt werden
		if (LOG.isDebugEnabled())
			LOG.debug("Prüfe das Programm " + program.getAnnouncement().getName());

		List<Problem> problems = checks.execute(program, races, true);
		for (Problem problem : problems) {
			System.out.println(problem);
		}

		List<Change> changes = new ArrayList<>();
		// List<ProgramRace> copies = null;
		// if (createCopy) {
		// copies = new ArrayList<>();
		// copies.addAll(races);
		// }

		return changes;
	}

	private void addEntryToProgram(Club club, RegEntry entry, Map<Long, List<Team>> races) {
		Race race = entry.getRace();
		List<Team> teams = races.get(race.getId());
		if (teams == null)
			races.put(race.getId(), teams = new ArrayList<>());
		Team team = new Team();
		team.setClub(club);
		List<TeamMember> members = new ArrayList<>();
		team.setMembers(members);
		List<Participant> participants = entry.getParticipants();
		// jeder entry beinhaltet bereits ein Team
		for (Participant participant : participants) {
			TeamMember member = new TeamMember();
			member.setUser(participant.getUser());
			member.setPos(participant.getPos());
			member.setRemark(participant.getRemark());
			members.add(member);
		}
		teams.add(team);
	}
}
