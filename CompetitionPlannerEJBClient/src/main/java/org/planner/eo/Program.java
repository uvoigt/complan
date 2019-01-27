package org.planner.eo;

import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;

import org.planner.model.LocalizedEnum;
import org.planner.util.CommonMessages;
import org.planner.util.NLSBundle;
import org.planner.util.Visibilities;
import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@NLSBundle("programs")
public class Program extends AbstractEntity {

	public enum ProgramStatus implements LocalizedEnum {
		created, announced, running, finished;

		@Override
		public String getText() {
			return CommonMessages.getEnumText(this);
		}
	}

	private static final long serialVersionUID = 1L;

	@OneToOne(fetch = FetchType.LAZY)
	@Visible(order = 2)
	@Visibilities(@Visible(path = "club.name", order = 2, mandatory = true))
	private Announcement announcement;

	// entweder so oder als @ElementCollection (Map<String, Object>
	@Embedded
	private ProgramOptions options;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "program_id")
	@OrderBy("startTime")
	private List<ProgramRace> races;

	@Lob
	private String text;

	@Column(/* nullable = false */)
	@Visible(depth = 0, mandatory = true, order = 6, initial = false)
	private ProgramStatus status;

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

	public List<ProgramRace> getRaces() {
		return races;
	}

	public void setRaces(List<ProgramRace> races) {
		this.races = races;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public ProgramStatus getStatus() {
		return status;
	}

	public void setStatus(ProgramStatus status) {
		this.status = status;
	}
}
