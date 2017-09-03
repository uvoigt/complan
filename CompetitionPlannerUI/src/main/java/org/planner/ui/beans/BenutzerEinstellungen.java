package org.planner.ui.beans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.ObjectUtils;
import org.planner.eo.Properties;
import org.planner.remote.ServiceFacade;

/**
 * Ermöglicht den Zugriff auf User-Einstellungen in der DB.
 * 
 * @author Uwe Voigt - IBM
 */
@Named
@SessionScoped
public class BenutzerEinstellungen implements Serializable {

	private static final long serialVersionUID = 1L;

	private Map<String, Properties> benutzerEinstellungen;

	@Inject
	private ServiceFacade service;

	@PostConstruct
	public void init() {
		benutzerEinstellungen = service.leseBenutzerEinstellungen();
	}

	public Properties getProp(String name) {
		Properties eo = benutzerEinstellungen.get(name);
		if (eo == null) {
			eo = new Properties();
			eo.setName(name);
			benutzerEinstellungen.put(name, eo);
		}
		return eo;
	}

	public String getValue(String name) {
		return getProp(name).getValue();
	}

	public String getValue(String name, String defaultValue) {
		String value = getValue(name);
		return value != null ? value : defaultValue;
	}

	public <T> T getTypedValue(String name, Class<T> type) {
		return getProp(name).getTypedValue(type);
	}

	public <T> T getTypedValue(String name, Class<T> type, T defaultValue) {
		T value = getTypedValue(name, type);
		return value != null ? value : defaultValue;
	}

	public void setValue(String name, String value) {
		Properties eo = getProp(name);
		String old = eo.getValue();
		eo.setValue(value);
		if (ObjectUtils.notEqual(old, value)) {
			benutzerEinstellungen.clear();
			benutzerEinstellungen.putAll(service.speichernBenutzerEinstellungen(Arrays.asList(eo)));
		}
	}

	public void setValue(String name, Object value) {
		Properties eo = getProp(name);
		Object old = eo.getTypedValue(value.getClass());
		eo.setValue(value);
		if (ObjectUtils.notEqual(old, value)) {
			benutzerEinstellungen.clear();
			benutzerEinstellungen.putAll(service.speichernBenutzerEinstellungen(Arrays.asList(eo)));
		}
	}
}
