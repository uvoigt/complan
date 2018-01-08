package org.planner.business.program;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.planner.eo.Program;
import org.planner.eo.ProgramRace;

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
	public List<Problem> execute(Program program, List<ProgramRace> races, boolean applyChanges) {
		List<Problem> overallResult = new ArrayList<>();
		for (Check check : checks) {
			List<Problem> result = check.execute(program, races, applyChanges);
			if (result != null)
				overallResult.addAll(result);
		}
		return overallResult;
	}

	@Override
	protected List<Problem> executeOn(ProgramRace race, int offset, Program program, List<Problem> problems) {
		return null;
	}
}
