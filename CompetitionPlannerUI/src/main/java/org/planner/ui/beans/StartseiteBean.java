package org.planner.ui.beans;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.planner.remote.ServiceFacade;
import org.planner.util.Logged;
import org.planner.util.LoggingInterceptor.Silent;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.MenuModel;

@Logged
@Named
@SessionScoped
public class StartseiteBean implements Serializable {
	// private static final Log LOG =
	// LogFactory.getLog(StartseiteVerwaltenBean.class);

	private static final long serialVersionUID = 1L;

	private String mainContent = "/sections/start.xhtml";

	private String redirectTarget;

	private MenuModel menu;

	@Inject
	private ServiceFacade service;

	@Inject
	private Messages messages;

	@Inject
	private BenutzerEinstellungen benutzerEinstellungen;

	@Silent
	public String getMainContent() {
		return mainContent;
	}

	public void setMainContent(String mainContent) {
		setMainContent(mainContent, false);
	}

	public void setMainContent(String mainContent, boolean redirect) {
		if (redirect) {
			this.mainContent = "/sections/redirect.xhtml";
			redirectTarget = mainContent;
		} else {
			this.mainContent = mainContent;
		}
	}

	public void setMainContent(String mainContent, String paramName, Object paramValue) {
		setMainContent(mainContent, true);
		FacesContext instance = FacesContext.getCurrentInstance();
		// UIViewRoot viewRoot = instance.getViewRoot();
		Map<String, Object> map = instance.getExternalContext().getSessionMap();
		map.put(paramName, paramValue);
		// ValueExpression ex =
		// instance.getApplication().getExpressionFactory().createValueExpression(paramValue,
		// Object.class);
		// viewRoot.setValueExpression(paramName, ex);
		// JsfUtil.setContextVariable(paramName, paramValue);
	}

	public String getRedirectTarget() {
		return redirectTarget;
	}

	public MenuModel getMenu() {
		if (menu != null)
			return menu;
		menu = new DefaultMenuModel();
		// if (schema == null)
		// return menu;
		// DefaultSubMenu dynamicMenu = new
		// DefaultSubMenu(bundle.get("menu.klasse"));
		// menu.addElement(dynamicMenu);
		// int index = 0;
		// for (SchemaTyp typ : schema.getSchemaTypes()) {
		// if (!typ.isRootType())
		// continue;
		// String label = typ.getLabel();
		// DefaultMenuItem item = new DefaultMenuItem(label != null ? label :
		// typ.getName());
		// item.setId("0_" + index++);
		// item.setCommand("#{startseiteVerwaltenBean.setMainContent('/mandanten/suchen.xhtml',
		// 'schemaName', '" + typ.getName() + "')}");
		// item.setProcess("@this");
		// item.setOnclick("selectSidebarLink(this)");
		// if (mainContent != null &&
		// mainContent.contains("/mandanten/suchen.xhtml")
		// &&
		// typ.getName().equals(FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("schemaName")))
		// {
		// item.setStyleClass("ui-state-active");
		// }
		// item.setDisabled(ausgewaehlteKonfiguration == null);
		// item.setUpdate(":mainContent :messages");
		// dynamicMenu.addElement(item);
		// }
		return menu;
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

	public void logout() {
		FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
	}

	public String getHelp() throws IOException {
		String help = messages.bundle("help").getStringOrNull(mainContent);
		if (help == null) {
			Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
			InputStream in = getClass().getResourceAsStream("/help/" + locale.getLanguage() + "/" + mainContent);
			if (in != null) {
				try {
					help = IOUtils.toString(in, "UTF8");
				} finally {
					in.close();
				}
			}
		}
		return help;
	}
}