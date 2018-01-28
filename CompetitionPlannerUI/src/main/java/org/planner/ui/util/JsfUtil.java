package org.planner.ui.util;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.view.facelets.FaceletContext;

import org.planner.ui.beans.Messages;
import org.primefaces.util.Constants;

/**
 * Platz für JSF-spezifische statische Utility-Methoden.
 * 
 * @author Uwe Voigt - IBM
 */
public class JsfUtil {

	/**
	 * @return Liefert das unter dem Key "bundle" in den Context-Attributen gespeicherte {@link Messages}.
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
		FaceletContext ctx = (FaceletContext) FacesContext.getCurrentInstance().getAttributes()
				.get(FaceletContext.FACELET_CONTEXT_KEY);
		return ctx != null ? ctx.getAttribute(name) : null;
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
		FaceletContext ctx = (FaceletContext) FacesContext.getCurrentInstance().getAttributes()
				.get(FaceletContext.FACELET_CONTEXT_KEY);
		ctx.setAttribute(name, value);
	}

	@SuppressWarnings("rawtypes")
	public static Object getViewVariable(String name) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		StateSaver stateSaver = (StateSaver) ctx.getViewRoot().findComponent("stateSaver");
		if (stateSaver == null)
			stateSaver = new StateSaver();
		if (ctx.getCurrentPhaseId() == PhaseId.RESTORE_VIEW) {
			Map<Object, Object> attributes = ctx.getAttributes();
			// da das restore view im UiRoot in dieser Phase umgangen wird, mach ich das hier selber
			Object[] state = (Object[]) attributes.get("com.sun.faces.FACES_VIEW_STATE");
			Object viewState = ((Map) state[1]).get("stateSaver");
			stateSaver.restoreState(ctx, viewState);
		}
		return stateSaver.get(name);
	}

	public static void setViewVariable(String name, Object value) {
		StateSaver stateSaver = (StateSaver) FacesContext.getCurrentInstance().getViewRoot()
				.findComponent("stateSaver");
		if (stateSaver == null)
			return;
		if (value != null)
			stateSaver.put(name, value);
		else
			stateSaver.remove(name);
	}

	public static boolean isFromSource(String id) {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		String sourceValue = params.get(Constants.RequestParams.PARTIAL_SOURCE_PARAM);
		return sourceValue != null && sourceValue.endsWith(id);
	}

	private JsfUtil() {
	}
}
