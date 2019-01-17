package org.planner.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Suchkriterien für die Suche von Entitäten. Es werden Attribute für das Blättern (Paging) des Resultats, Sortierung,
 * Projektion (select-clause) und Selektion (where-clause) angeboten.
 * 
 * @author Uwe Voigt - IBM
 */
public class Suchkriterien implements Serializable {
	/**
	 * Kriterien für die Sortierung.
	 */
	public static class SortField implements Serializable {
		private static final long serialVersionUID = 1L;

		private String sortierFeld;
		private boolean asc;
		private boolean ignoreCase = true;

		public SortField(String sortierFeld, boolean asc) {
			this.sortierFeld = sortierFeld;
			this.asc = asc;
		}

		public String getSortierFeld() {
			return sortierFeld;
		}

		public void setSortierFeld(String sortierFeld) {
			this.sortierFeld = sortierFeld;
		}

		public boolean isAsc() {
			return asc;
		}

		public void setAsc(boolean asc) {
			this.asc = asc;
		}

		public boolean isIgnoreCase() {
			return ignoreCase;
		}

		public void setIgnoreCase(boolean ignoreCase) {
			this.ignoreCase = ignoreCase;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}
	}

	public static class Filter implements Serializable {

		public enum Comparison {
			eq, ne
		}

		private static final long serialVersionUID = 1L;

		private Comparison comparisonOp = Comparison.eq;
		private String name;
		private Object value;

		public Filter(String name, Object value) {
			this.name = name;
			this.value = value;
		}

		public Filter(Comparison comparison, String name, Object value) {
			this.comparisonOp = comparison;
			this.name = name;
			this.value = value;
		}

		public Comparison getComparisonOperator() {
			return comparisonOp;
		}

		public String getName() {
			return name;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}
	}

	public static class Property implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String name;
		private final String multiRowGroup;

		public Property(String name) {
			this(name, null);
		}

		public Property(String name, String multiRowGroup) {
			this.name = name;
			this.multiRowGroup = multiRowGroup;
		}

		public String getName() {
			return name;
		}

		public String getMultiRowGroup() {
			return multiRowGroup;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}
	}

	private static final long serialVersionUID = 1L;

	private int zeilenOffset;
	private int zeilenAnzahl = Integer.MAX_VALUE;
	private Map<String, Filter> filter;
	private boolean ignoreCase = true;
	private boolean exact;
	private List<SortField> sortierung;
	private List<Property> properties;

	public int getZeilenOffset() {
		return zeilenOffset;
	}

	public void setZeilenOffset(int zeilenOffset) {
		this.zeilenOffset = zeilenOffset;
	}

	public int getZeilenAnzahl() {
		return zeilenAnzahl;
	}

	public void setZeilenAnzahl(int zeilenAnzahl) {
		this.zeilenAnzahl = zeilenAnzahl;
	}

	public Map<String, Filter> getFilter() {
		return filter;
	}

	public void addFilter(Filter filter) {
		if (this.filter == null)
			this.filter = new HashMap<>();
		this.filter.put(filter.getName(), filter);
	}

	public void addFilter(String name, Object value) {
		addFilter(new Filter(name, value));
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}

	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	public boolean isExact() {
		return exact;
	}

	public void setExact(boolean exact) {
		this.exact = exact;
	}

	public List<SortField> getSortierung() {
		return sortierung;
	}

	public void addSortierung(String sortierFeld, boolean asc) {
		if (sortierung == null)
			sortierung = new ArrayList<>();
		sortierung.add(new SortField(sortierFeld, asc));
	}

	public List<Property> getProperties() {
		return properties;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
