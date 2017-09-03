package org.planner.ui.beans;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.AbstractEntity;
import org.planner.util.Logged;
import org.planner.util.Visible;

@Named
@Logged
@ApplicationScoped
public class ColumnHandler {
	public class Column {
		private String name;
		private boolean visible;

		public Column(String name, boolean visible) {
			this.name = name;
			this.visible = visible;
		}

		public String getName() {
			return name;
		}

		public boolean isVisible() {
			return visible;
		}
	}

	@Inject
	private BenutzerEinstellungen benutzerEinstellungen;

	private Map<Class<?>, Column[]> columns = new HashMap<>();

	public Column[] getColumns(Class<?> type) {
		Column[] columns = this.columns.get(type);
		if (columns == null) {
			List<Column> columnList = new ArrayList<>();
			getColumnsForType(type, null, columnList, 0);
			columns = columnList.toArray(new Column[columnList.size()]);
			this.columns.put(type, columns);
		}

		return applyBenutzerEinstellungen(type, columns);
	}

	private void getColumnsForType(Class<?> type, String parentProperty, List<Column> result, int level) {
		for (Field field : type.getDeclaredFields()) {
			Visible visible = field.getAnnotation(Visible.class);
			if (visible != null) {
				if (level > visible.depth())
					continue;
				Class<?> fieldType = field.getType();
				// Endlos-Rekursion muss durch geeignete Verwendung vermieden
				// werden
				String propertyName = parentProperty != null ? parentProperty + "." + field.getName() : field.getName();
				getColumnsForType(fieldType, propertyName, result, level + 1);
				if (AbstractEntity.class.isAssignableFrom(fieldType)) {
				} else {
					result.add(new Column(propertyName, visible.initial()));
				}
			}
		}
		Class<?> superclass = type.getSuperclass();
		if (superclass != null)
			getColumnsForType(superclass, parentProperty, result, level);
	}

	private Column[] applyBenutzerEinstellungen(Class<?> type, Column[] columns) {
		String propertyName = "columns." + type.getName();
		Integer[] state = benutzerEinstellungen.getTypedValue(propertyName, Integer[].class);
		if (state == null)
			return columns;
		List<Integer> stateList = Arrays.asList(state);
		for (int i = 0; i < columns.length; i++) {
			columns[i].visible = stateList.contains(i);
		}
		return columns;
	}

	public void persistToggleState(Class<? extends AbstractEntity> type, Integer index, boolean visible) {
		String propertyName = "columns." + type.getName();
		Integer[] state = benutzerEinstellungen.getTypedValue(propertyName, Integer[].class);
		Set<Integer> stateSet;
		if (state == null) {
			stateSet = new TreeSet<>();
			Column[] columns = getColumns(type);
			for (int i = 0; i < columns.length; i++) {
				if (columns[i].visible)
					stateSet.add(i);
			}
		} else {
			stateSet = new TreeSet<>(Arrays.asList(state));
		}
		if (visible)
			stateSet.add(index);
		else
			stateSet.remove(index);
		benutzerEinstellungen.setValue(propertyName, stateSet.toArray(new Integer[stateSet.size()]));
	}
}
