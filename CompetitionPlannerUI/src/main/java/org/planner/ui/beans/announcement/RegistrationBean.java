package org.planner.ui.beans.announcement;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.AbstractEntity_;
import org.planner.eo.Announcement;
import org.planner.eo.Participant;
import org.planner.eo.Race;
import org.planner.eo.RegEntry;
import org.planner.eo.Registration;
import org.planner.eo.User;
import org.planner.model.BoatClass;
import org.planner.model.Suchkriterien;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.Messages;
import org.planner.ui.util.JsfUtil;

@Named
@RequestScoped
public class RegistrationBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	@Inject
	private Messages messages;

	private Registration registration;

	private List<Race> races;

	private List<User> athletes;

	private List<Race> selectedRaces;

	private List<User> selectedAthletes;

	private RegEntry selectedEntry;

	@PostConstruct
	public void init() {
		Long id = getIdFromRequestParameters();
		if (id == null)
			id = (Long) JsfUtil.getViewVariable("id");
		if (id != null) {
			loadRegistration(id);
			JsfUtil.setViewVariable("id", registration.getId());
		}
	}

	@Override
	public void setItem(Object item) {
		// wird nochmals geladen aufgrund der Detailtiefe
		loadRegistration(((Registration) item).getId());
		JsfUtil.setViewVariable("id", registration.getId());
	}

	private void loadRegistration(Long id) {
		// TODO auch hier... über einen search-view-parameter die detailtiefe festlegen
		registration = service.getObject(Registration.class, id, 2);
	}

	public Registration getRegistration() {
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
		Long announcementId = (Long) ctx.getApplication().getELResolver().getValue(ctx.getELContext(), announcement,
				AbstractEntity_.id.getName());
		Registration r = new Registration();
		r.setAnnouncement(service.getObject(Announcement.class, announcementId, 1));
		Long registrationId = service.createRegistration(r);
		loadRegistration(registrationId);
		startseiteBean.setMainContent("/announcement/registrationEdit.xhtml", this.registration.getId());
	}

	public void addToRegistration() {
		if (selectedEntry != null) {
			// es wir zu einem existierenden RegEntry hinzugefügt
			// sofern das impl-seitig gestattet wird

			List<RegEntry> entries = new ArrayList<>(1);
			entries.add(selectedEntry);
			setAthletesToEntry(selectedEntry);
			service.saveRegEntries(registration.getId(), entries);
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
		}
		loadRegistration(registration.getId());
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

	public void deleteFromRegistration(RegEntry entry) {
		service.deleteFromRegEntry(registration.getId(), entry);
		loadRegistration(registration.getId());
	}

	public List<Race> getRaces() {
		if (races == null) {
			Announcement announcement = registration.getAnnouncement();
			races = service.getRaces(announcement.getId());
		}
		return races;
	}

	public List<User> getAthletes() {
		if (athletes == null) {
			Suchkriterien criteria = new Suchkriterien();
			criteria.addFilter("club", service.getLoggedInUser().getClub().getId());
			criteria.addFilter("roles.role", "Sportler");
			athletes = service.search(User.class, criteria).getListe();
		}
		return athletes;
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

	public String getRaceString(RegEntry entry) {
		Race race = entry.getRace();
		StringBuilder sb = new StringBuilder();
		sb.append("Renn-Nr.: ");
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

	public int getMinimumTeamSize(Race race) {
		switch (race.getBoatClass()) {
		case c1:
		case k1:
			return 1;
		case c2:
		case k2:
			return 2;
		case c4:
		case k4:
			return 4;
		case c8:
			return 8;
		}
		return 0;
	}

	public void submitRegistration(Object registration) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		Long registrationId = (Long) ctx.getApplication().getELResolver().getValue(ctx.getELContext(), registration,
				AbstractEntity_.id.getName());
		service.submitRegistration(registrationId);
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(null, messages.get("registrations.statusSet")));
	}

	@Override
	protected void doSave() {
	}
}
