package org.planner.ejb;

import java.util.Locale;
import java.util.Map;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.planner.util.ClientContext;

@Context
@Interceptor
public class ContextInterceptor {

	@AroundInvoke
	public Object intercept(InvocationContext context) throws Exception {
		Map<String, Object> data = context.getContextData();
		@SuppressWarnings("unchecked")
		Map<Object, Object> map = (Map<Object, Object>) data.get("org.jboss.ejb.client.invocation.attachments");
		Locale locale = (Locale) map.values().iterator().next();
		ClientContext clientContext = new ClientContext();
		clientContext.setLocale(locale);
		ClientContext.set(clientContext);
		try {
			return context.proceed();
		} finally {
			ClientContext.clear();
		}
	}
}
