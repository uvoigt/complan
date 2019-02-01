package org.planner.ui.beans.announcement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.event.ComponentSystemEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.AbstractEntity_;
import org.planner.eo.Announcement;
import org.planner.eo.HasId;
import org.planner.eo.Participant;
import org.planner.eo.Participant_;
import org.planner.eo.Race;
import org.planner.eo.RegEntry;
import org.planner.eo.RegEntry_;
import org.planner.eo.Registration;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Registration_;
import org.planner.eo.User;
import org.planner.eo.User_;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.FetchInfo;
import org.planner.model.IResultProvider;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.BenutzerEinstellungen;
import org.planner.ui.beans.ColumnHandler;
import org.planner.ui.beans.ColumnHandler.Column;
import org.planner.ui.beans.Messages;
import org.planner.ui.beans.RemoteDataModel;
import org.planner.ui.beans.SearchBean.ColumnModel;
import org.planner.ui.util.JsfUtil;
import org.primefaces.PrimeFaces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.feature.DataTableFeatureKey;
import org.primefaces.component.datatable.feature.FilterFeature;
import org.primefaces.component.datatable.feature.SortFeature;
import org.primefaces.component.selectonemenu.SelectOneMenu;
import org.primefaces.event.data.FilterEvent;
import org.primefaces.event.data.SortEvent;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

@Named
@RequestScoped
public class RegistrationBean extends AbstractEditBean implements IResultProvider {

	private static final long serialVersionUID = 1L;

	@Inject
	private ColumnHandler columnHandler;

	@Inject
	private BenutzerEinstellungen einstellungen;

	private Long announcementId;

	private Long registrationId;

	private Registration registration;

	private List<Race> races;

	private RemoteDataModel<? extends Serializable> athletes;

	private List<Race> selectedRaces;

	private List<User> selectedAthletes;

	private RegEntry selectedEntry;

	private boolean clubVisible;

	private boolean showEffect;

	private DataTable registrationTable;

	private List<String> remarks;

	private int numEntries;

	@Override
	@PostConstruct
	public void init() {
		super.init();

		registrationId = getIdFromRequestParameters();
		announcementId = (Long) getFromRequestParameters(2);
		if (registrationId == null) {
			registrationId = (Long) JsfUtil.getViewVariable("id");
			announcementId = (Long) JsfUtil.getViewVariable("aid");
		}
		if (registrationId != null) {
			JsfUtil.setViewVariable("id", registrationId);
			JsfUtil.setViewVariable("aid", announcementId);
		}

		String[] remarks = einstellungen.getTypedValue("requestMsg", String[].class,
				// da die Seite sowohl von Announcements als auch von Registrations aufgerufen wird,
				// muss der Key voll qualifiziert sein
				new String[] { JsfUtil.getScopedBundle().get("registrations.requestMsg") });
		this.remarks = new ArrayList<>(Arrays.asList(remarks));

		if (isCancelPressed())
			resetViewState();
	}

	@Override
	public void setItem(Object item) {
		registration = (Registration) item;
		numEntries = registration.getEntries().size();
		registrationId = registration.getId();
		announcementId = registration.getAnnouncement().getId();
		JsfUtil.setViewVariable("id", registrationId);
		JsfUtil.setViewVariable("aid", announcementId);

		// Merken für Reload
		setRequestParameter(2, registration.getAnnouncement().getId());

		resetViewState();
	}

	private void resetViewState() {
		JsfUtil.setViewVariable("rFilters", null);
		JsfUtil.setViewVariable("rSortState", null);
	}

	@Override
	public FetchInfo[] getFetchInfo() {
		return new FetchInfo[] { new FetchInfo(Registration_.club, true),
				new FetchInfo(Registration_.entries, false).add(new FetchInfo(RegEntry_.race, false),
						new FetchInfo(RegEntry_.participants, false)
								.add(new FetchInfo(Participant_.user, false).add(new FetchInfo(User_.club, false)))) };
	}

	public boolean canDelete(Map<String, Object> item) {
		return item.get("club.name").equals(auth.getLoggedInUser().getClub().getName())
				&& RegistrationStatus.created.equals(item.get("status"));
	}

	private void loadRegistration(Long id) {
		registration = service.getObject(Registration.class, id, getFetchInfo());
		numEntries = registration.getEntries().size();
	}

	private void loadRegistrationAndUpdateModel() {
		loadRegistration(registration.getId());
		// da der value-Getter beim Table update nicht aufgerufen wird, muss das Table-Model manuell aktualisiert werden
		registrationTable.setValue(registration.getEntries());
	}

	public void onFilterRegistrationTable(FilterEvent event) {
		JsfUtil.setViewVariable("rFilters", event.getFilters());
	}

	public void onSortRegistrationTable(SortEvent event) {
		// speichere nur asc/desc. die SortMeta würden im StateSaver ausgetauscht, wonach die sortBy-Expression
		// der column nicht wiederhergestellt wird
		JsfUtil.setViewVariable("rSortState", event.isAscending());
	}

	public void onPostValidateRegistrationTable(ComponentSystemEvent event) {
		filterRegistrations(event.getFacesContext(), (DataTable) event.getComponent());
	}

	public void onPrerenderRegistrationTable(ComponentSystemEvent event) {
		DataTable table = (DataTable) event.getComponent();
		filterRegistrations(event.getFacesContext(), table);
		Boolean ascending = (Boolean) JsfUtil.getViewVariable("rSortState");
		if (ascending != null) {
			SortMeta meta = new SortMeta(table.getColumns().get(0), null,
					ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING, null);
			table.setMultiSortMeta(Arrays.asList(meta));
			SortFeature feature = (SortFeature) table.getFeature(DataTableFeatureKey.SORT);
			feature.multiSort(event.getFacesContext(), table);
		}
		PrimeFaces.current().executeScript(
				"updateCount('.registrationCount','" + JsfUtil.getScopedBundle().get("registrationsCount") + "',"
						+ numEntries + "," + registration.getEntries().size() + ")");
	}

	@SuppressWarnings("unchecked")
	private void filterRegistrations(FacesContext ctx, DataTable table) {
		Map<String, Object> filters = (Map<String, Object>) JsfUtil.getViewVariable("rFilters");
		table.setFilters(filters);
		if (filters != null) {
			FilterFeature feature = (FilterFeature) table.getFeature(DataTableFeatureKey.FILTER);
			List<FilterMeta> filterMetadata = feature.populateFilterMetaData(ctx, table);
			feature.filter(ctx, table, filterMetadata, null);
			registration.getEntries().clear();
			registration.getEntries().addAll(table.getFilteredValue());
		}
	}

	private boolean isTableTargeted(String tableId) {
		if (isCancelPressed())
			return false;
		PartialViewContext ctx = FacesContext.getCurrentInstance().getPartialViewContext();
		Collection<String> ids = ctx.getExecuteIds();
		// mainContent -> Aufruf von den Ausschreibungen
		// main -> Aktion oder Aufruf von den Registrierungen
		// !partialRequest -> full page reload
		return ids.contains(tableId) || ids.contains("main") || !ctx.isPartialRequest()
				|| ctx.getRenderIds().contains("mainContent");
	}

	public Registration getRegistration() {
		if (registration == null && (isTableTargeted("main:racesTable") || isTableTargeted("main:registrationTable")))
			loadRegistration(registrationId);
		return registration;
	}

	public List<Race> getSelectedRaces() {
		return selectedRaces;
	}

	public void setSelectedRaces(List<Race> selectedRaces) {
		this.selectedRaces = selectedRaces;
	}

	public List<User> getSelectedAthletes() {
		return selectedAthletes;
	}

	public void setSelectedAthletes(List<User> selectedAthletes) {
		this.selectedAthletes = selectedAthletes;
	}

	public RegEntry getSelectedEntry() {
		return selectedEntry;
	}

	public void setSelectedEntry(RegEntry selectedEntry) {
		this.selectedEntry = selectedEntry;
	}

	public void createRegistration(Object announcement) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		announcementId = (Long) ctx.getApplication().getELResolver().getValue(ctx.getELContext(), announcement,
				AbstractEntity_.id.getName());
		Registration r = new Registration();
		r.setAnnouncement(service.getObject(Announcement.class, announcementId));
		registrationId = service.createRegistration(r);
		loadRegistration(registrationId);
		JsfUtil.setViewVariable("id", registrationId);
		JsfUtil.setViewVariable("aid", announcementId);
		resetViewState();
		startseiteBean.setMainContent("/announcement/registrationEdit.xhtml", registrationId, announcementId);
	}

	public void addToRegistration() {
		if (selectedEntry != null) {
			// es wir zu einem existierenden RegEntry hinzugefügt
			// sofern das impl-seitig gestattet wird

			List<RegEntry> entries = new ArrayList<>(1);
			entries.add(selectedEntry);
			setAthletesToEntry(selectedEntry);

			service.saveRegEntries(registration.getId(), entries);
			// wurde etwas hinzugefügt. Um das im UI sichtbar zu machen
			// kommt ein Effekt im Header der Tabelle in Frage
			showEffect = true;

		} else if (selectedRaces != null && !selectedRaces.isEmpty()) {
			// es wird ein neuer RegEntry angelegt!

			List<RegEntry> entries = new ArrayList<>(selectedRaces.size());
			for (Race race : selectedRaces) {

				// Sonderfall: wenn das/die ausgewählten Rennen 1er-Renner sind,
				// dann verteile die Sportler über die Rennen
				if (race.getBoatClass() == BoatClass.k1 || race.getBoatClass() == BoatClass.c1) {
					for (User user : selectedAthletes) {
						int pos = 0;
						List<Participant> participants = new ArrayList<>(1);
						Participant participant = new Participant();
						participant.setUser(user);
						participant.setPos(++pos);
						participants.add(participant);
						RegEntry entry = new RegEntry();
						entry.setParticipants(participants);
						entry.setRace(race);
						entries.add(entry);
					}
				} else {
					RegEntry entry = new RegEntry();
					entry.setRace(race);
					setAthletesToEntry(entry);
					entries.add(entry);
				}
			}
			service.saveRegEntries(registration.getId(), entries);
			showEffect = true;
		}
		if (showEffect)
			loadRegistrationAndUpdateModel();
	}

	private void setAthletesToEntry(RegEntry entry) {
		int pos = 0;
		List<Participant> participants = new ArrayList<>();
		for (User user : selectedAthletes) {
			Participant participant = new Participant();
			participant.setUser(user);
			participant.setPos(++pos);
			participants.add(participant);
		}
		entry.setParticipants(participants);
	}

	public void addRequest() {
		if (selectedEntry != null) {
			List<RegEntry> entries = new ArrayList<>(1);
			entries.add(selectedEntry);
			Participant request = new Participant();
			request.setRemark(remarks.get(0));
			selectedEntry.setParticipants(Arrays.asList(request));
			service.saveRegEntries(registration.getId(), entries);
			showEffect = true;
			loadRegistrationAndUpdateModel();
		}
	}

	public void deleteFromRegistration(RegEntry entry) {
		service.deleteFromRegEntry(registration.getId(), entry);
		showEffect = true;
		loadRegistrationAndUpdateModel();
	}

	public List<Race> getRaces() {
		if (races == null && isTableTargeted("main:racesTable")) {
			races = service.getRaces(announcementId);
		}
		return races;
	}

	public RemoteDataModel<? extends Serializable> getAthletes() {
		if (athletes == null && isTableTargeted("main:athletesTable")) {

			List<ColumnModel> columns = new ArrayList<>();
			Column[] userColumns = columnHandler.getColumns(User.class);

			columns.add(new ColumnModel(null, userColumns[8].getName(), clubVisible));
			columns.add(new ColumnModel(null, userColumns[1].getName(), true));
			columns.add(new ColumnModel(null, userColumns[2].getName(), true));
			columns.add(new ColumnModel(null, userColumns[4].getName(), true));
			columns.add(new ColumnModel(null, userColumns[5].getName(), true));
			athletes = new RemoteDataModel<>(this, User.class, columns, null, false, "athletes");
			HashMap<String, Object> filters = new HashMap<>();
			filters.put("club.name", auth.getLoggedInUser().getClub().getName());
			athletes.setFilterPreset(filters);
		}
		return athletes;
	}

	public int getNumEntries() {
		return numEntries;
	}

	public String getRaceString(RegEntry entry, Messages bundle) {
		Race race = entry.getRace();
		StringBuilder sb = new StringBuilder();
		sb.append(bundle.get("raceNo"));
		sb.append(" ");
		sb.append(race.getNumber());
		sb.append(" - ");
		sb.append(race.getBoatClass().getText());
		sb.append(" ");
		sb.append(race.getAgeType().getText());
		sb.append(" ");
		sb.append(race.getGender().getText());
		sb.append(" ");
		sb.append(race.getDistance());
		return sb.toString();
	}

	public String getRaceFilterPattern(Messages bundle) {
		StringBuilder sb = new StringBuilder();
		sb.append(bundle.get("raceNo"));
		sb.append(" $ -");
		return sb.toString();
	}

	public String getRegisteredParticipantsString(RegEntry entry) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < entry.getParticipants().size(); i++) {
			Participant participant = entry.getParticipants().get(i);
			if (i > 0)
				sb.append(" ");
			getRegisteredParticipantString(entry, participant, sb);
		}
		return sb.toString();
	}

	public String getRegisteredParticipantString(RegEntry entry, Participant participant, StringBuilder sb) {
		StringBuilder result = sb != null ? sb : new StringBuilder();
		if (participant.getRemark() != null) {
			result.append(participant.getRemark());
		} else {
			result.append(participant.getUser().getFirstName());
			result.append(" ");
			result.append(participant.getUser().getLastName());
			if (!participant.getUser().getClub().getId().equals(entry.getRegistration().getClub().getId())) {
				result.append(" (");
				result.append(participant.getUser().getClub().getShortNameOrName());
				result.append(")");
			}
		}
		return result != sb ? result.toString() : null;
	}

	public void setStatus(Object registration, RegistrationStatus status) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		Long registrationId = (Long) ctx.getApplication().getELResolver().getValue(ctx.getELContext(), registration,
				AbstractEntity_.id.getName());
		service.setRegistrationStatus(registrationId, status);
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(null, JsfUtil.getScopedBundle().get("registrations.statusSet_" + status)));
	}

	public boolean isClubVisible() {
		return clubVisible;
	}

	public void setClubVisible(boolean clubVisible) {
		this.clubVisible = clubVisible;
	}

	public boolean isShowEffect() {
		return showEffect;
	}

	public DataTable getRegistrationTable() {
		// für den View-Mode!
		if (registrationTable != null && registration != null
				&& registration.getStatus() == RegistrationStatus.submitted)
			registrationTable.setSelectionMode(null);
		return registrationTable;
	}

	public void setRegistrationTable(DataTable registrationTable) {
		this.registrationTable = registrationTable;
	}

	public List<String> getRemarks() {
		return remarks;
	}

	public String getRemark() {
		return remarks.get(0);
	}

	public void setRemark(String remark) {
		if (remark != null) {
			if (remark.length() > 50)
				remark = remark.substring(0, 50);
			remarks.remove(remark);
			if (remarks.size() >= 10)
				remarks.remove(9);
			remarks.add(0, remark);
			einstellungen.setValue("requestMsg", remarks.toArray(new String[remarks.size()]));
			Object[] ids = FacesContext.getCurrentInstance().getPartialViewContext().getExecuteIds().toArray();
			SelectOneMenu menu = (SelectOneMenu) FacesContext.getCurrentInstance().getViewRoot()
					.findComponent((String) ids[ids.length - 1]);
			menu.setValue(remarks);
		}
	}

	public Object renderClubName(Map<String, Object> user) {
		Object shortName = user.get("club.shortName");
		if (shortName != null)
			return shortName;
		return user.get("club.name");
	}

	public String renderAgeType(Map<String, Object> user) {
		AgeType ageType = AgeType.getAgeType((Date) user.get("birthDate"));
		return ageType != null ? ageType.getText() : null;
	}

	@Override
	protected void doSave() {
	}

	@Override
	public <T extends Serializable> Suchergebnis<T> search(Class<T> entityType, Suchkriterien criteria) {
		return service.getAthletes(criteria);
	}

	@Override
	public <T extends HasId> T getObject(Class<T> type, long id, FetchInfo... fetchInfo) {
		return service.getObject(type, id, fetchInfo);
	}
}
