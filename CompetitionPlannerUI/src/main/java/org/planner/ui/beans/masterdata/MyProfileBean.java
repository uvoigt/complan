package org.planner.ui.beans.masterdata;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.planner.eo.Address;
import org.planner.eo.Club;
import org.planner.eo.User;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.util.Logged;

@Logged
@Named
@RequestScoped
public class MyProfileBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private User user;

	private Club newClub;

	@Override
	@PostConstruct
	public void init() {
		super.init();

		if (!isCancelPressed())
			user = auth.getLoggedInUser();
	}

	public User getUser() {
		return user;
	}

	public Club getNewClub() {
		if (newClub == null) {
			newClub = new Club();
			// damit besteht überhaupt erst die Chance, diese einzugeben
			newClub.setAddress(new Address());
		}
		return newClub;
	}

	@Override
	public void setItem(Object item) {
	}

	@Override
	protected void doSave() {
		if (newClub != null)
			user.setClub(newClub);
		service.saveUser(user);
		startseiteBean.setProfileSaved();
	}
}
