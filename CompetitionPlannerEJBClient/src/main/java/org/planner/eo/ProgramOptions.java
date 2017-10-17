package org.planner.eo;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Embeddable
public class ProgramOptions extends HasHeatMode implements Serializable {

	private static final long serialVersionUID = 1L;

	// die Zeiten, an denen am jeweiligen Tag die Rennen beginnen
	// das ist eine Komma-separierte Liste von Times HH:mm
	private String beginTimes;

	// Schutzzeiten
	private boolean childProtection;
	private Integer racesPerDay;
	// Minuten
	private Integer protectionPeriod;

	// zeitlicher Abstand zwischen den Startzeiten in Minuten
	private int timeLag;

	@Temporal(TemporalType.TIME)
	private Date launchBreak;

	// in Minuten
	private int breakDuration;

	public Date[] getBeginTimes() {
		if (beginTimes == null)
			return null;
		String[] split = beginTimes.split(",");
		Date[] times = new Date[split.length];
		DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
		for (int i = 0; i < split.length; i++) {
			try {
				times[i] = new Date(dateFormat.parse(split[i]).getTime());
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return times;
	}

	public void setBeginTimes(Date[] beginTimes) {
		if (beginTimes == null || beginTimes.length == 0) {
			this.beginTimes = null;
		} else {
			StringBuilder sb = new StringBuilder();
			DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
			for (Date time : beginTimes) {
				if (sb.length() > 0)
					sb.append(",");
				sb.append(dateFormat.format(time));
			}
			this.beginTimes = sb.toString();
		}
	}

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

	public int getTimeLag() {
		return timeLag;
	}

	public void setTimeLag(int timeLag) {
		this.timeLag = timeLag;
	}

	public Date getLaunchBreak() {
		return launchBreak;
	}

	public void setLaunchBreak(Date launchBreak) {
		this.launchBreak = launchBreak;
	}

	public int getBreakDuration() {
		return breakDuration;
	}

	public void setBreakDuration(int breakDuration) {
		this.breakDuration = breakDuration;
	}
}
