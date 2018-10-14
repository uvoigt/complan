package org.planner.business.program;

import java.util.List;

import org.planner.model.Change;

public class Changes {

	private List<Change> changes;

	public void addChange(Change change) {
		changes.add(change);
	}

	public void apply() {

	}

	public void undo() {

	}
}
