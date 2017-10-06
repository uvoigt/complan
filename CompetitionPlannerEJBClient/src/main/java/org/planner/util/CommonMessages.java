package org.planner.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;

// Als bean hat das nicht funktioniert: es gab "ambiguous dependencies for type"
// da die Klasse im EAR sowohl über den classspath des EJB.jars, als auch 
public class CommonMessages {
	private static final String BUNDLE_NAME = "CommonMessages";
	private static ResourceBundle bundle;

	public static String getMessage(String key) {
		return getResourceBundle().getString(key);
	}

	public static String getFormattedMessage(String key, Object... arguments) {
		String message = getMessage(key);
		return MessageFormat.format(message, arguments);
	}

	public static String getEnumText(Enum<?> e) {
		return getMessage(e.getClass().getSimpleName() + "." + e.name());
	}

	public static ResourceBundle getResourceBundle() {
		if (bundle == null)
			bundle = ResourceBundle.getBundle(BUNDLE_NAME);
		return bundle;
	}

	public static String niceTimeString(int minutes) {
		int hours = minutes / 60;
		StringBuilder sb = new StringBuilder();
		if (hours > 1)
			sb.append(getFormattedMessage("hours", hours));
		else if (hours > 0)
			sb.append(getFormattedMessage("hour", hours));
		minutes -= hours * 60;
		if (sb.length() > 0 && minutes > 0)
			sb.append(", ");
		if (minutes > 1)
			sb.append(getFormattedMessage("minutes", minutes));
		else if (minutes > 0)
			sb.append(getFormattedMessage("minute", minutes));
		return sb.toString();
	}
}
