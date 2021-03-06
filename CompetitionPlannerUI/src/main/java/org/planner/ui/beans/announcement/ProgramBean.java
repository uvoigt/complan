package org.planner.ui.beans.announcement;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.AbstractEntity_;
import org.planner.eo.Announcement;
import org.planner.eo.Placement;
import org.planner.eo.Program;
import org.planner.eo.Program.ProgramStatus;
import org.planner.eo.ProgramRace;
import org.planner.eo.ProgramRaceTeam;
import org.planner.model.FetchInfo;
import org.planner.model.ResultExtra;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.Messages;
import org.planner.ui.beans.UploadBean;
import org.planner.ui.beans.UploadBean.DownloadHandler;
import org.planner.ui.util.BerichtGenerator;
import org.planner.ui.util.JsfUtil;
import org.planner.util.CommonMessages;
import org.primefaces.PrimeFaces;
import org.primefaces.event.CellEditEvent;

@Named
@RequestScoped
public class ProgramBean extends AbstractEditBean implements DownloadHandler {

	private static final long serialVersionUID = 1L;

	@Inject
	private ProgramOptionsBean options;

	@Inject
	private BerichtGenerator generator;

	@Inject
	private RenderBean renderBean;

	private UploadBean uploadBean;

	private Program program;

	private List<ProgramRace> selectedRaces;

	private boolean showTeams = true;

	private Long cellEditingProgramRaceId;

	@Override
	@PostConstruct
	public void init() {
		super.init();

		uploadBean = new UploadBean(this, null, null);

		Long id = getIdFromRequestParameters();
		if (id == null)
			id = (Long) JsfUtil.getViewVariable("id");
		if (id != null) {
			JsfUtil.setViewVariable("id", id);
			if (!isCancelPressed()) {
				cellEditingProgramRaceId = detectCellEvent();
				loadProgram(id);
				if (cellEditingProgramRaceId != null)
					loadResult(cellEditingProgramRaceId);
			}
		}
	}

	private Long detectCellEvent() {
		Collection<String> ids = FacesContext.getCurrentInstance().getPartialViewContext().getExecuteIds();
		String exId = ids.size() == 1 ? ids.iterator().next() : null;
		if (exId == null)
			return null;
		// cell edit Init oder cell edit
		String cellInfo = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
				.get(exId + "_cellInfo");
		if (cellInfo == null)
			return null;
		String[] split = cellInfo.split(",");
		return Long.parseLong(split[2]);
	}

	private void loadResult(Long programRaceId) {
		ProgramRace programRace = findProgramRace(programRaceId);
		if (programRace.getPlacements() == null)
			programRace.setPlacements(new ArrayList<Placement>());
		List<Placement> placements = programRace.getPlacements();
		List<ProgramRaceTeam> participants = programRace.getParticipants();
		if (participants != null) {
			for (ProgramRaceTeam team : participants) {
				boolean found = false;
				for (int i = 0; i < placements.size(); i++) {
					Placement placement = placements.get(i);
					if (!placement.getTeam().getTeamId().equals(team.getTeamId()))
						continue;
					found = true;
					break;
				}
				if (!found)
					placements.add(new Placement(team, null, null));
			}
		}
		Collections.sort(placements, new Comparator<Placement>() {
			@Override
			public int compare(Placement o1, Placement o2) {
				return o1.getPosition() - o2.getPosition();
			}
		});
	}

	/*
	 * Diese Variante des Filterns (lazy=true und anstelle filterBy="#{renderBean.getRaceFilter(race)}"
	 * filterFunction="#{renderBean.filterRaces}" diese Lösung dient dem Vermeiden von setValue(filteredValue).
	 */
	public void onPrerenderTable(@SuppressWarnings("unused") ComponentSystemEvent event) {
		List<String> filter = getFilter();
		if (program != null) {
			if (filter != null)
				renderBean.filterRaces(program.getRaces().iterator(), filter, Locale.getDefault(), true);
			StringBuilder script = new StringBuilder();
			script.append("updateCount('.raceCount', '");
			script.append(JsfUtil.getScopedBundle().get("raceCount"));
			script.append("', ");
			script.append(program.getRaces().size());
			script.append(")");
			if (getProgram().getStatus() == ProgramStatus.created) {
				script.append(";programEdit.enableSwap()");
			}
			PrimeFaces.current().executeScript(script.toString());
		}
	}

	@Override
	public void setItem(Object item) {
		loadProgram((Long) item);
		JsfUtil.setViewVariable("id", program.getId());
		JsfUtil.setViewVariable("filter", null);
	}

	@Override
	public FetchInfo[] getFetchInfo() {
		return null;
	}

	public boolean canDelete(Map<String, String> item) {
		return item.get("announcement.club.name").equals(auth.getLoggedInUser().getClub().getName());
	}

	private void loadProgram(Long id) {
		// implizite Abfrage, ob es sich um ein CellEditing handelt
		program = service.getProgram(id, cellEditingProgramRaceId != null, false);
		options.setProgram(program);
	}

	@Override
	protected void doSave() {
	}

	@Override
	public String getDownloadFileName(String typ, Object selection) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		Long id = (Long) ctx.getApplication().getELResolver().getValue(ctx.getELContext(), selection, "id");
		loadProgram(id);
		Announcement announcement = program.getAnnouncement();
		return JsfUtil.getScopedBundle().format("pdfName", announcement.getName(), announcement.getStartDate());
	}

	@Override
	public void handleDownload(OutputStream out, String typ, Object selection) throws Exception {
		generator.generate(program, out);
	}

	public UploadBean getUploadBean() {
		return uploadBean;
	}

	public Program getProgram() {
		return program;
	}

	public void createProgram(Object announcement) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		Long announcementId = (Long) ctx.getApplication().getELResolver().getValue(ctx.getELContext(), announcement,
				AbstractEntity_.id.getName());
		Long programId = service.createProgram(announcementId);
		loadProgram(programId);
		startseiteBean.setMainContent("/announcement/programEdit.xhtml", program.getId());
		JsfUtil.setViewVariable("filter", null);
	}

	public void setStatus(Object program, ProgramStatus status) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		Long programId = (Long) ctx.getApplication().getELResolver().getValue(ctx.getELContext(), program,
				AbstractEntity_.id.getName());
		service.setProgramStatus(programId, status);
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(null, JsfUtil.getScopedBundle().get("programs.statusSet_" + status)));
	}

	public void generateProgram() {
		program.getOptions().setExpr(options.getProgram().getOptions().getExpr());
		service.generateProgram(program);
		loadProgram(program.getId());
	}

	public void checkProgram() {
		service.checkProgram(program);
		loadProgram(program.getId());
	}

	public void swapRaces() {
		if (selectedRaces.size() == 2) {
			ProgramRace r1 = selectedRaces.get(0);
			ProgramRace r2 = selectedRaces.get(1);
			service.swapRaces(r1, r2);
			loadProgram(program.getId());
			selectedRaces.clear();
		}
	}

	public ResultExtra[] getResultExtras() {
		return ResultExtra.values();
	}

	@SuppressWarnings("unchecked")
	public List<String> getFilter() {
		return (List<String>) JsfUtil.getViewVariable("filter");
	}

	public void setFilter(List<String> filter) {
		JsfUtil.setViewVariable("filter", filter);
	}

	public List<ProgramRace> getSelectedRaces() {
		return selectedRaces;
	}

	public void setSelectedRaces(List<ProgramRace> selectedRaces) {
		this.selectedRaces = selectedRaces;
	}

	public boolean isShowTeams() {
		return showTeams;
	}

	public void setShowTeams(boolean showTeams) {
		this.showTeams = showTeams;
	}

	public String getOrderListId(UIComponent component) {
		int index = component.getParent().getChildren().indexOf(component);
		return component.getParent().getChildren().get(index + 1).getClientId();
	}

	private ProgramRace findProgramRace(Long programRaceId) {
		for (ProgramRace programRace : program.getRaces()) {
			if (programRace != null && programRace.getId().equals(programRaceId)) {
				return programRace;
			}
		}
		return null;
	}

	private int indexOfProgramRace(List<ProgramRace> races, Long programRaceId) {
		for (int i = 0; i < races.size(); i++) {
			ProgramRace programRace = races.get(i);
			if (programRace != null && programRace.getId().equals(programRaceId)) {
				return i;
			}
		}
		return -1;
	}

	public void onResultUpdate(final CellEditEvent evt) {
		List<?> newValue = (List<?>) evt.getNewValue();
		@SuppressWarnings("unchecked")
		List<Placement> placements = (List<Placement>) newValue.get(0);
		ProgramRace programRace = findProgramRace(Long.valueOf(evt.getRowKey()));
		if (programRace != null && programRace.getPlacements() != null
				&& !Boolean.FALSE.equals(JsfUtil.getRequestVariable("modified"))) {
			doSaveResult(programRace, placements);
		}
	}

	/*
	 * @return null, wenn keine Resultate existieren - true wenn eine Änderung stattgefunden hat, ansonsten false
	 */
	private Boolean checkForExistingResults(ProgramRace programRace, List<Placement> placements) {
		List<Placement> savedPlacements = service.getPlacements(programRace.getId());
		Placement result = savedPlacements.size() > 0 ? savedPlacements.get(0) : null;
		if (result == null) {
			return null;
		} else {
			boolean modified = !placementsEqual(savedPlacements, placements);
			JsfUtil.setRequestVariable("modified", modified);
			return modified;
		}
	}

	private boolean placementsEqual(List<Placement> l1, List<Placement> l2) {
		if (l1 == null || l2 == null)
			return false;
		if (l1.size() != l2.size())
			return false;
		for (int i = 0; i < l1.size(); i++) {
			Placement p1 = l1.get(i);
			Placement p2 = l2.get(i);
			if (p1 == null || p2 == null)
				return false;
			if (!p1.getTeam().getId().equals(p2.getTeam().getId()) || p1.getExtra() != p2.getExtra()
					|| (p1.getTime() == null && p1.getTime() != p2.getTime()
							|| p1.getTime() != null && !p1.getTime().equals(p2.getTime())))
				return false;
		}
		return true;
	}

	private void doSaveResult(ProgramRace programRace, List<Placement> placements) {
		List<ProgramRace> followUpRaces = service.saveResult(programRace.getId(), placements);
		if (followUpRaces != null) {
			List<Integer> rowIndexes = new ArrayList<>();
			List<String> raceTexts = new ArrayList<>();
			for (int i = 0; i < followUpRaces.size(); i++) {
				ProgramRace race = followUpRaces.get(i);
				StringBuilder text = new StringBuilder();
				text.append(renderBean.renderRaceNumber(race));
				raceTexts.add(text.toString());
				int index = indexOfProgramRace(program.getRaces(), race.getId());
				if (index != -1)
					rowIndexes.add(index);
			}

			PrimeFaces.current().executeScript("PF('programTable').filter()");
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(null,
					CommonMessages.formatMessage(JsfUtil.getScopedBundle().get("followUpRacesFilled"), raceTexts)));
		}
	}

	public void validateResult(FacesContext ctx, UIComponent component, List<Placement> placements) {
		Boolean isConsistent = null;
		for (Placement placement : placements) {
			if (placement.getExtra() != null)
				continue;
			boolean hasTime = placement.getTime() != null;
			if (isConsistent == null)
				isConsistent = hasTime;
			else if (isConsistent != hasTime)
				throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
						JsfUtil.getScopedBundle().get("timesNeeded"), null));
		}
		Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();
		String value = params.get(component.getClientId().replace(component.getId(), "override_input"));
		boolean override = "on".equals(value);
		ProgramRace programRace = placements.size() > 0 ? placements.get(0).getTeam().getProgramRace() : null;
		Boolean modified = programRace != null ? checkForExistingResults(programRace, placements) : Boolean.FALSE;
		if (Boolean.TRUE.equals(modified) && !override) {
			Messages messages = JsfUtil.getScopedBundle();
			String message = messages.format("resultsExisting", messages.get("overrideResult"));
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
		}
	}

	public void deleteProgram(Long programId) {
		service.deleteProgram(programId);
		String msg = JsfUtil.getScopedBundle().get("deleteSuccess");
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(null, msg));
	}
}
