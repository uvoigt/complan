package org.planner.ui.beans.announcement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.Announcement;
import org.planner.eo.Race;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Gender;
import org.planner.remote.ServiceFacade;
import org.planner.ui.beans.AbstractEditBean;
import org.primefaces.event.CellEditEvent;

@Named
@SessionScoped
public class RacesEditBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private static final SimpleDateFormat FORMAT_DAY = new SimpleDateFormat("EEEE");
	private static final SimpleDateFormat FORMAT_TIME = new SimpleDateFormat("HH:mm");

	@Inject
	private ServiceFacade service;

	private Long announcementId;
	private Announcement announcement;

	private String[] selectedAgeTypes;
	private String[] selectedBoatClasses;
	private String[] selectedGenders;
	private String[] selectedDistances;
	private List<Race> selectedRaces = new ArrayList<>();

	private Date selectedDay;

	private List<Race> races;

	public Long getAnnouncementId() {
		return announcementId;
	}

	public void setAnnouncementId(Long announcementId) {
		this.announcementId = announcementId;
	}

	public Announcement getAnnouncement() {
		if (announcement == null)
			announcement = service.getObject(Announcement.class, announcementId);
		return announcement;
	}

	public List<Race> getRaces() {
		if (races == null)
			races = service.getRaces(announcementId);
		return races;
	}

	public String getRaceDay(Integer offset) {
		if (offset == null)
			return "";
		Calendar cal = Calendar.getInstance();
		cal.setTime(announcement.getStartDate());
		cal.add(Calendar.DAY_OF_YEAR, offset);
		return offset != null ? FORMAT_DAY.format(cal.getTime()) : "";
	}

	public void delete() {
		List<Long> ids = new ArrayList<>();
		for (Race race : selectedRaces) {
			ids.add(race.getId());
		}
		service.delete(Race.class, ids);
		selectedRaces.clear();
		races = null;
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
		announcement = null;
		selectedAgeTypes = null;
		selectedBoatClasses = null;
		selectedDistances = null;
		selectedGenders = null;
		selectedDay = null;
		selectedRaces.clear();
		races = null;
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
		Race race = service.getObject(Race.class, Long.valueOf(key));
		String columnId = event.getColumn().getColumnKey();
		if (columnId.endsWith(":time"))
			race.setStartTime((Date) event.getNewValue());
		else if (columnId.endsWith(":number"))
			race.setNumber((Integer) event.getNewValue());
		else
			return;
		service.saveRace(race);
	}
}