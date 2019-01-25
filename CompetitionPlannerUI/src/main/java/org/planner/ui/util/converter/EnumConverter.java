package org.planner.ui.util.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.planner.eo.AbstractEnum;
import org.planner.remote.ServiceFacade;

public abstract class EnumConverter implements Converter<Object> {

	private Class<? extends AbstractEnum> entityType;

	protected EnumConverter(Class<? extends AbstractEnum> entityType) {
		this.entityType = entityType;
	}

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		AbstractEnum en = getService().getEnumByName(value, entityType);
		if (en == null) {
			try {
				en = entityType.newInstance();
				en.setName(value);
			} catch (Exception e) {
			}
		}
		return en;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value instanceof AbstractEnum)
			return ((AbstractEnum) value).getName();
		if (value instanceof String)
			return (String) value;
		return null;
	}

	protected abstract ServiceFacade getService();
}
