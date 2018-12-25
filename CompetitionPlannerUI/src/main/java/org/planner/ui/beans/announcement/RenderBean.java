package org.planner.ui.beans.announcement;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.planner.eo.ProgramRace;
import org.planner.eo.ProgramRace.RaceType;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.ui.beans.Messages;
import org.planner.ui.util.JsfUtil;

@Named
@ApplicationScoped
public class RenderBean {

	@Inject
	private Messages messages;

	public String renderRaceTitle(ProgramRace race) {
		StringBuilder sb = new StringBuilder();
		sb.append(messages.get("programs.race")); // TODO evtl. woanders hin
		sb.append(" ");
		sb.append(race.getRace().getNumber());
		if (race.getNumber() != null)
			sb.append("-").append(race.getNumber());
		return sb.toString();
	}

	public String renderRaceNumer(ProgramRace race) {
		StringBuilder sb = new StringBuilder();
		if (race.getNumber() != null) {
			sb.append(race.getNumber());
			sb.append(".");
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

	public String getRaceFilter(ProgramRace race) {
		StringBuilder sb = new StringBuilder();
		sb.append(JsfUtil.getScopedBundle().get("race")).append(" ");
		sb.append(race.getRace().getNumber());
		if (race.getNumber() != null)
			sb.append("-").append(race.getNumber());
		sb.append(" ").append(race.getRace().getBoatClass().getText()).append(" ");
		sb.append(race.getRace().getGender().getAgeFriendlyText(race.getRace().getAgeType())).append(" ");
		sb.append(race.getRace().getAgeType().getText()).append(" ");
		sb.append(race.getRace().getDistance()).append(" m ");
		sb.append(renderStartTime(race.getStartTime()));
		return sb.toString();
	}

	public boolean filterRaces(String columnValue, String filterValue, @SuppressWarnings("unused") Locale locale) {
		for (String value : filterValue.split(",")) {
			if (StringUtils.containsIgnoreCase(columnValue, value.trim()))
				return true;
		}
		return false;
	}
}
