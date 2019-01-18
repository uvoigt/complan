package org.planner.ui.util;

import java.util.Locale;

import javax.faces.context.FacesContext;

import org.jboss.ejb.client.AttachmentKey;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.planner.util.ClientContext;

/*
 https://github.com/xcoulon/wildfly-quickstart/tree/master/ejb-security-interceptors
*/
public class ContextInterceptor implements EJBClientInterceptor {

	@SuppressWarnings("deprecation")
	public void register() {
		EJBClientContext.requireCurrent().registerInterceptor(0, this);
	}

	@Override
	public void handleInvocation(EJBClientInvocationContext context) throws Exception {
		FacesContext ctx = FacesContext.getCurrentInstance();
		if (ctx != null) {
			Locale locale = ctx.getApplication().getDefaultLocale();
			ClientContext clientContext = new ClientContext();
			clientContext.setLocale(locale);
			context.putAttachment(new AttachmentKey<>(), locale);
		}
		context.sendRequest();
	}

	@Override
	public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
		return context.getResult();
	}
}
