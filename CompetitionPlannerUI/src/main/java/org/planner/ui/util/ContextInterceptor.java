package org.planner.ui.util;

import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;

import org.jboss.ejb.client.AttachmentKey;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.planner.util.ClientContext;

//@Singleton
//@Startup
public class ContextInterceptor implements EJBClientInterceptor {

	@PostConstruct
	public void register() {
		EJBClientContext.requireCurrent().registerInterceptor(0, this);
	}

	@Override
	public void handleInvocation(EJBClientInvocationContext context) throws Exception {
		Locale locale = FacesContext.getCurrentInstance().getApplication().getDefaultLocale();
		ClientContext clientContext = new ClientContext();
		clientContext.setLocale(locale);
		context.putAttachment(new AttachmentKey<>(), locale);
		context.sendRequest();
	}

	@Override
	public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
		return context.getResult();
	}
}
