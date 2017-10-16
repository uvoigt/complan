package org.planner.ui.util.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Named;

import org.planner.model.Gender;

@Named
public class BooleanGenderConverter implements Converter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		if ("false".equals(value))
			return Gender.m;
		if ("true".equals(value))
			return Gender.f;
		throw new IllegalArgumentException(value);
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value == Gender.m)
			return "false";
		if (value == Gender.f)
			return "true";
		return null;
	}
}
