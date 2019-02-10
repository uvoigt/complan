package org.planner.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;
import org.planner.eo.Announcement;
import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Placement;
import org.planner.eo.Program;
import org.planner.eo.Program.ProgramStatus;
import org.planner.eo.ProgramRace;
import org.planner.eo.ProgramRaceTeam;
import org.planner.eo.Race;
import org.planner.eo.Registration;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Result;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Gender;
import org.planner.model.RaceType;
import org.planner.util.LogUtil.FachlicheException;

public class ProgramTest extends BaseTest {

	@Inject
	private AnnouncementServiceImpl announcementService;

	@Inject
	private ProgramServiceImpl programService;

	private static Long programId;

	@Test
	@InSequence(10)
	public void createProgram() {
		Announcement announcement = TestUtils.createAnnouncement(getEm(), getCallingUser().getClub(),
				AnnouncementStatus.announced, true);

		setTestRoles("Sportwart");
		programId = programService.createProgram(announcement.getId());
	}

	@Test
	@InSequence(20)
	public void failGenerateProgramNoRegistrations() {
		thrown.expect(FachlicheException.class);
		thrown.expectMessage(messages.getMessage("program.noRegistrations"));

		Program program = getEm().find(Program.class, programId);
		program.getAnnouncement().getCategory(); // fetch

		flushAndClear();
		setTestRoles("Sportwart");
		programService.generateProgram(program);
	}

	@Test
	@InSequence(30)
	public void generateProgram() {
		Program program = getEm().find(Program.class, programId);
		program.getAnnouncement().getCategory(); // fetch

		Registration registration = new Registration();
		registration.setStatus(RegistrationStatus.created);
		registration.setAnnouncement(program.getAnnouncement());
		registration.setClub(getCallingUser().getClub());
		getEm().persist(registration);

		TestUtils.createRace(getEm(), program.getAnnouncement(), 1, AgeType.junioren, BoatClass.k1, Gender.m, 500,
				false);
		Race race = TestUtils.createRace(getEm(), program.getAnnouncement(), 2, AgeType.junioren, BoatClass.k2,
				Gender.m, 500, false);
		TestUtils.createRace(getEm(), program.getAnnouncement(), 3, AgeType.junioren, BoatClass.k4, Gender.m, 500,
				false);
		commitTransaction();
		beginTransaction();
		flushAndClear();

		for (int i = 0; i < 28; i++)
			TestUtils.createEntry(getEm(), registration, race, 2, true);
		commitTransaction();
		beginTransaction();
		clear();

		setTestRoles("Sportwart");
		announcementService.setRegistrationStatus(registration.getId(), RegistrationStatus.submitted);
		programService.generateProgram(program);
	}

	@Test
	@InSequence(40)
	public void failSetProgramStatusWrongStatus() {
		thrown.expect(FachlicheException.class);
		thrown.expectMessage(messages.getMessage("accessSetStatus"));
		setTestRoles("Sportwart");
		programService.setProgramStatus(programId, ProgramStatus.created);
	}

	@Test
	@InSequence(40)
	public void setProgramStatusRunning() {
		setTestRoles("Sportwart");
		programService.setProgramStatus(programId, ProgramStatus.running);
	}

	@Test
	@InSequence(45)
	public void getProgram() {
		programService.getProgram(programId, true, true);
	}

	@Test
	@InSequence(50)
	public void getPlacementsEmpty() {
		setTestRoles("Sportwart");
		List<Placement> placements = programService.getPlacements(programId);
		assertNotNull(placements);
		assertEquals(0, placements.size());
	}

	private ProgramRace getRace(Program program, int raceNumber, int programRaceNumber) {
		for (ProgramRace race : program.getRaces()) {
			if (race.getRace().getNumber() == raceNumber && race.getNumber() == programRaceNumber)
				return race;
		}
		return null;
	}

	@Test
	@InSequence(60)
	public void saveResultAllHeats() {
		// für dieses Rennen wurde in generateProgram gemeldet
		List<ProgramRace> result = saveResultHeat(2, 1);
		assertNull(result);
		result = saveResultHeat(2, 2);
		assertNull(result);
		result = saveResultHeat(2, 3);
		assertNull(result);
		result = saveResultHeat(2, 4);
		assertNotNull(result);
		assertEquals(3, result.size());
		for (ProgramRace semi : result) {
			assertEquals(RaceType.semiFinal, semi.getRaceType());
		}
	}

	private List<ProgramRace> saveResultHeat(int raceNumber, int programRaceNumber) {
		Program program = getEm().find(Program.class, programId);
		ProgramRace race = getRace(program, raceNumber, programRaceNumber);
		List<Placement> placements = new ArrayList<>();
		List<ProgramRaceTeam> participants = race.getParticipants();
		List<Long> times = new ArrayList<>();
		for (ProgramRaceTeam team : participants) {
			long time = (long) ((Math.random() * 200) + 1);
			placements.add(new Placement(team, time, null));
			times.add(time);
		}
		Collections.sort(times);

		flushAndClear();
		setTestRoles("Sportwart");
		List<ProgramRace> result = programService.saveResult(race.getId(), placements);

		getEm().clear();
		program = getEm().find(Program.class, programId);
		List<Placement> storedPlacements = getRace(program, raceNumber, programRaceNumber).getPlacements();
		assertNotNull(storedPlacements);
		assertEquals(times.size(), storedPlacements.size());
		for (int i = 0; i < times.size(); i++) {
			assertEquals(times.get(i), storedPlacements.get(i).getTime());
		}
		getEm().clear();
		return result;
	}

	@Test
	@InSequence(70)
	public void deleteResult() {
		common.delete(Result.class, 37L);
	}

	@Test
	@InSequence(80)
	public void failDeleteProgramWrongStatus() {
		thrown.expect(FachlicheException.class);
		thrown.expectMessage(messages.getMessage("program.alreadyRunning"));
		setTestRoles("Sportwart");
		programService.deleteProgram(programId);
	}

	@Test
	@InSequence(90)
	public void setProgramStatusCreated() {
		setTestRoles("Sportwart", "Tester");
		programService.setProgramStatus(programId, ProgramStatus.created);
	}

	@Test
	@InSequence(100)
	public void deleteProgram() {
		setTestRoles("Sportwart");
		programService.deleteProgram(programId);
	}
}
