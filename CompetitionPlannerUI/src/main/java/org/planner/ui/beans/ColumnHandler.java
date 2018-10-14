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
import javax.persistence.Entity;

import org.apache.commons.lang.ArrayUtils;
import org.planner.eo.AbstractEntity;
import org.planner.util.LogUtil.TechnischeException;
import org.planner.util.Logged;
import org.planner.util.Visibilities;
import org.planner.util.Visible;

@Named
@Logged
@ApplicationScoped
public class ColumnHandler {
	public class Column implements Cloneable, Comparable<Column> {
		private String name;
		private Visible visibility;
		private Visible parent;
		private boolean visible;
		private boolean visibleForCurrentUser;

		public Column(String name, Visible visibility, Visible parent) {
			this.name = name;
			this.visibility = visibility;
			this.parent = parent;
			this.visible = visibility.initial();
		}

		public String getName() {
			return name;
		}

		public boolean isMandatory() {
			return visibility.mandatory() || parent != null && parent.mandatory();
		}

		public String getMultiRowGroup() {
			return visibility.multiRowGroup().length() > 0 ? visibility.multiRowGroup()
					: parent != null ? parent.multiRowGroup() : "";
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
			return Integer.compare(getOrder(), o.getOrder());
		}

		private int getOrder() {
			return visibility.order() + (parent != null ? parent.order() : 0);
		}

		private boolean isExport() {
			return visibility.export() || parent != null && parent.export();
		}

		private String[] getRoles() {
			return (String[]) ArrayUtils.addAll(visibility.roles(), parent != null ? parent.roles() : null);
		}
	}

	private class Paths {
		private String[] paths;
		private Visible[] visibilities;
		private int current = -1;

		private Paths(Visibilities v) {
			if (v != null) {
				visibilities = v.value();
				paths = new String[visibilities.length];
				for (int i = 0; i < paths.length; i++) {
					paths[i] = visibilities[i].path();
				}
			}
		}

		private Paths(Visible[] vs, String[] p) {
			visibilities = vs;
			this.paths = p;
		}

		Visible getVisibility() {
			return visibilities != null && current >= 0 ? visibilities[current] : null;
		}

		boolean hasFirst(String part) {
			if (paths != null) {
				for (int i = 0; i < paths.length; i++) {
					String path = paths[i];
					String[] split = path.split("\\.");
					if (split[0].equals(part)) {
						current = i;
						return true;
					}
				}
			}
			return false;
		}

		Paths next() {
			if (visibilities == null)
				return this;
			String[] newPaths = new String[paths.length];
			for (int i = 0; i < paths.length; i++) {
				String path = paths[i];
				int index = path.indexOf('.');
				newPaths[i] = index != -1 ? path.substring(index + 1) : "";
			}
			return new Paths(visibilities, newPaths);
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
				getColumnsForType(type, null, columnList, 0, null, null);
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
			getColumnsForType(type, null, columnList, 0, null, null);
		} catch (IntrospectionException e) {
			throw new TechnischeException("Fehler beim Initialisieren der columns für " + type, e);
		}
		for (Iterator<Column> it = columnList.iterator(); it.hasNext();) {
			Column column = it.next();
			if (!column.isExport())
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

	private void getColumnsForType(Class<?> type, String parentProperty, List<Column> result, int level, Visible first,
			Paths paths) throws IntrospectionException {
		for (Field field : type.getDeclaredFields()) {
			boolean force = paths != null && paths.hasFirst(field.getName());
			Visible visible = field.getAnnotation(Visible.class);
			if (!force && (visible == null || level > visible.depth()))
				continue;
			String propertyName = parentProperty != null ? parentProperty + "." + field.getName() : field.getName();
			Class<?> propertyType = field.getType();
			if (field.getGenericType() instanceof ParameterizedType)
				propertyType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
			getColumnsForType(propertyType, propertyName, result, level + 1, first != null ? first : visible,
					paths != null ? paths.next() : new Paths(field.getAnnotation(Visibilities.class)));
			if (propertyType.getAnnotation(Entity.class) == null) {
				Visible v = paths != null ? paths.getVisibility() : null;
				if (v == null)
					v = visible;
				result.add(new Column(propertyName, v, first));
			}
		}
		Class<?> superclass = type.getSuperclass();
		if (superclass != null)
			getColumnsForType(superclass, parentProperty, result, level, first, paths);
	}

	private void applyRoleRestrictions(Column[] columns) {
		for (Column column : columns) {
			String[] roles = column.getRoles();
			boolean inRole = roles.length == 0;
			for (String role : roles) {
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
