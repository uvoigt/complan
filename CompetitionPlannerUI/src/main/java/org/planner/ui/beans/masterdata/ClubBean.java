package org.planner.ui.beans.masterdata;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.planner.eo.Club;
import org.planner.ui.beans.AbstractEditBean;

@Named
@RequestScoped
public class ClubBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Club club;

	@PostConstruct
	public void init() {
		Long id = getIdFromRequestParameters();
		if (id != null)
			club = service.getObject(Club.class, id, 1);
		else
			club = new Club();
	}

	@Override
	public void setItem(Object item) {
		club = (Club) item;
	}

	public Club getClub() {
		return club;
	}

	@Override
	protected void doSave() {
		service.saveClub(club);
	}
}
