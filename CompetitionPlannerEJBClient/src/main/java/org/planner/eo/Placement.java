package org.planner.eo;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

import org.planner.eo.ProgramRace.RaceType;
import org.planner.eo.ProgramRaceTeam.Id;
import org.planner.model.ResultExtra;

@Entity
@Access(AccessType.FIELD)
@NamedQuery(name = "latestResults", query = "select p, pr, r from Placement p join p.team prt join prt.programRace pr " //
		+ "join pr.race r join fetch r.announcement a join prt.team t join t.members m join m.user u " //
		+ "where a.startDate > :date and u.userId = :userId order by a.startDate")
public class Placement implements Serializable {

	private static final long serialVersionUID = 1L;

	@EmbeddedId
	private Id id;

	private int position;

	private Long time;

	private ResultExtra extra;

	private RaceType qualifiedFor;

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

	public RaceType getQualifiedFor() {
		return qualifiedFor;
	}

	public void setQualifiedFor(RaceType qualifiedFor) {
		this.qualifiedFor = qualifiedFor;
	}

	public ProgramRaceTeam getTeam() {
		return team;
	}
}
