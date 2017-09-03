package org.planner.ui.util.converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.ui.beans.Messages;
import org.primefaces.component.calendar.Calendar;

@Named
public class DateConverter implements Converter {

	@Inject
	private Messages bundle;

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {

		Calendar calendar = (Calendar) component;
		String pattern = calendar.getPattern();
		String xmlPattern = (String) calendar.getAttributes().get("xmlPattern");
		try {
			if (context.getCurrentPhaseId() != PhaseId.RENDER_RESPONSE) {
				Date date = new SimpleDateFormat(pattern).parse(value);
				return new SimpleDateFormat(xmlPattern).format(date);
			} else {
				Date date = new SimpleDateFormat(xmlPattern).parse(value);
				return date;
			}
		} catch (ParseException e) {
			// Fehler in den Primefaces CalendarUtils: die Exception wird nicht
			// gefangen
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(bundle.get("validateError"), bundle.format("validateErrorDate", value)));
			return new Date();
		}
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
		value = getAsObject(context, component, (String) value);
		String pattern = ((Calendar) component).getPattern();
		return new SimpleDateFormat(pattern).format(value);
	}
}
