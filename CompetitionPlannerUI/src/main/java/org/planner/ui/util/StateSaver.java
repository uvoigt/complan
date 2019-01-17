package org.planner.ui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

import org.primefaces.component.api.UIColumn;
import org.primefaces.component.column.Column;
import org.primefaces.model.SortMeta;

@FacesComponent(tagName = "stateSaver", createTag = true, namespace = "http://planner.org/ui")
public class StateSaver extends UIComponentBase {
	private static class MySortMeta extends SortMeta {
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

		private MySortMeta(SortMeta meta) {
			sortBy = meta.getColumn().getColumnKey();
			setSortField(meta.getSortField());
			setSortFunction(meta.getSortFunction());
			setSortOrder(meta.getSortOrder());
		}

		@Override
		public UIColumn getColumn() {
			if (column == null)
				column = new MyColumn(sortBy);
			return column;
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
		// Leider steckt da irgendwo eine nicht serialisierbare Column drin :-/
		for (Entry<String, Object> e : map.entrySet()) {
			if (e.getValue() instanceof List) {
				List<?> list = (List<?>) e.getValue();
				if (list.size() > 0 && list.get(0) instanceof SortMeta) {
					List<MySortMeta> replacement = new ArrayList<>();
					for (Object object : list) {
						replacement.add(new MySortMeta((SortMeta) object));
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
