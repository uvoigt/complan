package org.planner.business;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.LazyInitializationException;
import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;
import org.planner.business.program.ListView;
import org.planner.business.program.SwapChange;
import org.planner.eo.City;
import org.planner.eo.Properties;
import org.planner.eo.Role_;
import org.planner.eo.User;
import org.planner.eo.User_;
import org.planner.model.AgeType;
import org.planner.model.FetchInfo;

public class CommonTest extends BaseTest {

	@Test
	@InSequence(1)
	public void speichernBenutzerEinstellungen() {
		User callingUser = getCallingUser();
		List<Properties> properties = new ArrayList<>();
		addProperty(properties, "key1", 25);
		addProperty(properties, "key2", "value");
		addProperty(properties, "key3", AgeType.akB);
		addProperty(properties, "key4", new Integer[] { 1, 2, 3 });
		addProperty(properties, "key5", 45.6D);
		Map<String, Properties> result = common.speichernBenutzerEinstellungen(properties, callingUser.getUserId());
		assertProperties(result);
	}

	private void addProperty(List<Properties> properties, String key, Object value) {
		Properties p = new Properties();
		p.setName(key);
		p.setValue(value);
		properties.add(p);
	}

	private void assertProperties(Map<String, Properties> result) {
		assertNotNull(result);
		assertEquals(5, result.size());
		assertNotNull(result.get("key1"));
		assertEquals(Integer.valueOf(25), result.get("key1").getTypedValue(Integer.class));
		assertNotNull(result.get("key2"));
		assertEquals("value", result.get("key2").getValue());
		assertNotNull(result.get("key3"));
		assertEquals(AgeType.akB.toString(), result.get("key3").getValue());
		assertNotNull(result.get("key4"));
		assertArrayEquals(new Integer[] { 1, 2, 3 }, result.get("key4").getTypedValue(Integer[].class));
		assertNotNull(result.get("key5"));
		assertEquals(Double.valueOf(45.6), result.get("key5").getTypedValue(Double.class));
	}

	@Test
	@InSequence(2)
	public void leseEinstellungen() {
		User callingUser = getCallingUser();
		Map<String, Properties> result = common.leseBenutzerEinstellungen(callingUser.getUserId());
		assertProperties(result);
	}

	@Test
	public void handleNonExistingEnum() {
		common.handleEnum(null);
	}

	@Test
	@InSequence(1)
	public void handleEnum() {
		City city = new City();
		city.setName("Leipzig");
		common.handleEnum(city);
	}

	@Test
	@InSequence(2)
	public void getEnumByName() {
		City city = common.getEnumByName("Leipzig", City.class);
		assertNotNull(city);
		assertEquals("Leipzig", city.getName());

		city = common.getEnumByName("Berlin", City.class);
		assertNull(city);
	}

	@Test
	public void deleteNonExisting() {
		common.delete(User.class, Long.MAX_VALUE);
	}

	@Test(expected = LazyInitializationException.class)
	public void failPropertyAfterGetByIdWithoutFetch() {
		User user = getCallingUser();
		flushAndClear();
		User result = common.getById(User.class, user.getId());
		flushAndClear();
		assertNotNull(result);
		result.getClub().getName();
	}

	@Test
	public void getByIdWithJoinFetch() {
		User user = getCallingUser();
		User result = common.getById(User.class, user.getId(),
				new FetchInfo(User_.roles, true).add(new FetchInfo(Role_.roles, true)),
				new FetchInfo(User_.club, true));
		flushAndClear();
		assertNotNull(result);
	}

	@Test
	public void getByIdWithFetch() {
		User user = getCallingUser();
		User result = common.getById(User.class, user.getId(),
				new FetchInfo(User_.roles, false).add(new FetchInfo(Role_.roles, false)),
				new FetchInfo(User_.club, false));
		flushAndClear();
		assertNotNull(result);
	}

	@Test
	public void listView() {
		List<String> l = Arrays.asList("1", "2", "3", "4", "5");
		ListView<String> t = new ListView<>(l);
		assertEquals("[1, 2, 3, 4, 5]", t.toString());
		new SwapChange(3, 4, null).applyTo(t);
		assertEquals("[1, 2, 3, 5, 4]", t.toString());
		new SwapChange(0, 2, null).applyTo(t);
		assertEquals("[3, 2, 1, 5, 4]", t.toString());
	}
}
