package org.planner.eo;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
public class Address extends HasId {

	private static final long serialVersionUID = 1L;

	@Version
	private int version;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Country country;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Visible
	private City city;

	@Column(nullable = false, length = 10)
	@Visible(initial = false, order = 1)
	private String postCode;

	@Column(nullable = false)
	@Visible(initial = false, order = 2)
	private String street;

	private String addition;

	private String homepage;

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	public City getCity() {
		return city;
	}

	public void setCity(City city) {
		this.city = city;
	}

	public String getPostCode() {
		return postCode;
	}

	public void setPostCode(String postCode) {
		this.postCode = postCode;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getAddition() {
		return addition;
	}

	public void setAddition(String addition) {
		this.addition = addition;
	}

	public String getHomepage() {
		return homepage;
	}

	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}
}
