package org.planner.ui.beans.announcement;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.planner.eo.Placement;
import org.planner.eo.ProgramRace;
import org.planner.eo.ProgramRaceTeam;
import org.planner.eo.Race;
import org.planner.eo.TeamMember;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.model.RaceType;
import org.planner.ui.beans.Messages;

@Named
@ApplicationScoped
public class RenderBean {

	@Inject
	private Messages messages;

	private DateFormat yearFormat = new SimpleDateFormat("EEEE");

	public String renderRaceTitle(ProgramRace race) {
		StringBuilder sb = new StringBuilder();
		createRaceTitle(race, sb);
		return sb.toString();
	}

	public String renderRaceNumber(ProgramRace race) {
		StringBuilder sb = new StringBuilder();
		createRaceNumberText(race, sb);
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
		StringBuilder sb = new StringBuilder();
		createStartTimeText(date, sb);
		return sb.toString();
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

	private void createStartTimeText(Date date, StringBuilder sb) {
		sb.append(yearFormat.format(date));
		sb.append(' ');
		sb.append(DateFormat.getTimeInstance(DateFormat.SHORT).format(date));
	}

	private void createRaceNumberText(ProgramRace race, StringBuilder sb) {
		if (race.getNumber() != null) {
			sb.append(race.getNumber());
			sb.append(". ");
		}
		sb.append(race.getRaceType().getText());
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

	private boolean compareRace(ProgramRace race, String filter) {
		StringBuilder sb = new StringBuilder();
		createRaceTitle(race, sb);
		if (StringUtils.containsIgnoreCase(sb, filter))
			return true;
		sb.setLength(0);
		createRaceText(race.getRace(), sb);
		if (StringUtils.containsIgnoreCase(sb, filter))
			return true;
		sb.setLength(0);
		createStartTimeText(race.getStartTime(), sb);
		if (StringUtils.containsIgnoreCase(sb, filter))
			return true;
		sb.setLength(0);
		createRaceNumberText(race, sb);
		if (StringUtils.containsIgnoreCase(sb, filter))
			return true;
		if (race.getParticipants() != null) {
			for (ProgramRaceTeam team : race.getParticipants()) {
				for (TeamMember member : team.getMembers()) {
					if (member.getUser() != null) {
						if (StringUtils.containsIgnoreCase(member.getUser().getFirstName(), filter))
							return true;
						if (StringUtils.containsIgnoreCase(member.getUser().getLastName(), filter))
							return true;
						if (StringUtils.containsIgnoreCase(member.getUser().getName(), filter))
							return true;
					}
				}
			}
		}
		return false;
	}

	private boolean filterRace(ProgramRace race, List<String> filters, @SuppressWarnings("unused") Locale locale,
			boolean logicalAnd) {
		for (String filter : filters) {
			boolean matches = compareRace(race, filter.trim());
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

	public void filterRaces(Iterator<ProgramRace> races, List<String> filters, Locale locale, boolean logicalAnd) {
		while (races.hasNext()) {
			ProgramRace race = races.next();
			if (!filterRace(race, filters, locale, logicalAnd))
				races.remove();
		}
	}
}
