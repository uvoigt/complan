package org.planner.ui.beans;

import java.io.Serializable;

import javax.inject.Inject;

import org.planner.model.Suchkriterien;
import org.planner.remote.ServiceFacade;
import org.planner.util.Logged;

@Logged
public abstract class AbstractEditBean implements ITarget, Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	protected ServiceFacade service;

	@Inject
	private StartseiteBean startseiteBean;

	public void save(String link) {
		doSave();
		startseiteBean.setMainContent(link);
	}

	protected abstract void doSave();

	protected Suchkriterien createAutocompleteCriteria(String text, String property) {
		Suchkriterien criteria = new Suchkriterien();
		criteria.addFilter(property, text);
		criteria.addSortierung(property, true);
		return criteria;
	}
}
