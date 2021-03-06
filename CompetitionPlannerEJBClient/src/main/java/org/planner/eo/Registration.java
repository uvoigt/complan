package org.planner.eo;

import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.planner.model.LocalizedEnum;
import org.planner.util.CommonMessages;
import org.planner.util.NLSBundle;
import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@NLSBundle("registrations")
public class Registration extends AbstractEntity {
	public enum RegistrationStatus implements LocalizedEnum {
		created, submitted;

		@Override
		public String getText() {
			return CommonMessages.getEnumText(this);
		}
	}

	private static final long serialVersionUID = 1L;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Visible(initial = false, mandatory = true, order = 4)
	private Club club;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Visible(order = 1, mandatory = true) // mandatory, damit der Name der Ausschreibung angezeigt wird
	private Announcement announcement;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "registration_id")
	private List<RegEntry> entries;

	@Column(nullable = false)
	@Visible(order = 6, mandatory = true, initial = false)
	private RegistrationStatus status;

	public Club getClub() {
		return club;
	}

	public void setClub(Club club) {
		this.club = club;
	}

	public Announcement getAnnouncement() {
		return announcement;
	}

	public void setAnnouncement(Announcement announcement) {
		this.announcement = announcement;
	}

	public List<RegEntry> getEntries() {
		return entries;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public void setStatus(RegistrationStatus status) {
		this.status = status;
	}
}
