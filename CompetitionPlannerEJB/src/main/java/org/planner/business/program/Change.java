package org.planner.business.program;

import java.util.List;

import org.planner.eo.ProgramRace;

public abstract class Change {

	public abstract void applyTo(List<ProgramRace> races);

	// public abstract
}