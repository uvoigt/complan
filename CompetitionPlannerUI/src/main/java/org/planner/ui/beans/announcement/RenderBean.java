package org.planner.ui.beans.announcement;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.planner.eo.Placement;
import org.planner.eo.ProgramRace;
import org.planner.eo.Race;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.model.RaceType;
import org.planner.ui.beans.Messages;

@Named
@ApplicationScoped
public class RenderBean {

	@Inject
	private Messages messages;

	public String renderRaceTitle(ProgramRace race) {
		StringBuilder sb = new StringBuilder();
		createRaceTitle(race, sb);
		return sb.toString();
	}

	public String renderRaceNumber(ProgramRace race) {
		StringBuilder sb = new StringBuilder();
		if (race.getNumber() != null) {
			sb.append(race.getNumber());
			sb.append(". ");
		}
		sb.append(race.getRaceType().getText());
		return sb.toString();
	}

	public String renderRaceMode(ProgramRace race) {
		StringBuilder sb = new StringBuilder();
		if (race.getRaceType() == RaceType.heat || race.getRaceType() == RaceType.semiFinal) {
			int intoFinal = race.getIntoFinal();
			int intoSemiFinal = race.getIntoSemiFinal();
			if (intoFinal > 0)
				sb.append(intoFinal > 1 ? "1. - " : "").append(intoFinal).append(". in den Endlauf"); // TODO bundle
			if (intoSemiFinal > 0) {
				if (sb.length() > 0)
					sb.append(" ");
				if (intoFinal == 0)
					sb.append("1. - ");
				else if (intoSemiFinal > intoFinal + 1)
					sb.append(intoFinal + 1).append(". - ");
				sb.append(intoSemiFinal).append(". in den Zwischenlauf"); // TODO
			}
		}
		return sb.toString();
	}

	public String renderAgeGroup(User user) {
		if (user == null || user.getAgeType().ordinal() >= AgeType.junioren.ordinal())
			return null;
		return new StringBuilder().append("(").append(user.getAge()).append(")").toString();
	}

	public String renderStartTime(Date date) {
		return new SimpleDateFormat("EEEE").format(date) + " "
				+ DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
	}

	public String renderFollowUpHint(ProgramRace race) {
		switch (race.getRaceType()) {
		default:
			return "";
		case heat:
			return messages.format("programs.semiFinalHint", renderStartTime(race.getFollowUpRace().getStartTime()));
		case semiFinal:
			return messages.format("programs.finalHint", renderStartTime(race.getFollowUpRace().getStartTime()));
		}
	}

	public String renderRaceText(Race race) {
		StringBuilder sb = new StringBuilder();
		createRaceText(race, sb);
		return sb.toString();
	}

	private void createRaceText(Race race, StringBuilder sb) {
		sb.append(race.getBoatClass().getText());
		sb.append(' ');
		sb.append(race.getGender().getAgeFriendlyText(race.getAgeType()));
		sb.append(' ');
		sb.append(race.getAgeType().getText());
		sb.append(' ');
		sb.append(race.getDistance());
		sb.append(" m");
	}

	private void createRaceTitle(ProgramRace race, StringBuilder sb) {
		sb.append(messages.get("programs.race"));
		sb.append(' ');
		sb.append(race.getRace().getNumber());
		if (race.getNumber() != null)
			sb.append('-').append(race.getNumber());
	}

	public boolean isSeparatorRendered(ProgramRace race, Placement placement, int nextIndex) {
		if (placement.getQualifiedFor() != null) {
			Placement next = nextIndex < race.getPlacements().size() ? race.getPlacements().get(nextIndex) : null;
			if (next == null || next.getQualifiedFor() != placement.getQualifiedFor())
				return true;
		}
		return false;
	}

	public Long computeDeficit(ProgramRace race, Placement placement) {
		if (placement.getTime() != null) {
			Placement first = race.getPlacements().get(0);
			return placement.getTime() - first.getTime();
		}
		return null;
	}

	private String getRaceFilter(ProgramRace race) {
		StringBuilder sb = new StringBuilder();
		createRaceTitle(race, sb);
		sb.append(' ');
		createRaceText(race.getRace(), sb);
		sb.append(' ');
		sb.append(renderStartTime(race.getStartTime()));
		return sb.toString();
	}

	private boolean filterRace(String columnValue, String[] filters, @SuppressWarnings("unused") Locale locale,
			boolean logicalAnd) {
		for (String value : filters) {
			boolean matches = StringUtils.containsIgnoreCase(columnValue, value.trim());
			if (matches) {
				if (!logicalAnd)
					return true;
			} else {
				if (logicalAnd)
					return false;
			}
		}
		return logicalAnd;
	}

	public void filterRaces(Iterator<ProgramRace> races, String filterValue, Locale locale, boolean logicalAnd) {
		// chips
		if (filterValue.startsWith("[") && filterValue.endsWith("]"))
			filterValue = filterValue.substring(1, filterValue.length() - 1);
		String[] filters = filterValue.split(",");

		while (races.hasNext()) {
			ProgramRace race = races.next();
			if (!filterRace(getRaceFilter(race), filters, locale, logicalAnd))
				races.remove();
		}
	}
}
