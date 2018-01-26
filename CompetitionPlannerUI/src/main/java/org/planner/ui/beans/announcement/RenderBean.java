package org.planner.ui.beans.announcement;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.planner.eo.ProgramRace;
import org.planner.eo.ProgramRace.RaceType;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.ui.beans.Messages;

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
		String s = "";
		if (race.getRaceType() == RaceType.heat) {
			int intoFinal = race.getIntoFinal();
			int intoSemiFinal = race.getIntoSemiFinal();
			if (intoFinal > 0)
				s = (intoFinal > 1 ? "1. - " : "") + intoFinal + ". in den Endlauf"; // TODO
			if (intoSemiFinal > 0) {
				if (s.length() > 0)
					s += " ";
				if (intoFinal == 0)
					s += "1. - ";
				else if (intoSemiFinal > intoFinal + 1)
					s += (intoFinal + 1) + ". - ";
				s += intoSemiFinal + ". in den Zwischenlauf"; // TODO
			}
		}
		return s;
	}

	public String renderAgeGroup(User user) {
		if (user == null || user.getAgeType().ordinal() >= AgeType.junioren.ordinal())
			return null;
		return new StringBuilder().append("(").append(user.getAge()).append(")").toString();
	}

	public String renderStartTime(Date date) {
		return new SimpleDateFormat("EEEE").format(date) + " " + DateFormat.getTimeInstance().format(date);
	}

}
