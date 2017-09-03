package org.planner.ui.beans;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

@Named
@ApplicationScoped
public class Messages extends AbstractMap<String, String> implements Serializable {
	private static final long serialVersionUID = 1L;

	// Applikations-Locale (nicht User-spezifisch!)
	private Locale locale;
	private Map<String, Messages> subs;
	private Map<String, String> delegate;
	private Messages parent;

	public Messages() {
	}

	private Messages(Map<String, String> map, Messages parent) {
		delegate = map;
		this.parent = parent;
	}

	@PostConstruct
	public void init() {
		locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
		ResourceBundle bundle = ResourceBundle.getBundle("MessagesBundle", locale);
		delegate = new HashMap<>();
		for (String k : bundle.keySet()) {
			delegate.put(k, bundle.getString(k));
		}
	}

	public Locale getLocale() {
		return locale;
	}

	@Override
	public String get(Object key) {
		try {
			return internalGet(key);
		} catch (Exception e) {
			return missing(key);
		}
	}

	/**
	 * Liefert den im Bundle enthaltenen String oder null, im Unterschied zu
	 * {@link #get(Object)}, welches in jedem Fall einen Wert liefert.
	 * 
	 * @param key
	 *            der Bundel-key
	 * @return Eintrag oder null
	 */
	public String getStringOrNull(Object key) {
		try {
			return internalGet(key);
		} catch (Exception e) {
			return null;
		}
	}

	private String internalGet(Object key) {
		if (key == null)
			return "null-key";
		String msg = delegate.get(key);
		if (msg != null)
			return msg;
		if (parent != null)
			return parent.internalGet(key);
		throw new MissingResourceException("Can't find message for key" + key, null, null);
	}

	private String escape(String string) {
		return string.replace("'", "\\\'");
	}

	private String missing(Object key) {
		return "MISSING: " + key + " :MISSING";
	}

	public Messages bundle(String key) {
		if (subs == null)
			subs = new HashMap<>();
		Messages messages = subs.get(key);
		if (messages != null)
			return messages;
		Map<String, String> map = new HashMap<String, String>();
		for (String k : delegate.keySet()) {
			int index = k.indexOf('.');
			if (index == -1)
				continue;
			if (k.substring(0, index).equals(key)) {
				map.put(k.substring(index + 1), delegate.get(k));
			}
		}
		messages = new Messages(map, this);
		subs.put(key, messages);
		return messages;
	}

	public String format(String key, Object arg) {
		return formatWithVararg(key, arg);
	}

	public String format(String key, Object arg1, Object arg2) {
		return formatWithVararg(key, arg1, arg2);
	}

	public String format(String key, Object arg1, Object arg2, Object arg3) {
		return formatWithVararg(key, arg1, arg2, arg3);
	}

	public String format(String key, Object arg1, Object arg2, Object arg3, Object arg4) {
		return formatWithVararg(key, arg1, arg2, arg3, arg4);
	}

	private String formatWithVararg(String key, Object... args) {
		try {
			MessageFormat format = new MessageFormat(internalGet(key),
					FacesContext.getCurrentInstance().getViewRoot().getLocale());
			return escape(format.format(args));
		} catch (MissingResourceException e) {
			return missing(key);
		} catch (Exception e) {
			return "Exception in message formatting of: " + key + " - " + e;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Entry<String, String>> entrySet() {
		return Collections.EMPTY_SET;
	}
}
