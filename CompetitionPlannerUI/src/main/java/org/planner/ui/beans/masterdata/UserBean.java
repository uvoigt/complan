package org.planner.ui.beans.masterdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIInput;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.Club;
import org.planner.eo.Role;
import org.planner.eo.User;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.Messages;

@Named
@SessionScoped
public class UserBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private User user;
	private List<Long> selectedRoles;

	private User myUser;

	@Inject
	private RoleBean roleBean;

	@Inject
	private Messages bundle;

	@Override
	public void setItem(Object item) {
		user = (User) item;
		selectedRoles = new ArrayList<>();
		for (Role role : user.getRoles()) {
			selectedRoles.add(role.getId());
		}
	}

	public User getUser() {
		return user;
	}

	public List<Club> getClubs() {
		List<Club> clubs = service.getClubs();
		if (user.getClub() == null || user.getClub().getId() == null)
			clubs.add(0, createSelectClub());
		return clubs;
	}

	public Long getClubId() {
		Club club = user.getClub();
		return club != null && club.getId() != null ? club.getId() : -1;
	}

	public void setClubId(Long value) {
		Club club = user.getClub();
		if (club != null)
			club.setId(value);
	}

	private Club createSelectClub() {
		Club club = new Club();
		club.setId(-1L);
		club.setName(bundle.get("labelChoose"));
		return club;
	}

	public void validate(UIInput menu) {
		if ("-1".equals(menu.getSubmittedValue()))
			throw new ValidatorException(new FacesMessage(menu.getValidatorMessage()));
	}

	public List<Long> getSelectedRoles() {
		return selectedRoles;
	}

	public void setSelectedRoles(List<String> selectedRoles) {
		this.selectedRoles = new ArrayList<>();
		for (String roleId : selectedRoles) {
			this.selectedRoles.add(Long.valueOf(roleId));
		}
	}

	public User getMyUser() {
		if (myUser == null)
			myUser = service.getLoggedInUser();
		return myUser;
	}

	@Override
	protected void doSave() {
		Set<Role> userRoles = user.getRoles();
		List<Role> roles = roleBean.getRoles();
		userRoles.clear();
		for (Long roleId : selectedRoles) {
			for (Role role : roles) {
				if (role.getId().equals(roleId))
					userRoles.add(role);
			}
		}
		service.saveUser(user);
	}
}
