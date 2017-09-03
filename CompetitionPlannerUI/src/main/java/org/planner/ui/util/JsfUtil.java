package org.planner.ui.util;

import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletContext;

import org.planner.ui.beans.Messages;

/**
 * Platz für JSF-spezifische statische Utility-Methoden.
 * 
 * @author Uwe Voigt - IBM
 */
public class JsfUtil {

	/**
	 * @return Liefert das unter dem Key "bundle" in den Context-Attributen
	 *         gespeicherte {@link Messages}.
	 */
	public static Messages getScopedBundle() {
		return (Messages) getContextVariable("bundle");
	}

	/**
	 * Liefert die Variable mit dem angegebenen Namen aus dem Context.
	 * 
	 * @param name
	 *            der Name der Variable
	 * @return das Objekt
	 */
	public static Object getContextVariable(String name) {
		FaceletContext ctx = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
		return ctx.getAttribute(name);
	}

	/**
	 * Setzt die Variable mit dem angegebenen Namen im Context.
	 * 
	 * @param name
	 *            der Name der Variable
	 * @param value
	 *            der Wert der Variable
	 */
	public static void setContextVariable(String name, Object value) {
		FaceletContext ctx = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
		ctx.setAttribute(name, value);
	}

	private JsfUtil() {
	}
}
