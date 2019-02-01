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
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Attribute;
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
import org.planner.eo.AbstractEnum;
import org.planner.eo.CanDelete;
import org.planner.eo.HasId;
import org.planner.eo.HasId_;
import org.planner.eo.Properties;
import org.planner.eo.Properties_;
import org.planner.eo.User;
import org.planner.model.FetchInfo;
import org.planner.model.LocalizedEnum;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.model.Suchkriterien.Filter;
import org.planner.model.Suchkriterien.Property;
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
	public <T extends Serializable> T getById(Class<T> type, Object id) {
		return em.find(type, id);
	}

	@Transactional(TxType.SUPPORTS)
	public <T extends HasId> T getById(Class<T> type, Object id, FetchInfo... fetchInfo) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<T> query = builder.createQuery(type);
		Root<T> root = query.from(type);
		addFetches(root, fetchInfo);
		query.where(builder.equal(root.get(HasId_.id), builder.parameter(Long.class, "id")));
		T result = em.createQuery(query).setParameter("id", id).getSingleResult();
		return result;
	}

	private void addFetches(FetchParent<?, ?> root, FetchInfo... fetchInfo) {
		if (fetchInfo == null)
			return;
		for (FetchInfo info : fetchInfo) {
			if (!info.isFetch())
				continue;
			Attribute<?, ?> attribute = info.getAttribute();
			Fetch<Object, Object> fetch = root.fetch(attribute.getName(), JoinType.LEFT);
			for (FetchInfo child : info.getChildren()) {
				addFetches(fetch, child);
			}
		}
	}

	@Transactional(TxType.SUPPORTS)
	public <T extends Serializable> Suchergebnis<T> search(Class<?> entityType, Class<T> returningType,
			Suchkriterien kriterien, QueryModifier queryModifier) {
		return executePagingQuery(em, entityType, kriterien, returningType, queryModifier);
	}

	@Transactional(TxType.SUPPORTS)
	public <T extends Serializable> Suchergebnis<T> search(Class<T> entityType, Suchkriterien kriterien,
			QueryModifier queryModifier) {
		return executePagingQuery(em, entityType, kriterien, entityType, queryModifier);
	}

	public void delete(Object entity) {
		if (entity instanceof CanDelete)
			((CanDelete) entity).delete(em);
		else if (entity != null)
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
			// für Kopien
			entity.setVersion(0);
			entity.setUpdateUser(null);
			entity.setUpdateTime(null);
			em.persist(entity);
		} else {
			entity.setUpdateUser(loggedInUser);
			entity.setUpdateTime(new Date());
			em.merge(entity);
		}
		return entity;
	}

	public <T extends AbstractEnum> T saveEnum(T entity, String loggedInUser) {
		if (entity.getId() == null) {
			entity.setCreateUser(loggedInUser);
			entity.setCreateTime(new Date());
			em.persist(entity);
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
		if (StringUtils.isEmpty(userId))
			return null;

		try {
			return (User) em.createNamedQuery("userById").setParameter("userId", userId).getSingleResult();
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

	public void saveToken(User user) {
		// verhindert das Eintragen des anonymous update users
		em.merge(user);
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
	 * @param queryModifier
	 *            Modifier für Queries
	 * @return das Ergebnis
	 */
	private <T extends Serializable> Suchergebnis<T> executePagingQuery(EntityManager em, Class<?> entityType,
			Suchkriterien kriterien, Class<T> returningType, QueryModifier queryModifier) {

		if (LOG.isDebugEnabled()) {
			LOG.debug((kriterien.isCountOnly() ? "Count" : "Paged") + " query: entityType:" + entityType
					+ "\nreturnType:" + returningType + "\n" + kriterien);
		}

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
		Root<?> countRoot = countQuery.from(entityType);
		countQuery.select(builder.count(countRoot));

		Map<String, Filter> filters = kriterien.getFilter();
		Map<String, Object> modifiedFilterValues = filters != null ? new HashMap<String, Object>() : null;

		buildWhereClause(filters, modifiedFilterValues, kriterien.isExact(), kriterien.isIgnoreCase(), builder,
				countQuery, countRoot, null, queryModifier);
		TypedQuery<Long> cQuery = em.createQuery(countQuery);
		setParameters(cQuery, filters, modifiedFilterValues, kriterien.isIgnoreCase(), queryModifier);
		// wir gehen mal davon aus, dass keine Treffer mit count >
		// Integer.MAX_VALUE vorkommen werden :-/
		int totalSize = cQuery.getSingleResult().intValue();
		if (kriterien.isCountOnly())
			return new Suchergebnis<>(null, totalSize);

		CriteriaQuery<T> dataQuery = builder.createQuery(returningType);
		Root<?> dataRoot = dataQuery.from(entityType);
		Set<String> joined = null;
		Map<Expression<?>, Expression<?>> columnReplacement = null;
		if (kriterien.getProperties() != null) {
			joined = new HashSet<>();
			From<?, ?> from = dataRoot;

			List<Selection<?>> selection = new ArrayList<>();
			for (Property property : kriterien.getProperties()) {
				String propertyName = property.getName();
				if (hasDottedNotation(propertyName)) {
					Entry<From<?, ?>, String> entry = followDots(propertyName, from, joined);
					from = entry.getKey();
					propertyName = entry.getValue();
				}
				Path<Object> propPath = from.get(propertyName);
				String multiRowGroup = property.getMultiRowGroup();
				if (StringUtils.isNotEmpty(multiRowGroup)) {
					Expression<String> concat = builder.function("group_concat", String.class, propPath, propPath);
					selection.add(concat);
					if (columnReplacement == null)
						columnReplacement = new HashMap<>();
					columnReplacement.put(propPath, concat);
					Expression<?> groupBy = dataRoot.get(multiRowGroup);
					dataQuery.groupBy(groupBy);
					// columnReplacement = .builder..
				} else {
					// MySql liefert sonst Bits als Strings, was bei Bit 0 " " liefert und zu Boolean.TRUE führt
					if (Boolean.class.equals(propPath.getJavaType()))
						selection.add(builder.function("to_boolean", Boolean.class, propPath));
					else
						selection.add(propPath);
				}
				from = dataRoot;
			}
			dataQuery.multiselect(selection.toArray(new Selection<?>[selection.size()]));
		}
		//
		buildWhereClause(filters, modifiedFilterValues, kriterien.isExact(), kriterien.isIgnoreCase(), builder,
				dataQuery, dataRoot, joined, queryModifier);
		buildOrderByClause(kriterien.getSortierung(), builder, dataQuery, dataRoot, joined, columnReplacement);

		TypedQuery<T> query = em.createQuery(dataQuery);
		setParameters(query, filters, modifiedFilterValues, kriterien.isIgnoreCase(), queryModifier);
		query.setMaxResults(kriterien.getZeilenAnzahl());
		query.setFirstResult(kriterien.getZeilenOffset());

		Suchergebnis<T> ergebnis = new Suchergebnis<>(query.getResultList(), totalSize);
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
				path = path.join(first, JoinType.LEFT); // fetch
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

	private void buildWhereClause(Map<String, Filter> filters, Map<String, Object> modifiedFilterValues, boolean exact,
			boolean ignoreCase, CriteriaBuilder builder, CriteriaQuery<?> query, Root<?> root, Set<String> joined,
			QueryModifier queryModifier) {

		List<Predicate> wherePredicates = null;
		if (queryModifier != null) {
			Predicate predicate = queryModifier.createPredicate(root, builder);
			if (wherePredicates == null)
				wherePredicates = new ArrayList<>();
			wherePredicates.add(predicate);
		}
		if (filters != null) {
			if (joined == null)
				joined = new HashSet<>();
			for (Filter filter : filters.values()) {

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
		} else if (Boolean.class.equals(columnExp.getJavaType()) || boolean.class.equals(columnExp.getJavaType())) {
			predicate = createComparison(builder, filter, columnExp, builder.parameter(Boolean.class, property));
			// null kann gleichbedeutend mit "false" interpretiert werden
			boolean isTrue = toBoolean(filter.getValue().toString());
			if (!isTrue)
				predicate = builder.or(predicate, builder.isNull(columnExp));
			modifiedFilterValues.put(filter.getName(), isTrue);
		} else if (Date.class.equals(columnExp.getJavaType())) {
			predicate = builder.like(
					builder.function("date_format", String.class, columnExp, builder.literal("%d.%m.%Y")),
					builder.parameter(String.class, property));
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

	@SuppressWarnings("unchecked")
	private void buildOrderByClause(List<SortField> sortierung, CriteriaBuilder builder, CriteriaQuery<?> query,
			From<?, ?> root, Set<String> joined, Map<Expression<?>, Expression<?>> columnReplacement) {

		List<Order> orders = null;
		if (sortierung != null) {
			if (joined == null)
				joined = new HashSet<>();
			for (SortField sort : sortierung) {

				From<?, ?> from = root;
				String property = sort.getSortierFeld();
				if (hasDottedNotation(property)) {
					Entry<From<?, ?>, String> entry = followDots(property, from, joined);
					from = entry.getKey();
					property = entry.getValue();
				}

				Expression<?> feld = from.get(property);
				if (orders == null) {
					orders = new ArrayList<>();
				}
				if (columnReplacement != null) {
					Expression<?> replacement = columnReplacement.get(feld);
					if (replacement != null)
						feld = replacement;
				}
				if (sort.isIgnoreCase() && String.class.equals(feld.getJavaType()))
					feld = builder.lower((Expression<String>) feld);
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
	 * @param filters
	 *            die Argumentliste
	 * @param modifiedFilterValues
	 *            ggf. modifizierte Filter
	 * @param queryModifier
	 *            optionaler QueryModifier
	 */
	private void setParameters(Query query, Map<String, Filter> filters, Map<String, Object> modifiedFilterValues,
			boolean ignoreCase, QueryModifier queryModifier) {
		if (queryModifier != null)
			queryModifier.setParameters(query);
		if (filters != null) {
			for (Filter filter : filters.values()) {
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

	public <T> T executeOperation(IOperation<T> op) {
		return op.execute(em);
	}
}