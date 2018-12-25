package org.planner.eo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.planner.model.LocalizedEnum;
import org.planner.util.CommonMessages;

@Entity
@Access(AccessType.FIELD)
public class ProgramRace extends HasHeatMode implements Serializable {

	public enum RaceType implements LocalizedEnum {
		heat, semiFinal, finalA, finalB;

		@Override
		public String getText() {
			return CommonMessages.getEnumText(this);
		}

		public boolean isFinal() {
			return this == finalA || this == finalB;
		}
	}

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	@OneToOne
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

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "programRace")
	private Result result;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}
}
