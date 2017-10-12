package org.planner.ui.beans.masterdata;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIInput;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.Club;
import org.planner.eo.Role;
import org.planner.eo.User;
import org.planner.model.Gender;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.Messages;
import org.planner.ui.util.JsfUtil;

//@Logged
@Named
@RequestScoped
public class UserBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private User user;

	private User myUser;

	private List<Club> clubs;

	private List<String> selectedRoles;

	private UIInput roleMenu;

	@Inject
	private Messages messages;

	@PostConstruct
	public void init() {
		Long id = getIdFromRequestParameters();
		if (id == null)
			id = (Long) JsfUtil.getViewVariable("id");
		if (id != null) {
			user = service.getObject(User.class, id, 1);
			JsfUtil.setViewVariable("id", user.getId());
		} else {
			user = new User();
		}
		myUser = service.getLoggedInUser();
		prepareSelectedRoles();
	}

	@Override
	public void setItem(Object item) {
		user = (User) item;
		prepareSelectedRoles();
	}

	public UIInput getRoleMenu() {
		return roleMenu;
	}

	public void setRoleMenu(UIInput roleMenu) {
		this.roleMenu = roleMenu;
	}

	public User getUser() {
		return user;
	}

	public String getMale() {
		return Gender.m.getText();
	}

	public String getFemale() {
		return Gender.w.getText();
	}

	public List<Club> getClubs() {
		if (this.clubs == null)
			this.clubs = service.getClubs();
		List<Club> clubs = new ArrayList<>(this.clubs);
		if (user.getClub() == null || user.getClub().getId() == null) {
			clubs.add(0, createSelectClub());
		}
		return clubs;
	}

	public Long getClubId() {
		Club club = user.getClub();
		return club != null && club.getId() != null ? club.getId() : -1;
	}

	public void setClubId(Long value) {
		Club club = user.getClub();
		if (club == null)
			user.setClub(club = new Club());
		club.setId(value);
	}

	private Club createSelectClub() {
		Club club = new Club();
		club.setId(-1L);
		club.setName(messages.get("labelChoose"));
		return club;
	}

	public boolean isRoleSportler() {
		Object value = roleMenu.getSubmittedValue();
		if (value != null) {
			for (String role : (String[]) value) {
				if ("Sportler".equals(role))
					return true;
			}
		}
		return false;
	}

	public void validateClub(UIInput menu) {
		if ("-1".equals(menu.getSubmittedValue()))
			throw new ValidatorException(new FacesMessage(menu.getValidatorMessage()));
	}

	public List<String> getSelectedRoles() {
		return selectedRoles;
	}

	public void setSelectedRoles(List<String> selectedRoles) {
		this.selectedRoles = selectedRoles;
	}

	public User getMyUser() {
		return myUser;
	}

	private void prepareSelectedRoles() {
		selectedRoles = new ArrayList<>();
		for (Role role : user.getRoles()) {
			selectedRoles.add(role.getRole());
		}
	}

	public List<Role> getRoles() {
		return service.getRoles();
	}

	public Date getToday() {
		return new Date();
	}

	@Override
	protected void doSave() {
		Set<Role> userRoles = user.getRoles();
		if (selectedRoles != null) {
			userRoles.clear();
			List<Role> roles = getRoles();
			for (String selectedRole : selectedRoles) {
				for (Role role : roles) {
					if (role.getRole().equals(selectedRole))
						userRoles.add(role);
				}
			}
		}
		user.setPassword(null);
		service.saveUser(user);
	}
}
