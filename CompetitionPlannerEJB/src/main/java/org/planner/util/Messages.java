package org.planner.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Messages {
	private static final String BUNDLE_NAME = "Messages";
	private ResourceBundle bundle;

	public String getMessage(String key) {
		return getResourceBundle().getString(key);
	}

	public String getFormattedMessage(String key, Object... arguments) {
		String message = getMessage(key);
		return MessageFormat.format(message, arguments);
	}

	public ResourceBundle getResourceBundle() {
		if (bundle == null)
			bundle = ResourceBundle.getBundle(BUNDLE_NAME);
		return bundle;
	}
}
