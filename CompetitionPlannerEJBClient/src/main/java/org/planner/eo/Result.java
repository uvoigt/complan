package org.planner.eo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.planner.model.ResultExtra;
import org.planner.util.NLSBundle;
import org.planner.util.Visibilities;
import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@NLSBundle("results")
public class Result extends AbstractEntity {

	public static class Placement implements Serializable {

		private static final long serialVersionUID = 1L;

		private ResultExtra extra;

		private ProgramRaceTeam team;

		public Placement(ProgramRaceTeam team) {
			this.team = team;
		}

		public Placement(String format) {
			String[] split = format.split(";");
			if (split.length == 2) {
				team = new ProgramRaceTeam(new ProgramRace(), new Team(Long.valueOf(split[0])));
				extra = ResultExtra.valueOf(split[1]);
			} else {
				team = new ProgramRaceTeam(new ProgramRace(), new Team(Long.valueOf(format)));
			}
		}

		public ResultExtra getExtra() {
			return extra;
		}

		public void setExtra(ResultExtra extra) {
			this.extra = extra;
		}

		public ProgramRaceTeam getTeam() {
			return team;
		}

		public void setTeam(ProgramRaceTeam team) {
			this.team = team;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}

		public void toString(StringBuilder sb) {
			sb.append(team.getTeamId());
			if (extra != null) {
				sb.append(";");
				sb.append(extra);
			}
		}
	}

	private static final long serialVersionUID = 1L;

	public static List<Placement> convertToEntityAttribute(String dbData) {
		String[] strings = dbData.split(",");
		List<Placement> result = new ArrayList<>(strings.length);
		for (String string : strings) {
			result.add(new Placement(string));
		}
		return result;
	}

	@OneToOne
	@Visible
	@Visibilities({ @Visible(path = "race.announcement.name", order = 1),
			@Visible(path = "race.announcement.club.name", order = 2),
			@Visible(path = "race.announcement.startDate", order = 3),
			@Visible(path = "race.announcement.endDate", initial = false, order = 4) })
	private ProgramRace programRace;

	private String placements;

	@Transient
	private Long programId;

	@Transient
	private transient List<Placement> placementList;

	public Result() {
	}

	public Result(Long programId, ProgramRace programRace, List<Placement> placementList) {
		this.programId = programId;
		this.programRace = programRace;
		placements = convertToDatabaseColumn(placementList);
	}

	/*
	 * schon klar, dass man das mit einem @Convert machen kann, aber jpamodelgen generiert einen Attribut-Typ, der zur
	 * Laufzeit mit einem Error im Log quittiert wird https://hibernate.atlassian.net/browse/HHH-11803
	 */
	private String convertToDatabaseColumn(List<Placement> attribute) {
		return StringUtils.join(attribute, ",");
	}

	public Long getProgramId() {
		return programId;
	}

	public ProgramRace getProgramRace() {
		return programRace;
	}

	public List<Placement> getPlacements() {
		// TODO diese Liste muss vlt. gewrappt werden
		if (placementList == null)
			placementList = convertToEntityAttribute(placements);
		return placementList;
	}
}
