package org.planner.eo;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.xml.bind.annotation.XmlRootElement;

import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@XmlRootElement
public class Address extends AbstractEntity {
	private static final long serialVersionUID = 1L;

	@ManyToOne(optional = false)
	private Country country;

	@ManyToOne(optional = false)
	@Visible
	private City city;

	@Column(nullable = false, length = 10)
	@Visible(initial = false)
	private String postCode;

	@Column(nullable = false)
	@Visible(initial = false)
	private String street;

	@Column(nullable = false, length = 10)
	@Visible(initial = false)
	private String number;

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

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
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
