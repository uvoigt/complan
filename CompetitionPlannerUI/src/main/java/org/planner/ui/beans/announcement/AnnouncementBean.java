package org.planner.ui.beans.announcement;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import org.planner.eo.Address;
import org.planner.eo.Announcement;
import org.planner.eo.Category;
import org.planner.eo.Category_;
import org.planner.eo.Club;
import org.planner.eo.Location;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.ui.beans.AbstractEditBean;
import org.planner.ui.beans.SearchBean.ColumnModel;
import org.planner.ui.util.PdfCreator;
import org.primefaces.context.RequestContext;

import com.itextpdf.text.DocumentException;

@Named
@SessionScoped
public class AnnouncementBean extends AbstractEditBean {

	private static final long serialVersionUID = 1L;

	private Announcement announcement;

	private String location;
	private String openingLocation;
	private String juryLocation;

	private List<ColumnModel> columns;

	@Override
	public void setItem(Object item) {
		announcement = (Announcement) item;
		location = announcement.getLocation().getClub() != null ? "club" : "address";
		openingLocation = announcement.getOpeningLocation().getClub() != null ? "club" : "address";
		juryLocation = announcement.getJuryLocation().getClub() != null ? "club" : "address";
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
		Club club = service.getLoggedInUser().getClub();
		handleLocation(location, announcement.getLocation(), club);
		handleLocation(juryLocation, announcement.getJuryLocation(), club);
		handleLocation(openingLocation, announcement.getOpeningLocation(), club);
		// handleLocation(announcer, announcement.getAnnouncer(), club); TODO
		// die Meldestelle
		service.saveAnnouncement(announcement);
	}

	private void handleLocation(String value, Location loc, Club club) {
		if ("club".equals(value)) {
			loc.setClub(club);
			loc.setAddress(new Address());
		} else {
			loc.setClub(null);
		}
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

	public void createPdf(Announcement announcement) throws DocumentException, IOException {

		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();

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
			PdfCreator.createAnnouncent(announcement, out);

			externalContext.setResponseStatus(HttpServletResponse.SC_OK);
			externalContext.responseFlushBuffer();

		} catch (Exception e) {
			externalContext.setResponseStatus(HttpServletResponse.SC_NO_CONTENT);
		} finally {
			facesContext.responseComplete();
		}

	}
}
