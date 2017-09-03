package org.planner.ui.beans.masterdata;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.planner.eo.Club;
import org.planner.ui.beans.AbstractEditBean;

@Named
@SessionScoped
public class ClubBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Club club = new Club();

	private Club myClub;

	@Override
	public void setItem(Object item) {
		club = (Club) item;
	}

	public Club getClub() {
		return club;
	}

	public Club getMyClub() {
		if (myClub == null)
			myClub = service.getLoggedInUser().getClub();
		return myClub;
	}

	@Override
	protected void doSave() {
		service.saveClub(club);
	}
}
