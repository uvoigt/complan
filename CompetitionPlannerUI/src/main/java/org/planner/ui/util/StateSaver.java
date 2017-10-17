package org.planner.ui.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.el.MethodExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

import org.primefaces.component.api.UIColumn;
import org.primefaces.component.column.Column;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

@FacesComponent(tagName = "stateSaver", createTag = true, namespace = "http://planner.org/ui")
public class StateSaver extends UIComponentBase {
	private static class SerializableSortMeta extends SortMeta implements Serializable {
		private static class MyColumn extends Column {
			private String column;

			private MyColumn(String column) {
				this.column = column;
			}

			@Override
			public String getColumnKey() {
				return column;
			}
		}

		private static final long serialVersionUID = 1L;

		private transient MyColumn column;
		private String sortBy;
		private String sortField;
		private MethodExpression sortFunction;
		private SortOrder sortOrder;

		private SerializableSortMeta(SortMeta meta) {
			sortBy = meta.getColumn().getColumnKey();
			sortField = meta.getSortField();
			sortFunction = meta.getSortFunction();
			sortOrder = meta.getSortOrder();
		}

		@Override
		public UIColumn getColumn() {
			if (column == null)
				column = new MyColumn(sortBy);
			return column;
		}

		@Override
		public String getSortField() {
			return sortField;
		}

		@Override
		public MethodExpression getSortFunction() {
			return sortFunction;
		}

		@Override
		public SortOrder getSortOrder() {
			return sortOrder;
		}
	}

	private Map<String, Object> map = new HashMap<>();

	@Override
	public String getFamily() {
		return "facelets.LiteralText";
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreState(FacesContext context, Object state) {
		map = (Map<String, Object>) state;
	}

	@Override
	public Object saveState(FacesContext context) {
		// Leider steckt da irgendwo ein nicht serialisierbarer SortMeta drin :-/
		for (Entry<String, Object> e : map.entrySet()) {
			if (e.getValue() instanceof List) {
				List<?> list = (List<?>) e.getValue();
				if (list.size() > 0 && list.get(0) instanceof SortMeta) {
					List<SerializableSortMeta> replacement = new ArrayList<>();
					for (Object object : list) {
						replacement.add(new SerializableSortMeta((SortMeta) object));
					}
					e.setValue(replacement);
				}
			}
		}
		return map;
	}

	public void remove(String name) {
		map.remove(name);
	}

	public void put(String name, Object value) {
		map.put(name, value);
	}

	public Object get(String name) {
		return map.get(name);
	}
}
