package org.planner.business.program;

public class Problem {
	private int index;
	private String text;

	public Problem(int index, String text) {
		this.index = index;
		this.text = text;
	}

	public int getIndex() {
		return index;
	}

	public String getText() {
		return text;
	}

	@Override
	public String toString() {
		return text;
	}
}
