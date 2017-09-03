package org.planner.ui.util.converter;

import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.Country;
import org.planner.remote.ServiceFacade;

@Named
public class CountryConverter extends EnumConverter {

	@Inject
	private ServiceFacade service;

	public CountryConverter() {
		super(Country.class);
	}

	@Override
	protected ServiceFacade getService() {
		return service;
	}
}
