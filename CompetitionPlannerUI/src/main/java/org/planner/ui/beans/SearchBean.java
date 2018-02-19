package org.planner.ui.beans;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.el.ELResolver;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.planner.eo.AbstractEntity;
import org.planner.model.LocalizedEnum;
import org.planner.remote.ServiceFacade;
import org.planner.ui.beans.ColumnHandler.Column;
import org.planner.ui.beans.UploadBean.DownloadHandler;
import org.planner.ui.beans.UploadBean.UploadHandler;
import org.planner.ui.util.JsfUtil;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.LogUtil.TechnischeException;
import org.planner.util.NLSBundle;
import org.primefaces.component.api.UIColumn;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.ToggleEvent;
import org.primefaces.event.data.FilterEvent;
import org.primefaces.event.data.PageEvent;
import org.primefaces.event.data.SortEvent;
import org.primefaces.model.SortMeta;
import org.primefaces.model.Visibility;

/**
 * Das Bean für die Suche nach XML-basierten Daten. Es wird von den Views
 * <ul>
 * <li>schema_suchen.xhtml</li>
 * <li>suchen.xhtml</li> verwendet. Da sich die Views um das Attribut 'entityTyp' bzw. 'schema' unterscheiden, müssen
 * die spezifischen Instanzvariablen, wie 'dataModel', 'columns' oder auch das Binding der Datatable in Maps vorgehalten
 * werden, deren Key jeweils der Name des Schema-Typs ist.
 * 
 * @author Uwe Voigt - IBM
 */
// @Logged
@Named
@RequestScoped
public class SearchBean implements DownloadHandler, UploadHandler, Serializable {
	public static class ColumnModel implements Serializable {
		private static final long serialVersionUID = 1L;
		private final String header;
		private final String property;
		private final boolean visible;
		private final String multiRowGroup;

		public ColumnModel(String header, String property, boolean visible) {
			this(header, property, visible, null);
		}

		public ColumnModel(String header, String property, boolean visible, String multiRowGroup) {
			this.header = header;
			this.property = property;
			this.visible = visible;
			this.multiRowGroup = multiRowGroup;
		}

		public String getHeader() {
			return header;
		}

		public String getProperty() {
			return property;
		}

		public boolean isVisible() {
			return visible;
		}

		public String getMultiRowGroup() {
			return multiRowGroup;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof ColumnModel))
				return false;
			ColumnModel other = (ColumnModel) obj;
			return property.equals(other.property);
		}
	}

	private static final long serialVersionUID = 1L;

	private RemoteDataModel<Serializable> dataModel;

	private Map<String, List<ColumnModel>> columns = new HashMap<String, List<ColumnModel>>();

	private UploadBean uploadBean;

	@Inject
	private CsvBean csvBean;

	@Inject
	private ServiceFacade service;

	@Inject
	private Messages messages;

	@Inject
	private StartseiteBean startseiteBean;

	@Inject
	private ColumnHandler columnHandler;

	@Inject
	private BenutzerEinstellungen settings;

	@PostConstruct
	public void init() {
		uploadBean = new UploadBean(this, this, null);
	}

	public UploadBean getUploadBean() {
		return uploadBean;
	}

	@Override
	public void handleDownload(OutputStream out, String typ, Object selection) throws Exception {
		List<ColumnModel> exportColumns = createExportColumns(typ);
		RemoteDataModel<Serializable> model = createDataModel(typ, exportColumns, null);
		List<Serializable> list = model.load(0, Integer.MAX_VALUE, null, null);
		csvBean.writeEntities(exportColumns, out, list, FacesContext.getCurrentInstance().getELContext());
	}

	private List<ColumnModel> createExportColumns(String typ) throws Exception {
		List<ColumnModel> exportColumns = new ArrayList<>();
		Class<Serializable> entityType = loadTyp(typ, Serializable.class);
		for (Column column : columnHandler.getExportColumns(entityType)) {
			exportColumns.add(new ColumnModel(null, column.getName(), true, column.getMultiRowGroup()));
		}
		return exportColumns;
	}

	@Override
	public String getDownloadFileName(String typ, Object selection) {
		String timestamp = DateFormat.getDateTimeInstance().format(new Date());
		timestamp = timestamp.replace(' ', '_');
		return typ + "_" + timestamp + ".csv";
	}

	@Override
	public void handleUpload(InputStream in, String typ) throws Exception {
		List<AbstractEntity> entities = csvBean.parse(loadTyp(typ, AbstractEntity.class), createExportColumns(typ),
				FacesContext.getCurrentInstance().getELContext(), in);
		// da das bean kein UploadProcessor ist, werden die Daten direkt in
		// die DB geschrieben!
		try {
			service.dataImport(entities);
		} catch (FachlicheException e) {
			List<String> messages = e.getMessages();
			if (messages == null)
				messages = Arrays.asList(e.getMessage());
			for (String msg : messages) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(null, msg));
			}
		}
	}

	public int getNumberOfRows() {
		String typ = (String) JsfUtil.getContextVariable("typ");
		String propertyName = "numrows." + typ;
		return settings.getTypedValue(propertyName, Integer.class, 50);
	}

	public int getLastRow() {
		int min = Math.min(dataModel.getRowCount(), getNumberOfRows()) - 1;
		return min;
	}

	public List<ColumnModel> getColumns(String typ) throws Exception {
		List<ColumnModel> list = columns.get(typ);
		if (list == null) {
			list = new ArrayList<>();
			List<ColumnModel> mandatoryColumns = new ArrayList<>();
			Class<Serializable> entityType = loadTyp(typ, Serializable.class);
			NLSBundle nlsBundle = entityType.getAnnotation(NLSBundle.class);
			if (nlsBundle == null)
				throw new TechnischeException("Entity " + entityType.getSimpleName() + " muss Annotation "
						+ NLSBundle.class.getSimpleName() + " besitzen", null);
			for (Column column : columnHandler.getColumns(entityType)) {
				if (column.isMandatory())
					mandatoryColumns.add(new ColumnModel(null, column.getName(), false, column.getMultiRowGroup()));
				if (column.isVisibleForCurrentUser())
					list.add(new ColumnModel(messages.get(nlsBundle.value() + "." + column.getName()), column.getName(),
							column.isVisible(), column.getMultiRowGroup()));
			}
			columns.put(typ, list);
			if (!mandatoryColumns.isEmpty())
				columns.put(typ + "_m", mandatoryColumns);
		}
		return list;
	}

	private List<ColumnModel> getMandatoryColumns(String typ) {
		return columns.get(typ + "_m");
	}

	public void onPagination(PageEvent event) {
		DataTable dataTable = (DataTable) event.getComponent();
		int rows = dataTable.getRowsToRender();
		int page = event.getPage();
		JsfUtil.setViewVariable("rows", rows);
		JsfUtil.setViewVariable("first", page * rows);
	}

	public void onFilter(FilterEvent event) {
		JsfUtil.setViewVariable("filters", event.getFilters());
	}

	public void onSort(SortEvent event) {
		List<SortMeta> meta = ((DataTable) event.getComponent()).getMultiSortMeta();
		JsfUtil.setViewVariable("sortState", meta);
	}

	public Object getSelectedItem() {
		return JsfUtil.getViewVariable("selectedItem");
	}

	public void setSelectedItem(Object selectedItem) {
		JsfUtil.setViewVariable("selectedItem", selectedItem);
	}

	public RemoteDataModel<? extends Serializable> getDataModel(String typ) throws Exception {
		if (dataModel == null)
			dataModel = createDataModel(typ, getColumns(typ), getMandatoryColumns(typ));
		return dataModel;
	}

	private RemoteDataModel<Serializable> createDataModel(String typ, List<ColumnModel> columnList,
			List<ColumnModel> mandatoryColumns) throws Exception {
		Class<Serializable> entityType = loadTyp(typ, Serializable.class);
		return new RemoteDataModel<Serializable>(service, entityType, columnList, mandatoryColumns);
	}

	@SuppressWarnings("unchecked")
	private <T> Class<T> loadTyp(String typ, Class<T> targetBase) throws Exception {
		Class<T> clazz = (Class<T>) Class.forName(typ);
		if (!targetBase.isAssignableFrom(clazz))
			throw new IllegalArgumentException(typ);
		return clazz;
	}

	public void anlegen(String link, String typ, ITarget targetBean) throws Exception {
		startseiteBean.setMainContent(link);
		AbstractEntity item = loadTyp(typ, AbstractEntity.class).newInstance();
		targetBean.setItem(item);
	}

	public void bearbeiten(String link, String typ, Object selectedItem, ITarget targetBean) throws Exception {
		FacesContext ctx = FacesContext.getCurrentInstance();
		Long id = (Long) ctx.getApplication().getELResolver().getValue(ctx.getELContext(), selectedItem, "id");
		AbstractEntity item = service.getObject(loadTyp(typ, AbstractEntity.class), id, 1);
		targetBean.setItem(item);
		startseiteBean.setMainContent(link, id);
	}

	public void bearbeiten(String link, String typ, ITarget targetBean) throws Exception {
		bearbeiten(link, typ, getSelectedItem(), targetBean);
	}

	public void bearbeiten(String link, Long selectedItemId, ITarget targetBean) throws Exception {
		startseiteBean.setMainContent(link, selectedItemId);
		targetBean.setItem(selectedItemId);
	}

	public void kopieren(String link, String typ, Map<String, Object> selectedItem, ITarget targetBean)
			throws Exception {
		startseiteBean.setMainContent(link);
		FacesContext ctx = FacesContext.getCurrentInstance();
		Object id = ctx.getApplication().getELResolver().getValue(ctx.getELContext(), selectedItem, "id");
		AbstractEntity item = service.getObjectForCopy(loadTyp(typ, AbstractEntity.class), (Long) id);
		targetBean.setItem(item);
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(null, messages.get("copyHint")));
	}

	public void loeschen(String typ, Object id) throws Exception {
		service.delete(loadTyp(typ, AbstractEntity.class), (Long) id);
		String msg = JsfUtil.getScopedBundle().get("deleteSuccess");
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(null, msg));
	}

	public String render(Object o, ColumnModel column) {
		// Auch in Prä-Render-Response-Phasen werden die Model-Getter
		// aufgerufen. Die Rückgabewerte dienen aber
		// nicht der Darstellung.
		if (FacesContext.getCurrentInstance().getCurrentPhaseId() != PhaseId.RENDER_RESPONSE)
			return null;
		return doRender(o, column);
	}

	private String doRender(Object o, ColumnModel column) {
		if (o == null || !column.visible)
			return null;
		if (o instanceof String)
			return (String) o;
		if (o instanceof Timestamp)
			return DateFormat.getDateTimeInstance().format(o);
		if (o instanceof Date)
			return DateFormat.getDateInstance().format(o);
		if (o instanceof Boolean)
			return messages.get("label" + StringUtils.capitalize(o.toString()));
		if (o instanceof LocalizedEnum)
			return ((LocalizedEnum) o).getText();
		FacesContext context = FacesContext.getCurrentInstance();
		ELResolver resolver = context.getApplication().getELResolver();
		Object v = resolver.getValue(context.getELContext(), o, column.property);
		if (v == null)
			return null;
		return doRender(v, column);
	}

	public void onPrerenderTable(ComponentSystemEvent event) {
		DataTable table = (DataTable) event.getComponent();
		Integer first = (Integer) JsfUtil.getViewVariable("first");
		if (first != null)
			table.setFirst(first);
		Integer rows = (Integer) JsfUtil.getViewVariable("rows");
		if (rows != null)
			table.setRows(rows);
		@SuppressWarnings("unchecked")
		Map<String, Object> filters = (Map<String, Object>) JsfUtil.getViewVariable("filters");
		if (filters != null)
			table.setFilters(filters);
		@SuppressWarnings("unchecked")
		List<SortMeta> sortState = (List<SortMeta>) JsfUtil.getViewVariable("sortState");
		if (sortState != null)
			table.setMultiSortMeta(sortState);
	}

	public void columnChooserListener(ToggleEvent event) throws Exception {
		String typ = (String) JsfUtil.getContextVariable("typ");
		columnHandler.persistToggleState(loadTyp(typ, AbstractEntity.class), (int) event.getData(),
				event.getVisibility() == Visibility.VISIBLE);
		dataModel = null;
		columns.remove(typ);
	}

	public int getButtonCount(DataTable table) {
		UIColumn column = table.getColumns().get(table.getColumnsCount() - 1);
		int buttonCount = 0;
		for (UIComponent c : column.getChildren()) {
			if (c instanceof HtmlCommandButton && c.isRendered())
				buttonCount++;
		}
		MutableInt maxButtons = (MutableInt) table.getAttributes().get("maxButtons");
		if (maxButtons == null)
			table.getAttributes().put("maxButtons", new MutableInt(buttonCount));
		else if (buttonCount > maxButtons.intValue())
			maxButtons.setValue(buttonCount);

		// 10 ist margin des ersten Buttons
		return buttonCount;
	}

	public int getMaxButtonCount(UIComponent datatable) {
		return ((MutableInt) datatable.getAttributes().get("maxButtons")).intValue();
	}
}