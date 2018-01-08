package org.planner.ui.beans.announcement;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import org.planner.eo.ProgramRace.RaceType;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.Messages;
import org.planner.ui.util.JsfUtil;
import org.planner.util.ExpressionParser;
import org.planner.util.LogUtil.FachlicheException;

@Named
@RequestScoped
public class ProgramBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private static DateFormat DF_WEEKDAY = new SimpleDateFormat("EEEE");

	@Inject
	private Messages messages;

	private Program program;

	private List<ProgramRace> selectedRaces;

	private String exprStatus;

	private boolean showTeams = true;

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
		service.generateProgram(program);
		loadProgram(program.getId());
	}

	public void checkProgram() {
		service.checkProgram(program);
		loadProgram(program.getId());
	}

	public String renderAgeGroup(User user) {
		if (user == null || user.getAgeType().ordinal() >= AgeType.junioren.ordinal())
			return null;
		return new StringBuilder().append("(").append(user.getAge()).append(")").toString();
	}

	public String renderRaceMode(ProgramRace race) {
		String s = "";
		if (race.getRaceType() == RaceType.heat) {
			int intoFinal = race.getIntoFinal();
			int intoSemiFinal = race.getIntoSemiFinal();
			if (intoFinal > 0)
				s = (intoFinal > 1 ? "1. - " : "") + intoFinal + ". in den Endlauf"; // TODO
			if (intoSemiFinal > 0) {
				if (s.length() > 0)
					s += " ";
				if (intoFinal == 0)
					s += "1. - ";
				else if (intoSemiFinal > intoFinal + 1)
					s += (intoFinal + 1) + ". - ";
				s += intoSemiFinal + ". in den Zwischenlauf"; // TODO
			}
		}
		return s;
	}

	public List<String> suggestExpr(String text) {
		return ExpressionParser.getCompletion(text);
	}

	public void checkExpr() {
		exprStatus = null;
		String expr = program.getOptions().getExpr();
		if (expr != null) {
			try {
				new ExpressionParser().evaluateExpression(expr, 0, 9); // TODO
			} catch (FachlicheException e) {
				exprStatus = e.getMessage();
			}
		}
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

	public String getExprStatus() {
		return exprStatus;
	}

	public String renderStartTime(Date date) {
		return DF_WEEKDAY.format(date) + " " + DateFormat.getTimeInstance().format(date);
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
