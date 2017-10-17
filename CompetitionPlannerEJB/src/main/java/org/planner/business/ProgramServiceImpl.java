package org.planner.business;

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

import org.planner.business.CommonImpl.Operation;
import org.planner.dao.IOperation;
import org.planner.dao.PlannerDao;
import org.planner.eo.Announcement;
import org.planner.eo.Club;
import org.planner.eo.Participant;
import org.planner.eo.Program;
import org.planner.eo.ProgramOptions;
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
import org.planner.model.Suchkriterien;
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
		private ProgramOptions options;
		private int day;
		private Calendar time;
		private boolean afterNoon;

		private EvalContext(ProgramOptions options) {
			this.options = options;
		}

		public Date nextTime() {
			if (time == null) {
				time = Calendar.getInstance();
				time.setTime(options.getBeginTimes()[day]);
			} else {
				time.add(Calendar.MINUTE, options.getTimeLag());
				if (!afterNoon && time.getTimeInMillis() >= options.getLaunchBreak().getTime()) {
					time.add(Calendar.MINUTE, options.getBreakDuration());
					afterNoon = true;
				}
			}
			return time.getTime();
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(ProgramServiceImpl.class);

	@Inject
	private CommonImpl common;

	@Inject
	private PlannerDao dao;

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

		int numberOfLanes = 8; // TODO announcement.get.. tracksSprint
		program.setRaces(new ArrayList<ProgramRace>());

		EvalContext context = new EvalContext(program.getOptions());

		// simples Hinzufügen eines Laufs ... das wird erweitert!
		List<ProgramRace> instantFinals = new ArrayList<>();
		List<ProgramRace> heats = new ArrayList<>();

		for (Race race : announcement.getRaces()) {
			Long raceId = race.getId();
			// das sind alle Teams, die für das Rennen gemeldet haben
			List<Team> teams = races.get(raceId);
			// die Teams werden nun auf die verfügbaren Startbahnen verteilt

			// upps.. hat niemand für dieses Rennen gemeldet?
			if (teams == null)
				continue;

			calculateHeats(numberOfLanes, race, teams, context, heats, instantFinals);
		}
		// nun aktualisiere die Startzeiten der Finale
		for (ProgramRace final_ : instantFinals) {
			final_.setStartTime(context.nextTime());
		}

		program.getRaces().addAll(heats);
		program.getRaces().addAll(instantFinals);

		common.save(program);
	}

	private void calculateHeats(int numberOfLanes, Race race, List<Team> teams, EvalContext context,
			List<ProgramRace> heats, List<ProgramRace> instantFinals) {

		int numberOfHeats = teams.size() / numberOfLanes;
		int remainder = teams.size() % numberOfLanes;

		if (LOG.isDebugEnabled())
			LOG.debug("Ermittle die Vorlaeufe fuer Rennen " + race.getNumber() + " fuer " + teams.size()
					+ " Einzelmeldungen");

		Collections.shuffle(teams);
		// Collections.sort(teams, new InitialOrder());

		int intoFinal = context.options.getIntoFinal();
		int intoSemiFinal = context.options.getIntoSemiFinal();

		// wenn so wenige Einzelmeldungen da sind, gibt es keine Vorläufe
		if (teams.size() <= numberOfLanes) {
			ProgramRace final_ = new ProgramRace();
			final_.setRace(race);
			final_.setRaceType(RaceType.finalA);
			// die Zeit wird erst nach den Endläufen eingetragen
			List<Team> participants = new ArrayList<>(teams);
			int lane = 1;
			for (Team team : participants) {
				team.setLane(lane++);
			}
			final_.setParticipants(participants);
			instantFinals.add(final_);
		} else {

			// TODO was soll passieren, wenn weniger als ein halbes Rennen übrigbleibt?
			int raceNumberOfLanes = numberOfLanes;
			if (remainder > 0 && remainder < numberOfLanes / 2) {
				raceNumberOfLanes = teams.size() / (numberOfHeats + 1) + 1;
				if (LOG.isDebugEnabled())
					LOG.debug(
							"Bei Rennen " + race.getNumber() + " bleibt ein Rennen mit nur " + remainder + " Startern");
			}

			for (int i = 0, teamOffset = 0; i <= numberOfHeats && remainder > 0; i++) {
				ProgramRace heat = new ProgramRace();
				heat.setRace(race);
				heat.setRaceType(RaceType.heat);
				heat.setNumber(i + 1);
				heat.setStartTime(context.nextTime());
				int endOffset = Math.min(teamOffset + raceNumberOfLanes, teams.size());
				List<Team> participants = teams.subList(teamOffset, endOffset);
				int lane = 1;
				for (Team team : participants) {
					team.setLane(lane++);
				}
				heat.setParticipants(participants);
				heats.add(heat);
				teamOffset = endOffset;
			}

			if (LOG.isDebugEnabled())
				LOG.debug("Rennen " + race.getNumber() + " hat " + heats.size() + " Vorlaeufe");
			int raceIntoFinal = 0;
			int raceIntoSemiFinal = 0;
			// versuche zuerst, ohne Semifinale auszukommen
			// dabei wird angenommen, dass es fair ist, der Hälfte eines Vorlaufs.. also z.B. 1-4
			// ein Chance zum Weiterkommen zu geben
			int fairAmount = numberOfLanes / 2;
			int potentialLaneNumbersInFinal = heats.size() * fairAmount;
			if (potentialLaneNumbersInFinal <= numberOfLanes) {
				raceIntoFinal = fairAmount;
				raceIntoSemiFinal = 0;
			}
			for (ProgramRace programRace : heats) {
				programRace.setIntoFinal(raceIntoFinal);
				programRace.setIntoSemiFinal(raceIntoSemiFinal);
			}
		}
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
			members.add(member);
		}
		teams.add(team);
	}
}
