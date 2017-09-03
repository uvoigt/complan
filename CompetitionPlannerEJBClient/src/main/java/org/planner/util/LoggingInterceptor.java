package org.planner.util;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simples entry und exit-Logging.
 * 
 * @author Uwe  Voigt - IBM
 */
@Logged
@Interceptor
public class LoggingInterceptor implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unterdrückt das Logging für Methoden, an deren Klassen die Logged-Annotation steht.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Silent {
	}

	/**
	 * Loggt Entry- und Exit-Informationen auf TRACE-Level.
	 * 
	 * @param ctx
	 *            der Aufruf-Kontext
	 * @return Aufruf-Ergebnis
	 * @throws Exception
	 *             Fehler
	 */
	@AroundInvoke
	public Object log(InvocationContext ctx) throws Exception {
		Class<?> targetClass = ctx.getTarget().getClass();
		Logger log = LoggerFactory.getLogger(targetClass);
		Method method = ctx.getMethod();
		boolean isSilent = method.getAnnotation(Silent.class) != null;
		if (log.isTraceEnabled() && !isSilent) {
			LogUtil.entry(log, method.getName(), ctx.getParameters());
		}
		Object result = null;
		try {
			result = ctx.proceed();

			if (log.isTraceEnabled() && !isSilent) {
				if (void.class.equals(method.getReturnType())) {
					LogUtil.exit(log, method.getName());
				} else {
					LogUtil.exit(log, method.getName(), result);
				}
			}

		} catch (Throwable e) {
			LogUtil.handleException(e, log, "Unbehandelter Fehler in " + targetClass.getName() + ":" + method.getName(), ctx.getParameters());
		}
		return result;
	}
}
