package org.planner.eo;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

import org.planner.util.NLSBundle;
import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@XmlRootElement
@NLSBundle("announcements")
public class Announcement extends AbstractEntity {

	public static enum Status {
		created, announced, finalized
	}

	private static final long serialVersionUID = 1L;

	// competition
	@Column(nullable = false)
	@Visible
	private String name;

	// Ort und Zeitangabe der Wettkämpfe
	@Column(nullable = false)
	@Temporal(TemporalType.DATE)
	@Visible(initial = false)
	private Date startDate;

	@Column(nullable = false)
	@Temporal(TemporalType.DATE)
	@Visible(initial = false)
	private Date endDate;

	@ManyToOne(optional = false)
	private Location location = new Location();

	// Kategorie des Wettkampfes
	@ManyToOne(optional = false)
	private Category category;
	// competition end

	// Reihenfolge und Startzeiten der Rennen mit Angaben der Streckenlänge,
	// Bootsgattungen, Bootsklassen- und Altersklassen
	@OneToMany(mappedBy = "announcement")
	private Set<Race> races;

	// Abmessungen der Regattabahn und Wassertiefen
	private String courseSize;
	private String waterDepth;

	// Anzahl der zur Verfügung stehenden Startbahnen bei Sprint-, Kurz- und
	// Mittelstrecken
	@Column(nullable = false)
	private Integer tracksSprint;
	@Column(nullable = false)
	private Integer tracksShort;
	@Column(nullable = false)
	private Integer tracksMedium;

	// Termin des Meldeschlusses
	@Column(nullable = false)
	@Temporal(TemporalType.DATE)
	@Visible(initial = false)
	private Date deadline;

	// den exakten Termin, zu dem Nachmeldungen spätestens beim Ausrichter und
	// beim Juryvorsitzenden eingegangen sein müssen
	@Column(nullable = false)
	private Date latestDeadline;

	// die Anschrift der Meldestelle
	@ManyToOne(optional = false)
	private Location announcer = new Location();

	// Orts-, Datums- und Zeitangabe der Meldeeröffnung und Startverlosung
	@Column(nullable = false)
	private Date opening;

	@ManyToOne(optional = false)
	private Location openingLocation = new Location();

	// Höhe der Meldegebühren
	@ElementCollection
	private Map<String, Float> fees;

	// die Adresse und elektronische Adresse des Juryvorsitzenden
	@ManyToOne(optional = false)
	private Location juryLocation = new Location();
	@Column(nullable = false)
	private String juryEmail;

	@Lob
	private String text;

	// @Column(nullable = false)
	private Status status;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getCourseSize() {
		return courseSize;
	}

	public void setCourseSize(String courseSize) {
		this.courseSize = courseSize;
	}

	public String getWaterDepth() {
		return waterDepth;
	}

	public void setWaterDepth(String waterDepth) {
		this.waterDepth = waterDepth;
	}

	public Integer getTracksSprint() {
		return tracksSprint;
	}

	public void setTracksSprint(Integer tracksSprint) {
		this.tracksSprint = tracksSprint;
	}

	public Integer getTracksShort() {
		return tracksShort;
	}

	public void setTracksShort(Integer tracksShort) {
		this.tracksShort = tracksShort;
	}

	public Integer getTracksMedium() {
		return tracksMedium;
	}

	public void setTracksMedium(Integer tracksMedium) {
		this.tracksMedium = tracksMedium;
	}

	public Date getDeadline() {
		return deadline;
	}

	public void setDeadline(Date deadline) {
		this.deadline = deadline;
	}

	public Date getLatestDeadline() {
		return latestDeadline;
	}

	public void setLatestDeadline(Date latestDeadline) {
		this.latestDeadline = latestDeadline;
	}

	public Location getAnnouncer() {
		return announcer;
	}

	public void setAnnouncer(Location announcer) {
		this.announcer = announcer;
	}

	public Date getOpening() {
		return opening;
	}

	public void setOpening(Date opening) {
		this.opening = opening;
	}

	public Location getOpeningLocation() {
		return openingLocation;
	}

	public void setOpeningLocation(Location openingLocation) {
		this.openingLocation = openingLocation;
	}

	public Map<String, Float> getFees() {
		return fees;
	}

	public Location getJuryLocation() {
		return juryLocation;
	}

	public void setJuryLocation(Location juryLocation) {
		this.juryLocation = juryLocation;
	}

	public String getJuryEmail() {
		return juryEmail;
	}

	public void setJuryEmail(String juryEmail) {
		this.juryEmail = juryEmail;
	}

	public Set<Race> getRaces() {
		return races;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}
