package org.planner.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.planner.model.Suchkriterien.Filter;

public abstract class QueryModifier {

	private List<Filter> params;

	@SuppressWarnings("rawtypes")
	public abstract Predicate createPredicate(Root root, CriteriaBuilder builder);

	protected Expression<?> nextParam(CriteriaBuilder builder, Object value) {
		if (params == null)
			params = new ArrayList<>();
		String param = "qparam" + params.size();
		params.add(new Filter(param, value));
		return builder.parameter(value.getClass(), param);
	}

	public void setParameters(Query query) {
		if (params != null) {
			for (Filter param : params) {
				query.setParameter(param.getName(), param.getValue());
			}
			params.clear();
		}
	}
}
