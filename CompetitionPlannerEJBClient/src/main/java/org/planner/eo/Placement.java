package org.planner.eo;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;

import org.planner.eo.ProgramRaceTeam.Id;
import org.planner.model.ResultExtra;

@Entity
@Access(AccessType.FIELD)
public class Placement implements Serializable {

	private static final long serialVersionUID = 1L;

	@EmbeddedId
	private Id id;

	private int position;

	private Long time;

	private ResultExtra extra;

	@OneToOne
	@JoinColumns({ @JoinColumn(name = "programrace_id"), @JoinColumn(name = "team_id") })
	private ProgramRaceTeam team;

	public Placement() {
	}

	public Placement(ProgramRaceTeam team, Long time, ResultExtra extra) {
		this.id = new Id(team.getProgramRace().getId(), team.getTeam().getId());
		this.team = team;
		this.time = time;
		this.extra = extra;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public Long getTime() {
		return time;
	}

	public ResultExtra getExtra() {
		return extra;
	}

	public ProgramRaceTeam getTeam() {
		return team;
	}
}
