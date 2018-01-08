package org.planner.business.program;

import java.util.List;

import org.planner.eo.ProgramRace;

public class SwapChange extends Change {
	private int from;
	private int to;

	public SwapChange(int from, int to) {
		this.from = from;
		this.to = to;
	}

	@Override
	public void applyTo(List<ProgramRace> races) {
		ProgramRace race1 = races.get(from);
		races.set(from, races.get(to));
		races.set(to, race1);
	}
}