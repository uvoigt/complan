package org.planner.ui.beans.masterdata;

import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.planner.eo.Address;
import org.planner.eo.City;
import org.planner.eo.City_;
import org.planner.eo.Country;
import org.planner.eo.Country_;
import org.planner.ui.beans.AbstractEditBean;

@Named
@SessionScoped
public class AddressBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Address address = new Address();

	// es ist einfacher, die komplexe Handhabung von JSF hier zu steuern als im
	// EO
	private Country country;

	private City city;

	public Address newAddress() {
		return new Address();
	}

	@Override
	public void setItem(Object item) {
		address = (Address) item;
	}

	public Address getAddress() {
		return address;
	}

	public Country getCountry() {
		return country;
	}

	public City getCity() {
		return city;
	}

	@Override
	protected void doSave() {
		service.saveAddress(address);
	}

	public List<Country> getCountries(String text) {
		return service.search(Country.class, createAutocompleteCriteria(text, Country_.name.getName())).getListe();
	}

	public List<City> getCities(String text) {
		return service.search(City.class, createAutocompleteCriteria(text, City_.name.getName())).getListe();
	}
}
