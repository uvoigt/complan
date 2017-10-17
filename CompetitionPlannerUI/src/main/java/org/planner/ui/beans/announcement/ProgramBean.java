package org.planner.ui.beans.announcement;

import java.util.Calendar;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.AbstractEntity_;
import org.planner.eo.Announcement;
import org.planner.eo.Program;
import org.planner.eo.ProgramOptions;
import org.planner.eo.ProgramRace;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.Messages;
import org.planner.ui.util.JsfUtil;

@Named
@RequestScoped
public class ProgramBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	@Inject
	private Messages messages;

	private Program program;

	private ProgramRace selectedRace;

	@PostConstruct
	public void init() {
		Long id = getIdFromRequestParameters();
		if (id == null)
			id = (Long) JsfUtil.getViewVariable("id");
		if (id != null) {
			loadProgram(id);
			JsfUtil.setViewVariable("id", program.getId());
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
		// TODO auch hier... über einen search-view-parameter die detailtiefe festlegen
		program = service.getObject(Program.class, id, 3);
	}

	@Override
	protected void doSave() {
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
		Date[] beginTimes = new Date[numberOfDays];
		for (int i = 0; i < beginTimes.length; i++) {
			beginTimes[i] = createTime(8, 0);
		}
		options.setBeginTimes(beginTimes);
		options.setChildProtection(true);
		options.setProtectionPeriod(60);
		options.setRacesPerDay(5);
		options.setIntoFinal(1);
		options.setIntoSemiFinal(3);
		options.setTimeLag(3);
		options.setLaunchBreak(createTime(12, 0));
		options.setBreakDuration(60);
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
		service.generateProgram(program);
		loadProgram(program.getId());
	}

	public String getAgeGroup(User user) {
		if (user.getAgeType().ordinal() >= AgeType.junioren.ordinal())
			return null;
		return new StringBuilder().append("(").append(user.getAge()).append(")").toString();
	}

	public ProgramRace getSelectedRace() {
		return selectedRace;
	}

	public void setSelectedRace(ProgramRace selectedRace) {
		this.selectedRace = selectedRace;
	}
}
