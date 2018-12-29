package org.planner.remote;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.ejb.Remote;

import org.planner.eo.AbstractEntity;
import org.planner.eo.AbstractEnum;
import org.planner.eo.Announcement;
import org.planner.eo.Announcement.AnnouncementStatus;
import org.planner.eo.Club;
import org.planner.eo.Program;
import org.planner.eo.Program.ProgramStatus;
import org.planner.eo.ProgramRace;
import org.planner.eo.Properties;
import org.planner.eo.Race;
import org.planner.eo.RegEntry;
import org.planner.eo.Registration;
import org.planner.eo.Registration.RegistrationStatus;
import org.planner.eo.Result;
import org.planner.eo.Role;
import org.planner.eo.User;
import org.planner.model.Change;
import org.planner.model.IResultProvider;
import org.planner.model.Suchergebnis;
import org.planner.model.Suchkriterien;

@Remote
public interface ServiceFacade extends IResultProvider {

	Map<String, Properties> leseBenutzerEinstellungen();

	Map<String, Properties> speichernBenutzerEinstellungen(List<Properties> properties);

	<T extends AbstractEntity> T getObjectForCopy(Class<T> type, long id);

	void delete(Class<? extends AbstractEntity> entityType, Long id);

	void deleteRaces(Long announcementId, List<Long> raceIds);

	String sendRegister(String email, String resetUrl);

	String sendPasswortReset(String logonName, String resetUrl);

	String sendPasswortReset(Long userId, String resetUrl);

	User authenticate(String token, boolean email);

	String resetPassword(String token, String password);

	String rememberMe(String currentToken);

	void forgetMe(String currentToken);

	<T extends AbstractEnum> T getEnumByName(String name, Class<T> enumType);

	User saveUser(User user);

	User getLoggedInUser();

	void saveLastLogonTime();

	String getUserName(String userId);

	List<Role> getAllRoles();

	List<Role> getExternalRoles();

	List<Club> getClubs();

	Club saveClub(Club club);

	Role saveRole(Role role);

	Announcement saveAnnouncement(Announcement announcement);

	void dataImport(List<AbstractEntity> entities);

	List<Race> getRaces(Long announcementId);

	void createRaces(Long announcementId, String[] selectedAgeTypes, String[] selectedBoatClasses,
			String[] selectedGenders, String[] selectedDistances, Integer dayOffset);

	void saveRace(Race race);

	List<Announcement> getOpenAnnouncements();

	Long createRegistration(Registration registration);

	void setAnnouncementStatus(Long announcementId, AnnouncementStatus status);

	<T extends Serializable> Suchergebnis<T> getAthletes(Suchkriterien criteria);

	void saveRegEntries(Long registrationId, List<RegEntry> entries);

	void deleteFromRegEntry(Long registrationId, RegEntry entry);

	void setRegistrationStatus(Long registrationId, RegistrationStatus status);

	List<RegEntry> getMyUpcomingRegistrations();

	List<Result> getMyLatestResults();

	List<ProgramRace> saveResult(Result result);

	Long createProgram(Program program);

	void generateProgram(Program program);

	void setProgramStatus(Long programId, ProgramStatus status);

	Program getProgram(Long id);

	void deleteProgram(Long programId);

	Program getResults(Long id);

	// TODO temporär
	List<Change> checkProgram(Program program);

	void swapRaces(ProgramRace r1, ProgramRace r2);
}
