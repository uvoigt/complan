package org.planner.dao;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TemporalType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.apache.commons.lang.StringUtils;
import org.planner.eo.AbstractEntity;
import org.planner.eo.Properties;
import org.planner.eo.Properties_;
import org.planner.eo.User;
import org.planner.eo.User_;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;

/**
 * <p>
 * Das Datenzugriffsobjekt
 * </p>
 * 
 * @author Uwe Voigt, IBM
 */
@Transactional
public class PlannerDao {

	/**
	 * Entity-Manager fuer den Datenbankzugriff
	 */
	@Inject
	@PlannerDB
	private EntityManager em;

	public <T extends AbstractEntity> T find(Class<T> type, Long id) {
		return em.find(type, id);
	}

	public <T extends AbstractEntity> Suchergebnis<T> findEntities(Class<T> entityType, Suchkriterien kriterien) {
		return ZugriffUtil.executePagingQuery(em, entityType, kriterien);
	}

	public void delete(Class<? extends AbstractEntity> type, Long id) {
		AbstractEntity eo = em.find(type, id);
		if (eo != null)
			em.remove(eo);
	}

	public int delete(Class<? extends AbstractEntity> type, List<Long> ids) {
		StringBuilder sql = new StringBuilder(50 + 5 * ids.size());
		sql.append("delete from ");
		Table table = type.getAnnotation(Table.class);
		if (table != null)
			sql.append(table.name());
		else
			sql.append(type.getSimpleName());
		sql.append(" where id in(");
		for (Long id : ids) {
			sql.append(id);
			sql.append(",");
		}
		sql.setLength(sql.length() - 1);
		sql.append(")");
		Query query = em.createNativeQuery(sql.toString());
		return query.executeUpdate();
	}

	public <T extends AbstractEntity> T save(T entity, String loggedInUser) {
		if (entity.getId() == null) {
			entity.setCreateUser(loggedInUser);
			entity.setCreateTime(new Date());
			em.persist(entity);
		} else {
			entity.setUpdateUser(loggedInUser);
			entity.setUpdateTime(new Date());
			em.merge(entity);
		}
		return entity;
	}

	public List<Properties> leseBenutzerEinstellungen(String userId) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Properties> query = builder.createQuery(Properties.class);
		Root<Properties> root = query.from(Properties.class);
		if (userId != null) {
			query.where(builder.equal(root.get(Properties_.userId), userId));
		} else {
			query.where(builder.isNull(root.get(Properties_.userId)));
		}

		return em.createQuery(query).getResultList();
	}

	public void speichernBenutzerEinstellungen(List<Properties> properties, String userId) {
		for (Properties p : properties) {
			// kein allgemeingültiges Property überschreiben!
			if (p.getUserId() == null && p.getId() != null)
				p.setId(null);
			p.setUserId(userId);
			save(p, userId);
		}
	}

	public User getUserByUserId(String userId) {
		// das sind Nutzer, die manuell angelegt wurden
		if (StringUtils.isEmpty(userId)) {
			User benutzer = new User();
			benutzer.setFirstName("Entwickler");
			benutzer.setLastName("");
			return benutzer;
		}
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> root = query.from(User.class);
		query.where(builder.equal(root.get(User_.userId), userId));
		return em.createQuery(query).getSingleResult();
	}

	public void saveLastLogonTime(String userId) {
		// verhindert das Eintragen des anonymous update users
		em.createQuery("update User u set u.lastLogon = current_timestamp where u.userId = :userId")
				.setParameter("userId", userId).executeUpdate();
	}

	public void saveToken(Long id, String token, Date expiry) {
		// verhindert das Eintragen des anonymous update users
		em.createQuery("update User u set u.token = :token, u.tokenExpires = :tokenExpires where u.id = :id")
				.setParameter("token", token).setParameter("tokenExpires", expiry, TemporalType.TIMESTAMP)
				.setParameter("id", id).executeUpdate();
	}
}