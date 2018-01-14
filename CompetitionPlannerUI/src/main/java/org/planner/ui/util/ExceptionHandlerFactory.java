package org.planner.ui.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ResourceBundle;

import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.planner.util.LogUtil.FachlicheException;
import org.primefaces.application.exceptionhandler.ExceptionInfo;
import org.primefaces.application.exceptionhandler.PrimeExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandlerFactory extends javax.faces.context.ExceptionHandlerFactory {
	private class ExceptionHandler extends PrimeExceptionHandler {

		private ExceptionHandler(javax.faces.context.ExceptionHandler wrapped) {
			super(wrapped);
		}

		@Override
		public Throwable getRootCause(Throwable throwable) {
			Throwable rootCause = super.getRootCause(throwable);
			if (rootCause instanceof EJBException) {
				Exception causedByException = ((EJBException) rootCause).getCausedByException();
				if (causedByException != null)
					return causedByException;
			}
			Throwable ejbException = lookupEJBException(rootCause);
			if (ejbException instanceof EJBAccessException)
				return ejbException;
			return rootCause;
		}

		private Throwable lookupEJBException(Throwable ex) {
			if (ex instanceof EJBException)
				return ex;
			Throwable cause = ex.getCause();
			if (cause == null)
				return null;
			if (cause instanceof InvocationTargetException)
				cause = ((InvocationTargetException) cause).getTargetException();
			return lookupEJBException(cause);
		}

		@Override
		protected ExceptionInfo createExceptionInfo(Throwable rootCause) throws IOException {
			if (rootCause instanceof EJBAccessException) {
				ExceptionInfo info = new ExceptionInfo();
				info.setMessage(ResourceBundle.getBundle("MessagesBundle").getString("illgalAccess"));
				return info;
			}
			ExceptionInfo info = super.createExceptionInfo(rootCause);
			info.setFormattedStackTrace(info.getFormattedStackTrace().replace("Caused by:", "<b>Caused by:</b>"));
			return info;
		}

		@Override
		protected boolean isLogException(FacesContext context, Throwable rootCause) {
			boolean result = super.isLogException(context, rootCause);
			return result && !(rootCause instanceof FachlicheException);
		}

		@Override
		protected void handleAjaxException(FacesContext context, Throwable rootCause, ExceptionInfo info)
				throws Exception {
			if (LOG.isInfoEnabled() && !(rootCause instanceof FachlicheException)) {
				LOG.info("handleAjaxException: " + getComponentTreeAsString());
			}
			super.handleAjaxException(context, rootCause, info);
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlerFactory.class);

	public static String getComponentTreeAsString() {
		StringBuilder tree = new StringBuilder();
		UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
		if (viewRoot == null)
			return "no view root";
		List<UIComponent> componentTree = viewRoot.getChildren();
		for (UIComponent uiComponent : componentTree) {
			appendComponentInfo(uiComponent, tree, 0);
		}
		return tree.toString();
	}

	private static String getComponentInfo(UIComponent comp) {
		return (comp == null) ? "" : (comp.getClass().getSimpleName() + ": " + comp.getId());
	}

	private static void appendComponentInfo(UIComponent comp, StringBuilder sb, int level) {
		if (comp == null) {
			return;
		}
		String indentation = (level == 0) ? "" : String.format("%" + (4 * level) + "s", " ");
		sb.append(indentation + getComponentInfo(comp) + "\n");
		List<UIComponent> children = comp.getChildren();
		int size = children.size();
		level = (size > 0) ? (level + 1) : level;
		for (UIComponent c : children) {
			appendComponentInfo(c, sb, level);
		}
		level = (size > 0) ? (level - 1) : level;
	}

	private final javax.faces.context.ExceptionHandlerFactory parent;

	public ExceptionHandlerFactory(final javax.faces.context.ExceptionHandlerFactory parent) {
		this.parent = parent;
	}

	@Override
	public ExceptionHandler getExceptionHandler() {
		return new ExceptionHandler(parent.getExceptionHandler());
	}
}
