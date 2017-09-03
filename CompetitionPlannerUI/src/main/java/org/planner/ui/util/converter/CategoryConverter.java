package org.planner.ui.util.converter;

import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.Category;
import org.planner.remote.ServiceFacade;

@Named
public class CategoryConverter extends EnumConverter {

	@Inject
	private ServiceFacade service;

	public CategoryConverter() {
		super(Category.class);
	}

	@Override
	protected ServiceFacade getService() {
		return service;
	}
}
