package org.planner.ui.beans.announcement;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.planner.eo.Address;
import org.planner.eo.Announcement;
import org.planner.eo.Category;
import org.planner.eo.Category_;
import org.planner.eo.Club;
import org.planner.eo.Location;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.Messages;
import org.planner.ui.beans.SearchBean.ColumnModel;
import org.planner.ui.util.BerichtGenerator;
import org.planner.util.LogUtil.TechnischeException;
import org.primefaces.component.calendar.Calendar;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;

import com.itextpdf.text.DocumentException;

@Named
@RequestScoped
public class AnnouncementBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	@Inject
	private Messages messages;

	private Announcement announcement;

	private String location;
	private String openingLocation;
	private String juryLocation;

	private List<ColumnModel> columns;

	private Calendar endDate;

	@PostConstruct
	public void init() {
		Long id = getIdFromRequestParameters();
		if (id != null)
			announcement = service.getObject(Announcement.class, id, 1);
		else
			announcement = new Announcement();
		populateLocation();
	}

	@Override
	public void setItem(Object item) {
		announcement = (Announcement) item;
		populateLocation();
		if (announcement.getId() == null) {
			announcement.setText(getTemplate());
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

	public void announce() {
		service.announce(announcement.getId());
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(null, messages.get("announcements.statusSet")));
	}

	public void createPdf(Map<String, Object> row) throws DocumentException, IOException {

		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		Announcement announcement = service.getObject(Announcement.class, (Long) row.get("id"), 1);

		String contentDispositionValue = "attachment";

		externalContext.setResponseContentType("application/pdf");
		// externalContext.setResponseCharacterEncoding("iso-8859-1");
		externalContext.setResponseHeader("Content-Disposition",
				contentDispositionValue + ";filename=\"" + announcement.getName() + ".pdf\"");

		if (RequestContext.getCurrentInstance().isSecure()) {
			externalContext.setResponseHeader("Cache-Control", "public");
			externalContext.setResponseHeader("Pragma", "public");
		}

		OutputStream out = externalContext.getResponseOutputStream();

		try {
			new BerichtGenerator().generate(announcement, out);

			externalContext.setResponseStatus(HttpServletResponse.SC_OK);
			externalContext.responseFlushBuffer();

		} catch (Exception e) {
			e.printStackTrace();
			externalContext.setResponseStatus(HttpServletResponse.SC_NO_CONTENT);
		} finally {
			facesContext.responseComplete();
		}

	}
}
