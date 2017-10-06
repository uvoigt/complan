package org.planner.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.planner.business.CommonImpl.Operation;
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

	public Long createProgram(Program program) {
		// gibt es bereits ein Programm für diese Ausschreibung? dann nimm dieses
		Suchkriterien krit = new Suchkriterien();
		krit.addFilter(Program_.announcement.getName(), program.getAnnouncement().getId());
		List<Program> programs = common.search(Program.class, krit).getListe();
		// theoretisch können mehrere existieren, wenn zwei User wirklich
		// gleichzeitig speichern
		if (programs.size() > 0)
			return programs.get(0).getId();

		Announcement announcement = common.getById(Announcement.class, program.getAnnouncement().getId(), 0);
		common.checkWriteAccess(program, Operation.create); // TODO

		// kopiere sämtliche Rennen der Ausschreibung
		Set<ProgramRace> races = new HashSet<>();
		for (Race race : announcement.getRaces()) {
			ProgramRace copiedRace = new ProgramRace();
			copiedRace.setRace(race);
			races.add(copiedRace);
		}
		program.setRaces(races);

		common.save(program);

		return program.getId();
	}

	public Program generateProgram(Program program) {
		common.checkWriteAccess(program, Operation.save);

		// lade alle Meldungen
		Suchkriterien crit = new Suchkriterien();
		crit.addFilter(Registration_.announcement.getName(), program.getAnnouncement().getId());
		crit.addFilter(Registration_.status.getName(), RegistrationStatus.submitted);
		List<Registration> registrations = common.search(Registration.class, crit).getListe();
		Map<Long, Set<Team>> races = new HashMap<>();

		// hier werden erst einmal alle Meldungen der Map hinzugefügt
		// am Ende ->
		for (Registration registration : registrations) {
			for (RegEntry entry : registration.getEntries()) {
				addEntryToProgram(registration.getClub(), entry, races);
			}
		}
		// simples Hinzufügen eines Laufs ... das wird erweitert!
		for (ProgramRace programRace : program.getRaces()) {
			Long raceId = programRace.getRace().getId();
			// das sind alle Teams, die für das Rennen gemeldet haben
			Set<Team> teams = races.get(raceId);
			programRace.setParticipants(teams);
		}

		return common.save(program);
	}

	private void addEntryToProgram(Club club, RegEntry entry, Map<Long, Set<Team>> races) {
		Race race = entry.getRace();
		Set<Team> teams = races.get(race.getId());
		if (teams == null)
			races.put(race.getId(), teams = new HashSet<>());
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
