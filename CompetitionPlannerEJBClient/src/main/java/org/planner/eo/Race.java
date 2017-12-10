package org.planner.eo;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Gender;

@Entity
@Access(AccessType.FIELD)
public class Race extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Column(nullable = false)
	private Integer number;

	private Integer day; // das ist ein offset, 0 ist der erste Tag usw.

	// soll ohne angelegt werden können und später verändert werden
	@Temporal(TemporalType.TIME)
	private Date startTime;

	private int distance;

	// @ManyToOne
	@Column(nullable = false)
	private BoatClass boatClass;
	// @ManyToOne
	@Column(nullable = false)
	private AgeType ageType;

	@Column(nullable = false)
	private Gender gender;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Announcement announcement;

	@Column(name = "announcement_id", insertable = false, updatable = false)
	private Long announcementId;

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public Integer getDay() {
		return day;
	}

	public void setDay(Integer day) {
		this.day = day;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public BoatClass getBoatClass() {
		return boatClass;
	}

	public void setBoatClass(BoatClass boatClass) {
		this.boatClass = boatClass;
	}

	public AgeType getAgeType() {
		return ageType;
	}

	public void setAgeType(AgeType ageType) {
		this.ageType = ageType;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Long getAnnouncementId() {
		return announcementId;
	}

	public void setAnnouncement(Announcement announcement) {
		this.announcement = announcement;
	}
}
