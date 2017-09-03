package org.planner.business;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.planner.dao.PlannerDao;
import org.planner.ejb.CallerProvider;
import org.planner.eo.AbstractEntity;
import org.planner.eo.AbstractEnum;
import org.planner.eo.AbstractEnum_;
import org.planner.eo.Address;
import org.planner.eo.Club;
import org.planner.eo.User;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.Messages;

@Named
public class CommonImpl {

	@Inject
	private CallerProvider caller;

	@Inject
	private PlannerDao dao;

	@Inject
	private Messages messages;

	public <T extends AbstractEntity> Suchergebnis<T> search(Class<T> entityType, Suchkriterien criteria) {
		return dao.findEntities(entityType, criteria);
	}

	public <T extends AbstractEnum> T getEnumByName(String name, Class<T> enumType) {
		Suchkriterien krit = new Suchkriterien();
		krit.addFilter(AbstractEnum_.name.getName(), name);
		Suchergebnis<T> result = dao.findEntities(enumType, krit);
		return result.getGesamtgroesse() == 1 ? result.getListe().get(0) : null;
	}

	public void delete(Class<? extends AbstractEntity> entityType, Long id) {
		checkWriteAccess(dao.find(entityType, id));
		dao.delete(entityType, id);
	}

	public void delete(Class<? extends AbstractEntity> entityType, List<Long> ids) {
		// checkWriteAccess(dao.find(entityType, id)); TODO
		dao.delete(entityType, ids);
	}

	public <T extends AbstractEntity> T save(T entity) {
		return dao.save(entity, caller.getLoginName());
	}

	public void dataImport(List<AbstractEntity> entities) {
		String loginName = caller.getLoginName();
		for (AbstractEntity entity : entities) {
			preprocessDataImport(entity, loginName);
			dao.save(entity, loginName);
		}
	}

	private void preprocessDataImport(AbstractEntity entity, String loginName) {
		if (entity instanceof User) {
			User user = dao.getUserByUserId(loginName);
			((User) entity).setClub(user.getClub());
		}
	}

	public void checkWriteAccess(AbstractEntity entity) {
		// admin darf alles
		if (caller.isInRole("Admin"))
			return;
		// die Alternative wäre eine Permission-Matrix mit Rechten als
		// Integer-Column, welche auf Objekte und Subjekte abgebildet werden.
		// z.B. | rights | object_id | subject_id
		// ---------------------------------
		// 01 ... ...
		// mit Objekten wie z.B. Meldungen, Ausschreibungen oder Vereinen
		// und Subjekten wie Usern oder Vereinen

		// das hat aber Nachteile: neue User müssen ggf. mit existieren
		// Permissions versehen werden

		User user = dao.getUserByUserId(caller.getLoginName());
		if (entity instanceof Club) {
			Club club = (Club) entity;
			if (club.getId() == null || !club.getId().equals(user.getClub().getId()))
				throw new FachlicheException(messages.getResourceBundle(), "club.cannot.save");
		} else if (entity instanceof Address) {
			// und da geht es schon los: die Adresse hat keine weitere
			// Subjekt-info
		} else if (entity instanceof User) {
			// darf sich nur selbst speichern
			if (!user.getUserId().equals(((User) entity).getUserId()))
				throw new FachlicheException(messages.getResourceBundle(), "user.cannot.save");
		}
	}
}
