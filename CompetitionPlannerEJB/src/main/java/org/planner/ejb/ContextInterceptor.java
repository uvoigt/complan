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
		ClientContext clientContext = new ClientContext();
		for (Object content : map.values()) {
			if (content instanceof Locale)
				clientContext.setLocale((Locale) content);
		}
		ClientContext.set(clientContext);
		try {
			return context.proceed();
		} finally {
			ClientContext.clear();
		}
	}
}
