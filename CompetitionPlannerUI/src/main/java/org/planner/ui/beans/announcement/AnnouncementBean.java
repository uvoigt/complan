package org.planner.ui.beans.announcement;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.planner.eo.Address;
import org.planner.eo.Announcement;
import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Category;
import org.planner.eo.Category_;
import org.planner.eo.Club;
import org.planner.eo.Location;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.Messages;
import org.planner.ui.beans.SearchBean.ColumnModel;
import org.planner.ui.beans.UploadBean;
import org.planner.ui.beans.UploadBean.DownloadHandler;
import org.planner.ui.util.BerichtGenerator;
import org.planner.ui.util.JsfUtil;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.LogUtil.TechnischeException;
import org.primefaces.component.calendar.Calendar;
import org.primefaces.event.SelectEvent;

@Named
@RequestScoped
public class AnnouncementBean extends AbstractEditBean implements DownloadHandler {

	private static final long serialVersionUID = 1L;

	@Inject
	private Messages messages;

	private UploadBean uploadBean;

	private Announcement announcement;

	private String location;
	private String openingLocation;
	private String juryLocation;

	private List<ColumnModel> columns;

	private Calendar endDate;

	@PostConstruct
	public void init() {
		uploadBean = new UploadBean(this, null, null);

		Long id = getIdFromRequestParameters();
		if (id == null)
			id = (Long) JsfUtil.getViewVariable("id");
		if (id != null) {
			announcement = service.getObject(Announcement.class, id, 1);
			JsfUtil.setViewVariable("id", announcement.getId());
		} else {
			announcement = new Announcement();
		}
		populateLocation();
		// TODO zentral
		// damit
		if (announcement.getLocation().getClub() != null)
			announcement.getLocation().setAddress(new Address());
	}

	@Override
	public void setItem(Object item) {
		announcement = (Announcement) item;
		populateLocation();
		if (announcement.getId() == null) {
			announcement.setText(getTemplate());
		} else {
			JsfUtil.setViewVariable("id", announcement.getId());
		}
	}

	private String getTemplate() {
		final String name = "/announcementTemplate.xhtml";
		InputStream in = getClass().getResourceAsStream(name);
		if (in == null)
			throw new TechnischeException(name + " nicht gefunden", null);
		try {
			try {
				return IOUtils.toString(in, "UTF-8");
			} finally {
				in.close();
			}
		} catch (IOException e) {
			throw new TechnischeException("Fehler beim Lesen von " + name, e);
		}
	}

	public Calendar getEndDate() {
		return endDate;
	}

	public void setEndDate(Calendar endDate) {
		this.endDate = endDate;
	}

	public Announcement getAnnouncement() {
		return announcement;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getOpeningLocation() {
		return openingLocation;
	}

	public void setOpeningLocation(String openingLocation) {
		this.openingLocation = openingLocation;
	}

	public String getJuryLocation() {
		return juryLocation;
	}

	public void setJuryLocation(String juryLocation) {
		this.juryLocation = juryLocation;
	}

	@Override
	protected void doSave() {
		Club club = getLoggedInUser().getClub();
		handleLocation(location, announcement.getLocation(), club);
		// handleLocation(juryLocation, announcement.getJuryLocation(), club);
		// handleLocation(openingLocation, announcement.getOpeningLocation(),
		// club);
		// handleLocation(announcer, announcement.getAnnouncer(), club); TODO
		// die Meldestelle

		// vorher sollte der Ausschreibungstext geprüft werden
		try {
			new BerichtGenerator().generate(announcement, new NullOutputStream());
		} catch (Exception e) {
			if (e instanceof FachlicheException)
				throw (FachlicheException) e;
			if (e instanceof TechnischeException)
				throw (TechnischeException) e;
			throw new FachlicheException(ResourceBundle.getBundle("MessagesBundle"), "announcements.textError", e);
		}

		service.saveAnnouncement(announcement);
	}

	private void populateLocation() {
		location = announcement.getId() == null || announcement.getLocation().getClub() != null ? "club" : "address";
		// openingLocation = announcement.getOpeningLocation().getClub() != null
		// ? "club" : "address";
		// juryLocation = announcement.getJuryLocation().getClub() != null ?
		// "club" : "address";
	}

	private void handleLocation(String value, Location loc, Club club) {
		if ("club".equals(value)) {
			loc.setClub(club);
			loc.setAddress(new Address());
		} else {
			loc.setClub(null);
		}
	}

	public void startDateChanged(SelectEvent event) {
		Object value = ((Calendar) event.getSource()).getValue();
		endDate.setMindate(value);
	}

	public List<Category> getCategories(String text) {
		return service.search(Category.class, createAutocompleteCriteria(text, Category_.name.getName())).getListe();
	}

	public AgeType[] getFeeRows() {
		return AgeType.values();
	}

	public List<ColumnModel> getFeeColumns() {
		if (columns == null) {
			columns = new ArrayList<>();
			for (BoatClass bc : BoatClass.values()) {
				columns.add(new ColumnModel(bc.getText(), bc.name(), true));
			}
		}
		return columns;
	}

	public void setStatus(AnnouncementStatus status) {
		service.setAnnouncementStatus(announcement.getId(), status);
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(null, messages.get("announcements.statusSet_" + status)));
	}

	public UploadBean getUploadBean() {
		return uploadBean;
	}

	@Override
	public String getDownloadFileName(Object selection) {
		FacesContext ctx = FacesContext.getCurrentInstance();
		// unterscheide die beiden Use-Cases Aufruf von der Suchseite und Aufruf von der Edit-Seite
		if (ctx.getExternalContext().getRequestParameterMap().containsKey("editForm")) {
			Club club = service.getLoggedInUser().getClub();
			announcement.setClub(club);
			handleLocation(location, announcement.getLocation(), club);
		} else {
			Long id = (Long) ctx.getApplication().getELResolver().getValue(ctx.getELContext(), selection, "id");
			announcement = service.getObject(Announcement.class, id, 1);
		}
		return JsfUtil.getScopedBundle().format("pdfName", announcement.getName(), announcement.getStartDate());
	}

	@Override
	public void handleDownload(OutputStream out, String typ, Object selection) throws Exception {
		new BerichtGenerator().generate(announcement, out);
	}
}
