package org.planner.model;

import java.util.List;

public abstract class Change {

	private String text;

	protected Change(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public abstract <T> void applyTo(List<T> list);

}