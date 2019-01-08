package org.planner.business;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.joda.time.Days;
import org.joda.time.Instant;
import org.joda.time.MutableDateTime;
import org.planner.business.CommonImpl.Operation;
import org.planner.business.program.Checks;
import org.planner.business.program.ListView;
import org.planner.dao.IOperation;
import org.planner.dao.PlannerDao;
import org.planner.ejb.CallerProvider;
import org.planner.eo.Announcement;
import org.planner.eo.Club;
import org.planner.eo.Participant;
import org.planner.eo.Placement;
import org.planner.eo.Program;
import org.planner.eo.Program.ProgramStatus;
import org.planner.eo.ProgramOptions;
import org.planner.eo.ProgramOptions.Break;
import org.planner.eo.ProgramOptions.DayTimes;
import org.planner.eo.ProgramRace;
import org.planner.eo.ProgramRace.RaceType;
import org.planner.eo.ProgramRaceTeam;
import org.planner.eo.ProgramRaceTeam_;
import org.planner.eo.ProgramRace_;
import org.planner.eo.Program_;
import org.planner.eo.Race;
import org.planner.eo.RegEntry;
import org.planner.eo.Registration;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Registration_;
import org.planner.eo.Result;
import org.planner.eo.Team;
import org.planner.eo.TeamMember;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Change;
import org.planner.model.Gender;
import org.planner.model.ResultExtra;
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
	private CallerProvider caller;

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
		program.setStatus(ProgramStatus.created);
		common.save(program);

		return program.getId();
	}

	public void generateProgram(final Program program) {
		common.checkWriteAccess(program, Operation.save);

		// lade alle Meldungen
		Suchkriterien crit = new Suchkriterien();
		crit.addFilter(Registration_.announcement.getName(), program.getAnnouncement().getId());
		crit.addFilter(Registration_.status.getName(), RegistrationStatus.submitted);
		List<Registration> registrations = dao.search(Registration.class, crit, null).getListe();
		if (registrations.isEmpty())
			throw new FachlicheException(messages.getResourceBundle(), "program.noRegistrations");
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
				teamsInSemiFinals += heat.getIntoSemiFinal() - heat.getIntoFinal();
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

		clearResultsAndRaces(program.getId());

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

		// wenn so wenige Einzelmeldungen da sind oder eine Langstrecke gefahren wird, gibt es keine Vorläufe
		if (calc.numRaces == 1 || race.getDistance() >= 2000) {

			if (LOG.isDebugEnabled())
				LOG.debug("Rennen " + race.getNumber() + " hat einen Endlauf");

			ProgramRace instantFinal = new ProgramRace();
			instantFinal.setRace(race);
			instantFinal.setRaceType(RaceType.finalA);
			// die Zeit wird erst nach den Endläufen eingetragen
			int lane = 1;
			for (Team team : teams) {
				ProgramRaceTeam raceTeam = new ProgramRaceTeam(instantFinal, team);
				raceTeam.setLane(lane++);
				instantFinal.addParticipant(raceTeam);
			}
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
					ProgramRaceTeam participant = new ProgramRaceTeam(heat, team);
					participant.setLane(lane++);
					heat.addParticipant(participant);
				}
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

	public void swapRaces(ProgramRace r1, ProgramRace r2) {
		// lade die unvollständigen Races
		final ProgramRace race1 = common.getById(ProgramRace.class, r1.getId(), 0);
		final ProgramRace race2 = common.getById(ProgramRace.class, r2.getId(), 0);
		common.checkWriteAccess(race1, Operation.save);
		common.checkWriteAccess(race2, Operation.save);

		Date st1 = race1.getStartTime();
		race1.setStartTime(race2.getStartTime());
		race2.setStartTime(st1);
		// man könnte natürlich auch save anpassem...
		dao.executeOperation(new IOperation<Void>() {
			@Override
			public Void execute(EntityManager em) {
				em.merge(race1);
				em.merge(race2);
				return null;
			}
		});
	}

	public Program getProgram(final Long id, final boolean withResults, final boolean orderByResults) {
		return dao.executeOperation(new IOperation<Program>() {
			@Override
			public Program execute(EntityManager em) {
				return doGetProgram(em, id, withResults, orderByResults);
			}
		});
	}

	private Program doGetProgram(EntityManager em, Long id, boolean withResults, boolean orderByResults) {
		// so werden die Options mitgeladen
		Program program = em.find(Program.class, id);
		if (program == null)
			throw new FachlicheException(messages.getResourceBundle(), "program.notfound");
		em.detach(program);

		// in JPQL gibt es eine MultipleBagException
		StringBuilder sql = new StringBuilder("select " //
				+ "r.id RaceId, r.number RaceNumber, r.gender, r.distance, r.boatClass, r.ageType, " //
				+ "pr.id ProgramRaceId, pr.number, pr.startTime, pr.raceType, pr.heatMode, " //
				+ "t.id TeamId, pt.lane, " //
				+ "tc.id TeamClubId, tc.shortName TCShort, tc.name TCName, " //
				+ "tm.id MemberId, tm.pos, tm.remark, " //
				+ "tu.firstName, tu.lastName, tu.birthDate, " //
				+ "uc.id UserClubId, uc.shortName UCShort, uc.name UCName");
		if (withResults) {
			sql.append(", pl.position, pl.time, pl.extra");
		}
		sql.append(" from Program p " //
				+ "inner join Announcement a on p.announcement_id=a.id "
				+ "inner join ProgramRace pr on pr.program_id=p.id " //
				+ "left join Race r on pr.race_id=r.id " //
				+ "left join ProgramRace_Team pt on pt.programrace_id=pr.id " //
				+ "left join Team t on pt.team_id=t.id " //
				+ "left join Club tc on t.club_id=tc.id " //
				+ "left join TeamMember tm on tm.team_id=t.id " //
				+ "left join User tu on tm.user_id=tu.id " //
				+ "left join Club uc on tu.club_id=uc.id ");
		if (withResults) {
			sql.append("left join Placement pl on pt.programrace_id=pl.programrace_id and pt.team_id=pl.team_id ");
		}
		sql.append("where p.id = :id ");
		sql.append("order by pr.startTime, ");
		sql.append(orderByResults && withResults ? "pl.position, pt.lane" : "pt.lane");
		sql.append(", tm.pos");
		Query query = em.createNativeQuery(sql.toString());
		query.setParameter("id", id);
		long time = System.currentTimeMillis();
		@SuppressWarnings("unchecked")
		List<Object[]> result = query.getResultList();
		Map<Integer, FollowUpRaces> racesByNumber = new HashMap<>();
		// dient dem effizienteren Serialisieren
		Map<BigInteger, TeamMember> members = new HashMap<>();
		Map<BigInteger, Club> clubs = new HashMap<>();
		ArrayList<ProgramRace> races = new ArrayList<ProgramRace>();
		program.setRaces(races);
		Race race = null;
		ProgramRaceTeam raceTeam = null;
		ProgramRace programRace = null;
		for (Object[] row : result) {
			BigInteger raceId = (BigInteger) row[0];
			if (race == null || raceId.longValue() != race.getId()) {
				race = new Race();
				race.setId(raceId.longValue());
				race.setNumber((Integer) row[1]);
				race.setGender(byOrdinal(Gender.class, (Integer) row[2]));
				race.setDistance(((Number) row[3]).intValue());
				race.setBoatClass(byOrdinal(BoatClass.class, (Integer) row[4]));
				race.setAgeType(byOrdinal(AgeType.class, (Integer) row[5]));
			}
			BigInteger programRaceId = (BigInteger) row[6];
			if (programRace == null || programRaceId.longValue() != programRace.getId()) {
				programRace = new ProgramRace();
				programRace.setId(programRaceId.longValue());
				programRace.setNumber((Integer) row[7]);
				programRace.setStartTime((Date) row[8]);
				programRace.setRaceType(byOrdinal(RaceType.class, (Integer) row[9]));
				programRace.setHeatMode((String) row[10]);

				programRace.setRace(race);
				races.add(programRace);

				FollowUpRaces followUpRaces = racesByNumber.get(race.getNumber());
				if (followUpRaces == null && !programRace.getRaceType().isFinal()) {
					followUpRaces = new FollowUpRaces();
					racesByNumber.put(race.getNumber(), followUpRaces);
				}
				if (followUpRaces != null) {
					if (programRace.getRaceType() == RaceType.heat)
						followUpRaces.heats.add(programRace);
					else if (programRace.getRaceType() == RaceType.semiFinal)
						followUpRaces.semiFinals.add(programRace);
					else
						followUpRaces.finals.add(programRace);
				}
				// sobald ein neues ProgramRace beginnt, handelt es sich auch um andere RaceTeams
				raceTeam = null;
			}
			BigInteger teamId = (BigInteger) row[11];
			if (teamId != null) {
				if (raceTeam == null || teamId.longValue() != raceTeam.getTeamId()) {
					Team team = new Team(teamId.longValue());
					team.setClub(getClub(clubs, row, 13));
					raceTeam = new ProgramRaceTeam(programRace, team);
					raceTeam.setLane((int) row[12]);
					programRace.addParticipant(raceTeam);

					if (withResults && row[25] != null) {
						if (programRace.getPlacements() == null)
							programRace.setPlacements(new ArrayList<Placement>());
						int position = (int) row[25];
						Long resultTime = row[26] != null ? ((BigInteger) row[26]).longValue() : null;
						ResultExtra extra = row[27] != null ? ResultExtra.class.getEnumConstants()[(int) row[27]]
								: null;
						Placement placement = new Placement(raceTeam, resultTime, extra);
						placement.setPosition(position);
						programRace.getPlacements().add(placement);
					}
				}

				BigInteger memberId = (BigInteger) row[16];
				TeamMember member = members.get(memberId);
				if (member == null) {
					member = new TeamMember();
					member.setPos((int) row[17]);
					member.setRemark((String) row[18]);
					if (member.getRemark() == null) {
						User user = new User();
						user.setFirstName((String) row[19]);
						user.setLastName((String) row[20]);
						user.setBirthDate((Date) row[21]);
						user.setClub(getClub(clubs, row, 22));
						member.setUser(user);
					}
					members.put(memberId, member);
				}
				raceTeam.getTeam().addMember(member);
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
		if (LOG.isDebugEnabled())
			LOG.debug("Laden des Programms brauchte: " + (System.currentTimeMillis() - time) / 1000.0 + "s");
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

	public void deleteProgram(Long programId) {
		clearResultsAndRaces(programId);
		common.delete(Program.class, programId);
	}

	private void clearResultsAndRaces(final Long programId) {
		Program program = dao.getById(Program.class, programId);

		// da ein Team über ProgramRaceTeam mehreren ProgramRaces zugeordnet sein kann,
		// ist es nicht möglich, dies gemeinsam über Hibernate-Kaskadierung zu löschen
		// deshalb werden die Verknüpfungen für Folgerennen (Semifinale und Finale) explizit gelöscht
		dao.executeOperation(new IOperation<Void>() {
			@Override
			public Void execute(EntityManager em) {
				// falls Ergebnisse existieren, lösche diese
				Result result = dao.getById(Result.class, programId);
				if (result != null)
					result.delete(em);

				CriteriaBuilder builder = em.getCriteriaBuilder();
				CriteriaDelete<ProgramRaceTeam> deleteMapping = builder.createCriteriaDelete(ProgramRaceTeam.class);
				Root<ProgramRaceTeam> root = deleteMapping.from(ProgramRaceTeam.class);
				Subquery<Long> subquery = deleteMapping.subquery(Long.class);
				Root<ProgramRace> programRace = subquery.from(ProgramRace.class);
				subquery.select(programRace.get(ProgramRace_.id));
				ParameterExpression<Long> pProgramId = builder.parameter(Long.class, "programId");
				ParameterExpression<RaceType> pRaceType = builder.parameter(RaceType.class, "raceType");
				subquery.where(builder.and(builder.equal(programRace.get(ProgramRace_.programId), pProgramId),
						builder.greaterThan(programRace.get(ProgramRace_.raceType), pRaceType)));
				deleteMapping.where(root.get(ProgramRaceTeam_.programRace).get(ProgramRace_.id).in(subquery));
				em.createQuery(deleteMapping).setParameter(pProgramId, programId).setParameter(pRaceType, RaceType.heat)
						.executeUpdate();
				return null;
			}
		});
		// leere dann auch das vorher generierte Programm
		program.getRaces().clear();
		common.save(program);
	}

	public List<Change> checkProgram(Program program) {
		return checkProgram(program.getRaces(), program);
	}

	private List<Change> checkProgram(List<ProgramRace> races, Program program) {
		// Altersklassen trennen, so dass die Rennen erreichbar sind!
		// Endläufe nach hinten
		// die einzelnen Prüfungen müssen separate Module sein.
		// Rennen Austausch muss gemerkt und nicht wiederholt werden
		if (LOG.isDebugEnabled())
			LOG.debug("Prüfe das Programm " + program.getAnnouncement().getName());

		List<ProgramRace> wrappedRaces = new ListView<>(races);
		List<Change> changes = new ArrayList<>();
		checks.execute(program, wrappedRaces, changes);

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
		List<Participant> participants = entry.getParticipants();
		// jeder entry beinhaltet bereits ein Team
		for (Participant participant : participants) {
			TeamMember member = new TeamMember();
			member.setUser(participant.getUser());
			member.setPos(participant.getPos());
			member.setRemark(participant.getRemark());
			team.addMember(member);
		}
		teams.add(team);
	}

	public void setProgramStatus(Long programId, ProgramStatus status) {
		Program program = common.getById(Program.class, programId, 0);
		common.checkWriteAccess(program, Operation.save);
		if (!caller.isInRole("Tester") && status == ProgramStatus.created)
			throw new FachlicheException(messages.getResourceBundle(), "accessSetStatus");
		program.setStatus(status);
		common.save(program);
	}

	public List<ProgramRace> saveResult(Long programRaceId, List<Placement> placements) {
		final ProgramRace programRace = common.getById(ProgramRace.class, programRaceId, 0);
		common.checkWriteAccess(programRace, Operation.save);

		// ordnen
		List<Placement> timed = new ArrayList<>();
		List<Placement> dns = new ArrayList<>();
		List<Placement> dnf = new ArrayList<>();
		List<Placement> dq = new ArrayList<>();
		for (Iterator<Placement> it = placements.iterator(); it.hasNext();) {
			Placement p = it.next();
			if (p.getExtra() == ResultExtra.dns)
				dns.add(p);
			else if (p.getExtra() == ResultExtra.dnf)
				dnf.add(p);
			else if (p.getExtra() == ResultExtra.dq)
				dq.add(p);
			else if (p.getTime() != null)
				timed.add(p);
			else
				continue;
			it.remove();
		}
		Collections.sort(timed, new Comparator<Placement>() {
			@Override
			public int compare(Placement o1, Placement o2) {
				return (int) (o1.getTime() - o2.getTime());
			}
		});
		placements.addAll(timed);
		placements.addAll(dns);
		placements.addAll(dnf);
		placements.addAll(dq);

		for (int i = 0; i < placements.size(); i++) {
			Placement placement = placements.get(i);
			placement.setPosition(i + 1);
		}
		programRace.setPlacements(placements);

		dao.executeOperation(new IOperation<Void>() {
			@Override
			public Void execute(EntityManager em) {
				em.merge(programRace);
				return null;
			}
		});
		// handelt es sich um einen Vor- oder Zwischenlauf?
		if (programRace.getRaceType() != RaceType.heat && programRace.getRaceType() != RaceType.semiFinal)
			return null;
		// sind alle anderen zugehörigen Läufe bereits gespeichert?
		// vergleiche dafür die Anzahl der gespeicherten Ergebnisse mit der Anzahl der Läufe dieses Rennens
		Number difference = dao.executeOperation(new IOperation<Number>() {
			@Override
			public Number execute(EntityManager em) {
				em.flush();
				String sql = "select (select count(id) from ProgramRace where racetype=:raceType and race_id=:raceId)"
						+ " - (select count(distinct programrace_id) from Placement where programrace_id in (select id from ProgramRace where racetype=:raceType and race_id=:raceId))"
						+ " from Dual";
				return (Number) em.createNativeQuery(sql).setParameter("raceId", programRace.getRace().getId())
						.setParameter("raceType", programRace.getRaceType().ordinal()).getSingleResult();
			}
		});
		if (difference.intValue() != 0)
			return null;
		// Fülle nun die nachfolgenden Rennen, beginnend mit dem Finale
		List<ProgramRace> followUpRaces = dao.executeOperation(new IOperation<List<ProgramRace>>() {
			@Override
			public List<ProgramRace> execute(EntityManager em) {
				return em
						.createQuery("from ProgramRace where race=:race and racetype>:raceType " //
								+ "order by racetype desc, number", ProgramRace.class)
						.setParameter("race", programRace.getRace())
						.setParameter("raceType", programRace.getRaceType().ordinal()).getResultList();
			}
		});
		// Resultate
		List<ProgramRace> results = dao.executeOperation(new IOperation<List<ProgramRace>>() {
			@Override
			public List<ProgramRace> execute(EntityManager em) {
				if (em.contains(programRace))
					em.refresh(programRace);
				String sql = "select p from ProgramRace p where p.race=:race and p.raceType=:raceType";
				return em.createQuery(sql, ProgramRace.class).setParameter("race", programRace.getRace())
						.setParameter("raceType", programRace.getRaceType()).getResultList();
			}
		});
		if (LOG.isDebugEnabled())
			LOG.debug("Es liegen Ergebnisse für " + results.size() + " Rennen vor, ermittle Belegung der Folgerennen");

		List<ProgramRace> semiFinals = new ArrayList<>();
		for (Iterator<ProgramRace> it = followUpRaces.iterator(); it.hasNext();) {
			ProgramRace next = it.next();
			if (next.getRaceType() == RaceType.finalA) {
				if (!addQualifiedTeams(Arrays.asList(next), results))
					it.remove();
			} else {
				semiFinals.add(next);
			}
		}
		if (!semiFinals.isEmpty()) {
			if (!addQualifiedTeams(semiFinals, results))
				followUpRaces.removeAll(semiFinals);
		}

		return followUpRaces;
	}

	private boolean addQualifiedTeams(List<ProgramRace> followUpRaces, List<ProgramRace> results) {
		boolean followUpModified = false;
		Map<Long, Iterator<Placement>> qualified = new HashMap<>();
		for (int i = 0, j = 0, k = 0; !allIteratorsEmpty(qualified); i = ++i < results.size() ? i
				: 0, j = ++j < followUpRaces.size() ? j : 0, k++) {

			ProgramRace r = results.get(i);
			ProgramRace next = followUpRaces.get(j);
			if (!next.getPlacements().isEmpty())
				throw new FachlicheException(messages.getResourceBundle(), "program.followUpResultsExist",
						getRaceText(next), getRaceText(r));

			int from = 0;
			int to = 0;
			RaceType raceType = next.getRaceType();
			if (raceType == RaceType.finalA) {
				to = r.getIntoFinal();
			} else if (raceType == RaceType.semiFinal) {
				from = r.getIntoFinal();
				to = r.getIntoSemiFinal();
			}
			if (to == 0)
				break;

			Iterator<Placement> qualifiedIt = qualified.get(r.getId());
			if (qualifiedIt == null) {
				if (LOG.isDebugEnabled())
					LOG.debug("Im Rennen " + r.getId() + " qualifizierten sich Platz " + (from + 1) + " bis " + to
							+ " für das Rennen " + next.getId() + " ein " + raceType);

				// kopiert die sub-list für Modifikation
				qualifiedIt = new ArrayList<>(r.getPlacements().subList(from, Math.min(to, r.getPlacements().size())))
						.iterator();
				qualified.put(r.getId(), qualifiedIt);
			}

			// aufgrund der order-by-clause sind die Finale zuerst gelistet
			if (!qualifiedIt.hasNext())
				continue;
			Placement placement = qualifiedIt.next();
			// TODO die sollten besser in er Query aussortiert werden
			if (placement.getExtra() != null)
				continue;

			ProgramRaceTeam raceTeam = new ProgramRaceTeam(next, placement.getTeam().getTeam());

			// Das Setzsystem beinhaltet im Prinzip, dass von den mittleren Bahnen nach außen jeweils
			// die vorderen Plätze belegt werden mit einer jeweils gleichen Anzahl an Startern aus den
			// jeweiligen Vorläufen
			List<ProgramRaceTeam> participants = next.getParticipants();
			// diese Prüfung erfolgt nur für den ersten Durchlauf der Folgerennen
			if (k < followUpRaces.size() && next.getRaceType().ordinal() < RaceType.finalA.ordinal()
					&& participants.size() > 0)
				throw new FachlicheException(messages.getResourceBundle(), "program.followUpExists");
			int lane = computeLane(participants);
			raceTeam.setLane(lane);
			participants.add(raceTeam);
			followUpModified = true;
			if (LOG.isDebugEnabled())
				LOG.debug("Der " + placement.getPosition() + "te aus Rennen " + r.getId() + " wird im Rennen "
						+ next.getId() + " auf Bahn " + lane + " gesetzt");
		}
		return followUpModified;
	}

	private boolean allIteratorsEmpty(Map<Long, Iterator<Placement>> its) {
		for (Iterator<Placement> it : its.values()) {
			if (it.hasNext())
				return false;
		}
		return its.size() > 0;
	}

	private int computeLane(List<ProgramRaceTeam> participants) {
		int numLanes = 9; // TODO vom Programm
		int upper = (int) Math.round(numLanes / 2.0);
		if (!isOccuppied(participants, upper))
			return upper;
		int lower = upper;
		do {
			if (!isOccuppied(participants, --lower))
				return lower;
			if (!isOccuppied(participants, ++upper))
				return upper;
		} while (true);
	}

	private boolean isOccuppied(List<ProgramRaceTeam> participants, int lane) {
		for (ProgramRaceTeam team : participants) {
			if (team.getLane() == lane)
				return true;
		}
		return false;
	}

	private String getRaceText(ProgramRace race) {
		StringBuilder sb = new StringBuilder();
		sb.append(messages.getMessage("program.race"));
		sb.append(" ");
		sb.append(race.getRace().getNumber());
		if (race.getNumber() != null)
			sb.append("-").append(race.getNumber());
		sb.append(" (");
		if (race.getNumber() != null) {
			sb.append(race.getNumber());
			sb.append(". ");
		}
		sb.append(race.getRaceType().getText());
		sb.append(")");
		return sb.toString();
	}
}
