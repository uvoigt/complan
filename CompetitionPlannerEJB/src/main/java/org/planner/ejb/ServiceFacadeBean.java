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
import org.planner.eo.Announcement;
import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Club;
import org.planner.eo.HasId;
import org.planner.eo.Placement;
import org.planner.eo.Program;
import org.planner.eo.Program.ProgramStatus;
import org.planner.eo.ProgramRace;
import org.planner.eo.Properties;
import org.planner.eo.Race;
import org.planner.eo.RegEntry;
import org.planner.eo.Registration;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Role;
import org.planner.eo.User;
import org.planner.model.AgeType;
import org.planner.model.BoatClass;
import org.planner.model.Change;
import org.planner.model.FetchInfo;
import org.planner.model.Gender;
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
	public <T extends HasId> T getObject(Class<T> type, long id, FetchInfo... fetchInfo) {
		return common.getById(type, id, fetchInfo);
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
	public void delete(Class<? extends Serializable> entityType, Long id) {
		common.delete(entityType, id);
	}

	@Override
	public void deleteRaces(Long announcementId, List<Long> raceIds) {
		announcement.deleteRaces(announcementId, raceIds);
	}

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
	public User authenticate(String token, boolean email) {
		return registry.authenticate(token, email);
	}

	@Override
	@PermitAll
	public String resetPassword(String token, String password) {
		return registry.resetPassword(token, password);
	}

	@Override
	public String rememberMe(String currentToken) {
		return registry.rememberMe(currentToken);
	}

	@Override
	public void forgetMe(String currentToken) {
		registry.forgetMe(currentToken);
	}

	@Override
	public String getUserName(String userId) {
		return masterData.getUserName(userId);
	}

	@Override
	public User getLoggedInUser() {
		return common.getCallingUser();
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
	@RolesAllowed("Admin")
	public List<Role> getAllRoles() {
		return masterData.getRoles(false);
	}

	@Override
	public List<Role> getExternalRoles() {
		return masterData.getRoles(true);
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
	@RolesAllowed("Admin")
	public Role saveRole(Role role) {
		return masterData.saveRole(role);
	}

	@Override
	public Announcement saveAnnouncement(Announcement announcement, boolean copy) {
		return this.announcement.saveAnnouncement(announcement, copy);
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
	public void createRaces(Long announcementId, AgeType[] selectedAgeTypes, BoatClass[] selectedBoatClasses,
			Gender[] selectedGenders, int[] selectedDistances, Integer dayOffset) {
		announcement.createRaces(announcementId, selectedAgeTypes, selectedBoatClasses, selectedGenders,
				selectedDistances, dayOffset);
	}

	@Override
	public void saveRace(Race race) {
		announcement.saveRace(race);
	}

	@Override
	public Long createRegistration(Registration registration) {
		return announcement.createRegistration(registration);
	}

	@Override
	public void setAnnouncementStatus(Long announcementId, AnnouncementStatus status) {
		announcement.setAnnouncementStatus(announcementId, status);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Serializable> Suchergebnis<T> getAthletes(Suchkriterien criteria) {
		return (Suchergebnis<T>) announcement.getAthletes(criteria);
	}

	@Override
	public void saveRegEntries(Long registrationId, List<RegEntry> entries) {
		announcement.saveRegEntries(registrationId, entries);
	}

	@Override
	public void deleteFromRegEntry(Long registrationId, RegEntry entry) {
		announcement.deleteFromRegEntry(registrationId, entry);
	}

	@Override
	public void setRegistrationStatus(Long registrationId, RegistrationStatus status) {
		announcement.setRegistrationStatus(registrationId, status);
	}

	@Override
	public List<RegEntry> getMyUpcomingRegistrations() {
		return announcement.getMyUpcomingRegistrations();
	}

	@Override
	public List<Placement> getMyLatestResults(int months) {
		return announcement.getMyLatestResults(months);
	}

	@Override
	public List<Placement> getPlacements(Long programRaceId) {
		return program.getPlacements(programRaceId);
	}

	@Override
	public List<ProgramRace> saveResult(Long programRaceId, List<Placement> placements) {
		return program.saveResult(programRaceId, placements);
	}

	@Override
	public Long createProgram(Long announcementId) {
		return this.program.createProgram(announcementId);
	}

	@Override
	public void generateProgram(Program program) {
		this.program.generateProgram(program);
	}

	@Override
	public void setProgramStatus(Long programId, ProgramStatus status) {
		program.setProgramStatus(programId, status);
	}

	@Override
	public Program getProgram(Long id, boolean withResults, boolean orderByResults) {
		return program.getProgram(id, withResults, orderByResults);
	}

	@Override
	public void deleteProgram(Long programId) {
		program.deleteProgram(programId);
	}

	@Override
	public List<Change> checkProgram(Program program) {
		return this.program.checkProgram(program);
	}

	@Override
	public void swapRaces(ProgramRace r1, ProgramRace r2) {
		this.program.swapRaces(r1, r2);
	}
}
