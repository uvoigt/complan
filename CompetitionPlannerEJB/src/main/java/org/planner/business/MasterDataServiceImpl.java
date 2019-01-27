package org.planner.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.RollbackException;

import org.planner.business.CommonImpl.Operation;
import org.planner.dao.IOperation;
import org.planner.dao.PlannerDao;
import org.planner.ejb.CallerProvider;
import org.planner.eo.AbstractEntity;
import org.planner.eo.Address;
import org.planner.eo.Club;
import org.planner.eo.Club_;
import org.planner.eo.Role;
import org.planner.eo.Role_;
import org.planner.eo.User;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
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

	public User saveUser(User user) {
		if (user == null)
			throw new IllegalArgumentException();
		User existing = user.getId() != null ? dao.getById(User.class, user.getId()) : null;
		User loggedInUser = common.checkWriteAccess(existing != null ? existing : user, Operation.save);
		if (user.getId() == null) {
			User eo = dao.getUserByUserId(user.getUserId());
			if (eo != null)
				throw new FachlicheException(messages.getResourceBundle(), "user.exists", user.getUserId());
		}
		common.checkWriteAccess(user, user.getId() == null ? Operation.create : Operation.save);
		if (existing != null) {
			// wenn der User sich gerade selbst speichert (MyProfile), dann ist ggf. das Passwort mit dabei
			if ((loggedInUser == null || loggedInUser.getId().equals(user.getId())) && user.getPassword() != null)
				user.setPassword(RegistryImpl.encodePw(user.getPassword()));
			else
				user.setPassword(existing.getPassword());
			// sichere extra Properties vor dem Überschreiben
			user.setUserId(existing.getUserId());
			if (loggedInUser != null) // nicht Admin
				user.setLocked(existing.isLocked());
			// sichere ggf. bestehende Rollen, die nur der Admin speichern darf
			for (Role role : existing.getRoles()) {
				if ("Tester".equals(role.getRole()))
					user.getRoles().add(role);
			}
		}
		if (user.getClub().getId() == null)
			saveClub(user.getClub());

		return common.save(user);
	}

	public void saveLastLogonTime(String userId) {
		dao.saveLastLogonTime(userId);
	}

	private Address saveAddress(Address address) {
		if (address.getCountry() == null || address.getCity() == null || address.getPostCode() == null
				|| address.getStreet() == null)
			return null;
		common.handleEnum(address.getCountry());
		common.handleEnum(address.getCity());
		return address;
	}

	public Club saveClub(Club club) {
		common.checkWriteAccess(club, Operation.save);
		if (club.getId() == null) {
			Suchkriterien krit = new Suchkriterien();
			krit.setExact(true); // ignore case ist ok
			krit.addFilter(Club_.name.getName(), club.getName());
			if (dao.search(Club.class, krit, null).getGesamtgroesse() > 0)
				throw new FachlicheException(messages.getResourceBundle(), "club.exists", club.getName());
		}
		// nur, da hier auch null zurückkommen kann
		Address address = club.getAddress();
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

	public List<Role> getRoles(final boolean restrictToExternal) {
		return dao.executeOperation(new IOperation<List<Role>>() {
			@Override
			public List<Role> execute(EntityManager em) {
				CriteriaBuilder builder = em.getCriteriaBuilder();
				CriteriaQuery<Role> query = builder.createQuery(Role.class);
				Root<Role> root = query.from(Role.class);
				List<Predicate> restrictions = new ArrayList<>();
				List<Order> orderBy = new ArrayList<>();
				// filtere Admin und Tester heraus, so dass niemand
				// außer einem Admin einen Admin bzw. Tester anlegen kann.
				// Beim checkWriteAccess wird das aber auch geprüft
				if (!caller.isInRole("Admin")) {
					restrictions.add(builder.and(builder.notEqual(root.get(Role_.role), "Admin"),
							builder.notEqual(root.get(Role_.role), "Tester")));
				}
				if (restrictToExternal)
					restrictions.add(builder.equal(root.get(Role_.internal), false));
				else
					orderBy.add(builder.asc(root.get(Role_.internal)));
				orderBy.add(builder.asc(root.get(Role_.role)));
				if (!restrictions.isEmpty())
					query.where(restrictions.toArray(new Predicate[restrictions.size()]));
				query.orderBy(orderBy);
				return em.createQuery(query).getResultList();
			}
		});
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
				context.put("roles", roles = getRoles(true));
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
