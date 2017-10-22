package org.planner.ui.beans;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.planner.eo.User;
import org.planner.model.Suchkriterien;
import org.planner.remote.ServiceFacade;
import org.planner.util.LogUtil.TechnischeException;

//@Logged
public abstract class AbstractEditBean implements ITarget, Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	protected ServiceFacade service;

	@Inject
	protected StartseiteBean startseiteBean;

	@Inject
	private UrlParameters urlParameters;

	private User loggedInUser;

	public void save(String link) {
		doSave();
		startseiteBean.setMainContent(link);
	}

	protected abstract void doSave();

	protected Long getIdFromRequestParameters() {
		urlParameters.init();
		return urlParameters.getId();
	}

	protected <T> T getRequestParameter(String name, Class<T> type) {
		String value = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(name);
		try {
			return value != null ? type.getConstructor(String.class).newInstance(value) : null;
		} catch (Exception e) {
			throw new TechnischeException(
					"Fehler beim Erzeugen eines Objekts aus dem Request-Parameter " + name + "=" + value, e);
		}
	}

	public static Suchkriterien createAutocompleteCriteria(String text, String property) {
		Suchkriterien criteria = new Suchkriterien();
		criteria.addFilter(property, text);
		criteria.addSortierung(property, true);
		return criteria;
	}

	public User getLoggedInUser() {
		if (loggedInUser == null)
			loggedInUser = service.getLoggedInUser();
		return loggedInUser;
	}
}
