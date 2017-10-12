package org.planner.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.planner.business.CommonImpl.Operation;
import org.planner.dao.PlannerDao;
import org.planner.eo.Announcement;
import org.planner.eo.Club;
import org.planner.eo.Participant;
import org.planner.eo.Program;
import org.planner.eo.ProgramRace;
import org.planner.eo.Program_;
import org.planner.eo.Race;
import org.planner.eo.RegEntry;
import org.planner.eo.Registration;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Registration_;
import org.planner.eo.Team;
import org.planner.eo.TeamMember;
import org.planner.model.Suchkriterien;

@Named
public class ProgramServiceImpl {

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

		// lade alle Meldungen
		Suchkriterien crit = new Suchkriterien();
		crit.addFilter(Registration_.announcement.getName(), program.getAnnouncement().getId());
		crit.addFilter(Registration_.status.getName(), RegistrationStatus.submitted);
		List<Registration> registrations = dao.search(Registration.class, crit, null).getListe();
		Map<Long, List<Team>> races = new HashMap<>();

		// hier werden erst einmal alle Meldungen der Map hinzugefügt
		// eine Startbahn (lane) ist noch nicht zugeordnet!
		for (Registration registration : registrations) {
			for (RegEntry entry : registration.getEntries()) {
				addEntryToProgram(registration.getClub(), entry, races);
			}
		}
		Announcement announcement = common.getById(Announcement.class, program.getAnnouncement().getId(), 0);

		int numberOfLanes = 8; // TODO announcement.get.. tracksSprint
		program.setRaces(new ArrayList<ProgramRace>());

		// simples Hinzufügen eines Laufs ... das wird erweitert!
		for (Race race : announcement.getRaces()) {
			Long raceId = race.getId();
			// das sind alle Teams, die für das Rennen gemeldet haben
			List<Team> teams = races.get(raceId);
			// die Teams werden nun auf die verfügbaren Startbahnen verteilt

			// upps.. hat niemand für dieses Rennen gemeldet?
			if (teams == null)
				continue;

			List<ProgramRace> programRaces = calculateHeats(numberOfLanes, race, teams);
			program.getRaces().addAll(programRaces);
		}

		common.save(program);
	}

	private List<ProgramRace> calculateHeats(int numberOfLanes, Race race, List<Team> teams) {
		List<ProgramRace> programRaces = new ArrayList<>();

		int numberOfHeats = teams.size() / numberOfLanes;
		int remainder = teams.size() % numberOfLanes;

		// TODO was soll passieren, wenn weniger als ein halbes Rennen übrigbleibt?
		if (remainder < numberOfLanes / 2) {

		}

		for (int i = 0, teamOffset = 0; i < numberOfHeats; i++) {
			ProgramRace programRace = new ProgramRace();
			programRace.setRace(race);
			programRace.setNumber(i + 1);
			int endOffset = Math.min(teamOffset + numberOfLanes, teams.size() - 1);
			List<Team> participants = teams.subList(teamOffset, endOffset);
			int lane = 1;
			for (Team team : participants) {
				team.setLane(lane++);
			}
			programRace.setParticipants(participants);
			programRaces.add(programRace);
			teamOffset = endOffset;
		}

		return programRaces;
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
