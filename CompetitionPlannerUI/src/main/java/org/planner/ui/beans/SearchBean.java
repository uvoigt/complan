package org.planner.ui.beans;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.el.ELResolver;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.PhaseId;
import javax.faces.view.ViewScoped;
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
@ViewScoped
public class SearchBean implements DownloadHandler, UploadHandler, Serializable {
	public static class ColumnModel implements Serializable {
		private static final long serialVersionUID = 1L;
		private String header;
		private String property;
		private boolean visible;

		public ColumnModel(String header, String property, boolean visible) {
			this.header = header;
			this.property = property;
			this.visible = visible;
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

	private Map<String, RemoteDataModel<? extends Serializable>> dataModels = new HashMap<String, RemoteDataModel<? extends Serializable>>();

	private Map<String, List<ColumnModel>> columns = new HashMap<String, List<ColumnModel>>();

	private Map<String, DataTable> datatables = new HashMap<String, DataTable>();

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
	public void handleDownload(OutputStream out, String typ) throws Exception {
		List<ColumnModel> exportColumns = createExportColumns(typ);
		RemoteDataModel<Serializable> model = createDataModel(typ, exportColumns);
		// TODO das war schemaName
		Map<String, Object> filter = datatables.get(null).getFilters();

		List<Serializable> list = model.load(0, Integer.MAX_VALUE, null, filter);
		csvBean.writeEntities(exportColumns, out, list, FacesContext.getCurrentInstance().getELContext());
	}

	private List<ColumnModel> createExportColumns(String typ) throws Exception {
		List<ColumnModel> exportColumns = new ArrayList<>();
		Class<Serializable> entityType = loadTyp(typ, Serializable.class);
		for (Column column : columnHandler.getExportColumns(entityType)) {
			exportColumns.add(new ColumnModel(null, column.getName(), true));
		}
		return exportColumns;
	}

	@Override
	public String getDownloadFileName() {
		String typ = (String) JsfUtil.getContextVariable("typ");
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

	public List<ColumnModel> getColumns(String typ) throws Exception {
		List<ColumnModel> list = columns.get(typ);
		if (list == null) {
			list = new ArrayList<ColumnModel>();
			Class<Serializable> entityType = loadTyp(typ, Serializable.class);
			NLSBundle nlsBundle = entityType.getAnnotation(NLSBundle.class);
			if (nlsBundle == null)
				throw new TechnischeException("Entity " + entityType.getSimpleName() + " muss Annotation "
						+ NLSBundle.class.getSimpleName() + " besitzen", null);
			for (Column column : columnHandler.getColumns(entityType)) {
				if (column.isVisibleForCurrentUser())
					list.add(new ColumnModel(messages.get(nlsBundle.value() + "." + column.getName()), column.getName(),
							column.isVisible()));
			}
			columns.put(typ, list);
		}
		return list;
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

	public Map<String, DataTable> getDatatable() {
		return datatables;
	}

	public RemoteDataModel<? extends Serializable> getDataModel(String typ) throws Exception {
		RemoteDataModel<? extends Serializable> dataModel = dataModels.get(typ);
		if (dataModel == null) {
			dataModel = createDataModel(typ, getColumns(typ));
			dataModels.put(typ, dataModel);
		}
		return dataModel;
	}

	private RemoteDataModel<Serializable> createDataModel(String typ, List<ColumnModel> columnList) throws Exception {
		Class<Serializable> entityType = loadTyp(typ, Serializable.class);
		return new RemoteDataModel<Serializable>(service, entityType, columnList);
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

	public void bearbeiten(String link, String typ, Map<String, Object> selectedItem, ITarget targetBean)
			throws Exception {
		FacesContext ctx = FacesContext.getCurrentInstance();
		Long id = (Long) ctx.getApplication().getELResolver().getValue(ctx.getELContext(), selectedItem, "id");
		AbstractEntity item = service.getObject(loadTyp(typ, AbstractEntity.class), id, 1);
		targetBean.setItem(item);
		startseiteBean.setMainContent(link, id);
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

	public void alleKopieren(String link, String typ, String schemaName, Object targetBean) throws Exception {
		// ((UIInput)
		// FacesContext.getCurrentInstance().getViewRoot().findComponent("copyForm:selectedAttribute")).resetValue();
		// ((UIInput)
		// FacesContext.getCurrentInstance().getViewRoot().findComponent("copyForm:attributeValue")).resetValue();
		// Map<String, Object> filter = datatables.get(schemaName).getFilters();
		// DatenSuchkriterien kriterien = new DatenSuchkriterien();
		// kriterien.setFilter(filter);
		// kriterien.setKonfigSetId(startseiteVerwaltenBean.getAusgewaehlteKonfiguration().getId());
		// SchemaTyp schemaTyp =
		// startseiteVerwaltenBean.getSchema().getSchemaEntry(schemaName);
		// String selectedAttribute =
		// FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("copyForm:selectedAttribute");
		// if (selectedAttribute == null)
		// return;
		// String attributeValue =
		// FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("copyForm:attributeValue");
		// kriterien.setSchemaId(schemaTyp.getId());
		// SchemaAttribut attr = schemaTyp.byName(selectedAttribute);
		// kriterien.setProperties(Arrays.asList(attr.getXpath(),
		// attributeValue));
		// int count = adminService.kopieren(loadTyp(typ, AbstraktEO.class),
		// kriterien);
		// FacesContext.getCurrentInstance().addMessage(null, new
		// FacesMessage(null,
		// JsfUtil.getScopedBundle().format("massCopySuccess", count,
		// schemaName, selectedAttribute, attributeValue)));
	}

	public void loeschen(String typ, Object id) throws Exception {
		service.delete(loadTyp(typ, AbstractEntity.class), (Long) id);
		String msg = JsfUtil.getScopedBundle().get("deleteSuccess");
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(null, msg));
	}

	public void alleLoeschen(String link, String typ, Object targetBean) throws Exception {
		// Map<String, Object> filter = datatables.get(schemaName).getFilters();
		// DatenSuchkriterien kriterien = new DatenSuchkriterien();
		// kriterien.setFilter(filter);
		// KonfigsetEO konfigSet =
		// startseiteVerwaltenBean.getAusgewaehlteKonfiguration();
		// kriterien.setKonfigSetId(konfigSet.getId());
		// SchemaTyp schemaTyp =
		// startseiteVerwaltenBean.getSchema().getSchemaEntry(schemaName);
		// if (schemaTyp != null)
		// kriterien.setSchemaId(schemaTyp.getId());
		// int count = adminService.loeschen(loadTyp(typ, AbstraktEO.class),
		// kriterien);
		// FacesContext.getCurrentInstance().addMessage(null, new
		// FacesMessage(null,
		// JsfUtil.getScopedBundle().format("massDeleteSuccess", count,
		// schemaName)));
		// if ("Konfigschema".equals(schemaName)) {
		// startseiteVerwaltenBean.setAusgewaehlteKonfiguration(null);
		// startseiteVerwaltenBean.setAusgewaehlteKonfiguration(konfigSet);
		// }
	}

	public void alleBearbeiten(String link, String typ, String schemaName, Object targetBean) throws Exception {
		// anlegen(link, typ, schemaName, targetBean);
		// PropertyUtils.setProperty(targetBean, "filters",
		// datatables.get(schemaName).getFilters());
		// FacesContext.getCurrentInstance().addMessage(null, new
		// FacesMessage(null,
		// bundle.format("sucheDaten.massModifyHint", getDataModel(typ,
		// schemaName).getRowCount())));
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
		if (o instanceof Date)
			return o.toString();
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

	public void clearDataModels() {
		dataModels.clear();
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
		dataModels.remove(typ);
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

	public int getMaxButtonCount(UIOutput output) {
		return ((MutableInt) datatables.get(null).getAttributes().get("maxButtons")).intValue();
	}
}