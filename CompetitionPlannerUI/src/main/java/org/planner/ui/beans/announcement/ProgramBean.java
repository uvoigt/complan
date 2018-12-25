package org.planner.ui.beans.announcement;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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
import org.planner.eo.ProgramOptions;
import org.planner.eo.ProgramOptions.DayTimes;
import org.planner.eo.ProgramRace;
import org.planner.eo.ProgramRaceTeam;
import org.planner.eo.Result;
import org.planner.eo.Result_;
import org.planner.model.ResultExtra;
import org.planner.model.Suchkriterien;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.Messages;
import org.planner.ui.beans.UploadBean;
import org.planner.ui.beans.UploadBean.DownloadHandler;
import org.planner.ui.util.BerichtGenerator;
import org.planner.ui.util.JsfUtil;
import org.planner.util.CommonMessages;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.CellEditEvent;

@Named
@RequestScoped
public class ProgramBean extends AbstractEditBean implements DownloadHandler {

	private static final long serialVersionUID = 1L;

	@Inject
	private ProgramOptionsBean options;

	@Inject
	private BerichtGenerator generator;

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
				loadProgram(id);
				cellEditingProgramRaceId = detectCellEvent();
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
		ProgramRace programRace = findProgramRace(null, programRaceId);
		if (programRace.getResult() == null)
			programRace.setResult(new Result(programRace.getProgramId(), programRace, new ArrayList<Placement>()));
		List<Placement> placements = programRace.getResult().getPlacements();
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

	@SuppressWarnings("unchecked")
	public void onPrerenderResults(ComponentSystemEvent event) {
		if (cellEditingProgramRaceId != null) {
			DataTable dataTable = (DataTable) event.getComponent().getNamingContainer();
			// der Wert ist ggf. gefiltert
			Object value = dataTable.getValue();
			program.setRaces((List<ProgramRace>) value);
			loadResult(cellEditingProgramRaceId);
		}
	}

	@Override
	public void setItem(Object item) {
		program = (Program) item;
		// das könnte auch als Argument in der search-xhtml mitgegeben werden
		loadProgram(program.getId());
		JsfUtil.setViewVariable("id", program.getId());
		JsfUtil.setViewVariable("filter", null);
	}

	public boolean canDelete(Map<String, String> item) {
		return item.get("announcement.club.name").equals(auth.getLoggedInUser().getClub().getName());
	}

	private void loadProgram(Long id) {
		program = service.getProgram(id);
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
		Announcement announcementEntity = service.getObject(Announcement.class, announcementId, 1);
		Program p = new Program();
		ProgramOptions options = new ProgramOptions();
		int numberOfDays = (int) ((announcementEntity.getEndDate().getTime()
				- announcementEntity.getStartDate().getTime()) / 1000 / 60 / 60 / 24);
		numberOfDays++;
		List<DayTimes> beginTimes = new ArrayList<>(numberOfDays);
		for (int i = 0; i < numberOfDays; i++) {
			DayTimes dayTimes = new DayTimes(createTime(8, 0), createTime(18, 0));
			dayTimes.addBreak(createTime(12, 0), 60);
			beginTimes.add(dayTimes);
		}
		options.setDayTimes(beginTimes);
		options.setChildProtection(true);
		options.setProtectionPeriod(60);
		options.setRacesPerDay(5);
		options.setIntoFinal(1);
		options.setIntoSemiFinal(3);
		options.setTimeLag(3);
		p.setOptions(options);
		p.setAnnouncement(announcementEntity);
		Long programId = service.createProgram(p);
		loadProgram(programId);
		startseiteBean.setMainContent("/announcement/programEdit.xhtml", program.getId());
		JsfUtil.setViewVariable("filter", null);
	}

	private Date createTime(int hours, int minutes) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 0);
		calendar.set(Calendar.MONTH, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, hours);
		calendar.set(Calendar.MINUTE, minutes);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return new Date(calendar.getTimeInMillis());
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

	public String getFilter() {
		return (String) JsfUtil.getViewVariable("filter");
	}

	public void setFilter(String filter) {
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

	private ProgramRace findProgramRace(List<ProgramRace> races, Long programRaceId) {
		if (races == null)
			races = program.getRaces();
		for (ProgramRace programRace : races) {
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
		DataTable dataTable = (DataTable) evt.getSource();
		@SuppressWarnings("unchecked")
		List<ProgramRace> races = (List<ProgramRace>) dataTable.getValue();
		ProgramRace programRace = findProgramRace(races, Long.valueOf(evt.getRowKey()));
		if (programRace != null && programRace.getResult() != null
				&& !Boolean.FALSE.equals(JsfUtil.getRequestVariable("modified"))) {
			Result result = (Result) JsfUtil.getRequestVariable("result");
			doSaveResult(dataTable.getNamingContainer(), programRace.getId(), result != null ? result.getId() : null,
					result != null ? result.getVersion() : null, programRace.getResult().getPlacements());
		}
	}

	/*
	 * @return null, wenn keine Resultate existieren - true wenn eine Änderung stattgefunden hat, ansonsten false
	 */
	private Boolean checkForExistingResults(ProgramRace programRace, List<Placement> placements) {
		Suchkriterien criteria = new Suchkriterien();
		criteria.addFilter(Result_.programRace.getName() + ".id", programRace.getId());
		List<Result> list = service.search(Result.class, criteria).getListe();
		Result result = list.size() > 0 ? list.get(0) : null;
		if (result == null) {
			return null;
		} else {
			result = service.getObject(Result.class, result.getId(), 1);
			// zwischenspeichern
			JsfUtil.setRequestVariable("result", result);
			boolean modified = !placementsEqual(result.getPlacements(), placements);
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

	private void doSaveResult(UIComponent form, Long programRaceId, Long resultId, Integer version,
			List<Placement> placements) {
		ProgramRace programRace = new ProgramRace();
		programRace.setId(programRaceId);
		// ordnen
		SortedSet<Placement> timed = new TreeSet<>(new Comparator<Placement>() {
			@Override
			public int compare(Placement o1, Placement o2) {
				return (int) (o1.getTime() - o2.getTime());
			}
		});
		List<Placement> dns = new ArrayList<>();
		List<Placement> dnf = new ArrayList<>();
		List<Placement> dq = new ArrayList<>();
		for (Iterator<Placement> it = placements.iterator(); it.hasNext();) {
			Placement p = it.next();
			if (p.getExtra() == ResultExtra.dns)
				dns.add(p);
			else if (p.getExtra() == ResultExtra.dnf)
				dnf.add(p);
			else if (p.getExtra() == ResultExtra.dq)
				dq.add(p);
			else if (p.getTime() != null)
				timed.add(p);
			else
				continue;
			it.remove();
		}
		placements.addAll(timed);
		placements.addAll(dns);
		placements.addAll(dnf);
		placements.addAll(dq);
		//
		Result result = new Result(program.getId(), programRace, placements);
		result.setId(resultId);
		if (version != null)
			result.setVersion(version);
		List<ProgramRace> followUpRaces = service.saveResult(result);
		if (followUpRaces != null) {
			List<Integer> rowIndexes = new ArrayList<>();
			List<Integer> raceNumbers = new ArrayList<>();
			for (ProgramRace race : followUpRaces) {
				raceNumbers.add(race.getNumber());
				int index = indexOfProgramRace(program.getRaces(), race.getId());
				if (index != -1)
					rowIndexes.add(index);
			}

			List<String> expr = new ArrayList<>();
			for (Integer index : rowIndexes) {
				expr.add(form.getClientId() + ":programTable:@row(" + index + ")");
			}
			// TODO
			// PrimeFaces.current().ajax().update(expr);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(null,
					CommonMessages.formatMessage(JsfUtil.getScopedBundle().get("followUpRacesFilled"), raceNumbers)));
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
}
