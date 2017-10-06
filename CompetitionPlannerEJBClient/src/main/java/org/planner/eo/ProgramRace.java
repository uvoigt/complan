package org.planner.eo;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.planner.model.LocalizedEnum;
import org.planner.util.CommonMessages;

@Entity
@Access(AccessType.FIELD)
public class ProgramRace implements Serializable {

	public enum RaceType implements LocalizedEnum {
		heat, semiFinal, finalA, finalB;

		@Override
		public String getText() {
			return CommonMessages.getEnumText(this);
		}
	}

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	@OneToOne
	private Race race;

	@Temporal(TemporalType.TIME)
	private Date startTime;

	private RaceType raceType;

	// z.B. Vorlauf 1, 2 oder Finale A oder B
	private Integer number;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "programrace_id")
	private Set<Team> participants;

	public Long getId() {
		return id;
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

	public Set<Team> getParticipants() {
		return participants;
	}

	public void setParticipants(Set<Team> participants) {
		this.participants = participants;
	}
}
