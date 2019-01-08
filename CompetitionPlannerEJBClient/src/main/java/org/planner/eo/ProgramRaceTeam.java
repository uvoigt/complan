package org.planner.eo;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Mapping zwischen {@link ProgramRace} und {@link Team}, das nur auf diese Art existiert, um zusätzliche Properties, in
 * dem Fall {@link #lane} hinzuzufügen.
 */
@Entity
@Table(name = "ProgramRace_Team")
@Access(AccessType.FIELD)
public class ProgramRaceTeam implements Serializable {

	@Embeddable
	@Access(AccessType.FIELD)
	public static class Id implements Serializable {

		private static final long serialVersionUID = 1L;

		@Column(name = "programrace_id")
		private Long programRaceId;

		@Column(name = "team_id")
		private Long teamId;

		public Id() {
		}

		public Id(Long programRaceId, Long teamId) {
			this.programRaceId = programRaceId;
			this.teamId = teamId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((programRaceId == null) ? 0 : programRaceId.hashCode());
			result = prime * result + ((teamId == null) ? 0 : teamId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Id other = (Id) obj;
			if (programRaceId == null) {
				if (other.programRaceId != null)
					return false;
			} else if (!programRaceId.equals(other.programRaceId))
				return false;
			if (teamId == null) {
				if (other.teamId != null)
					return false;
			} else if (!teamId.equals(other.teamId))
				return false;
			return true;
		}
	}

	private static final long serialVersionUID = 1L;

	@EmbeddedId
	private Id id;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@MapsId("programRaceId")
	private ProgramRace programRace;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@MapsId("teamId")
	private Team team;

	private int lane;

	// Dient lediglich dem Erzeugen eines Foreign Keys
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumns({ @JoinColumn(name = "programrace_id"), @JoinColumn(name = "team_id") })
	private List<Placement> placements;

	public ProgramRaceTeam() {
	}

	public ProgramRaceTeam(ProgramRace programRace, Team team) {
		this.programRace = programRace;
		this.team = team;
		this.id = new Id(programRace.getId(), team.getId());
	}

	public Id getId() {
		return id;
	}

	public ProgramRace getProgramRace() {
		return programRace;
	}

	public Team getTeam() {
		return team;
	}

	public Long getTeamId() {
		return team.getId();
	}

	public List<TeamMember> getMembers() {
		return team.getMembers();
	}

	public Club getClub() {
		return team.getClub();
	}

	public int getLane() {
		return lane;
	}

	public void setLane(int lane) {
		this.lane = lane;
	}
}
