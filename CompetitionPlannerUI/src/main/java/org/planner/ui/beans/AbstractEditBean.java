package org.planner.ui.beans;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.planner.model.FetchInfo;
import org.planner.model.Suchkriterien;
import org.planner.remote.ServiceFacade;
import org.planner.ui.beans.common.AuthBean;
import org.planner.ui.util.JsfUtil;
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

	@Inject
	protected AuthBean auth;

	private boolean cancelPressed;

	protected void init() {
		cancelPressed = JsfUtil.isFromSource("btnCancel");
	}

	@Override
	public Object prepareForCopy(Object item) {
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(null, JsfUtil.getScopedBundle().get("copyHint")));
		return item;
	}

	public void save(String link) {
		doSave();
		startseiteBean.setMainContent(link);
	}

	protected abstract void doSave();

	protected Long getIdFromRequestParameters() {
		return urlParameters.getId();
	}

	protected Object getFromRequestParameters(int offset) {
		return urlParameters.get(offset);
	}

	protected void setRequestParameter(int offset, Object value) {
		urlParameters.set(offset, value);
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

	protected boolean isCancelPressed() {
		return cancelPressed;
	}

	@Override
	public FetchInfo[] getFetchInfo() {
		return new FetchInfo[0];
	}
}
