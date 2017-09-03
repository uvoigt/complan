package org.planner.ui.util.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.ui.beans.Messages;

/**
 * Enthält validate-Methoden für diverse Typen.
 * 
 * @author Uwe Voigt - IBM
 */
@Named
public class Validators {

	@Inject
	private Messages bundle;

	public void validateInteger(FacesContext context, UIComponent component, Object value) {
		validateNumber(context, component, value, Integer.class, true);
	}

	public void validateLong(FacesContext context, UIComponent component, Object value) {
		validateNumber(context, component, value, Long.class, true);
	}

	public void validateByte(FacesContext context, UIComponent component, Object value) {
		validateNumber(context, component, value, Byte.class, true);
	}

	public void validateUnsignedByte(FacesContext context, UIComponent component, Object value) {
		validateNumber(context, component, value, Byte.class, false);
	}

	private <N extends Number> void validateNumber(FacesContext context,
			UIComponent component, Object value, Class<N> type, boolean signed) {

		if (!(value instanceof String) || ((String) value).length() == 0)
			return;

		String stringValue = (String) value;
		try {
			if (type == Integer.class) {
				Integer.parseInt(stringValue);
			} else if (type == Long.class) {
				Long.parseLong(stringValue);
			} else if (type == Byte.class) {
				if (signed) {
					Byte.parseByte(stringValue);
				} else {
					short s = Short.parseShort(stringValue);
					if (s < 0 || s > 255)
						throw new NumberFormatException();
				}
			}
		} catch (NumberFormatException e) {
			throw new ValidatorException(new FacesMessage(
					bundle.get("validateError"), bundle.format("validateErrorNumber", stringValue, type.getSimpleName())),
					e);
		}
	}

}
