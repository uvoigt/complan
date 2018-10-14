package org.planner.eo;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

@Entity
@Access(AccessType.FIELD)
public class Team implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	private int lane;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Club club;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "team_id")
	@OrderBy("pos")
	private List<TeamMember> members;

	public Team() {
	}

	public Team(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public int getLane() {
		return lane;
	}

	public void setLane(int lane) {
		this.lane = lane;
	}

	public Club getClub() {
		return club;
	}

	public void setClub(Club club) {
		this.club = club;
	}

	public List<TeamMember> getMembers() {
		return members;
	}

	public void setMembers(List<TeamMember> members) {
		this.members = members;
	}
}
