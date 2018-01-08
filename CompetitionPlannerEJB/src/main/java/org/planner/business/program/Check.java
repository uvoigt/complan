package org.planner.business.program;

import java.util.List;

import org.planner.eo.Program;
import org.planner.eo.ProgramRace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Check {

	protected Logger log = LoggerFactory.getLogger(getClass());

	public List<Problem> execute(Program program, List<ProgramRace> races, boolean applyChanges) {

		List<Problem> problems = null;

		for (int i = 0; i < races.size(); i++) {
			ProgramRace race = races.get(i);
			problems = executeOn(race, i, program, problems);
		}

		return problems;
	}

	protected abstract List<Problem> executeOn(ProgramRace race, int offset, Program program, List<Problem> problems);

	protected void applyChanges(List<ProgramRace> races, List<Change> changes) {
		for (Change change : changes) {
			change.applyTo(races);
		}
	}
}
