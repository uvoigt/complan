package org.planner.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;
import org.planner.eo.Address;
import org.planner.eo.City;
import org.planner.eo.Club;
import org.planner.eo.Country;
import org.planner.eo.Role;
import org.planner.eo.User;
import org.planner.eo.User_;
import org.planner.util.LogUtil.FachlicheException;
import org.planner.util.LogUtil.TechnischeException;

public class MasterDataTest extends BaseTest {

	@Inject
	private MasterDataServiceImpl masterData;

	@Test
	public void failSaveUserMissingProperty() {
		thrown.expect(FachlicheException.class);
		thrown.expectMessage(
				messages.getFormattedMessage("attributeMandatory", User_.userId.getName(), User.class.getSimpleName()));

		User user = new User();
		user.setFirstName("Test");
		user.setLastName("Test");
		setTestRoles("Admin");
		user = masterData.saveUser(user);
	}

	@Test
	@InSequence(1)
	public void saveUserAsAdmin() {
		User user = new User();
		user.setFirstName("Test");
		user.setLastName("Test");
		user.setUserId("test20");
		user.getClub().setName("Testclub");
		setTestRoles("Admin");
		user = masterData.saveUser(user);
	}

	@Test
	@InSequence(2)
	public void failSaveUserAsSportwartWrongClub() {
		thrown.expect(FachlicheException.class);
		thrown.expectMessage(messages.getFormattedMessage("user.cannot", messages.getMessage("operation.save")));
		User user = new User();
		user.setFirstName("Test");
		user.setLastName("Test");
		user.setUserId("test30");
		user.getClub().setName("Testclub");
		setTestRoles("Sportwart");
		user = masterData.saveUser(user);
	}

	@Test
	@InSequence(2)
	public void saveUserWithPasswordAsAdmin() {
		User user = TestUtils.getUserByUserId(getEm(), "test20", "Testclub");
		user.setPassword("hallobello");
		setTestRoles("Admin");
		user = masterData.saveUser(user);
	}

	@Test
	@InSequence(2)
	public void saveUserWithoutPasswordAsAdmin() {
		User user = TestUtils.getUserByUserId(getEm(), "test20", "Testclub");
		setTestRoles("Admin");
		user = masterData.saveUser(user);
	}

	@Test
	public void saveUserWithPasswordFromProfile() {
		User user = getCallingUser();
		user.setPassword("hallobello");
		setTestRoles("Sportler");
		user = masterData.saveUser(user);
	}

	@Test
	public void saveUserAsSportwart() {
		User user = new User();
		user.setFirstName("Test");
		user.setLastName("Test");
		user.setUserId("test40");
		user.setClub(getCallingUser().getClub());
		setTestRoles("Sportwart");
		user = masterData.saveUser(user);
	}

	@Test(expected = FachlicheException.class)
	public void failSaveUserAsSportlerWrongUserId() {
		User user = new User();
		user.setFirstName("Test");
		user.setLastName("Test");
		user.setUserId("test50");
		user.setClub(getCallingUser().getClub());
		setTestRoles("Sportler");
		user = masterData.saveUser(user);
	}

	@Test(expected = FachlicheException.class)
	public void failSaveUserAsSportlerCreationNotAllowed() {
		User caller = getCallingUser();
		User user = new User();
		user.setFirstName("Test");
		user.setLastName("Test");
		user.setUserId(caller.getUserId());
		user.setClub(caller.getClub());
		flushAndClear();
		setTestRoles("Sportler");
		user = masterData.saveUser(user);
	}

	@Test(expected = FachlicheException.class)
	public void failSaveUserAsSportlerWrongClub() {
		User caller = getCallingUser();
		User user = new User();
		user.setFirstName("Test");
		user.setLastName("Test");
		user.setUserId(caller.getUserId());
		user.setId(caller.getId());
		flushAndClear();
		setTestRoles("Sportler");
		user = masterData.saveUser(user);
	}

	@Test
	public void saveUserAsSportler() {
		User caller = getCallingUser();
		User user = new User();
		user.setFirstName("Modified");
		user.setLastName("Modified");
		user.setUserId(caller.getUserId());
		user.setId(caller.getId());
		user.setVersion(caller.getVersion());
		user.setClub(caller.getClub());
		setTestRoles("Sportler");
		flushAndClear();
		user = masterData.saveUser(user);
		flushAndClear();
		User saved = common.getById(User.class, user.getId());
		assertEquals(user.getFirstName(), saved.getFirstName());
		assertEquals(user.getLastName(), saved.getLastName());
		assertEquals(user.getUserId(), saved.getUserId());
	}

	@Test
	public void getUserName() {
		User caller = getCallingUser();
		String userName = masterData.getUserName(caller.getUserId());
		assertEquals(caller.getFirstName() + " " + caller.getLastName(), userName);
	}

	@Test
	public void getUserNameNull1() {
		String userName = masterData.getUserName(null);
		assertNull(userName);
	}

	@Test
	public void getUserNameNull2() {
		String userName = masterData.getUserName("Rudi");
		assertEquals("Rudi", userName);
	}

	@Test
	public void saveLastLogonTime() {
		User caller = getCallingUser();
		Date date = new Date();
		setNow(date);
		masterData.saveLastLogonTime(caller.getUserId());
		flushAndClear();
		User user = common.getById(User.class, caller.getId());
		assertEquals(date, user.getLastLogon());
	}

	@Test(expected = FachlicheException.class)
	public void failCreateClubAsSportler() {
		Club club = new Club();
		setTestRoles("Sportler");
		masterData.saveClub(club);
	}

	@Test(expected = FachlicheException.class)
	public void failSaveClubAsSportlerNotAllowed() {
		Club club = new Club();
		club.setId(TestUtils.getClub(getEm(), TestUtils.random(0), true).getId());
		setTestRoles("Sportler");
		masterData.saveClub(club);
	}

	@Test
	public void failCreateClubAsSportwartClubExists() {
		String clubName = TestUtils.random(0);
		thrown.expect(FachlicheException.class);
		thrown.expectMessage(messages.getFormattedMessage("club.exists", clubName));
		User callingUser = getCallingUser();
		callingUser.setFirstName("");
		callingUser.setLastName("");
		Club c = TestUtils.getClub(getEm(), clubName, true);
		Club club = new Club();
		club.setName(c.getName());
		setTestRoles("Sportwart");
		masterData.saveClub(club);
	}

	@Test
	public void saveClubAsSportwart() {
		User callingUser = getCallingUser();
		Club club = new Club();
		club.setName(TestUtils.random(0));
		club.setId(callingUser.getClub().getId());
		setTestRoles("Sportwart");
		masterData.saveClub(club);
	}

	@Test
	public void saveClubWithEmptyAddress() {
		User callingUser = getCallingUser();
		Club club = new Club();
		club.setName(TestUtils.random(0));
		club.setId(callingUser.getClub().getId());
		club.setVersion(callingUser.getClub().getVersion());
		club.setAddress(new Address());
		setTestRoles("Sportwart");
		masterData.saveClub(club);
	}

	@Test
	public void saveClubAndAddress() {
		User callingUser = getCallingUser();
		Club club = new Club();
		club.setName(TestUtils.random(0));
		club.setId(callingUser.getClub().getId());
		club.setVersion(callingUser.getClub().getVersion());
		Address address = new Address();
		Country country = new Country();
		country.setName("Mein Land");
		address.setCountry(country);
		City city = new City();
		city.setName("Meine Stadt");
		address.setCity(city);
		address.setPostCode("123");
		address.setStreet("Meine Straße");
		club.setAddress(address);
		setTestRoles("Sportwart");
		masterData.saveClub(club);
	}

	@Test(expected = FachlicheException.class)
	public void failSaveRoleAsNonAdmin() {
		Role role = new Role();
		setTestRoles("Sportwart");
		masterData.saveRole(role);
	}

	@Test
	public void getClubs() {
		masterData.getClubs();
	}

	@Test
	@InSequence(1)
	public void saveRole() {
		Role externalRole = new Role();
		externalRole.setRole(TestUtils.random(32));
		externalRole.setDescription("description");
		setTestRoles("Admin");
		masterData.saveRole(externalRole);
		Role internalRole = new Role();
		internalRole.setInternal(true);
		internalRole.setRole(TestUtils.random(32));
		internalRole.setDescription("description");
		masterData.saveRole(internalRole);
	}

	@Test
	@InSequence(2)
	public void getRoles() {
		List<Role> roles = masterData.getRoles(true);
		assertEquals(1, roles.size());
		roles = masterData.getRoles(false);
		assertEquals(2, roles.size());
	}

	@Test
	@InSequence(2)
	public void getRolesAsAdmin() {
		List<Role> roles = masterData.getRoles(true);
		assertEquals(1, roles.size());

		setTestRoles("Admin");
		roles = masterData.getRoles(false);
		assertEquals(2, roles.size());
	}

	@Test(expected = TechnischeException.class)
	public void failPreprocessDataImportNonUser() {
		Club club = new Club();
		masterData.preprocessDataImport(club, new HashMap<String, Object>());
	}

	@Test
	@InSequence(3)
	public void preprocessDataImport() {
		Role role1 = TestUtils.getRole(getEm(), TestUtils.random(32));
		Role role2 = TestUtils.getRole(getEm(), TestUtils.random(32));

		User user = new User();
		user.getRoles().add(role1);
		user.getRoles().add(role2);
		masterData.preprocessDataImport(user, new HashMap<String, Object>());
	}

	@Test
	public void dataImport() {
		User user1 = new User();
		user1.setUserId("import1");
		user1.setFirstName("Hallo");
		user1.setLastName("Bello");
		User user2 = new User();
		user2.setUserId("import2");
		user2.setFirstName("Hallo1");
		user2.setLastName("Bello1");
		List<User> users = Arrays.asList(user1, user2);
		common.dataImport(users, masterData);
	}
}
