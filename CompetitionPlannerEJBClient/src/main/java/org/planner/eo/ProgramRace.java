package org.planner.eo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Transient;

import org.planner.model.RaceType;

@Entity
@Access(AccessType.FIELD)
public class ProgramRace extends HasId {

	private static final long serialVersionUID = 1L;

	@OneToOne
	@JoinColumn(nullable = false)
	private Race race;

	// ist ein DateTime, um den Tag zu identifizieren
	private Date startTime;

	private RaceType raceType;

	// z.B. Vorlauf 1, 2 oder Finale A oder B
	private Integer number;

	@OneToMany(mappedBy = "programRace", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProgramRaceTeam> participants;

	@Column(name = "program_id", insertable = false, updatable = false)
	private Long programId;

	@Transient
	private ProgramRace followUpRace;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "programrace_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	@OrderBy("position")
	private List<Placement> placements;

	// ;-separierter String
	// <Platzierungen ins Finale>;<Platzierungen in Zwischenlauf>
	// Bsp.: 1;2 bedeutet Platz 1 ins Finale, Platz 2 - 3 ins Semifinale
	// Bsp.: 2;2 bedeutet Platz 1 - 2 ins Finale, Platz 3 - 4 ins Semifinale
	// Bsp.: 3 bedeutet Platz 1 - 3 ins Finale, keine Semifinale
	// Bsp.: ;3 bedeutet Platz 1 - 3 ins Semifinale, keiner direkt ins Finale
	private String heatMode;

	@Transient
	private transient Integer intoFinal;
	@Transient
	private transient Integer intoSemiFinal;

	public List<Placement> getPlacements() {
		return placements;
	}

	public void setPlacements(List<Placement> placements) {
		if (this.placements == null)
			this.placements = new ArrayList<>();
		else
			this.placements.clear();
		this.placements.addAll(placements);
	}

	public Race getRace() {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public RaceType getRaceType() {
		return raceType;
	}

	public void setRaceType(RaceType raceType) {
		this.raceType = raceType;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public List<ProgramRaceTeam> getParticipants() {
		return participants;
	}

	public void addParticipant(ProgramRaceTeam participant) {
		if (participants == null)
			participants = new ArrayList<>();
		participants.add(participant);
	}

	public Long getProgramId() {
		return programId;
	}

	public ProgramRace getFollowUpRace() {
		return followUpRace;
	}

	public void setFollowUpRace(ProgramRace followUpRace) {
		this.followUpRace = followUpRace;
	}

	private void parseHeatMode() {
		if (heatMode != null) {
			String[] split = heatMode.split(";");
			intoFinal = Integer.parseInt(split[0]);
			intoSemiFinal = Integer.parseInt(split[1]);
		} else {
			intoFinal = 0;
			intoSemiFinal = 0;
		}
	}

	private void updateHeatMode() {
		heatMode = intoFinal + ";" + intoSemiFinal;
	}

	// für das Setzen aus einer nativen Query
	public void setHeatMode(String heatMode) {
		this.heatMode = heatMode;
	}

	public int getIntoFinal() {
		if (intoFinal == null)
			parseHeatMode();
		return intoFinal;
	}

	public void setIntoFinal(int intoFinal) {
		this.intoFinal = intoFinal;
		updateHeatMode();
	}

	public int getIntoSemiFinal() {
		if (intoSemiFinal == null)
			parseHeatMode();
		return intoSemiFinal;
	}

	public void setIntoSemiFinal(int intoSemiFinal) {
		this.intoSemiFinal = intoSemiFinal;
		updateHeatMode();
	}
}
