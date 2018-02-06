package org.planner.ui.beans.announcement;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.AbstractEntity_;
import org.planner.eo.Announcement;
import org.planner.eo.Program;
import org.planner.eo.ProgramOptions;
import org.planner.eo.ProgramOptions.DayTimes;
import org.planner.eo.ProgramRace;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.UploadBean;
import org.planner.ui.beans.UploadBean.DownloadHandler;
import org.planner.ui.util.BerichtGenerator;
import org.planner.ui.util.JsfUtil;

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

	@Override
	@PostConstruct
	public void init() {
		super.init();

		uploadBean = new UploadBean(this, null, null);

		Long id = getIdFromRequestParameters();
		if (id == null)
			id = (Long) JsfUtil.getViewVariable("id");
		if (id != null) {
			if (!isCancelPressed())
				loadProgram(id);
			JsfUtil.setViewVariable("id", id);
		}
	}

	@Override
	public void setItem(Object item) {
		program = (Program) item;
		// das könnte auch als Argument in der search-xhtml mitgegeben werden
		loadProgram(program.getId());
		JsfUtil.setViewVariable("id", program.getId());
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
		this.program = service.getObject(Program.class, programId, 2);
		startseiteBean.setMainContent("/announcement/programEdit.xhtml", program.getId());
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
}
