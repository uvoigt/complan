package org.planner.ui.util.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Named;

import org.planner.eo.Placement;
import org.planner.eo.ProgramRace;
import org.planner.eo.ProgramRaceTeam;
import org.planner.eo.Team;
import org.planner.model.ResultExtra;

@Named
public class PlacementConverter implements Converter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		return getPlacement(value);
	}

	public Placement getPlacement(String value) {
		if (value == null)
			return null;
		String[] split = value.split(";");
		ProgramRace programRace = new ProgramRace();
		Team team = null;
		ResultExtra extra = null;
		Long time = null;
		if (split.length > 3) {
			extra = ResultExtra.valueOf(split[3]);
		} else if (split.length > 2) { // speichert die Zeit nur, wenn kein Extra vorhanden ist
			time = parseTime(split[2]);
		}
		if (split.length > 1)
			team = new Team(Long.valueOf(split[1]));
		if (split.length > 0)
			programRace.setId(Long.valueOf(split[0]));
		return new Placement(new ProgramRaceTeam(programRace, team), time, extra);
	}

	private Long parseTime(String string) {
		// hh:mm:ss[.SSS]
		// mm:ss[.SSS]
		// ss[.SSS]
		long time = 0L;
		char[] separators = { '.', ':', ':', ':' };
		int[] factors = { 100, 1000, 60 * 1000, 60 * 60 * 1000 };
		for (int i = 0; i < factors.length; i++) {
			int index = string.lastIndexOf(separators[i]);
			if (i == 0 && index == -1)
				continue;
			String part = index != -1 ? string.substring(index + 1) : string;
			int factor = factors[i];
			if (i == 0)
				factor /= Math.pow(10, part.length() - 1);
			if (part.length() > 0)
				time += Long.parseLong(part) * factor;
			if (index == -1)
				break;
			string = string.substring(0, index);
		}
		return time;
	}

	private String formatTime(long time) {
		StringBuilder sb = new StringBuilder();
		char[] separators = { '.', ':', ':', ':' };
		int[] factors = { 1, 1000, 60 * 1000, 60 * 60 * 1000 };
		int[] lengths = { 3, 2, 2, 2 };
		for (int i = factors.length - 1; i >= 0; i--) {
			if (time >= factors[i]) {
				long part = time / factors[i];
				StringBuilder s = new StringBuilder(Long.toString(part));
				while (s.length() < lengths[i])
					s.insert(0, '0');
				if (i == 0) {
					while (s.charAt(s.length() - 1) == '0')
						s.setLength(s.length() - 1);
					if (sb.length() == 0)
						sb.append("0.");
				}
				sb.append(s);
				time -= part * factors[i];
				if (i > 0 && time > 0)
					sb.append(separators[i - 1]);
			}
		}
		return sb.toString();
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value instanceof Long)
			return formatTime((Long) value);
		return getPlacementAsString((Placement) value);
	}

	public String getPlacementAsString(Placement placement) {
		StringBuilder sb = new StringBuilder();
		sb.append(placement.getTeam().getProgramRace().getId());
		sb.append(";");
		sb.append(placement.getTeam().getTeamId());
		sb.append(";");
		if (placement.getTime() != null)
			sb.append(formatTime(placement.getTime()));
		if (placement.getExtra() != null) {
			sb.append(";");
			sb.append(placement.getExtra());
		}
		return sb.toString();
	}
}
