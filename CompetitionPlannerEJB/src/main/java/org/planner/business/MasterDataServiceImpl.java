package org.planner.business;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.RollbackException;

import org.planner.business.CommonImpl.Operation;
import org.planner.dao.PlannerDao;
import org.planner.ejb.CallerProvider;
import org.planner.eo.AbstractEntity;
import org.planner.eo.Address;
import org.planner.eo.City;
import org.planner.eo.Club;
import org.planner.eo.Club_;
import org.planner.eo.Country;
import org.planner.eo.Role;
import org.planner.eo.Role_;
import org.planner.eo.User;
import org.planner.eo.User_;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.model.Suchkriterien.Filter;
import org.planner.model.Suchkriterien.Filter.Comparison;
import org.planner.model.Suchkriterien.Filter.Conditional;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.LogUtil.TechnischeException;
import org.planner.util.Messages;

@Named
public class MasterDataServiceImpl implements ImportPreprozessor {

	@Inject
	private CallerProvider caller;

	@Inject
	private PlannerDao dao;

	@Inject
	private Messages messages;

	@Inject
	private CommonImpl common;

	public String getUserName(String userId) {
		User user = dao.getUserByUserId(userId);
		return user != null ? user.getName() : userId;
	}

	public User getUserByUserId(String userId, boolean fetch) {
		Suchkriterien krit = new Suchkriterien();
		krit.addFilter(User_.userId.getName(), userId);
		krit.setIgnoreCase(false);
		krit.setExact(true);
		Suchergebnis<User> entities = dao.search(User.class, krit, null);
		User user = null;
		if (entities.getGesamtgroesse() > 0) {
			user = entities.getListe().get(0);
			if (fetch) {
				user.getRoles().size();
				if (user.getClub() != null)
					user.getClub().getId();
			}
		}
		return user;
	}

	public User saveUser(User user) {
		if (user == null)
			throw new IllegalArgumentException();
		User existing = user.getId() != null ? common.getById(User.class, user.getId(), 0) : null;
		User loggedInUser = common.checkWriteAccess(existing != null ? existing : user, Operation.save);
		if (user.getId() == null) {
			User eo = getUserByUserId(user.getUserId(), false);
			if (eo != null)
				throw new FachlicheException(messages.getResourceBundle(), "user.exists", user.getUserId());
		}
		if (user.getClub().getId() == null)
			saveClub(user.getClub());
		if (existing != null) {
			// wenn der User sich gerade selbst speichert (MyProfile), dann ist ggf. das Passwort mit dabei
			if ((loggedInUser == null || loggedInUser.getId().equals(user.getId())) && user.getPassword() != null)
				user.setPassword(RegistryImpl.encodePw(user.getPassword()));
			else
				user.setPassword(existing.getPassword());
			// sichere extra Properties vor dem Überschreiben
			user.setToken(existing.getToken());
			user.setTokenExpires(existing.getTokenExpires());
			if (loggedInUser != null) // nicht Admin
				user.setLocked(existing.isLocked());
		}

		return common.save(user);
	}

	public void saveLastLogonTime(String userId) {
		dao.saveLastLogonTime(userId);
	}

	public Address saveAddress(Address address) {
		common.checkWriteAccess(address, Operation.save);
		if (address.getCountry() == null || address.getCity() == null || address.getPostCode() == null
				|| address.getStreet() == null || address.getNumber() == null)
			return null;
		Country country = address.getCountry();
		if (country.getId() == null)
			common.save(country);
		else
			address.setCountry(common.getById(Country.class, country.getId(), 0));
		City city = address.getCity();
		if (city.getId() == null)
			common.save(city);
		else
			address.setCity(common.getById(City.class, city.getId(), 0));
		return common.save(address);
	}

	public Club saveClub(Club club) {
		common.checkWriteAccess(club, Operation.save);
		Address address = club.getAddress();
		if (club.getId() == null) {
			Suchkriterien krit = new Suchkriterien();
			krit.setExact(true); // ignore case ist ok
			krit.addFilter(Club_.name.getName(), club.getName());
			if (dao.search(Club.class, krit, null).getGesamtgroesse() > 0)
				throw new FachlicheException(messages.getResourceBundle(), "club.exists", club.getName());
		}
		// nur, da hier auch null zurückkommen kann
		if (address != null)
			club.setAddress(saveAddress(address));
		return common.save(club);
	}

	public Role saveRole(Role role) {
		common.checkWriteAccess(role, Operation.save);
		return common.save(role);
	}

	protected <T extends AbstractEntity> List<T> findWithoutFilter(Class<T> type) {
		Suchkriterien krit = new Suchkriterien();
		Suchergebnis<T> entities = dao.search(type, krit, null);
		return entities.getListe();
	}

	public List<Role> getRoles() {
		Suchkriterien krit = new Suchkriterien();
		krit.addSortierung(Role_.role.getName(), true);
		// filtere den Admin heraus, so dass niemand
		// außer einem Admin einen Admin anlegen kann.
		// Beim checkWriteAccess wird das aber auch geprüft
		if (!caller.isInRole("Admin")) {
			krit.setExact(true);
			krit.setIgnoreCase(false);
			krit.addFilter(new Filter(Conditional.and, Comparison.ne, Role_.role.getName(), "Admin"));
		}
		Suchergebnis<Role> entities = dao.search(Role.class, krit, null);
		return entities.getListe();
	}

	public List<Club> getClubs() {
		return findWithoutFilter(Club.class);
	}

	@Override
	public void preprocessDataImport(AbstractEntity entity, Map<String, Object> context) {
		if (entity instanceof User) {
			User user = (User) entity;
			Club club = common.getCallingUser().getClub();
			// den gesamten Club zu setzen führt zu: illegally attempted to associate a proxy with two open Sessions
			// es darf nur kein auto-cascading eingestellt sein
			Club detachedClub = new Club();
			detachedClub.setId(club.getId());
			user.setClub(detachedClub);
			@SuppressWarnings("unchecked")
			List<Role> roles = (List<Role>) context.get("roles");
			if (roles == null)
				context.put("roles", roles = getRoles());
			for (Role userRole : user.getRoles()) {
				boolean roleFound = false;
				for (Role role : roles) {
					if (!role.getRole().equals(userRole.getRole()))
						continue;
					roleFound = true;
					userRole.setId(role.getId());
					break;
				}
				if (!roleFound)
					throw new FachlicheException(messages.getResourceBundle(), "dataImport.illegalRole",
							userRole.getRole());
			}
		} else {
			throw new TechnischeException("Dieser Typ kann nicht importiert werden", null);
		}
	}

	@Override
	public String handleItemRollback(AbstractEntity entity, RollbackException e) {
		User user = (User) entity;
		return messages.getFormattedMessage("userImport.itemRollback", user.getUserId());
	}
}
