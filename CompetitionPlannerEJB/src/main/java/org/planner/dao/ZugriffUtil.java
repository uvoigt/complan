package org.planner.dao;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Bindable;

import org.apache.commons.lang.ClassUtils;
import org.planner.eo.AbstractEntity;
import org.planner.eo.AbstractEntity_;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.model.Suchkriterien.Filter;
import org.planner.model.Suchkriterien.Filter.Conditional;
import org.planner.model.Suchkriterien.SortField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementiert Methoden für das Suchen auf Entitäten. Dabei wird insbesondere
 * auf Tabellen mit XML-Type-Columns eingegangen.
 * 
 * @author Uwe Voigt - IBM
 */
public class ZugriffUtil {

	private static final Logger LOG = LoggerFactory.getLogger(ZugriffUtil.class);

	/**
	 * Liefert das Ergebnis einer Suche auf einer Entitäten-Tabelle (eine Zeile
	 * pro Entität).
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
	public static <T extends Serializable> Suchergebnis<T> executePagingQuery(EntityManager em, Class<?> entityType,
			Suchkriterien kriterien, Class<T> returningType, Authorizer authorizer) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Paged query: entityType:" + entityType + "\nreturnType:" + returningType + "\n" + kriterien);
		}

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
		Root<?> countRoot = countQuery.from(entityType);
		countQuery.select(builder.count(countRoot));

		List<Filter> filters = kriterien.getFilter();
		if (authorizer != null)
			filters = authorizer.addFilters(filters != null ? filters : new ArrayList<Filter>());
		Map<String, Object> modifiedFilterValues = filters != null ? new HashMap<String, Object>() : null;

		buildWhereClause(filters, modifiedFilterValues, kriterien.isExact(), kriterien.isIgnoreCase(), builder,
				countQuery, countRoot, null);
		TypedQuery<Long> cQuery = em.createQuery(countQuery);
		setParameters(cQuery, filters, kriterien.isIgnoreCase());
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
		}
		buildWhereClause(filters, modifiedFilterValues, kriterien.isExact(), kriterien.isIgnoreCase(), builder,
				dataQuery, dataRoot, joined);
		buildOrderByClause(kriterien.getSortierung(), builder, dataQuery, dataRoot);

		TypedQuery<T> query = em.createQuery(dataQuery);
		setParameters(query, filters, kriterien.isIgnoreCase());
		query.setMaxResults(kriterien.getZeilenAnzahl());
		query.setFirstResult(kriterien.getZeilenOffset());

		Suchergebnis<T> ergebnis = new Suchergebnis<T>(query.getResultList(), totalSize);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Paged query: gesamt:" + ergebnis.getGesamtgroesse());
		}
		return ergebnis;
	}

	private static boolean hasDottedNotation(String property) {
		return property.contains(".");
	}

	private static Entry<From<?, ?>, String> followDots(String property, From<?, ?> path, Set<String> joined) {
		for (int index; (index = property.indexOf('.')) != -1;) {
			String first = property.substring(0, index);
			String next = property.substring(index + 1);
			if (path != null && !joined.contains(first)) {
				path = path.join(first, JoinType.LEFT);
				joined.add(first);
			}
			property = next;
		}
		return new SimpleEntry<From<?, ?>, String>(path, property);
	}

	private static void buildWhereClause(List<Filter> filters, Map<String, Object> modifiedFilterValues, boolean exact,
			boolean ignoreCase, CriteriaBuilder builder, CriteriaQuery<?> query, From<?, ?> root, Set<String> joined) {

		List<Predicate> wherePredicates = null;
		List<Conditional> ops = null;
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
						property, columnExp);

				if (wherePredicates == null) {
					wherePredicates = new ArrayList<>();
					ops = new ArrayList<>();
				}
				wherePredicates.add(predicate);
				ops.add(filter.getConditionalOperator());
			}
		}
		if (wherePredicates != null) {
			Expression<Boolean> restriction = null;
			for (int i = 0; i < wherePredicates.size(); i++) {
				Predicate predicate = wherePredicates.get(i);
				Conditional op = ops.get(i);
				if (restriction == null) {
					restriction = predicate;
				} else {
					if (op == Conditional.and)
						restriction = builder.and(restriction, predicate);
					else
						restriction = builder.or(restriction, predicate);
				}
			}
			query.where(restriction);
		}
	}

	private static Predicate handleColumnTypes(Map<String, Object> modifiedFilterValues, boolean exact,
			boolean ignoreCase, CriteriaBuilder builder, Filter filter, String property, Expression<String> columnExp) {
		Predicate predicate = null;
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
			filter.setValue(toNumber(columnExp.getJavaType(), filter.getValue().toString()));
			predicate = createComparison(builder, filter, columnExp,
					builder.parameter(filter.getValue().getClass(), property));
		} else if (Boolean.class.equals(columnExp.getJavaType())) {
			predicate = createComparison(builder, filter, columnExp, builder.parameter(Boolean.class, property));
			modifiedFilterValues.put(filter.getName(), toBoolean(filter.getValue().toString()));
		} else if (Date.class.equals(columnExp.getJavaType())) {
			predicate = builder.like(columnExp.as(String.class), builder.parameter(String.class, property));
			filter.setValue("%" + filter.getValue() + "%");
		} else if (columnExp.getJavaType().isEnum()) {
			// TODO wenn ein Buchstabe angegeben wurde, der die Enum
			// eindeutig erkennbar macht, dann reicht das!
			Class<?> javaType = columnExp.getJavaType();
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Enum<?> enumValue = Enum.valueOf((Class<Enum>) javaType, (String) filter.getValue());
			predicate = builder.equal(columnExp.as(String.class), builder.parameter(String.class, property));
			filter.setValue(Integer.toString(enumValue.ordinal()));
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
			}
		}
		if (predicate == null)
			throw new IllegalArgumentException("Unrecognized type: " + columnExp.getJavaType().getName());
		return predicate;
	}

	private static Predicate createComparison(CriteriaBuilder builder, Filter filter, Expression<?> columnExp,
			ParameterExpression<?> parameterExpression) {
		switch (filter.getComparisonOperator()) {
		case eq:
			return builder.equal(columnExp, parameterExpression);
		case ne:
			return builder.notEqual(columnExp, parameterExpression);
		}
		throw new IllegalArgumentException("Unknown comparison operator: " + filter.getComparisonOperator());
	}

	private static void buildOrderByClause(List<SortField> sortierung, CriteriaBuilder builder, CriteriaQuery<?> query,
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

	private static boolean isNumber(Class<?> type) {
		return Number.class.isAssignableFrom(type) || type.isPrimitive() && !boolean.class.equals(type);
	}

	private static Object toNumber(Class<?> type, String value) {
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

	private static Object toBoolean(String value) {
		// TODO
		return null;
	}

	/**
	 * Setzt die übermittelten Parameter in die Query.
	 * 
	 * @param query
	 *            die JPA-Query
	 * @param args
	 *            die Argumentliste
	 */
	public static void setParameters(Query query, List<Filter> args, boolean ignoreCase) {
		if (args != null) {
			for (Filter filter : args) {
				String property = filter.getName();
				if (hasDottedNotation(property)) {
					property = followDots(property, null, null).getValue();
				}
				Object arg = filter.getValue();
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