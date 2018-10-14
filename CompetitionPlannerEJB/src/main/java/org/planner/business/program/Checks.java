package org.planner.business.program;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.planner.eo.Program;
import org.planner.eo.ProgramRace;
import org.planner.model.Change;

@ApplicationScoped
public class Checks extends Check {

	private List<Check> checks = new ArrayList<>();

	@PostConstruct
	public void init() {
		checks.add(new BoatClassCheck());
		checks.add(new AgeTypeCheck());
		checks.add(new FinalsAfterHeatsCheck());
	}

	@Override
	public void execute(Program program, List<ProgramRace> races, List<Change> changes) {
		for (Check check : checks) {
			check.execute(program, races, changes);
		}
	}

	@Override
	protected void executeOn(ProgramRace race, int offset, Program program, List<ProgramRace> races,
			List<Change> changes) {
	}
}
