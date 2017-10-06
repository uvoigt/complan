package org.planner.ui.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.planner.model.IResultProvider;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.ui.beans.SearchBean.ColumnModel;
import org.primefaces.component.column.Column;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

/**
 * <p>
 * Das <code>RemoteDataModel</code> erweitert das <code>LazyDataModel</code> von PrimeFaces um die Fähigkeit, mit Hilfe
 * der Datenbank zu filtern und zu sortieren.
 * </p>
 * 
 * @param <T>
 *            der Zeilentyp des Datamodels
 * 
 * @author Uwe Voigt, IBM
 * @version $Revision: 13814 $
 */
public class RemoteDataModel<T extends Serializable> extends LazyDataModel<T> {
	private static final long serialVersionUID = 1L;

	private IResultProvider dataProvider;
	private Class<T> zeilentyp;

	private List<ColumnModel> columns;

	public RemoteDataModel(IResultProvider provider, Class<T> type, List<ColumnModel> columns) {
		dataProvider = provider;
		zeilentyp = type;
		this.columns = columns;
	}

	@Override
	public T getRowData(String rowKey) {
		return dataProvider.getObject(zeilentyp, Long.parseLong(rowKey), 0);
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
			Map<String, Object> filters, List<ColumnModel> columns) {

		Suchkriterien krit = new Suchkriterien();
		krit.setZeilenOffset(first);
		krit.setZeilenAnzahl(pageSize);
		if (sortMeta != null) {
			for (SortMeta meta : sortMeta) {
				krit.addSortierung(meta.getSortField(), meta.getSortOrder() == SortOrder.ASCENDING);
			}
		}
		createFilters(krit, filters);
		List<String> properties = null;
		for (ColumnModel column : columns) {
			if (!column.isVisible())
				continue;
			if (properties == null)
				properties = new ArrayList<>();
			properties.add(column.getProperty());
		}
		krit.setProperties(properties);
		return krit;
	}

	protected void createFilters(Suchkriterien krit, Map<String, Object> filters) {
		// seit PrimeFaces 5.2 werden von der DataTable alle Filterfelder, ggfs.
		// mit leeren Strings geliefert
		if (filters != null) {
			for (Entry<String, Object> e : filters.entrySet()) {
				if (!"".equals(e.getKey()) && !"".equals(e.getValue()))
					krit.addFilter(e.getKey(), e.getValue());
			}
		}
	}
}
