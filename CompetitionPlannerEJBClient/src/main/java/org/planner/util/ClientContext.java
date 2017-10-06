package org.planner.util;

import java.util.Locale;

public class ClientContext {

	private static final ThreadLocal<ClientContext> local = new ThreadLocal<>();

	public static ClientContext get() {
		return local.get();
	}

	public static void set(ClientContext context) {
		local.set(context);
	}

	public static void clear() {
		local.remove();
	}

	private Locale locale;

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}
}
