package org.planner.ui.beans;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.Manifest;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.planner.eo.User;
import org.planner.remote.ServiceFacade;
import org.planner.ui.beans.common.AuthBean;
import org.planner.ui.util.JsfUtil;
import org.planner.util.LoggingInterceptor.Silent;
import org.primefaces.PrimeFaces;

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

	@Inject
	private AuthBean auth;

	private boolean identityWritten;

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
			if (mainContent == null)
				mainContent = urlParameters.getMainContent();
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

		if (Boolean.TRUE.equals(JsfUtil.getViewVariable("helpVisible")))
			FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("help");
		String queryString = "/sections/start.xhtml".equals(mainContent) ? "" : urlParameters.getEncoded();

		PrimeFaces.current().executeScript("setUrlParam('" + queryString + "')");
	}

	public void setMainContentAndReset(String mainContent) {
		setMainContent(mainContent);
		JsfUtil.setViewVariable("first", null);
		JsfUtil.setViewVariable("rows", null);
		JsfUtil.setViewVariable("filters", null);
		JsfUtil.setViewVariable("sortState", null);
		JsfUtil.setViewVariable("selectedItem", null);
	}

	public void setHelpVisible(boolean visible) {
		JsfUtil.setViewVariable("helpVisible", visible);
	}

	public String leseBenutzerNamen(String userId) {
		if (!identityWritten) {
			identityWritten = true;
			PrimeFaces.current().executeScript("PrimeFaces.identity='" + getIdentity() + "'");
		}
		return service.getUserName(userId);
	}

	private String getIdentity() {
		User user = auth.getLoggedInUser();
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-512");
			byte[] digest = sha
					.digest((user.getId().toString() + Long.toString(user.getCreateTime().getTime())).getBytes("UTF8"));
			return DatatypeConverter.printHexBinary(digest);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
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
				// entferne die komplette erste Zeile mit dem Meta-Element
				if (help.startsWith("<meta"))
					help = help.substring(help.indexOf("\r\n") + 2);
				help = substitute(help, "Implementation-Version", "Build-Timestamp");
			}
		}
		return help;
	}

	private String substitute(String text, String... attributes) {
		Manifest manifest = null;
		for (String attribute : attributes) {
			String pattern = "${" + attribute + "}";
			int index = text.indexOf(pattern);
			if (index == -1)
				continue;
			if (manifest == null) {
				try {
					manifest = getManifest("Implementation-Vendor", "Uwe Voigt");
				} catch (Exception e) {
				}
				if (manifest == null)
					return text;
			}
			String replacement = manifest.getMainAttributes().getValue(attribute);
			if (replacement == null)
				continue;
			text = text.substring(0, index) + replacement + text.substring(index + pattern.length());
		}
		return text;
	}

	private Manifest getManifest(String name, String value) throws IOException {
		Manifest manifest = lookupManifest(getClass().getClassLoader().getResources("META-INF/MANIFEST.MF"), name, //$NON-NLS-1$
				value);
		if (manifest != null)
			return manifest;
		manifest = lookupManifest(Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF"), //$NON-NLS-1$
				name, value);
		if (manifest != null)
			return manifest;
		return lookupManifest(ClassLoader.getSystemResources("META-INF/MANIFEST.MF"), name, value); //$NON-NLS-1$
	}

	private static Manifest lookupManifest(Enumeration<URL> en, String attributeName, String value) throws IOException {
		while (en.hasMoreElements()) {
			Manifest manifest = new Manifest(en.nextElement().openStream());
			String attributeValue = manifest.getMainAttributes().getValue(attributeName);
			if (value.equals(attributeValue))
				return manifest;
		}
		return null;
	}
}