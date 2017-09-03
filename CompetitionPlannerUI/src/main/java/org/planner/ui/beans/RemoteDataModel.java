package org.planner.ui.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.planner.eo.AbstractEntity;
import org.planner.model.IResultProvider;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.primefaces.component.column.Column;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

/**
 * <p>
 * Das <code>RemoteDataModel</code> erweitert das <code>LazyDataModel</code> von
 * PrimeFaces um die Fähigkeit, mit Hilfe der Datenbank zu filtern und zu
 * sortieren.
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2012
 * </p>
 * 
 * <p>
 * Organisation: Talanx Systeme AG
 * </p>
 * 
 * @param <T>
 *            der Zeilentyp des Datamodels
 * 
 * @author Uwe Voigt, IBM
 * @version $Revision: 13814 $
 */
public class RemoteDataModel<T extends AbstractEntity> extends LazyDataModel<T> {
	private static final long serialVersionUID = 1L;

	private IResultProvider dataProvider;
	private Class<T> zeilentyp;

	private List<String> columns;

	public RemoteDataModel(IResultProvider provider, Class<T> type) {
		dataProvider = provider;
		zeilentyp = type;
	}

	@Override
	public T getRowData(String rowKey) {
		return dataProvider.getObject(zeilentyp, Long.parseLong(rowKey));
	}

	@Override
	public List<T> load(int first, int pageSize, List<SortMeta> multiSortMeta, Map<String, Object> filters) {

		Suchergebnis<T> ergebnis = dataProvider.search(zeilentyp,
				createKriterien(first, pageSize, multiSortMeta, filters, columns));
		setRowCount(ergebnis.getGesamtgroesse());
		return ergebnis.getListe();
	}

	@Override
	public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {

		List<SortMeta> sortierung = null;
		if (sortField != null) {
			sortierung = new ArrayList<SortMeta>(1);
			Column column = new Column();
			sortierung.add(new SortMeta(column, sortField, sortOrder, null));
		}
		return load(first, pageSize, sortierung, filters);
	}

	protected Suchkriterien createKriterien(int first, int pageSize, List<SortMeta> sortMeta,
			Map<String, Object> filters, List<String> columns) {

		Suchkriterien krit = new Suchkriterien();
		krit.setZeilenOffset(first);
		krit.setZeilenAnzahl(pageSize);
		if (sortMeta != null) {
			for (SortMeta meta : sortMeta) {
				krit.addSortierung(meta.getSortField(), meta.getSortOrder() == SortOrder.ASCENDING);
			}
		}
		krit.setFilter(checkFilters(filters));
		krit.setProperties(columns);
		return krit;
	}

	protected Map<String, Object> checkFilters(Map<String, Object> filters) {
		// seit PrimeFaces 5.2 werden von der DataTable alle Filterfelder, ggfs.
		// mit leeren Strings geliefert
		Map<String, Object> modified = null;
		if (filters != null) {
			modified = new HashMap<String, Object>();
			for (Entry<String, Object> e : filters.entrySet()) {
				if (!"".equals(e.getKey()) && !"".equals(e.getValue()))
					modified.put(e.getKey(), e.getValue());
			}
		}
		return modified;
	}
}
