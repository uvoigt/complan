package org.planner.ejb;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.planner.business.AnnouncementServiceImpl;
import org.planner.business.CommonImpl;
import org.planner.business.MasterDataServiceImpl;
import org.planner.business.ProgramServiceImpl;
import org.planner.business.RegistryImpl;
import org.planner.eo.AbstractEntity;
import org.planner.eo.AbstractEnum;
import org.planner.eo.Address;
import org.planner.eo.Announcement;
import org.planner.eo.Club;
import org.planner.eo.Program;
import org.planner.eo.Properties;
import org.planner.eo.Race;
import org.planner.eo.RegEntry;
import org.planner.eo.Registration;
import org.planner.eo.Role;
import org.planner.eo.User;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;
import org.planner.remote.ServiceFacade;
import org.planner.util.Logged;

@Logged
// @Context
@Stateless
@RolesAllowed("User")
public class ServiceFacadeBean implements ServiceFacade {

	@Inject
	private CallerProvider caller;

	@Inject
	private CommonImpl common;

	@Inject
	private MasterDataServiceImpl masterData;

	@Inject
	private AnnouncementServiceImpl announcement;

	@Inject
	private ProgramServiceImpl program;

	@Inject
	private RegistryImpl registry;

	@Override
	public <T extends Serializable> Suchergebnis<T> search(Class<T> entityType, Suchkriterien criteria) {
		return common.search(entityType, criteria);
	}

	@Override
	public <T extends Serializable> T getObject(Class<T> type, long id, int fetchDepth) {
		return common.getById(type, id, fetchDepth);
	}

	@Override
	public <T extends AbstractEntity> T getObjectForCopy(Class<T> type, long id) {
		return common.getByIdForCopy(type, id);
	}

	@Override
	public <T extends AbstractEnum> T getEnumByName(String name, Class<T> enumType) {
		return common.getEnumByName(name, enumType);
	}

	@Override
	public Map<String, Properties> leseBenutzerEinstellungen() {
		return common.leseBenutzerEinstellungen(caller.getLoginName());
	}

	@Override
	public Map<String, Properties> speichernBenutzerEinstellungen(List<Properties> properties) {
		return common.speichernBenutzerEinstellungen(properties, caller.getLoginName());
	}

	@Override
	public void delete(Class<? extends AbstractEntity> entityType, Long id) {
		common.delete(entityType, id);
	}

	@Override
	public void deleteRaces(Long announcementId, List<Long> raceIds) {
		announcement.deleteRaces(announcementId, raceIds);
	}

	// @Override
	// public int speichernMitFilter(DatenSuchkriterien kriterien, Map<String,
	// String> werte) {
	// return adminGFO.speichernMitFilter(kriterien, werte,
	// benutzer.getLoginName());
	// }
	//
	// @Override
	//// @RolesAllowed("User")
	// public int loeschen(Class<? extends AbstractEO> entityType,
	// DatenSuchkriterien kriterien) {
	// return adminGFO.loeschenMitFilter(entityType, kriterien);
	// }
	//
	// @Override
	//// @RolesAllowed("User")
	// public int kopieren(Class<AbstractEO> entityType, DatenSuchkriterien
	// kriterien) {
	// return adminGFO.kopierenMitFilter(entityType, kriterien,
	// benutzer.getLoginName());
	// }

	@Override
	@PermitAll
	public String sendRegister(String email, String resetUrl) {
		return registry.sendRegister(email, resetUrl);
	}

	@Override
	public String sendPasswortReset(Long userId, String resetUrl) {
		return registry.sendPasswortReset(userId, resetUrl);
	}

	@Override
	@PermitAll
	public String sendPasswortReset(String logonName, String resetUrl) {
		return registry.sendPasswortReset(logonName, resetUrl);
	}

	@Override
	@PermitAll
	public User authenticate(String token) {
		return registry.authenticate(token);
	}

	@Override
	@PermitAll
	public String resetPassword(String token, String password) {
		return registry.resetPassword(token, password);
	}

	@Override
	public String getUserName(String userId) {
		return masterData.getUserName(userId);
	}

	@Override
	public User getLoggedInUser() {
		return masterData.getUserByUserId(caller.getLoginName(), true);
	}

	@Override
	public void saveLastLogonTime() {
		masterData.saveLastLogonTime(caller.getLoginName());
	}

	@Override
	public User saveUser(User user) {
		return masterData.saveUser(user);
	}

	@Override
	public List<Role> getRoles() {
		return masterData.getRoles();
	}

	@Override
	public List<Club> getClubs() {
		return masterData.getClubs();
	}

	@Override
	public Club saveClub(Club club) {
		return masterData.saveClub(club);
	}

	@Override
	public Address saveAddress(Address address) {
		return masterData.saveAddress(address);
	}

	@Override
	@RolesAllowed("Admin")
	public Role saveRole(Role role) {
		return masterData.saveRole(role);
	}

	@Override
	public Announcement saveAnnouncement(Announcement announcement) {
		return this.announcement.saveAnnouncement(announcement);
	}

	@Override
	public void dataImport(List<AbstractEntity> entities) {
		common.dataImport(entities, masterData);
	}

	@Override
	public List<Race> getRaces(Long announcementId) {
		return announcement.getRaces(announcementId);
	}

	@Override
	public void createRaces(Long announcementId, String[] selectedAgeTypes, String[] selectedBoatClasses,
			String[] selectedGenders, String[] selectedDistances, Integer dayOffset) {
		announcement.createRaces(announcementId, selectedAgeTypes, selectedBoatClasses, selectedGenders,
				selectedDistances, dayOffset);
	}

	@Override
	public void saveRace(Race race) {
		announcement.saveRace(race);
	}

	@Override
	public List<Announcement> getOpenAnnouncements() {
		return announcement.getOpenAnnouncements();
	}

	@Override
	public Long createRegistration(Registration registration) {
		return announcement.createRegistration(registration);
	}

	@Override
	public void announce(Long announcementId) {
		this.announcement.announce(announcementId);
	}

	@Override
	public void saveRegEntries(Long registrationId, List<RegEntry> entries) {
		this.announcement.saveRegEntries(registrationId, entries);
	}

	@Override
	public void deleteFromRegEntry(Long registrationId, RegEntry entry) {
		this.announcement.deleteFromRegEntry(registrationId, entry);
	}

	@Override
	public void submitRegistration(Long registrationId) {
		this.announcement.submitRegistration(registrationId);
	}

	@Override
	public Long createProgram(Program program) {
		return this.program.createProgram(program);
	}

	@Override
	public Program generateProgram(Program program) {
		return this.program.generateProgram(program);
	}
}
