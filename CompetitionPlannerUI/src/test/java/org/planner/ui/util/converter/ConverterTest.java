package org.planner.ui.util.converter;

import org.junit.Test;
import org.planner.eo.Placement;
import org.planner.eo.ProgramRace;
import org.planner.eo.ProgramRaceTeam;
import org.planner.eo.Team;

import junit.framework.Assert;

public class ConverterTest {

	@Test
	public void parseResult() {
		PlacementConverter c = new PlacementConverter();
		Assert.assertEquals(39 * 1000, parseTime(c, "39"));
		Assert.assertEquals(39 * 1000, parseTime(c, "39."));
		Assert.assertEquals(39 * 1000, parseTime(c, "39.000"));
		Assert.assertEquals(39 * 1000 + 900, parseTime(c, "39.9"));
		Assert.assertEquals(41 * 1000 + 40, parseTime(c, "41.04"));
		Assert.assertEquals(41 * 1000 + 3, parseTime(c, "41.003"));
		Assert.assertEquals(43 * 1000 + 220, parseTime(c, "43.22"));
		Assert.assertEquals(52 * 1000 + 505, parseTime(c, "52.505"));
		Assert.assertEquals(55 * 1000 + 555, parseTime(c, "55.555"));
		Assert.assertEquals(59 * 1000 + 999, parseTime(c, "59.999"));
		Assert.assertEquals(61 * 1000 + 100, parseTime(c, "61.1"));

		Assert.assertEquals(12 * 60 * 1000 + 55 * 1000, parseTime(c, "12:55"));
		Assert.assertEquals(12 * 60 * 1000 + 59 * 1000 + 20, parseTime(c, "12:59.02"));
	}

	private long parseTime(PlacementConverter c, String s) {
		return c.getPlacement("0;1;" + s).getTime().longValue();
	}

	@Test
	public void formatResult() {
		PlacementConverter c = new PlacementConverter();
		Assert.assertEquals("39.7", formatTime(c, 39700));
		Assert.assertEquals("0.7", formatTime(c, 700));
		Assert.assertEquals("39.765", formatTime(c, 39765));
		Assert.assertEquals("39", formatTime(c, 39000));
		Assert.assertEquals("01:06.7", formatTime(c, 66700));
	}

	private String formatTime(PlacementConverter c, long time) {
		Placement placement = new Placement(new ProgramRaceTeam(new ProgramRace(), new Team()), time, null);
		String string = c.getPlacementAsString(placement);
		Assert.assertNotNull(string);
		String[] split = string.split(";");
		Assert.assertEquals(3, split.length);
		return split[2];
	}
}
