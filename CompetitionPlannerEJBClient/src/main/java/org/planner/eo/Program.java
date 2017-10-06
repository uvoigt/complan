package org.planner.eo;

import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;

import org.planner.util.NLSBundle;
import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@NLSBundle("programs")
public class Program extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Visible
	@OneToOne
	private Announcement announcement;

	// entweder so oder als @ElementCollection (Map<String, Object>
	@Embedded
	private ProgramOptions options;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "program_id")
	@OrderBy("startTime")
	private Set<ProgramRace> races;

	@Lob
	private String text;

	public Announcement getAnnouncement() {
		return announcement;
	}

	public void setAnnouncement(Announcement announcement) {
		this.announcement = announcement;
	}

	public ProgramOptions getOptions() {
		return options;
	}

	public void setOptions(ProgramOptions options) {
		this.options = options;
	}

	public Set<ProgramRace> getRaces() {
		return races;
	}

	public void setRaces(Set<ProgramRace> races) {
		this.races = races;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
