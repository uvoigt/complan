package org.planner.ui.beans;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.planner.eo.AbstractEntity;
import org.planner.remote.ServiceFacade;
import org.planner.ui.beans.ColumnHandler.Column;
import org.planner.ui.beans.UploadBean.DownloadHandler;
import org.planner.ui.beans.UploadBean.UploadHandler;
import org.planner.ui.util.CSVParser;
import org.planner.ui.util.ExtendedBeanELResolver;
import org.planner.ui.util.JsfUtil;
import org.planner.util.LogUtil.TechnischeException;
import org.planner.util.Logged;
import org.planner.util.NLSBundle;
import org.primefaces.component.api.UIColumn;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.ToggleEvent;
import org.primefaces.event.data.SortEvent;
import org.primefaces.model.SortMeta;
import org.primefaces.model.Visibility;

/**
 * Das Bean für die Suche nach XML-basierten Daten. Es wird von den Views
 * <ul>
 * <li>schema_suchen.xhtml</li>
 * <li>suchen.xhtml</li> verwendet. Da sich die Views um das Attribut
 * 'entityTyp' bzw. 'schema' unterscheiden, müssen die spezifischen
 * Instanzvariablen, wie 'dataModel', 'columns' oder auch das Binding der
 * Datatable in Maps vorgehalten werden, deren Key jeweils der Name des
 * Schema-Typs ist.
 * 
 * @author Uwe Voigt - IBM
 */
@Logged
@Named
@SessionScoped
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
	}

	private static final long serialVersionUID = 1L;

	private Map<String, RemoteDataModel<? extends AbstractEntity>> dataModels = new HashMap<String, RemoteDataModel<? extends AbstractEntity>>();

	private Map<String, List<ColumnModel>> columns = new HashMap<String, List<ColumnModel>>();

	private Map<String, DataTable> datatables = new HashMap<String, DataTable>();

	// Da die Primefaces-Datatable Schwierigkeiten hat, den Sort-State über
	// Requests zu persistieren, dieser Workaround.
	private Map<String, List<SortMeta>> sortState = new HashMap<String, List<SortMeta>>();

	private UploadBean uploadBean;

	@Inject
	private ServiceFacade service;

	@Inject
	private Messages bundle;

	@Inject
	private StartseiteBean startseiteBean;

	@Inject
	private ColumnHandler columnHandler;

	@PostConstruct
	public void init() {
		uploadBean = new UploadBean(this, this, null);
	}

	public UploadBean getUploadBean() {
		return uploadBean;
	}

	@Override
	public void handleDownload(OutputStream out, String typ) throws Exception {
		RemoteDataModel<?> model = createDataModel(typ, new ArrayList<ColumnModel>(), true);
		List<ColumnModel> columns = getColumns(typ);
		// TODO das war schemaName
		Map<String, Object> filter = datatables.get(null).getFilters();
		List<? extends AbstractEntity> list = model.load(0, Integer.MAX_VALUE, null, filter);
		for (AbstractEntity entity : list) {
			writeEntity(out, entity, columns);
		}
	}

	protected void writeEntity(OutputStream out, AbstractEntity entity, List<ColumnModel> columns) throws IOException {
		for (ColumnModel column : columns) {
			String string = doRender(entity, column);
			if (string != null)
				out.write(string.getBytes("iso-8859-1"));
			out.write(";".getBytes());
		}
		out.write("\r\n".getBytes());
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
		Class<AbstractEntity> entityType = loadTyp(typ, AbstractEntity.class);
		CSVParser<AbstractEntity> csvParser = new CSVParser<>(entityType);
		List<AbstractEntity> entities = csvParser.parse(in);
		// da das bean kein UploadProcessor ist, werden die Daten direkt in
		// die DB geschrieben!
		service.dataImport(entities);
	}

	public List<ColumnModel> getColumns(String typ) {
		return columns.get(typ);
	}

	public void onSort(SortEvent event) {
		List<SortMeta> meta = ((DataTable) event.getComponent()).getMultiSortMeta();
		String typ = (String) JsfUtil.getContextVariable("typ");
		sortState.put(typ, meta);
	}

	public Map<String, DataTable> getDatatable() {
		return datatables;
	}

	public RemoteDataModel<? extends Serializable> getDataModel(String typ) throws Exception {
		RemoteDataModel<? extends AbstractEntity> dataModel = dataModels.get(typ);
		if (dataModel == null) {
			List<ColumnModel> columnList = new ArrayList<ColumnModel>();
			dataModel = createDataModel(typ, columnList, false);

			columns.put(typ, columnList);
			dataModels.put(typ, dataModel);
		}
		return dataModel;
	}

	private RemoteDataModel<? extends AbstractEntity> createDataModel(String typ, List<ColumnModel> columnList,
			boolean onlyId) throws Exception {

		Class<AbstractEntity> entityType = loadTyp(typ, AbstractEntity.class);
		NLSBundle nlsBundle = entityType.getAnnotation(NLSBundle.class);
		if (nlsBundle == null)
			throw new TechnischeException("Entity " + entityType.getSimpleName() + " muss Annotation "
					+ NLSBundle.class.getSimpleName() + " besitzen", null);
		for (Column column : columnHandler.getColumns(entityType)) {
			columnList.add(new ColumnModel(bundle.get(nlsBundle.value() + "." + column.getName()), column.getName(),
					column.isVisible()));
		}

		return new RemoteDataModel<AbstractEntity>(service, entityType);
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
		// adminService.speichern(entity);
		targetBean.setItem(item);
		// PropertyUtils.setProperty(targetBean, "item", item);
	}

	public void bearbeiten(String link, String typ, AbstractEntity selectedItem, ITarget targetBean) throws Exception {
		startseiteBean.setMainContent(link);
		AbstractEntity item = service.getObject(loadTyp(typ, AbstractEntity.class), selectedItem.getId());
		targetBean.setItem(item);
	}

	public void bearbeiten(String link, Long selectedItemId, ITarget targetBean) throws Exception {
		startseiteBean.setMainContent(link);
		targetBean.setItem(selectedItemId);
	}

	public void kopieren(String link, String typ, AbstractEntity selectedItem, ITarget targetBean) throws Exception {
		startseiteBean.setMainContent(link);
		AbstractEntity item = service.getObjectForCopy(loadTyp(typ, AbstractEntity.class), selectedItem.getId());
		targetBean.setItem(item);
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(null, bundle.get("sucheDaten.copyHint")));
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
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(null, JsfUtil.getScopedBundle().get("deleteSuccess")));
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
		if (o == null)
			return null;
		if (o instanceof String)
			return (String) o;
		if (o instanceof Date)
			return o.toString();
		if (o instanceof Boolean)
			return bundle.get("label" + StringUtils.capitalize(o.toString()));
		FacesContext context = FacesContext.getCurrentInstance();
		// context.getApplication().createConverter(o.getClass().getSuperclass())
		ExtendedBeanELResolver resolver = new ExtendedBeanELResolver(context.getApplication().getELResolver());
		Object v = resolver.getValue(context.getELContext(), o, column.property);
		if (v == null)
			return null;
		// if (value instanceof AbstractEntity) {
		// try {
		// Field field = base.getClass().getDeclaredField(propertyString);
		// Visible visible = field.getAnnotation(Visible.class);
		// if (visible != null) {
		// if (visible.asOneColumn()) {
		// value = delegate.getValue(context, value, propertyString);
		// } else {
		//
		// }
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }

		// Converter converter =
		// context.getApplication().createConverter(v.getClass());
		// if (converter != null)
		// return converter.getAsString(context, null, v);

		return doRender(v, column);
	}

	public void clearDataModels() {
		dataModels.clear();
	}

	public void preRenderViewListener(ComponentSystemEvent event) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (!facesContext.isPostback())
			return;

		String typ = (String) JsfUtil.getContextVariable("typ");
		DataTable table = getDatatable().get(null);
		if (table == null)
			return;
		List<SortMeta> meta = sortState.get(typ);
		if (meta != null /* && table.getMultiSortMeta() == null */) {
			table.setMultiSortMeta(meta);
			// das sollte dazu führen, dass der Renderer die richtige Column
			// als sortiert darstellt
			// in PF 5.0 ist da allerdings ein Bug
			table.setSortBy(meta.get(0).getSortField());
		}
	}

	public void columnChooserListener(ToggleEvent event) throws Exception {
		String typ = (String) JsfUtil.getContextVariable("typ");
		columnHandler.persistToggleState(loadTyp(typ, AbstractEntity.class), (int) event.getData(),
				event.getVisibility() == Visibility.VISIBLE);
	}

	public int calcColWidth(DataTable table) {
		// default war 96px
		int count = table.getColumnsCount();
		UIColumn column = table.getColumns().get(count - 1);
		int btnCount = 0;
		for (UIComponent c : column.getChildren()) {
			if (c instanceof HtmlCommandButton)
				btnCount++;
		}
		return btnCount * 29 + 10; // 10 ist margin des ersten Buttons
	}
}