package org.planner.business;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.planner.dao.PlannerDao;
import org.planner.eo.AbstractEntity;
import org.planner.eo.Address;
import org.planner.eo.City;
import org.planner.eo.Club;
import org.planner.eo.Country;
import org.planner.eo.Role;
import org.planner.eo.Role_;
import org.planner.eo.User;
import org.planner.eo.User_;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.Messages;

@Named
public class MasterDataServiceImpl {

	@Inject
	private PlannerDao dao;

	@Inject
	private Messages messages;

	@Inject
	private CommonImpl common;

	private Map<String, String> userNameCache = new HashMap<String, String>();

	public String getUserName(String userId) {
		String name = userNameCache.get(userId);
		if (name == null) {
			User user = dao.getUserByUserId(userId);
			name = user.getFirstName() + " " + user.getLastName();
			userNameCache.put(userId, name);
		}
		return name;

	}

	public User getUserByUserId(String userId) {
		Suchkriterien krit = new Suchkriterien();
		krit.addFilter(User_.userId.getName(), userId);
		krit.setIgnoreCase(false);
		krit.setExact(true);
		Suchergebnis<User> entities = dao.findEntities(User.class, krit);
		return entities.getGesamtgroesse() > 0 ? entities.getListe().get(0) : null;
	}

	public User getUserById(Long id) {
		return dao.find(User.class, id);
	}

	public User saveUser(User user) {
		if (user == null)
			throw new IllegalArgumentException();
		common.checkWriteAccess(user);
		if (user.getId() == null) {
			User eo = getUserByUserId(user.getUserId());
			if (eo != null)
				throw new FachlicheException(messages.getResourceBundle(), "user.exists", user.getUserId());
		}
		return common.save(user);
	}

	public void saveLastLogonTime(String userId) {
		dao.saveLastLogonTime(userId);
	}

	public Address saveAddress(Address address) {
		common.checkWriteAccess(address);
		if (address.getCountry() == null || address.getCity() == null || address.getPostCode() == null
				|| address.getStreet() == null || address.getNumber() == null)
			return null;
		Country country = address.getCountry();
		if (country.getId() == null)
			common.save(country);
		City city = address.getCity();
		if (city.getId() == null)
			common.save(city);
		return common.save(address);
	}

	public Club saveClub(Club club) {
		common.checkWriteAccess(club);
		Address address = club.getAddress();
		// nur, da hier auch null zurückommen kann
		club.setAddress(saveAddress(address));
		return common.save(club);
	}

	public Role saveRole(Role role) {
		return common.save(role);
	}

	protected <T extends AbstractEntity> List<T> findWithoutFilter(Class<T> type) {
		Suchkriterien krit = new Suchkriterien();
		Suchergebnis<T> entities = dao.findEntities(type, krit);
		return entities.getListe();
	}

	public List<Role> getRoles() {
		Suchkriterien krit = new Suchkriterien();
		krit.addSortierung(Role_.role.getName(), true);
		Suchergebnis<Role> entities = dao.findEntities(Role.class, krit);
		return entities.getListe();
	}

	public List<Club> getClubs() {
		return findWithoutFilter(Club.class);
	}
}
