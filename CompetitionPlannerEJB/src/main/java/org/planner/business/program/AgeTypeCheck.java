package org.planner.business.program;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.planner.eo.Program;
import org.planner.eo.ProgramRace;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Change;
import org.planner.model.Gender;

public class AgeTypeCheck extends Check {

	// prüft, ob ein Rennen der gleichen Altersklasse von den Startern erreicht werden kann

	@Override
	protected void executeOn(ProgramRace race, int offset, Program program, List<ProgramRace> races,
			List<Change> changes) {

		// check 1: die Rennen gleicher Alters- Bootsklassen und Geschlecht (mixed berücksichtigt)
		// müssen einen Mindestabstand haben
		// ein Rennen wird zuerst als Ganzes betrachtet, d.h. nicht die einzelnen Vorläufe
		int minimumDistance = 60;
		ProgramRace nextRace = findNextConflictingRace(program, offset, race);
		if (nextRace != null) {
			int distance = getTimeDistanceInMinutes(race, nextRace);
			if (distance > 0 && distance < minimumDistance) {
				String text = "Der zeitliche Abstand der Rennen " + race.getRace().getNumber() + " und "
						+ nextRace.getRace().getNumber() + " ist mit " + distance + " min zu klein...";
				if (log.isDebugEnabled())
					log.debug(text);
				// zwei Möglichkeiten:
				// das erste Rennen nach vorn tauschen
				// das zweite Rennen nach hinten tauschen
				// evtl. beides

				// es werden generell nur Changes ausgeführt, die zu einer sofortigen Verbesserung führen
				int minNewOffset = minimumDistance / program.getOptions().getTimeLag();
				if (offset - minNewOffset >= 0) {
					int newOffset = offset - minNewOffset;
					SwapChange change = new SwapChange(offset, newOffset, text);
					changes.add(change);
					change.applyTo(races);
					// if (applyChanges)
					// applyChanges(races, changes);
					// checkProgram(program, createCopy)
				}
			}
		}
	}

	private int getTimeDistanceInMinutes(ProgramRace race1, ProgramRace race2) {
		Minutes minutes = Minutes.minutesBetween(new DateTime(race1.getStartTime()),
				new DateTime(race2.getStartTime()));
		return minutes.getMinutes();
	}

	private ProgramRace findNextConflictingRace(Program program, int offset, ProgramRace race) {
		List<ProgramRace> races = program.getRaces();
		AgeType ageType = race.getRace().getAgeType();
		// aufgrund dieser Prüfung wäre vielleicht. die Trennung in Bootsgattung und Bootsklasse sinnvoll
		BoatClass boatClass = race.getRace().getBoatClass();
		boolean isKayak = boatClass.toString().startsWith("k");
		Gender gender = race.getRace().getGender();
		int raceNumber = race.getRace().getNumber();
		for (int i = offset + 1; i < races.size(); i++) {
			ProgramRace r = races.get(i);
			if (r.getRace().getNumber().equals(raceNumber))
				continue;
			if (r.getRace().getBoatClass().toString().startsWith("k") != isKayak)
				continue;
			if (r.getRace().getAgeType() != ageType)
				continue;
			if (gender != Gender.mixed && r.getRace().getGender() != gender)
				continue;
			// TODO mixed
			return r;
		}
		return null;
	}
}
