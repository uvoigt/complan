package org.planner.business.program;

import java.util.List;

import org.planner.eo.Program;
import org.planner.eo.ProgramRace;
import org.planner.model.Change;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Check {

	protected Logger log = LoggerFactory.getLogger(getClass());

	public void execute(Program program, List<ProgramRace> races, List<Change> changes) {

		for (int i = 0; i < races.size(); i++) {
			ProgramRace race = races.get(i);
			executeOn(race, i, program, races, changes);
		}
	}

	protected abstract void executeOn(ProgramRace race, int offset, Program program, List<ProgramRace> races,
			List<Change> changes);
}
