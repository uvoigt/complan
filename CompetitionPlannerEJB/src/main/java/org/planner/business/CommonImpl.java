package org.planner.business;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.RollbackException;

import org.planner.dao.Authorizer;
import org.planner.dao.Authorizer.AnnouncementAuthorizer;
import org.planner.dao.Authorizer.RegistrationAuthorizer;
import org.planner.dao.Authorizer.UserAuthorizer;
import org.planner.dao.PlannerDao;
import org.planner.dao.QueryModifier;
import org.planner.ejb.CallerProvider;
import org.planner.eo.AbstractEntity;
import org.planner.eo.AbstractEnum;
import org.planner.eo.AbstractEnum_;
import org.planner.eo.Announcement;
import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Club;
import org.planner.eo.Program;
import org.planner.eo.Properties;
import org.planner.eo.Race;
import org.planner.eo.RegEntry;
import org.planner.eo.Registration;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Role;
import org.planner.eo.User;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.util.LogUtil;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.LogUtil.TechnischeException;
import org.planner.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class CommonImpl {
	enum Operation {
		create, save, delete
	}

	private static final Logger LOG = LoggerFactory.getLogger(CommonImpl.class);

	@Inject
	private CallerProvider caller;

	@Inject
	private PlannerDao dao;

	@Inject
	private Messages messages;

	public <T extends Serializable> Suchergebnis<T> search(Class<T> entityType, Suchkriterien criteria) {
		if (caller.isInRole("Admin"))
			return internalSearch(entityType, criteria, null);

		Authorizer authorizer = null;
		if (entityType == User.class)
			authorizer = new UserAuthorizer(getCallingUser());
		else if (entityType == Announcement.class)
			authorizer = new AnnouncementAuthorizer(getCallingUser());
		else if (entityType == Registration.class)
			authorizer = new RegistrationAuthorizer(getCallingUser());
		return internalSearch(entityType, criteria, authorizer);
	}

	@SuppressWarnings("unchecked")
	<T extends Serializable> Suchergebnis<T> internalSearch(Class<T> entityType, Suchkriterien criteria,
			QueryModifier queryModifier) {

		List<String> properties = criteria.getProperties();
		if (properties != null) {
			// die id sollte immer enthalten sein
			// außerdem lesen wir noch die last-Edit-Eigenschaften
			// die werden im DAO gesondert behandelt
			final List<String> additionalProperties = Arrays.asList(AbstractEnum_.id
					.getName()/*
								 * , AbstractEntity_.createUser.getName(), AbstractEntity_.createTime.getName(),
								 * AbstractEntity_.updateUser.getName(), AbstractEntity_.updateTime.getName()
								 */);
			try {
				properties.addAll(additionalProperties);
			} catch (UnsupportedOperationException e) {
				List<String> list = new ArrayList<String>(properties.size() + additionalProperties.size());
				list.addAll(additionalProperties);
				list.addAll(properties);
				properties = list;
				criteria.setProperties(properties);
			}

			Suchergebnis<Object[]> data = dao.search(entityType, Object[].class, criteria, queryModifier);
			List<Object[]> list = data.getListe();
			List<HashMap<String, Object>> result = new ArrayList<HashMap<String, Object>>(list.size());
			Class<?> metaType = loadMetaType(entityType);
			Map<String, Object> previousRow = null;
			for (Object entry : data) {
				HashMap<String, Object> resultRow = new HashMap<String, Object>();
				if (entry instanceof Object[]) {
					Object[] row = (Object[]) entry;
					// eine Zeile muss genauso lang sein wie die
					// Properties-Liste
					for (int i = 0; i < row.length; i++) {
						String prop = properties.get(i);
						resultRow.put(prop, row[i]);
					}
					if (previousRow != null && previousRow.get(AbstractEnum_.id.getName())
							.equals(resultRow.get(AbstractEnum_.id.getName()))) {
						mergeRows(previousRow, resultRow, metaType);
					} else {
						result.add(resultRow);
						previousRow = resultRow;
					}
				} else {
					// sind keine Properties übergeben worden, wird lediglich
					// die ID geliefert
					resultRow.put("id", entry);
					result.add(resultRow);
				}
			}
			return (Suchergebnis<T>) new Suchergebnis<HashMap<String, Object>>(result, data.getGesamtgroesse());
		} else {
			return dao.search(entityType, criteria, queryModifier);
		}
	}

	private void mergeRows(Map<String, Object> row, HashMap<String, Object> nextRow, Class<?> metaType) {
		for (String key : nextRow.keySet()) {
			Object value = row.get(key);
			Object nextValue = nextRow.get(key);
			if (value != null && nextValue != null && !value.equals(nextValue))
				row.put(key, value + ", " + nextValue);
		}
	}

	private Class<?> loadMetaType(Class<?> entityType) {
		try {
			return Class.forName(entityType.getName() + "_");
		} catch (ClassNotFoundException e) {
			throw new TechnischeException("Fehler beim Laden des Meta-Typen für " + entityType, e);
		}
	}

	public <T extends Serializable> T getById(Class<T> typ, Long id, int fetchDepth) {
		T entity = dao.getById(typ, id);
		if (fetchDepth > 0 && entity != null)
			fetch(entity, fetchDepth - 1);
		return entity;
	}

	private void fetch(Object entity, int depth) {
		if (entity == null)
			return;
		try {
			for (PropertyDescriptor pd : Introspector.getBeanInfo(entity.getClass()).getPropertyDescriptors()) {
				Method readMethod = pd.getReadMethod();
				if (readMethod == null)
					continue;
				Object value = readMethod.invoke(entity);
				if (value instanceof Collection) {
					Collection<?> collection = (Collection<?>) value;
					collection.size();
					if (depth > 0) {
						for (Object object : collection) {
							fetch(object, depth - 1);
						}
					}
				} else if (value instanceof Map) {
					Map<?, ?> map = (Map<?, ?>) value;
					map.size();
					if (depth > 0) {
						for (Object key : map.keySet()) {
							fetch(key, depth - 1);
						}
					}
				} else if (value instanceof AbstractEntity) {
					// kein Fetch bei Property-Access
					AbstractEntity member = (AbstractEntity) value;
					member.getId();
					if (depth > 0)
						fetch(member, depth - 1);
				} else if (value instanceof AbstractEnum) {
					AbstractEnum anEnum = (AbstractEnum) value;
					anEnum.getName();
				}
			}
		} catch (Exception e) {
			throw new TechnischeException("Fehler beim Fetchen der Properties", e);
		}
	}

	public <T extends AbstractEntity> T getByIdForCopy(Class<T> typ, Long id) {
		T object = getById(typ, id, 1);
		if (object != null) {
			// object.setId(null);
			try {
				for (PropertyDescriptor pd : Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors()) {
					Object propertyValue = pd.getReadMethod().invoke(object);
					if (propertyValue instanceof AbstractEntity) {
						// ((AbstractEntity) propertyValue).setId(null);
					} else if (propertyValue instanceof Collection) {
						for (Object o : ((Collection<?>) propertyValue)) {
							// if (o instanceof AbstractEntity)
							// ((AbstractEntity) o).setId(null);
						}
					} else if (propertyValue instanceof Map) {
						for (Entry<?, ?> e : ((Map<?, ?>) propertyValue).entrySet()) {
							// if (e.getValue() instanceof AbstractEntity)
							// ((AbstractEntity) e.getKey()).setId(null);
						}
					}
				}
			} catch (Exception e) {
				throw new TechnischeException("Fehler beim Lesen der Properties", e);
			}
		}
		return object;
	}

	public User getCallingUser() {
		return dao.getUserByUserId(caller.getLoginName());
	}

	public <T extends AbstractEnum> T getEnumByName(String name, Class<T> enumType) {
		Suchkriterien krit = new Suchkriterien();
		krit.addFilter(AbstractEnum_.name.getName(), name);
		Suchergebnis<T> result = dao.search(enumType, krit, null);
		return result.getGesamtgroesse() == 1 ? result.getListe().get(0) : null;
	}

	public void delete(Class<? extends AbstractEntity> entityType, Long id) {
		AbstractEntity entity = dao.getById(entityType, id);
		if (entity != null) {
			checkWriteAccess(entity, Operation.delete);
			dao.delete(entity);
		}
	}

	public <T extends AbstractEntity> T save(T entity) {
		return dao.save(entity, caller.getLoginName());
	}

	public void createEnum(AbstractEnum anEnum) {
		anEnum.setCreateUser(caller.getLoginName());
		anEnum.setCreateTime(new Date());
	}

	public void dataImport(List<AbstractEntity> entities, ImportPreprozessor preprozessor) {
		String loginName = caller.getLoginName();
		FachlicheException ex = null;
		Map<String, Object> context = new HashMap<>();
		for (int i = 0; i < entities.size(); i++) {
			AbstractEntity entity = entities.get(i);
			preprozessor.preprocessDataImport(entity, context);
			try {
				dao.saveWithCommit(entity, loginName);
			} catch (RollbackException e) {
				LogUtil.logException(e, LOG, "Fehler beim Importieren", entities);
				if (ex == null)
					ex = new FachlicheException(messages.getResourceBundle(), "dataImport.importError", e);
				String errorMsg = preprozessor.handleItemRollback(entity, e);
				ex.addMessage(messages.getResourceBundle(), "dataImport.itemError", i + 1, errorMsg);
			}
		}
		if (ex != null)
			throw ex;
	}

	/**
	 * Prüft die Berechtigung für verschiedene Entity-Typen.
	 * 
	 * @param entity
	 *            die Entität
	 * @param operation
	 *            die Operation
	 * @return den angemeldeten Benutzer oder null für den Admin
	 */
	public User checkWriteAccess(AbstractEntity entity, Operation operation) {
		// admin darf alles
		if (caller.isInRole("Admin"))
			return null;
		// die Alternative wäre eine Permission-Matrix mit Rechten als
		// Integer-Column, welche auf Objekte und Subjekte abgebildet werden.
		// z.B. | rights | object_id | subject_id
		// ---------------------------------
		// 01 ... ...
		// mit Objekten wie z.B. Meldungen, Ausschreibungen oder Vereinen
		// und Subjekten wie Usern oder Vereinen

		// das hat aber Nachteile: neue User müssen ggf. mit existieren
		// Permissions versehen werden

		User callingUser = getCallingUser();
		try {
			getClass().getDeclaredMethod("checkWriteAccess", entity.getClass(), Operation.class, User.class)
					.invoke(this, entity, operation, callingUser);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof FachlicheException)
				throw (FachlicheException) e.getTargetException();
			else
				throw new IllegalStateException(e.getTargetException());
		}
		return callingUser;
	}

	@SuppressWarnings("unused")
	private void checkWriteAccess(Club club, Operation operation, User callingUser) {
		// Spezialfall für den neuen User
		boolean isNewUser = callingUser.getFirstName().length() == 0 && callingUser.getLastName().length() == 0;
		if (isNewUser && caller.isInRole("Sportwart"))
			return;
		if (club.getId() == null || !club.getId().equals(callingUser.getClub().getId()))
			throwAccessException(club, operation);
	}

	@SuppressWarnings("unused")
	private void checkWriteAccess(User user, Operation operation, User callingUser) {

		// der Sportwart darf Benutzer im gleichen Club anlegen
		if (caller.isInRole("Sportwart")) {
			// der einzige Fall, in dem das ok ist, wenn der neue User
			// noch keinen Club hat
			if (callingUser.getFirstName().length() > 0 && callingUser.getFirstName().length() > 0
					&& !callingUser.getClub().getId().equals(user.getClub().getId()))
				throwAccessException(user, operation);
		} else {
			// alle anderen dürfen sich nur selbst speichern
			if (!callingUser.getUserId().equals(user.getUserId()))
				throwAccessException(user, operation);
		}
		// niemand außer einem Admin darf einen Admin anlegen
		for (Role role : user.getRoles()) {
			if ("Admin".equals(role.getRole()))
				throwAccessException(user, operation);
		}
		// sich selbst zu löschen erlauben wir auch nicht
		if (operation == Operation.delete && user.getId().equals(callingUser.getId()))
			throw new FachlicheException(messages.getResourceBundle(), "user.cannot.deleteself");
	}

	@SuppressWarnings("unused")
	private void checkWriteAccess(Role role, Operation operation, User callingUser) {
		if (!caller.isInRole("Admin"))
			throwAccessException(role, operation);
	}

	private void checkWriteAccess(Announcement announcement, Operation operation, User callingUser) {
		// Nur ein Clubmitglied, das die Rollen Sportwart oder Trainer hat darf speichern
		if ((!caller.isInRole("Sportwart") && !caller.isInRole("Trainer")) || announcement.getClub() != null
				&& !announcement.getClub().getId().equals(callingUser.getClub().getId()))
			throwAccessException(announcement, operation);

		// Eine bereits veröffentlichte Ausschreibung darf nicht mehr geändert werden
		AnnouncementStatus status = announcement.getStatus();
		if (status != null && status != AnnouncementStatus.created)
			throw new FachlicheException(messages.getResourceBundle(), "announcement.alreadyAnnounced");
	}

	private void checkWriteAccess(Race race, Operation operation, User callingUser) {
		Announcement announcement = dao.getById(Announcement.class, race.getAnnouncementId());
		checkWriteAccess(announcement, operation, callingUser);
	}

	@SuppressWarnings("unused")
	private void checkWriteAccess(Registration registration, Operation operation, User callingUser) {
		// Eine existierende Meldung darf nur von Clubmitgliedern geändert
		// werden
		if (registration.getClub() != null && !registration.getClub().getId().equals(callingUser.getClub().getId()))
			throwAccessException(registration, operation);
		// nur die Rollen "Sportwart", "Trainer" und "Mastersportler" dürfen Meldungen
		// speichern oder löschen
		if (!caller.isInRole("Sportwart") && !caller.isInRole("Trainer") && !caller.isInRole("Mastersportler"))
			throw new FachlicheException(messages.getResourceBundle(), "registration.cannot.save");

		// Eine bereits übermittelte Meldung darf nicht mehr geändert werden
		if (registration.getStatus() == RegistrationStatus.submitted)
			throw new FachlicheException(messages.getResourceBundle(), "registration.alreadySubmitted");
	}

	@SuppressWarnings("unused")
	private void checkWriteAccess(RegEntry entry, Operation operation, User callingUser) {
		checkWriteAccess(entry.getRegistration(), operation, callingUser);
	}

	@SuppressWarnings("unused")
	private void checkWriteAccess(Program program, Operation operation, User callingUser) {
		Announcement announcement = program.getAnnouncement();
		// Nur ein Clubmitglied!, das die Rollen Sportwart oder Trainer hat darf speichern
		if ((!caller.isInRole("Sportwart") && !caller.isInRole("Trainer")) || announcement.getClub() != null
				&& !announcement.getClub().getId().equals(callingUser.getClub().getId()))
			throwAccessException(program, operation);
	}

	private void throwAccessException(AbstractEntity entity, Operation operation) {
		String key = entity.getClass().getSimpleName().toLowerCase() + ".cannot." + operation.name();
		Object arg = null;
		if (!messages.getResourceBundle().containsKey(key)) {
			key = entity.getClass().getSimpleName().toLowerCase() + ".cannot";
			if (!messages.getResourceBundle().containsKey(key))
				key = "accessProblem";
			arg = messages.getMessage("operation." + operation.name());
		}
		throw new FachlicheException(messages.getResourceBundle(), key, arg);
	}

	public Map<String, Properties> leseBenutzerEinstellungen(String angemeldeterBenutzter) {
		List<Properties> userProps = dao.leseBenutzerEinstellungen(angemeldeterBenutzter);
		List<Properties> defaultProps = dao.leseBenutzerEinstellungen(null);
		Map<String, Properties> result = new HashMap<String, Properties>(
				Math.max(userProps.size(), defaultProps.size()));
		for (Properties p : defaultProps) {
			result.put(p.getName(), p);
		}
		// benutzerspezifische Properties überschreiben die Defaults
		for (Properties p : userProps) {
			result.put(p.getName(), p);
		}
		return result;
	}

	public Map<String, Properties> speichernBenutzerEinstellungen(List<Properties> properties,
			String angemeldeterBenutzter) {
		dao.speichernBenutzerEinstellungen(properties, angemeldeterBenutzter);
		return leseBenutzerEinstellungen(angemeldeterBenutzter);
	}
}
