package org.planner.ui.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.planner.eo.AbstractEntity;
import org.planner.util.LogUtil.TechnischeException;

public class CSVParser<T extends AbstractEntity> {

	private Class<T> entityType;
	private String separator = ";";
	private String[] propertyNames;

	public CSVParser(Class<T> entityType) {
		this.entityType = entityType;
	}

	private String[] parseFirstLine(String line) {
		return line.split(separator);
	}

	private T parseLine(String line) {
		T t = createInstance();
		String[] split = line.split(separator);
		for (int i = 0; i < split.length; i++) {
			setProperty(t, propertyNames[i], split[i]);

		}
		return t;
	}

	public List<T> parse(InputStream in) {
		List<T> result = new ArrayList<>();
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF8"));
			for (String line; (line = r.readLine()) != null;) {
				propertyNames = parseFirstLine(line);
				if (propertyNames != null)
					break;
			}
			for (String line; (line = r.readLine()) != null;) {
				T t = parseLine(line);
				result.add(t);
			}
		} catch (Exception e) {
			throw new TechnischeException("Fehler beim Parsen von CSV", e);
		}
		return result;
	}

	private T createInstance() {
		try {
			return entityType.newInstance();
		} catch (Exception e) {
			throw new TechnischeException("Fehler beim Anlegen des Typs: " + entityType.getName(), e);
		}
	}

	private Object createPropertyValue(String string, Class<?> type) throws Exception {
		return type.getConstructor(String.class).newInstance(string);
	}

	private void setProperty(T object, String propertyName, String stringValue) {
		try {
			Field field = object.getClass().getDeclaredField(propertyName);
			field.setAccessible(true);
			Object propertyValue = createPropertyValue(stringValue, field.getType());
			field.set(object, propertyValue);
		} catch (Exception e) {
			throw new TechnischeException("Fehler beim Setzen des Wertes von " + propertyName + " in " + object
					+ " mit dem Wert " + stringValue, e);
		}
	}

}
