package org.planner.dao;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Bindable;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.UserTransaction;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.planner.eo.AbstractEntity;
import org.planner.eo.AbstractEntity_;
import org.planner.eo.Properties;
import org.planner.eo.Properties_;
import org.planner.eo.User;
import org.planner.eo.User_;
import org.planner.model.LocalizedEnum;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.model.Suchkriterien.Filter;
import org.planner.model.Suchkriterien.SortField;
import org.planner.util.LogUtil.TechnischeException;
import org.planner.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Das Datenzugriffsobjekt
 * </p>
 * 
 * @author Uwe Voigt, IBM
 */
@Transactional
public class PlannerDao {

	private static final Logger LOG = LoggerFactory.getLogger(PlannerDao.class);

	/**
	 * Entity-Manager fuer den Datenbankzugriff
	 */
	@Inject
	@PlannerDB
	private EntityManager em;

	@Inject
	private UserTransaction transaction;

	@Inject
	private Messages messages;

	@Transactional(TxType.SUPPORTS)
	public <T extends Serializable> T getById(Class<T> type, Long id) {
		return em.find(type, id);
	}

	@Transactional(TxType.SUPPORTS)
	public <T extends Serializable> Suchergebnis<T> search(Class<?> entityType, Class<T> returningType,
			Suchkriterien kriterien, Authorizer authorizer) {
		return executePagingQuery(em, entityType, kriterien, returningType, authorizer);
	}

	@Transactional(TxType.SUPPORTS)
	public <T extends Serializable> Suchergebnis<T> search(Class<T> entityType, Suchkriterien kriterien,
			Authorizer authorizer) {
		return executePagingQuery(em, entityType, kriterien, entityType, authorizer);
	}

	public void delete(AbstractEntity entity) {
		if (entity != null)
			em.remove(entity);
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

	@Transactional(TxType.NOT_SUPPORTED)
	public <T extends AbstractEntity> T saveWithCommit(T entity, String loggedInUser) throws RollbackException {
		try {
			try {
				transaction.begin();
			} catch (NotSupportedException | SystemException e) {
				throw new TechnischeException("Fehler beim Transaction.begin", e);
			}
			return save(entity, loggedInUser);
		} catch (Exception e) {
			try {
				transaction.rollback();
			} catch (IllegalStateException | SecurityException | SystemException e1) {
				throw new TechnischeException("Fehler beim Transaction.rollback", e);
			}
			throw new TechnischeException("Fehler beim Speichern der Entität", e);
		} finally {
			try {
				if (transaction.getStatus() == Status.STATUS_ACTIVE) {
					try {
						transaction.commit();
					} catch (SecurityException | IllegalStateException | HeuristicMixedException
							| HeuristicRollbackException | SystemException e) {
						throw new TechnischeException("Fehler beim Transaction.commit", e);
					}
				}
			} catch (SystemException e) {
				throw new TechnischeException("Fehler beim Transaction.getStatus", e);
			}
		}
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

	@Transactional(TxType.SUPPORTS)
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

	@Transactional(TxType.SUPPORTS)
	public User getUserByUserId(String userId) {
		// das sind Nutzer, die manuell angelegt wurden
		if (StringUtils.isEmpty(userId)) {
			return null;
		}
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> root = query.from(User.class);
		query.where(builder.equal(root.get(User_.userId), userId));
		try {
			return em.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public void saveLastLogonTime(String userId) {
		// verhindert das Eintragen des anonymous update users
		// current_timestamp erfordert korrekte Zeiteinstellung in der DB :-(
		em.createQuery("update User u set u.lastLogon = :current_timestamp where u.userId = :userId")
				.setParameter("current_timestamp", new Date()).setParameter("userId", userId).executeUpdate();
	}

	public void saveToken(Long id, String token, Long expiry) {
		// verhindert das Eintragen des anonymous update users
		em.createQuery("update User u set u.token = :token, u.tokenExpires = :tokenExpires where u.id = :id")
				.setParameter("token", token).setParameter("tokenExpires", expiry).setParameter("id", id)
				.executeUpdate();
	}

	/**
	 * Liefert das Ergebnis einer Suche auf einer Entitäten-Tabelle (eine Zeile pro Entität).
	 * 
	 * @param em
	 *            der Entity-Manager
	 * @param entityType
	 *            der Entity-Typ
	 * @param kriterien
	 *            die Suchkriterien
	 * @param returningType
	 *            der Ergebnis-Typ als Inhalt der Liste im Suchergebnis
	 * @param authorizer
	 *            authorizer für Queries
	 * @return das Ergebnis
	 */
	private <T extends Serializable> Suchergebnis<T> executePagingQuery(EntityManager em, Class<?> entityType,
			Suchkriterien kriterien, Class<T> returningType, Authorizer authorizer) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Paged query: entityType:" + entityType + "\nreturnType:" + returningType + "\n" + kriterien);
		}

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
		Root<?> countRoot = countQuery.from(entityType);
		countQuery.select(builder.count(countRoot));

		List<Filter> filters = kriterien.getFilter();
		Map<String, Object> modifiedFilterValues = filters != null ? new HashMap<String, Object>() : null;

		buildWhereClause(filters, modifiedFilterValues, kriterien.isExact(), kriterien.isIgnoreCase(), builder,
				countQuery, countRoot, null, authorizer);
		TypedQuery<Long> cQuery = em.createQuery(countQuery);
		setParameters(cQuery, filters, modifiedFilterValues, kriterien.isIgnoreCase(), authorizer);
		// wir gehen mal davon aus, dass keine Treffer mit count >
		// Integer.MAX_VALUE vorkommen werden :-/
		int totalSize = cQuery.getSingleResult().intValue();

		CriteriaQuery<T> dataQuery = builder.createQuery(returningType);
		Root<?> dataRoot = dataQuery.from(entityType);
		Set<String> joined = null;
		if (kriterien.getProperties() != null) {
			joined = new HashSet<>();
			From<?, ?> from = dataRoot;

			List<Selection<?>> selection = new ArrayList<Selection<?>>();
			for (String property : kriterien.getProperties()) {
				if (hasDottedNotation(property)) {
					Entry<From<?, ?>, String> entry = followDots(property, from, joined);
					from = entry.getKey();
					property = entry.getValue();
				}
				Path<Object> propPath = from.get(property);
				selection.add(propPath);
				from = dataRoot;
			}
			dataQuery.multiselect(selection.toArray(new Selection<?>[selection.size()]));

			// joine den Benutzer, um nicht später weitere Queries ausführen zu
			// müssen
			if (kriterien.getProperties().contains(AbstractEntity_.updateTime)) {
				// dataQuery.from(User.);
				// Join<Object, Object> join1 =
				// dataRoot.join(AbstractEntity_.createUser.getName(),
				// JoinType.LEFT);
				// join1.on(builder.equal(join1.get(""), y));

			}
		}
		//
		buildWhereClause(filters, modifiedFilterValues, kriterien.isExact(), kriterien.isIgnoreCase(), builder,
				dataQuery, dataRoot, joined, authorizer);
		buildOrderByClause(kriterien.getSortierung(), builder, dataQuery, dataRoot);

		TypedQuery<T> query = em.createQuery(dataQuery);
		setParameters(query, filters, modifiedFilterValues, kriterien.isIgnoreCase(), authorizer);
		query.setMaxResults(kriterien.getZeilenAnzahl());
		query.setFirstResult(kriterien.getZeilenOffset());

		Suchergebnis<T> ergebnis = new Suchergebnis<T>(query.getResultList(), totalSize);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Paged query: gesamt:" + ergebnis.getGesamtgroesse());
		}
		return ergebnis;
	}

	private boolean hasDottedNotation(String property) {
		return property.contains(".");
	}

	private Entry<From<?, ?>, String> followDots(String property, From<?, ?> path, Set<String> joined) {
		for (int index; (index = property.indexOf('.')) != -1;) {
			String first = property.substring(0, index);
			String next = property.substring(index + 1);
			if (path != null && !joined.contains(first)) {
				path = path.join(first, JoinType.LEFT);
				joined.add(first);
			} else if (path != null) {
				for (Join<?, ?> join : path.getJoins()) {
					if (join.getAttribute().getName().equals(first)) {
						path = join;
						break;
					}
				}
			}
			property = next;
		}
		return new SimpleEntry<From<?, ?>, String>(path, property);
	}

	private void buildWhereClause(List<Filter> filters, Map<String, Object> modifiedFilterValues, boolean exact,
			boolean ignoreCase, CriteriaBuilder builder, CriteriaQuery<?> query, Root<?> root, Set<String> joined,
			Authorizer authorizer) {

		List<Predicate> wherePredicates = null;
		if (authorizer != null) {
			Predicate predicate = authorizer.createPredicate(root, builder);
			if (wherePredicates == null)
				wherePredicates = new ArrayList<>();
			wherePredicates.add(predicate);
		}
		if (filters != null) {
			if (joined == null)
				joined = new HashSet<>();
			for (Filter filter : filters) {

				From<?, ?> from = root;
				String property = filter.getName();
				if (hasDottedNotation(property)) {
					Entry<From<?, ?>, String> entry = followDots(property, from, joined);
					from = entry.getKey();
					property = entry.getValue();
				}
				Expression<String> columnExp = from.get(property);

				Predicate predicate = handleColumnTypes(modifiedFilterValues, exact, ignoreCase, builder, filter,
						columnExp);

				if (wherePredicates == null)
					wherePredicates = new ArrayList<>();
				wherePredicates.add(predicate);
			}
		}
		if (wherePredicates != null)
			query.where(builder.and(wherePredicates.toArray(new Predicate[wherePredicates.size()])));
	}

	private Predicate handleColumnTypes(Map<String, Object> modifiedFilterValues, boolean exact, boolean ignoreCase,
			CriteriaBuilder builder, Filter filter, Expression<String> columnExp) {
		Predicate predicate = null;
		String property = filter.getName().replace('.', '_');
		if (String.class.equals(columnExp.getJavaType())) {
			if (exact) {
				if (ignoreCase)
					columnExp = builder.lower(columnExp);
				predicate = createComparison(builder, filter, columnExp, builder.parameter(String.class, property));
			} else {
				if (ignoreCase)
					columnExp = builder.lower(columnExp);
				switch (filter.getComparisonOperator()) {
				default:
					throw new IllegalArgumentException(
							"Unknown comparison operator: " + filter.getComparisonOperator());
				case eq:
					predicate = builder.like(columnExp, builder.parameter(String.class, property));
					break;
				case ne:
					predicate = builder.notLike(columnExp, builder.parameter(String.class, property));
					break;
				}
				modifiedFilterValues.put(filter.getName(), "%" + filter.getValue() + "%");
			}
		} else if (isNumber(columnExp.getJavaType())) {
			predicate = createComparison(builder, filter, columnExp,
					builder.parameter(columnExp.getJavaType(), property));
			modifiedFilterValues.put(filter.getName(), toNumber(columnExp.getJavaType(), filter.getValue().toString()));
		} else if (Boolean.class.equals(columnExp.getJavaType())) {
			predicate = createComparison(builder, filter, columnExp, builder.parameter(Boolean.class, property));
			// null kann gleichbedeutend mit "false" interpretiert werden
			boolean isTrue = toBoolean(filter.getValue().toString());
			if (!isTrue)
				predicate = builder.or(predicate, builder.isNull(columnExp));
			modifiedFilterValues.put(filter.getName(), isTrue);
		} else if (Date.class.equals(columnExp.getJavaType())) {
			predicate = builder.like(columnExp.as(String.class), builder.parameter(String.class, property));
			modifiedFilterValues.put(filter.getName(), "%" + filter.getValue() + "%");
		} else if (columnExp.getJavaType().isEnum()) {
			if (filter.getValue() instanceof Enum) {
				predicate = builder.equal(columnExp, builder.parameter(Enum.class, property));
			} else {
				predicate = builder.equal(columnExp.as(String.class), builder.parameter(String.class, property));
				modifiedFilterValues.put(filter.getName(),
						toEnum(columnExp.getJavaType(), filter.getValue().toString()));
			}
		} else if (columnExp instanceof Path) {
			Path<?> path = (Path<?>) columnExp;
			Bindable<?> model = path.getModel();
			if (model != null) {
				if (AbstractEntity.class.isAssignableFrom(model.getBindableJavaType()))
					predicate = createComparison(builder, filter, path.get(AbstractEntity_.id.getName()),
							builder.parameter(Long.class, property));
				else if (model.getBindableJavaType().isEnum())
					predicate = createComparison(builder, filter, path,
							builder.parameter(model.getBindableJavaType(), property));
			} else if (Collection.class.isAssignableFrom(columnExp.getJavaType())) {

			}
		}
		if (predicate == null)
			throw new IllegalArgumentException("Unrecognized type: " + columnExp.getJavaType().getName());
		return predicate;
	}

	private Predicate createComparison(CriteriaBuilder builder, Filter filter, Expression<?> columnExp,
			ParameterExpression<?> parameterExpression) {
		switch (filter.getComparisonOperator()) {
		case eq:
			return builder.equal(columnExp, parameterExpression);
		case ne:
			return builder.notEqual(columnExp, parameterExpression);
		}
		throw new IllegalArgumentException("Unknown comparison operator: " + filter.getComparisonOperator());
	}

	private void buildOrderByClause(List<SortField> sortierung, CriteriaBuilder builder, CriteriaQuery<?> query,
			From<?, ?> root) {

		List<Order> orders = null;
		if (sortierung != null) {
			Set<String> joined = new HashSet<>();
			for (SortField sort : sortierung) {

				From<?, ?> from = root;
				String property = sort.getSortierFeld();
				if (hasDottedNotation(property)) {
					Entry<From<?, ?>, String> entry = followDots(property, from, joined);
					from = entry.getKey();
					property = entry.getValue();
				}

				Expression<String> feld = from.get(property);
				if (orders == null) {
					orders = new ArrayList<Order>();
				}
				if (sort.isIgnoreCase() && String.class.equals(feld.getJavaType()))
					feld = builder.lower(feld);
				orders.add(sort.isAsc() ? builder.asc(feld) : builder.desc(feld));
			}
		}
		if (orders != null) {
			query.orderBy(orders);
		}
	}

	private boolean isNumber(Class<?> type) {
		return Number.class.isAssignableFrom(type) || type.isPrimitive() && !boolean.class.equals(type);
	}

	private Object toNumber(Class<?> type, String value) {
		try {
			Class<?> wrapperType = ClassUtils.primitiveToWrapper(type);
			if (wrapperType != null)
				type = wrapperType;
			return type.getConstructor(String.class).newInstance(value);
		} catch (Exception e) {
			throw new IllegalArgumentException("Kann keine Instanz von " + type
					+ " mittels one-String-arg-konstruktor mittels " + value + " erzeugen", e);
		}
	}

	private boolean toBoolean(String value) {
		boolean containsTrue = StringUtils.containsIgnoreCase(messages.getMessage("labelTrue"), value);
		return containsTrue;
	}

	private Object toEnum(Class<?> enumType, String value) {
		Object[] values;
		try {
			values = (Object[]) enumType.getMethod("values").invoke(null);
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot get enum values of " + enumType, e);
		}
		for (Object enumValue : values) {
			String text = enumValue.toString();
			if (enumValue instanceof LocalizedEnum)
				text = ((LocalizedEnum) enumValue).getText();
			if (StringUtils.containsIgnoreCase(text, value))
				return Integer.toString(((Enum<?>) enumValue).ordinal());
		}
		return null;
	}

	/**
	 * Setzt die übermittelten Parameter in die Query.
	 * 
	 * @param query
	 *            die JPA-Query
	 * @param args
	 *            die Argumentliste
	 * @param modifiedFilterValues
	 *            ggf. modifizierte Filter
	 * @param authorizer
	 *            optionaler Authorizer
	 */
	private void setParameters(Query query, List<Filter> args, Map<String, Object> modifiedFilterValues,
			boolean ignoreCase, Authorizer authorizer) {
		if (authorizer != null)
			authorizer.setParameters(query);
		if (args != null) {
			for (Filter filter : args) {
				String property = filter.getName();
				Object arg = modifiedFilterValues.containsKey(property) ? modifiedFilterValues.get(property)
						: filter.getValue();
				property = property.replace('.', '_');
				if (arg instanceof Date) {
					query.setParameter(property, (Date) arg, TemporalType.DATE);
				} else {
					query.setParameter(property,
							ignoreCase && arg instanceof String ? ((String) arg).toLowerCase() : arg);
				}
			}
		}
	}

}