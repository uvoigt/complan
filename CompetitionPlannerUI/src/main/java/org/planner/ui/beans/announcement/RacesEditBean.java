package org.planner.ui.beans.announcement;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.validator.ValidatorException;
import javax.inject.Named;

import org.planner.eo.Announcement;
import org.planner.eo.Race;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Gender;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.util.JsfUtil;
import org.primefaces.event.CellEditEvent;

@Named
@RequestScoped
public class RacesEditBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private static final SimpleDateFormat FORMAT_DAY = new SimpleDateFormat("EEEE");
	private static final SimpleDateFormat FORMAT_TIME = new SimpleDateFormat("HH:mm");

	private Long announcementId;
	private Announcement announcement;

	private String[] selectedAgeTypes;
	private String[] selectedBoatClasses;
	private String[] selectedGenders;
	private String[] selectedDistances;
	private List<Race> selectedRaces;

	private Date selectedDay;

	private List<Race> races;

	private List<Integer> distances;

	@Override
	@PostConstruct
	@SuppressWarnings("unchecked")
	public void init() {
		announcementId = getIdFromRequestParameters();
		if (announcementId == null)
			announcementId = (Long) JsfUtil.getViewVariable("id");
		JsfUtil.setViewVariable("id", announcementId);
		selectedRaces = new ArrayList<>();
		distances = (List<Integer>) JsfUtil.getViewVariable("distances");
		if (distances == null) {
			distances = new ArrayList<>();
			distances.add(200);
			distances.add(500);
			distances.add(1000);
			distances.add(2000);
			distances.add(5000);
		}
		JsfUtil.setViewVariable("distances", distances);
	}

	public Announcement getAnnouncement() {
		if (announcement == null)
			announcement = service.getObject(Announcement.class, announcementId, 1);
		return announcement;
	}

	public List<Race> getRaces() {
		if (races == null)
			races = service.getRaces(announcementId);
		return races;
	}

	public List<Integer> getDistances() {
		return distances;
	}

	public String getRaceDay(Integer offset) {
		if (offset == null)
			return "";
		Calendar cal = Calendar.getInstance();
		cal.setTime(getAnnouncement().getStartDate());
		cal.add(Calendar.DAY_OF_YEAR, offset);
		return offset != null ? FORMAT_DAY.format(cal.getTime()) : "";
	}

	public void delete() {
		List<Long> ids = new ArrayList<>();
		for (Race race : selectedRaces) {
			ids.add(race.getId());
		}
		service.deleteRaces(announcementId, ids);
		races = null;
		selectedRaces = null;
	}

	public Gender[] getGenders() {
		return Gender.values();
	}

	public AgeType[] getAgeTypes() {
		// Suchkriterien krit = new Suchkriterien();
		// krit.addSortierung(AgeType_.name.getName(), true);
		// return service.search(AgeType.class, krit).getListe();
		return AgeType.values();
	}

	public BoatClass[] getBoatClasses() {
		// Suchkriterien krit = new Suchkriterien();
		// krit.addSortierung(BoatClass_.name.getName(), true);
		// return service.search(BoatClass.class, krit).getListe();
		return BoatClass.values();
	}

	public String[] getSelectedAgeTypes() {
		return selectedAgeTypes;
	}

	public void setSelectedAgeTypes(String[] selectedAgeTypes) {
		this.selectedAgeTypes = selectedAgeTypes;
	}

	public String[] getSelectedBoatClasses() {
		return selectedBoatClasses;
	}

	public void setSelectedBoatClasses(String[] selectedBoatClasses) {
		this.selectedBoatClasses = selectedBoatClasses;
	}

	public String[] getSelectedGenders() {
		return selectedGenders;
	}

	public void setSelectedGenders(String[] selectedGenders) {
		this.selectedGenders = selectedGenders;
	}

	public String[] getSelectedDistances() {
		return selectedDistances;
	}

	public void setSelectedDistances(String[] selectedDistances) {
		this.selectedDistances = selectedDistances;
	}

	public Date getSelectedDay() {
		return selectedDay;
	}

	public void setSelectedDay(Date selectedDay) {
		this.selectedDay = selectedDay;
	}

	public List<Race> getSelectedRaces() {
		return selectedRaces;
	}

	public void setSelectedRaces(List<Race> selectedRaces) {
		this.selectedRaces = selectedRaces;
	}

	@Override
	public void setItem(Object item) {
		// Announcement wäre keine gute Idee, da die Races lazy gezogen werden
		// und der Logger damit hinfällt
		announcementId = (Long) item;
	}

	@Override
	protected void doSave() {
	}

	public void createRaces() {
		Integer dayOffset = null;
		if (selectedDay != null) {
			long distance = selectedDay.getTime() - announcement.getStartDate().getTime();
			dayOffset = (int) (distance / 1000 / 60 / 60 / 24);
		}
		service.createRaces(announcementId, selectedAgeTypes, selectedBoatClasses, selectedGenders, selectedDistances,
				dayOffset);
		races = null;
	}

	public String formatTime(Date date) {
		return date != null ? FORMAT_TIME.format(date) : null;
	}

	public void onCellEdit(CellEditEvent event) {
		String key = event.getRowKey();
		Race race = service.getObject(Race.class, Long.valueOf(key), 0);
		String columnId = event.getColumn().getColumnKey();
		if (columnId.endsWith(":time"))
			race.setStartTime((Date) event.getNewValue());
		else if (columnId.endsWith(":number"))
			race.setNumber((Integer) event.getNewValue());
		else
			return;
		service.saveRace(race);
	}

	public void validateDay(org.primefaces.component.calendar.Calendar calendar) throws ParseException {
		String string = (String) calendar.getSubmittedValue();
		if (string != null && string.length() > 0) {
			Date date = new SimpleDateFormat(calendar.calculatePattern()).parse(string);
			if (date.before(getAnnouncement().getStartDate()) || date.after(getAnnouncement().getEndDate()))
				throw new ValidatorException(new FacesMessage(calendar.getValidatorMessage()));
		}
	}

	public Integer getNewDistance() {
		return null;
	}

	public void setNewDistance(Integer distance) {
		if (distance != null) {
			distances.add(distance);
			Collections.sort(distances);
		}
	}
}
