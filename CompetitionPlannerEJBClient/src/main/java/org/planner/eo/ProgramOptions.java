package org.planner.eo;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class ProgramOptions implements Serializable {

	private static final long serialVersionUID = 1L;

	// Schutzzeiten
	private boolean childProtection;
	private Integer racesPerDay;
	// Minuten
	private Integer protectionPeriod;

	// ;-separierter String
	// <Platzierungen ins Finale>;<Platzierungen in Zwischenlauf>
	// Bsp.: 1;2 bedeutet Platz 1 ins Finale, Platz 2 - 3 ins Semifinale
	// Bsp.: 2;2 bedeutet Platz 1 - 2 ins Finale, Platz 3 - 4 ins Semifinale
	private String heatMode;

	public boolean isChildProtection() {
		return childProtection;
	}

	public void setChildProtection(boolean childProtection) {
		this.childProtection = childProtection;
	}

	public Integer getRacesPerDay() {
		return racesPerDay;
	}

	public void setRacesPerDay(Integer racesPerDay) {
		this.racesPerDay = racesPerDay;
	}

	public Integer getProtectionPeriod() {
		return protectionPeriod;
	}

	public void setProtectionPeriod(Integer protectionPeriod) {
		this.protectionPeriod = protectionPeriod;
	}

	public String getHeatMode() {
		return heatMode;
	}

	public void setHeatMode(String heatMode) {
		this.heatMode = heatMode;
	}
}
