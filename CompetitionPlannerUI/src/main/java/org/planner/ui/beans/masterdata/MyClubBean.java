package org.planner.ui.beans.masterdata;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.planner.eo.Address;
import org.planner.eo.Club;
import org.planner.ui.beans.AbstractEditBean;

@Named
@RequestScoped
public class MyClubBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Club club;

	@PostConstruct
	public void init() {
		club = service.getLoggedInUser().getClub();
		if (club.getAddress() == null)
			club.setAddress(new Address());
	}

	public Club getClub() {
		return club;
	}

	@Override
	public void setItem(Object item) {
	}

	@Override
	protected void doSave() {
		service.saveClub(club);
	}
}
