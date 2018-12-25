package org.planner.eo;

import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Transient;

import org.planner.util.NLSBundle;
import org.planner.util.Visibilities;
import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@NLSBundle("results")
public class Result extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@OneToOne
	@Visible
	@Visibilities({ @Visible(path = "race.announcement.name", order = 1),
			@Visible(path = "race.announcement.club.name", order = 2),
			@Visible(path = "race.announcement.startDate", order = 3),
			@Visible(path = "race.announcement.endDate", initial = false, order = 4) })
	private ProgramRace programRace;

	@Transient
	private Long programId;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "result_id")
	@OrderBy("position")
	private List<Placement> placements;

	public Result() {
	}

	public Result(Long programId, ProgramRace programRace, List<Placement> placements) {
		this.programId = programId;
		this.programRace = programRace;
		this.placements = placements;
	}

	public Long getProgramId() {
		return programId;
	}

	public ProgramRace getProgramRace() {
		return programRace;
	}

	public List<Placement> getPlacements() {
		return placements;
	}

	public void setPlacements(List<Placement> placements) {
		this.placements = placements;
	}
}
