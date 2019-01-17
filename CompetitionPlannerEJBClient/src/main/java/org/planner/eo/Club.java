package org.planner.eo;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

import org.planner.util.NLSBundle;
import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@NLSBundle("clubs")
public class Club extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Column(nullable = false, unique = true)
	@Visible(mandatory = true)
	private String name;

	@Column(length = 32)
	private String shortName; // wird z.B. in Programmen angezeigt

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@Visible(initial = false, depth = 2)
	private Address address;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortNameOrName() {
		return shortName != null ? shortName : name;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
}