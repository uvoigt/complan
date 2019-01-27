package org.planner.eo;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Lob;
import javax.persistence.Transient;

@Embeddable
public class ProgramOptions implements Serializable {
	public static class DayTimes implements Serializable {
		private static final long serialVersionUID = 1L;

		// <HH:mm>,[<HH:mm>;<mm>,]*[<HH:mm>]
		private static DayTimes create(String s) throws ParseException {
			if (s == null || s.length() == 0)
				return null;
			String[] parts = s.split(",");
			DayTimes dayTimes = new DayTimes(DF.parse(parts[0]));
			if (parts.length > 1) {
				for (int i = 1; i < parts.length; i++) {
					String part = parts[i];
					Break b = Break.create(part);
					if (b != null) {
						if (dayTimes.breaks == null)
							dayTimes.breaks = new ArrayList<>();
						dayTimes.breaks.add(b);
					} else {
						dayTimes.end = DF.parse(part);
					}
				}
			}
			return dayTimes;
		}

		public void toString(StringBuilder sb) {
			sb.append(DF.format(start));
			if (breaks != null) {
				for (Break b : breaks) {
					sb.append(",");
					b.toString(sb);
				}
			}
			if (end != null) {
				sb.append(",");
				sb.append(DF.format(end));
			}
		}

		private Date start;
		private Date end;
		private List<Break> breaks;

		public DayTimes(Date start) {
			this.start = start;
		}

		public DayTimes(Date start, Date end) {
			this(start);
			this.end = end;
		}

		public void addBreak(Date time, int duration) {
			if (breaks == null)
				breaks = new ArrayList<>();
			breaks.add(new Break(time, duration));
		}

		public Date getStart() {
			return start;
		}

		public void setStart(Date start) {
			this.start = start;
		}

		public Date getEnd() {
			return end;
		}

		public void setEnd(Date end) {
			this.end = end;
		}

		public List<Break> getBreaks() {
			return breaks;
		}
	}

	public static class Break implements Serializable {
		private static final long serialVersionUID = 1L;

		// [<HH:mm>;<mm>,]*
		private static Break create(String s) throws NumberFormatException, ParseException {
			if (s == null || !s.contains(";"))
				return null;
			String[] parts = s.split(";");
			return new Break(DF.parse(parts[0]), Integer.parseInt(parts[1]));
		}

		public void toString(StringBuilder sb) {
			sb.append(DF.format(time));
			sb.append(";");
			sb.append(duration);
		}

		private Date time;
		private int duration;

		public Break(Date time, int duration) {
			this.time = time;
			this.duration = duration;
		}

		public Date getTime() {
			return time;
		}

		public void setTime(Date time) {
			this.time = time;
		}

		public int getDuration() {
			return duration;
		}

		public void setDuration(int duration) {
			this.duration = duration;
		}
	}

	private static final long serialVersionUID = 1L;

	private static final DateFormat DF = DateFormat.getTimeInstance(DateFormat.SHORT);

	@Access(AccessType.PROPERTY)
	private String times;
	@Transient
	private List<DayTimes> dayTimes;

	// Schutzzeiten
	private boolean childProtection;
	private Integer racesPerDay;
	// Minuten
	private Integer protectionPeriod;

	// zeitlicher Abstand zwischen den Startzeiten in Minuten
	private int timeLag;

	@Lob
	private String expr;

	public List<DayTimes> getDayTimes() {
		if (dayTimes == null)
			parseTimes(times);
		return dayTimes;
	}

	public void setDayTimes(List<DayTimes> dayTimes) {
		this.dayTimes = dayTimes;
	}

	public String getTimes() {
		if (dayTimes != null)
			formatTimes();
		return times;
	}

	public void setTimes(String times) {
		this.times = times;
	}

	// [<HH:mm>,[<HH:mm>;<mm>,]*[<HH:mm>]]~*
	private void parseTimes(String s) {
		if (s == null)
			return;
		dayTimes = new ArrayList<>();
		for (String part : s.split("~")) {
			try {
				DayTimes dayTime = DayTimes.create(part);
				if (dayTime != null)
					dayTimes.add(dayTime);
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	private void formatTimes() {
		if (dayTimes != null) {
			StringBuilder sb = new StringBuilder();
			for (DayTimes dayTime : dayTimes) {
				if (sb.length() > 0)
					sb.append("~");
				dayTime.toString(sb);
			}
			times = sb.toString();
		}
		dayTimes = null;
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

	public String getExpr() {
		return expr;
	}

	public void setExpr(String expr) {
		this.expr = expr;
	}
}
