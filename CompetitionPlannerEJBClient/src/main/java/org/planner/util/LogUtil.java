package org.planner.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.ejb.ApplicationException;
import javax.ejb.EJBException;
import javax.faces.FacesException;
import javax.persistence.OptimisticLockException;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.StandardToStringStyle;
import org.slf4j.Logger;

/**
 * Contains utilities for logging.
 * 
 * @author voigtu002
 */
public class LogUtil {
	/**
	 * Exception für das EJB-Interface.
	 */
	@ApplicationException(rollback = true)
	public abstract static class KonfigException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		protected transient boolean logged;

		/**
		 * Technischer Fehler. Die Nachricht sollte nicht an der Oberfläche erscheinen.
		 * 
		 * @param message
		 *            Meldung für das Log
		 * @param ex
		 *            die causing Exception
		 */
		protected KonfigException(String message, Throwable ex) {
			super(message, ex);
		}

		/**
		 * Fachlicher Fehler. Die Nachricht im Bundle ist für die Oberfläche.
		 * 
		 * @param bundle
		 *            ResourceBundle
		 * @param key
		 *            Key in diesem Bundle
		 * @throws MissingResourceException
		 *             der Key ist nicht im Bundle enthalten
		 * @param args
		 *            optionale Argumente für das Message-Formatting
		 */
		protected KonfigException(ResourceBundle bundle, String key, Object... args) throws MissingResourceException {
			super(MessageFormat.format(bundle.getString(key), args));
		}
	}

	/**
	 * Fehler, dessen Text an der Oberfläche erscheint.
	 */
	public static class FachlicheException extends KonfigException {
		private static final long serialVersionUID = 1L;

		private List<String> messages;

		public FachlicheException(ResourceBundle bundle, String key, Object... args) {
			super(bundle, key, args);
		}

		public void addMessage(ResourceBundle bundle, String key, Object... args) {
			if (messages == null) {
				messages = new ArrayList<>();
				messages.add(getMessage());
			}
			messages.add(MessageFormat.format(bundle.getString(key), args));
		}

		public List<String> getMessages() {
			return messages;
		}
	}

	/**
	 * Fehler, dessen Text nicht an der Oberfläche erscheint.
	 */
	@ApplicationException(rollback = true)
	public static class TechnischeException extends KonfigException {
		private static final long serialVersionUID = 1L;

		private String causingTrace;

		public TechnischeException(String message, Throwable cause) {
			super(message, cause);
			saveCause(cause);
		}

		private void saveCause(Throwable cause) {
			if (cause != null) {
				StringWriter sw = new StringWriter(12000);
				cause.printStackTrace(new PrintWriter(sw));
				causingTrace = sw.toString();
			}
		}

		@Override
		public void printStackTrace(PrintWriter err) {
			super.printStackTrace(err);
			if (causingTrace != null) {
				err.println("\nCaused by:" + causingTrace);
			}
		}
	}

	private static class ToStringStyle extends StandardToStringStyle {

		private static final long serialVersionUID = 1L;

		@Override
		protected void appendInternal(StringBuffer buffer, String fieldName, Object value, boolean detail) {
			try {
				super.appendInternal(buffer, fieldName, value, detail);
			} catch (Exception e) {
				// normalerweise LazyInitialisationException
				super.appendDetail(buffer, fieldName, "***" + e + "***");
			}
		}
	}

	/**
	 * Konstruktor.
	 */
	private LogUtil() {
		// no need to instantiate
	}

	/**
	 * Behandelt die Exception indem sie geloggt wird. Im Client-Kontext (wenn Primefaces im Classpath gefunden wird)
	 * wird außerdem für eine {@link FachlicheException} eine <code>FacesMessage</code> erzeugt.
	 * 
	 * @param t
	 *            die Exception
	 * @param log
	 *            Logger
	 * @param message
	 *            log und Fehlermessage
	 * @param params
	 *            Methodenparameter
	 * @throws FachlicheException
	 * @throws KonfigException
	 */
	public static void handleException(Throwable t, Logger log, String message, Object... params)
			throws KonfigException {
		if (t instanceof OptimisticLockException)
			// nur für die Serverseite
			throw new FachlicheException(CommonMessages.getResourceBundle(), "optimisticLock");
		if (t instanceof FachlicheException) {
			FachlicheException fex = (FachlicheException) t;
			if (!fex.logged) {
				log.debug(message + " - Parameter der Methode: " + paramString(-1, params), t);
				fex.logged = true;
			}
			if (isClient()) {
				// wird im ExceptionHandler behandelt
				// FacesContext.getCurrentInstance().addMessage(null,
				// new FacesMessage(FacesMessage.SEVERITY_WARN, null,
				// t.getMessage()));
				// return;
				throw fex;
			} else {
				throw fex;
			}
		}
		if (t instanceof InvocationTargetException)
			t = ((InvocationTargetException) t).getTargetException();
		// Faces-Exception nicht wrappen, sie wird ausgewertet (z.B. die
		// ViewExpiredExcpetion)
		if (t instanceof FacesException)
			throw (FacesException) t;
		if (t instanceof EJBException) {
			t = ((EJBException) t).getCausedByException();
		}
		TechnischeException tex = t instanceof TechnischeException ? (TechnischeException) t : null;
		if (tex == null || !tex.logged)
			logException(t, log, message, params);
		if (tex != null) {
			tex.logged = true;
			throw tex;
		}
		throw new TechnischeException(message, t);
	}

	public static void logException(Throwable t, Logger log, String message, Object... params) {
		log.error(message + " - Parameter der Methode: " + paramString(-1, params), t);
	}

	private static boolean isClient() {
		try {
			Thread.currentThread().getContextClassLoader().loadClass("org.primefaces.context.ApplicationContext");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Logs a method entry at the trace level.
	 * 
	 * @param pLog
	 *            the log, must not be null
	 * @param pMethodName
	 *            the method name
	 * @param pParams
	 *            optional parameters, this may be an object array
	 */
	public static void entry(Logger pLog, String pMethodName, Object... pParams) {
		pLog.trace("Entry: " + pMethodName + "(" + paramString(pParams) + ")");
	}

	/**
	 * Logs a method exit at the trace level.
	 * 
	 * @param pLog
	 *            the log, must not be null
	 * @param pMethodName
	 *            the method name
	 * @param pResult
	 *            the method result
	 */
	public static void exit(Logger pLog, String pMethodName, Object pResult) {
		pLog.trace("Exit: " + pMethodName + "(" + paramString(pResult) + ")");
	}

	/**
	 * Logs a method exit at the trace level.
	 * 
	 * @param pLog
	 *            the log, must not be null
	 * @param pMethodName
	 *            the method name
	 */
	public static void exit(Logger pLog, String pMethodName) {
		pLog.trace("Exit: " + pMethodName + "()");
	}

	/**
	 * Returns a string in the following format.
	 * <ul>
	 * <li>parameter class name = parameter value</li>
	 * <li>if the parameter is an object array, a list of string representations is returned</li>
	 * </ul>
	 * The resulting string is cut if it is longer than 200 characters
	 * 
	 * @param pParams
	 *            the parameter
	 * @return the string format
	 */
	public static String paramString(Object... pParams) {
		return paramString(500, pParams);
	}

	/**
	 * Returns a string in the following format.
	 * <ul>
	 * <li>parameter class name = parameter value</li>
	 * <li>if the parameter is an object array, a list of string representations is returned</li>
	 * </ul>
	 * 
	 * @param pMaxLength
	 *            the maximal length of the resulting string, if -1, the result string length is never cut
	 * @param pParams
	 *            the parameter
	 * @return the string format
	 */
	public static String paramString(int pMaxLength, Object... pParams) {
		if (pParams == null) {
			return null;
		}
		String[] strings = new String[pParams.length];
		int length = 0;
		for (int i = 0; i < pParams.length; i++) {
			String s = paramName(pParams[i]) + "=" + paramValue(pParams[i]);
			if (pMaxLength != -1 && s.length() > pMaxLength) {
				s = s.substring(0, pMaxLength) + "...............";
			}
			length += s.length();
			strings[i] = s;
		}
		StringBuilder result = new StringBuilder(length + (length >> 3));
		boolean doLbr = length > 50;
		for (int i = 0, n = strings.length; i < n; i++) {
			if (doLbr) {
				if (i > 0 || n > 1) {
					result.append('\n');
				}
				if (n > 1) {
					result.append("   ");
				}
			} else {
				if (i > 0) {
					result.append(", ");
				}
			}
			result.append(strings[i]);
		}
		return result.toString();
	}

	private static String paramName(Object pObject) {
		if (pObject == null) {
			return null;
		}
		String className = pObject.getClass().getName();
		int index = className.lastIndexOf('$');
		if (index == -1) {
			index = className.lastIndexOf('.');
		}
		return className.substring(index + 1);
	}

	private static Object paramValue(Object pObject) {
		// class-Objekte werden mit Classloader, Classpath und allem Kram
		// ausgegeben
		// darauf kann in den meisten Fällen verzichtet werden
		if (pObject instanceof Class<?>)
			return ((Class<?>) pObject).getName();
		if (pObject instanceof String)
			return pObject;
		ToStringStyle style = new ToStringStyle();
		style.setDefaultFullDetail(true);
		return ReflectionToStringBuilder.reflectionToString(pObject, style);
	}
}
