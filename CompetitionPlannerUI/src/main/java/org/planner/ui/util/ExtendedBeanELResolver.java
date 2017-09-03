package org.planner.ui.util;

import javax.el.BeanELResolver;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;

public class ExtendedBeanELResolver extends BeanELResolver {
	private final ELResolver delegate;

	public ExtendedBeanELResolver(ELResolver delegate) {
		this.delegate = delegate;
	}

	@Override
	public Object getValue(ELContext context, Object base, Object property)
			throws NullPointerException, PropertyNotFoundException, ELException {

		String propertyString = property.toString();

		if (propertyString != null && propertyString.contains(".")) {
			Object value = base;

			for (String propertyPart : propertyString.split("\\.")) {
				value = delegate.getValue(context, value, propertyPart);
			}

			return value;
		} else {
			return delegate.getValue(context, base, property);
		}
	}
}