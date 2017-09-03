package org.planner.dao;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Date;
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
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Bindable;

import org.apache.commons.lang.ClassUtils;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
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
	 * Liefert das Ergebnis einer Suche auf einer EntitÃ¤ten-Tabelle (eine Zeile
	 * pro Entität).
	 * 
	 * @param em
	 *            der Entity-Manager
	 * @param entityType
	 *            der Entity-Typ
	 * @param kriterien
	 *            die Suchkriterien
	 * @return das Ergebnis
	 */
	public static <T extends Serializable> Suchergebnis<T> executePagingQuery(EntityManager em, Class<T> entityType,
			Suchkriterien kriterien) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Paged query: entityType:" + entityType + "\n" + kriterien);
		}

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
		Root<?> countRoot = countQuery.from(entityType);
		countQuery.select(builder.count(countRoot));
		buildWhereClause(kriterien.getFilter(), kriterien.isExact(), kriterien.isIgnoreCase(), builder, countQuery,
				countRoot);
		TypedQuery<Long> cQuery = em.createQuery(countQuery);
		setParameters(cQuery, kriterien.getFilter(), kriterien.isIgnoreCase());
		// wir gehen mal davon aus, dass keine Treffer mit count >
		// Integer.MAX_VALUE vorkommen werden :-/
		int totalSize = cQuery.getSingleResult().intValue();

		CriteriaQuery<T> dataQuery = builder.createQuery(entityType);
		Root<T> dataRoot = dataQuery.from(entityType);
		buildWhereClause(kriterien.getFilter(), kriterien.isExact(), kriterien.isIgnoreCase(), builder, dataQuery,
				dataRoot);
		buildOrderByClause(kriterien.getSortierung(), builder, dataQuery, dataRoot);

		TypedQuery<T> query = em.createQuery(dataQuery);
		setParameters(query, kriterien.getFilter(), kriterien.isIgnoreCase());
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

	private static void buildWhereClause(Map<String, Object> filters, boolean exact, boolean ignoreCase,
			CriteriaBuilder builder, CriteriaQuery<?> query, From<?, ?> root) {

		List<Predicate> wherePredicates = null;
		if (filters != null) {
			Set<String> joined = new HashSet<>();
			for (Entry<String, Object> filter : filters.entrySet()) {

				String property = filter.getKey();
				if (hasDottedNotation(property)) {
					Entry<From<?, ?>, String> entry = followDots(property, root, joined);
					root = entry.getKey();
					property = entry.getValue();
				}
				Expression<String> columnExp = root.get(property);

				Predicate predicate = null;
				if (String.class.equals(columnExp.getJavaType())) {
					if (exact) {
						if (ignoreCase)
							columnExp = builder.lower(columnExp);
						predicate = builder.equal(columnExp, builder.parameter(String.class, property));
					} else {
						if (ignoreCase)
							columnExp = builder.lower(columnExp);
						predicate = builder.like(columnExp, builder.parameter(String.class, property));
						filter.setValue("%" + filter.getValue() + "%");
					}
				} else if (isNumber(columnExp.getJavaType())) {
					filter.setValue(toNumber(columnExp.getJavaType(), filter.getValue().toString()));
					predicate = builder.equal(columnExp, builder.parameter(filter.getValue().getClass(), property));
				} else if (columnExp instanceof Path) {
					Path<String> path = (Path<String>) columnExp;
					Bindable<String> model = path.getModel();
					if (model != null)
						predicate = builder.equal(path.get("id"), builder.parameter(Long.class, property));
				}
				if (predicate == null)
					throw new IllegalArgumentException("Unrecognized type: " + columnExp.getJavaType().getName());

				if (wherePredicates == null)
					wherePredicates = new ArrayList<Predicate>();
				wherePredicates.add(predicate);
			}
		}
		if (wherePredicates != null)
			query.where(builder.and(wherePredicates.toArray(new Predicate[wherePredicates.size()])));
	}

	private static void buildOrderByClause(List<SortField> sortierung, CriteriaBuilder builder, CriteriaQuery<?> query,
			From<?, ?> root) {

		List<Order> orders = null;
		if (sortierung != null) {
			Set<String> joined = new HashSet<>();
			for (SortField sort : sortierung) {

				String property = sort.getSortierFeld();
				if (hasDottedNotation(property)) {
					Entry<From<?, ?>, String> entry = followDots(property, root, joined);
					root = entry.getKey();
					property = entry.getValue();
				}

				Expression<String> feld = root.get(property);
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

	/**
	 * Setzt die übermittelten Parameter in die Query.
	 * 
	 * @param query
	 *            die JPA-Query
	 * @param args
	 *            die Argumentliste
	 */
	public static void setParameters(Query query, Map<String, Object> args, boolean ignoreCase) {
		if (args != null) {
			for (Entry<String, Object> e : args.entrySet()) {
				String property = e.getKey();
				if (hasDottedNotation(property)) {
					property = followDots(property, null, null).getValue();
				}
				Object arg = e.getValue();
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