package org.planner.ui.util.converter;

import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.City;
import org.planner.remote.ServiceFacade;

@Named
public class CityConverter extends EnumConverter {

	@Inject
	private ServiceFacade service;

	public CityConverter() {
		super(City.class);
	}

	@Override
	protected ServiceFacade getService() {
		return service;
	}
}
