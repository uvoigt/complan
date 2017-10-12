package org.planner.ui.beans.announcement;

import java.util.Calendar;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.planner.eo.AbstractEntity_;
import org.planner.eo.Announcement;
import org.planner.eo.Program;
import org.planner.eo.ProgramOptions;
import org.planner.eo.ProgramRace;
import org.planner.eo.Race;
import org.planner.eo.User;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.util.JsfUtil;

@Named
@RequestScoped
public class ProgramBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Program program;

	private int intoSemiFinal;
	private int intoFinal;

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
		parseHeatMode(program.getOptions().getHeatMode());
	}

	private void loadProgram(Long id) {
		// TODO auch hier... über einen search-view-parameter die detailtiefe festlegen
		program = service.getObject(Program.class, id, 3);
	}

	private void parseHeatMode(String string) {
		if (string != null) {
			String[] split = string.split(";");
			intoFinal = Integer.parseInt(split[0]);
			intoSemiFinal = Integer.parseInt(split[1]);
		}
	}

	private void updateHeatMode() {
		program.getOptions().setHeatMode(intoFinal + ";" + intoSemiFinal);
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
		Program p = new Program();
		ProgramOptions options = new ProgramOptions();
		options.setHeatMode("1;3");
		options.setChildProtection(true);
		options.setProtectionPeriod(60);
		options.setRacesPerDay(5);
		p.setOptions(options);
		p.setAnnouncement(service.getObject(Announcement.class, announcementId, 1));
		Long programId = service.createProgram(p);
		this.program = service.getObject(Program.class, programId, 2);
		parseHeatMode(options.getHeatMode());
		startseiteBean.setMainContent("/announcement/programEdit.xhtml", program.getId());
	}

	public void generateProgram() {
		updateHeatMode();
		service.generateProgram(program);
		loadProgram(program.getId());
	}

	public String getRaceString(ProgramRace programRace) {
		Race race = programRace.getRace();
		StringBuilder sb = new StringBuilder();
		sb.append("Rennen "); // TODO messages
		sb.append(race.getNumber());
		sb.append("-");
		sb.append(programRace.getNumber());
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

	public String getAgeGroup(User user) {
		Date birthDate = user.getBirthDate();
		// Sportler sollten! eigentlich ein Geburtsdatum haben
		if (birthDate == null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(birthDate);
		int ageGroup = cal.get(Calendar.YEAR);
		return Integer.toString(ageGroup);
	}

	public int getIntoSemiFinal() {
		return intoSemiFinal;
	}

	public void setIntoSemiFinal(int intoSemiFinal) {
		this.intoSemiFinal = intoSemiFinal;
	}

	public int getIntoFinal() {
		return intoFinal;
	}

	public void setIntoFinal(int intoFinal) {
		this.intoFinal = intoFinal;
	}

	public ProgramRace getSelectedRace() {
		return selectedRace;
	}

	public void setSelectedRace(ProgramRace selectedRace) {
		this.selectedRace = selectedRace;
	}
}
