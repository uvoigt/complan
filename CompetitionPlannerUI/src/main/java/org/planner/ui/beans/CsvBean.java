package org.planner.ui.beans;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.el.ELContext;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.planner.eo.AbstractEntity;
import org.planner.ui.beans.SearchBean.ColumnModel;
import org.planner.util.LogUtil.TechnischeException;

@Named
@RequestScoped
public class CsvBean {
	private String separator = ",|;";

	private String nextLine(BufferedReader r) throws IOException {
		String line = r.readLine();
		if (line != null && line.startsWith("#"))
			return nextLine(r);
		return line;
	}

	private String[] parseFirstLine(String line) {
		return line.split(separator);
	}

	private AbstractEntity parseLine(Class<AbstractEntity> entityType, List<ColumnModel> columns, ELContext context,
			String line) {
		AbstractEntity t = createInstance(entityType);
		String[] split = line.split(separator);
		for (int i = 0; i < split.length && i < columns.size(); i++) {
			setValue(t, columns.get(i).getProperty(), context, split[i]);
		}
		return t;
	}

	public List<AbstractEntity> parse(Class<AbstractEntity> entityTyp, List<ColumnModel> columns, ELContext context,
			InputStream in) {
		List<AbstractEntity> result = new ArrayList<>();
		try {
			// Das Analysieren der Headerzeile und Annpassen der Properties
			// erlaubt das Importieren von Dateien mit weniger Infos
			BufferedReader r = new BufferedReader(new InputStreamReader(in, "iso-8859-1"));
			for (String line; (line = nextLine(r)) != null;) {
				String[] firstLine = parseFirstLine(line);
				List<ColumnModel> newColumns = new ArrayList<>(firstLine.length);
				for (int i = 0; i < firstLine.length; i++) {
					int index = columns.indexOf(new ColumnModel(null, firstLine[i], true));
					if (index == -1)
						throw new TechnischeException("Ungültige CSV-Struktur", null);
					newColumns.add(columns.get(index));
				}
				columns = newColumns;
				break;
			}
			for (String line; (line = nextLine(r)) != null;) {
				AbstractEntity t = parseLine(entityTyp, columns, context, line);
				result.add(t);
			}
		} catch (Exception e) {
			throw new TechnischeException("Fehler beim Parsen von CSV", e);
		}
		return result;
	}

	public void writeEntities(List<ColumnModel> columns, OutputStream out, List<Serializable> entities,
			ELContext context) {
		try {
			for (ColumnModel column : columns) {
				out.write(column.getProperty().getBytes("iso-8859-1"));
				out.write(";".getBytes());
			}
			out.write("\r\n".getBytes());
			for (Object entity : entities) {
				for (ColumnModel column : columns) {
					Object value = getValue(entity, column.getProperty(), context);
					if (value != null)
						out.write(createPropertyString(value).getBytes("iso-8859-1"));
					out.write(";".getBytes());
				}
				out.write("\r\n".getBytes());
			}
		} catch (Exception e) {
			throw new TechnischeException("Fehler beim Exportieren als CSV", e);
		}
	}

	private <T> T createInstance(Class<T> entityType) {
		try {
			return entityType.newInstance();
		} catch (Exception e) {
			throw new TechnischeException("Fehler beim Anlegen des Typs: " + entityType.getName(), e);
		}
	}

	private String createPropertyString(Object value) {
		if (value instanceof String)
			return (String) value;
		return value.toString();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object createPropertyValue(String string, Class<?> type) throws Exception {
		if (Date.class.equals(type)) {
			try {
				return new SimpleDateFormat("dd.MM.yyyy").parse(string);
			} catch (ParseException e) {
				return new SimpleDateFormat("MM/dd/yyyy").parse(string);
			}
		}
		if (Enum.class.isAssignableFrom(type))
			return Enum.valueOf((Class<Enum>) type, string);
		return type.getConstructor(String.class).newInstance(string);
	}

	private Object getValue(Object object, String property, ELContext context) {
		try {
			return context.getELResolver().getValue(context, object, property);
		} catch (Exception e) {
			throw new TechnischeException("Fehler beim Lesen des Wertes von " + property + " in " + object, e);
		}
	}

	@SuppressWarnings("unchecked")
	private void setValue(Object object, String property, ELContext context, String stringValue) {
		Object propertyValue;
		int index = property.indexOf('.');
		try {
			if (index != -1) {
				String firstProperty = property.substring(0, index);
				Type genericType = FieldUtils.getDeclaredField(object.getClass(), firstProperty, true).getGenericType();
				Class<?> propertyType;
				if (genericType instanceof ParameterizedType)
					propertyType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
				else
					propertyType = (Class<?>) genericType;
				propertyValue = createInstance(propertyType);
				setValue(propertyValue, property.substring(index + 1), context, stringValue);
				property = firstProperty;
			} else {
				propertyValue = createPropertyValue(stringValue,
						context.getELResolver().getType(context, object, property));
				if ("".equals(propertyValue))
					propertyValue = null;
			}
			Object prevValue = context.getELResolver().getValue(context, object, property);
			if (prevValue instanceof Collection)
				((Collection<Object>) prevValue).add(propertyValue);
			else
				context.getELResolver().setValue(context, object, property, propertyValue);
		} catch (Exception e) {
			throw new TechnischeException(
					"Fehler beim Setzen des Wertes von " + property + " in " + object + " mit dem Wert " + stringValue,
					e);
		}
	}
}
