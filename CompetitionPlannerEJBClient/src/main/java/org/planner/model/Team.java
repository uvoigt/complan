package org.planner.model;

import java.util.List;

public class Team {

	private String name;
	private List<Person> members;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Person> getMembers() {
		return members;
	}
	public void setMembers(List<Person> members) {
		this.members = members;
	}
}
