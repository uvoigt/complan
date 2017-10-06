package org.planner.ui.beans;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Locale;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.planner.remote.ServiceFacade;
import org.planner.ui.util.JsfUtil;
import org.planner.util.LoggingInterceptor.Silent;
import org.primefaces.context.RequestContext;

@Named
@RequestScoped
public class StartseiteBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private UrlParameters urlParameters;

	@Inject
	private ServiceFacade service;

	@Inject
	private Messages messages;

	@Inject
	private BenutzerEinstellungen benutzerEinstellungen;

	@Silent
	public String getMainContent() {

		// prüfe auf erste Anmeldung
		FacesContext ctx = FacesContext.getCurrentInstance();
		// da dies nach dem Logout erneut aufgerufen wird....
		String mainContent = null;
		if (ctx.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE && ctx.getExternalContext().getSession(false) != null) {
			Boolean firstLogin = (Boolean) ctx.getExternalContext().getSessionMap().get("firstLogin");
			if (firstLogin == null || firstLogin == true) {
				String userName = service.getUserName(ctx.getExternalContext().getRemoteUser());
				// der Username muss dann ein Space sein
				firstLogin = " ".equals(userName);
				ctx.getExternalContext().getSessionMap().put("firstLogin", firstLogin);
				if (firstLogin) {
					ctx.addMessage(null, new FacesMessage(null, messages.get("firstLogin")));
					mainContent = "/masterdata/myprofile.xhtml";
				}
			}
		}
		if (mainContent == null) {
			mainContent = (String) JsfUtil.getViewVariable("mainContent");
			if (mainContent == null) {
				urlParameters.init();
				mainContent = urlParameters.getMainContent();
			}
		}
		if (mainContent != null) {
			JsfUtil.setViewVariable("mainContent", mainContent);
			return mainContent;
		} else {
			return "/sections/start.xhtml";
		}
	}

	public void setMainContent(String mainContent) {
		setMainContent(mainContent, null);
	}

	public void setMainContent(String mainContent, Long id) {
		JsfUtil.setViewVariable("mainContent", mainContent);
		JsfUtil.setViewVariable("id", id);
		urlParameters.setMainContent(mainContent);
		urlParameters.setId(id);
		RequestContext.getCurrentInstance().execute("setUrlParam('" + urlParameters.getEncoded() + "')");
	}

	public void setMainContentAndReset(String mainContent) {
		setMainContent(mainContent);
		JsfUtil.setViewVariable("first", null);
		JsfUtil.setViewVariable("rows", null);
		JsfUtil.setViewVariable("filters", null);
		JsfUtil.setViewVariable("sortState", null);
	}

	public String leseBenutzerNamen(String userId) {
		return service.getUserName(userId);
	}

	@Silent
	public void poll() {
		// if (ausgewaehlteKonfiguration == null)
		// return;
		// KonfigsetEO konfig = adminService.leseEntity(KonfigsetEO.class,
		// ausgewaehlteKonfiguration.getId());
		// if
		// (konfig.getUpdateTime().isAfter(ausgewaehlteKonfiguration.getUpdateTime()))
		// {
		// FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
		// bundle.get("poll.title"), bundle.format("poll.msg", konfig.getName(),
		// konfig.getUpdateTime().toDate(), konfig.getUpdateUser())));
		// }
	}

	public String getTheme() {
		return benutzerEinstellungen.getValue("theme", "aristo");
	}

	public void setTheme(String theme) {
		// das ist ein Zugeständnis an das process="@this"-Attribut, das beim
		// Download nicht klappt
		if (StringUtils.isNotEmpty(theme)) {
			benutzerEinstellungen.setValue("theme", theme);
		}
	}

	public void saveLastLogonTime() {
		Object object = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("logonSaved");
		if (!Boolean.TRUE.equals(object)) {
			FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("logonSaved", Boolean.TRUE);
			service.saveLastLogonTime();
		}
	}

	public void setProfileSaved() {
		FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("firstLogin", Boolean.FALSE);
	}

	public String getHelp() throws IOException {
		String mainContent = getMainContent();
		String help = messages.bundle("help").getStringOrNull(mainContent);
		if (help == null) {
			Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
			InputStream in = getClass().getResourceAsStream("/help/" + locale.getLanguage() + "/" + mainContent);
			if (in != null) {
				try {
					help = IOUtils.toString(in, "UTF-8");
				} finally {
					in.close();
				}
			}
		}
		return help;
	}
}