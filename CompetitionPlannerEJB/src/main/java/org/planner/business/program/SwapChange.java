package org.planner.business.program;

import java.util.List;

import org.planner.model.Change;

public class SwapChange extends Change {
	private int from;
	private int to;

	public SwapChange(int from, int to, String text) {
		super(text);
		this.from = from;
		this.to = to;
	}

	@Override
	public <T> void applyTo(List<T> list) {
		T o1 = list.get(from);
		list.set(from, list.get(to));
		list.set(to, o1);
	}
}