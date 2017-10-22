package org.planner.ui.beans.masterdata;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.planner.eo.Address;
import org.planner.eo.City;
import org.planner.eo.City_;
import org.planner.eo.Country;
import org.planner.eo.Country_;
import org.planner.remote.ServiceFacade;
import org.planner.ui.beans.AbstractEditBean;

@Named
@RequestScoped
public class AddressBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private ServiceFacade service;

	private Address address = new Address();

	// es ist einfacher, die komplexe Handhabung von JSF hier zu steuern als im
	// EO
	private Country country;

	private City city;

	// binding
	private UIInput uiCountry;
	private UIInput uiCity;
	private UIInput uiPostCode;
	private UIInput uiStreet;

	public Address getAddress() {
		return address;
	}

	public Country getCountry() {
		return country;
	}

	public City getCity() {
		return city;
	}

	public boolean isRequired() {
		if (FacesContext.getCurrentInstance().getCurrentPhaseId() != PhaseId.PROCESS_VALIDATIONS)
			return false;
		String submittedCountry = (String) uiCountry.getSubmittedValue();
		String submittedCity = (String) uiCity.getSubmittedValue();
		String submittedPostCode = (String) uiPostCode.getSubmittedValue();
		String submittedStreet = (String) uiStreet.getSubmittedValue();
		Country countryValue = (Country) uiCountry.getValue();
		City cityValue = (City) uiCity.getValue();
		String postCodeValue = (String) uiPostCode.getValue();
		String streetValue = (String) uiStreet.getSubmittedValue();
		boolean required = StringUtils.isNotEmpty(submittedCountry) || countryValue != null
				|| StringUtils.isNotEmpty(submittedCity) || cityValue != null
				|| StringUtils.isNotEmpty(submittedPostCode) || StringUtils.isNotEmpty(postCodeValue)
				|| StringUtils.isNotEmpty(submittedStreet) || StringUtils.isNotEmpty(streetValue);
		return required;
	}

	public List<Country> getCountries(String text) {
		return service.search(Country.class, AbstractEditBean.createAutocompleteCriteria(text, Country_.name.getName()))
				.getListe();
	}

	public List<City> getCities(String text) {
		return service.search(City.class, AbstractEditBean.createAutocompleteCriteria(text, City_.name.getName()))
				.getListe();
	}

	public UIInput getUiCountry() {
		return uiCountry;
	}

	public void setUiCountry(UIInput uiCountry) {
		this.uiCountry = uiCountry;
	}

	public UIInput getUiCity() {
		return uiCity;
	}

	public void setUiCity(UIInput uiCity) {
		this.uiCity = uiCity;
	}

	public UIInput getUiPostCode() {
		return uiPostCode;
	}

	public void setUiPostCode(UIInput uiPostCode) {
		this.uiPostCode = uiPostCode;
	}

	public UIInput getUiStreet() {
		return uiStreet;
	}

	public void setUiStreet(UIInput uiStreet) {
		this.uiStreet = uiStreet;
	}
}
