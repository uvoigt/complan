package org.planner.util;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.ExtendedMessageFormat;
import org.apache.commons.lang3.text.FormatFactory;

// Als bean hat das nicht funktioniert: es gab "ambiguous dependencies for type"
// da die Klasse im EAR sowohl über den classspath des EJB.jars, als auch 
public class CommonMessages {
	private static class ArticleFormat extends Format implements FormatFactory {
		private String sexSuffix;
		boolean capitalize;

		private static final long serialVersionUID = 1L;

		@Override
		public Format getFormat(String name, String arguments, Locale locale) {
			if (arguments != null) {
				String[] split = arguments.split(",");
				sexSuffix = split[0];
				if (split.length > 1)
					capitalize = split[1].equals("c");
			}
			return this;
		}

		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
			int size = obj instanceof Collection ? ((Collection<?>) obj).size() : 1;
			String article;
			if (size > 1) {
				article = getMessage("plural");
			} else {
				article = getMessage("singular_" + (sexSuffix != null ? sexSuffix : "m"));
			}
			if (capitalize)
				article = StringUtils.capitalize(article);
			toAppendTo.append(article);
			return toAppendTo;
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {
			return null;
		}
	}

	private static class ListFormat extends ArticleFormat implements FormatFactory {

		private static final long serialVersionUID = 1L;

		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
			if (obj instanceof Collection) {
				Object[] array = ((Collection<?>) obj).toArray();
				for (int i = 0; i < array.length; i++) {
					toAppendTo.append(array[i]);
					if (i < array.length - 2)
						toAppendTo.append(", ");
					else if (i < array.length - 1)
						toAppendTo.append(" ").append(getMessage("and")).append(" ");
				}
			} else {

			}
			return toAppendTo;
		}
	}

	private static final String BUNDLE_NAME = "CommonMessages";
	private static ResourceBundle bundle;

	public static String getMessage(String key) {
		return getResourceBundle().getString(key);
	}

	public static String getFormattedMessage(String key, Object... arguments) {
		String message = getMessage(key);
		return formatMessage(message, arguments);
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

	public static String formatMessage(String message, Object... args) {
		Map<String, FormatFactory> registry = new HashMap<>();
		registry.put("article", new ArticleFormat());
		registry.put("list", new ListFormat());

		return new ExtendedMessageFormat(message, registry).format(args);
	}
}
