package org.planner.ui.beans;

import java.beans.IntrospectionException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.AbstractEntity;
import org.planner.util.LogUtil.TechnischeException;
import org.planner.util.Logged;
import org.planner.util.Visible;

@Named
@Logged
@ApplicationScoped
public class ColumnHandler {
	public class Column implements Cloneable, Comparable<Column> {
		private String name;
		private Visible visibility;
		private boolean visible;
		private boolean visibleForCurrentUser;

		public Column(String name, Visible visibility) {
			this.name = name;
			this.visibility = visibility;
			this.visible = visibility.initial();
		}

		public String getName() {
			return name;
		}

		public boolean isVisible() {
			return visible;
		}

		public boolean isVisibleForCurrentUser() {
			return visibleForCurrentUser;
		}

		@Override
		protected Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				throw new InternalError();
			}
		}

		@Override
		public int compareTo(Column o) {
			return Integer.compare(visibility.order(), o.visibility.order());
		}
	}

	@Inject
	private BenutzerEinstellungen benutzerEinstellungen;

	private Map<Class<?>, Column[]> columns = new HashMap<>();

	public Column[] getColumns(Class<?> type) {
		Column[] columns = this.columns.get(type);
		if (columns == null) {
			List<Column> columnList = new ArrayList<>();
			try {
				getColumnsForType(type, null, columnList, 0, null);
			} catch (IntrospectionException e) {
				throw new TechnischeException("Fehler beim Initialisieren der columns für " + type, e);
			}
			columns = columnList.toArray(new Column[columnList.size()]);
			Arrays.sort(columns);
			this.columns.put(type, columns);
		}
		columns = cloneColumns(columns);
		applyRoleRestrictions(columns);
		applyBenutzerEinstellungen(type, columns);
		return columns;
	}

	public Column[] getExportColumns(Class<?> type) {
		List<Column> columnList = new ArrayList<>();
		try {
			getColumnsForType(type, null, columnList, 0, null);
		} catch (IntrospectionException e) {
			throw new TechnischeException("Fehler beim Initialisieren der columns für " + type, e);
		}
		for (Iterator<Column> it = columnList.iterator(); it.hasNext();) {
			Column column = it.next();
			if (!column.visibility.export())
				it.remove();
		}
		Column[] columns = columnList.toArray(new Column[columnList.size()]);
		Arrays.sort(columns);
		return columns;
	}

	private Column[] cloneColumns(Column[] columns) {
		columns = columns.clone();
		for (int i = 0; i < columns.length; i++) {
			columns[i] = (Column) columns[i].clone();
		}
		return columns;
	}

	private void getColumnsForType(Class<?> type, String parentProperty, List<Column> result, int level,
			Visible visibility) throws IntrospectionException {
		for (Field field : type.getDeclaredFields()) {
			Visible visible = field.getAnnotation(Visible.class);
			if (visible == null || level > visible.depth())
				continue;
			String propertyName = parentProperty != null ? parentProperty + "." + field.getName() : field.getName();
			Class<?> propertyType = field.getType();
			if (field.getGenericType() instanceof ParameterizedType)
				propertyType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
			getColumnsForType(propertyType, propertyName, result, level + 1, visible);
			if (!AbstractEntity.class.isAssignableFrom(propertyType)) {
				result.add(new Column(propertyName, visibility != null ? visibility : visible));
			}
		}
		Class<?> superclass = type.getSuperclass();
		if (superclass != null)
			getColumnsForType(superclass, parentProperty, result, level, visibility);
	}

	private void applyRoleRestrictions(Column[] columns) {
		for (Column column : columns) {
			boolean inRole = column.visibility.roles().length == 0;
			for (String role : column.visibility.roles()) {
				inRole |= FacesContext.getCurrentInstance().getExternalContext().isUserInRole(role);
			}
			column.visibleForCurrentUser = inRole;
		}
	}

	private void applyBenutzerEinstellungen(Class<?> type, Column[] columns) {
		String propertyName = "columns." + type.getName();
		Integer[] state = benutzerEinstellungen.getTypedValue(propertyName, Integer[].class);
		if (state == null)
			return;
		List<Integer> stateList = Arrays.asList(state);
		for (int i = 0; i < columns.length; i++) {
			columns[i].visible = stateList.contains(i);
		}
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
