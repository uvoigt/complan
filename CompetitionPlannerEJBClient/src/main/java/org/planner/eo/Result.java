package org.planner.eo;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.planner.util.NLSBundle;
import org.planner.util.Visibilities;
import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@NLSBundle("results")
public class Result extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	public static List<Long> convertToEntityAttribute(String dbData) {
		String[] strings = dbData.split(",");
		List<Long> result = new ArrayList<>(strings.length);
		for (String string : strings) {
			result.add(Long.valueOf(string));
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
	private transient List<Long> placementList;

	public Result() {
	}

	public Result(Long programId, ProgramRace programRace, List<Long> placementList) {
		this.programId = programId;
		this.programRace = programRace;
		placements = convertToDatabaseColumn(placementList);
	}

	/*
	 * schon klar, dass man das mit einem @Convert machen kann, aber jpamodelgen generiert einen Attribut-Typ, der zur
	 * Laufzeit mit einem Error im Log quittiert wird https://hibernate.atlassian.net/browse/HHH-11803
	 */
	private String convertToDatabaseColumn(List<Long> attribute) {
		return StringUtils.join(attribute, ",");
	}

	public Long getProgramId() {
		return programId;
	}

	public ProgramRace getProgramRace() {
		return programRace;
	}

	public List<Long> getPlacements() {
		// TODO diese Liste muss vlt. gewarppt werden
		if (placementList == null)
			placementList = convertToEntityAttribute(placements);
		return placementList;
	}
}
