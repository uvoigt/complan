package org.planner.eo;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Entity
@Access(AccessType.FIELD)
public class Location extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private Address address = new Address();

	@ManyToOne
	private Club club;

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Club getClub() {
		return club;
	}

	public void setClub(Club club) {
		this.club = club;
	}
}
