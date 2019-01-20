package org.planner.eo;

import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

@Entity
@Access(AccessType.FIELD)
@NamedQuery(name = "upcomingRegistrations", query = "select re from RegEntry re join re.registration r join r.announcement a " //
		+ "join re.participants p where a.startDate >= :today and p.user.userId = :userId order by a.startDate")
public class RegEntry extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne
	private Registration registration;

	@ManyToOne
	private Race race;

	// beinhaltet ebenfalls die Ersatz-Besetzungen
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "entry_id")
	@OrderBy("pos")
	private List<Participant> participants;

	public Registration getRegistration() {
		return registration;
	}

	public Race getRace() {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	public List<Participant> getParticipants() {
		return participants;
	}

	public void setParticipants(List<Participant> participants) {
		this.participants = participants;
	}
}