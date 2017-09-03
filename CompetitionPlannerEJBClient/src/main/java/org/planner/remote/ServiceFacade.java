package org.planner.remote;

import java.util.List;
import java.util.Map;

import javax.ejb.Remote;

import org.planner.eo.AbstractEntity;
import org.planner.eo.AbstractEnum;
import org.planner.eo.Address;
import org.planner.eo.Announcement;
import org.planner.eo.Club;
import org.planner.eo.Properties;
import org.planner.eo.Race;
import org.planner.eo.Role;
import org.planner.eo.User;
import org.planner.model.IResultProvider;

@Remote
public interface ServiceFacade extends IResultProvider {

	Map<String, Properties> leseBenutzerEinstellungen();

	Map<String, Properties> speichernBenutzerEinstellungen(List<Properties> properties);

	<T extends AbstractEntity> T getObjectForCopy(Class<T> type, long id);

	void delete(Class<? extends AbstractEntity> entityType, Long id);

	void delete(Class<? extends AbstractEntity> entityType, List<Long> ids);

	// int speichernMitFilter(DatenSuchkriterien kriterien, Map<String, String>
	// werte);
	// int loeschen(Class<? extends AbstractEO> entityType, DatenSuchkriterien
	// kriterien);
	//
	// int kopieren(Class<AbstractEO> entityType, DatenSuchkriterien kriterien);

	String sendRegister(String email, String resetUrl);

	String sendPasswortReset(String logonName, String resetUrl);

	String sendPasswortReset(Long userId, String resetUrl);

	User authenticate(String token);

	String resetPassword(String token, String password);

	<T extends AbstractEnum> T getEnumByName(String name, Class<T> enumType);

	User saveUser(User user);

	User getLoggedInUser();

	void saveLastLogonTime();

	User getUserById(Long id);

	String getUserName(String userId);

	List<Role> getRoles();

	List<Club> getClubs();

	Club saveClub(Club club);

	Address saveAddress(Address address);

	Role saveRole(Role role);

	Announcement saveAnnouncement(Announcement announcement);

	void dataImport(List<AbstractEntity> entities);

	List<Race> getRaces(Long announcementId);

	void createRaces(Long announcementId, String[] selectedAgeTypes, String[] selectedBoatClasses,
			String[] selectedGenders, String[] selectedDistances, Integer dayOffset);

	void saveRace(Race race);

}
