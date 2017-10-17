package org.planner.eo;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;
import org.joda.time.Years;
import org.planner.model.AgeType;
import org.planner.model.Gender;
import org.planner.util.NLSBundle;
import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@NLSBundle("users")
public class User extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Column(nullable = false, unique = true, length = 32)
	@Visible(initial = false, export = true, mandatory = true, order = 0)
	private String userId;

	@Column(nullable = false)
	@Visible(export = true, order = 1)
	private String firstName;

	@Column(nullable = false)
	@Visible(export = true, order = 2)
	private String lastName;

	@Visible(export = true, order = 3)
	private String email;

	@Visible(initial = false, export = true, order = 4)
	@Temporal(TemporalType.DATE)
	private Date birthDate;

	@Visible(initial = false, export = true, order = 5)
	private Gender gender;

	private String password;
	private String token;
	private Long tokenExpires;

	@ManyToMany
	@JoinTable(inverseJoinColumns = @JoinColumn(name = "role_id"))
	@OrderBy("role")
	@Visible(export = true, order = 6)
	private Set<Role> roles = new HashSet<>();

	// aufgrund der Neuanlage bei Registrierung muss das bei der Erstanmeldung
	// gesetzt werden
	@ManyToOne(fetch = FetchType.LAZY) /* (optional = false) */
	@Visible(initial = false, roles = "Admin", order = 7)
	private Club club = new Club();

	@Visible(initial = false, roles = "Admin", order = 8)
	private Date lastLogon;

	@Visible(initial = false, order = 6)
	private Boolean locked;

	public User() {
	}

	// spezifischer Konstruktor für getAthletes
	// public User(Long id, String clubName, String firstName, String lastName, Date birthDate, Gender gender) {
	// this.id = id;
	// this.club = new Club();
	// this.club.setName(clubName);
	// this.firstName = firstName;
	// this.lastName = lastName;
	// this.birthDate = birthDate;
	// this.gender = gender;
	// }

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Long getTokenExpires() {
		return tokenExpires;
	}

	public void setTokenExpires(Long tokenExpires) {
		this.tokenExpires = tokenExpires;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Club getClub() {
		return club;
	}

	public void setClub(Club club) {
		this.club = club;
	}

	public Date getLastLogon() {
		return lastLogon;
	}

	public void setLastLogon(Date lastLogon) {
		this.lastLogon = lastLogon;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public boolean isLocked() {
		return locked != null && locked.booleanValue();
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public String getName() {
		return firstName + " " + lastName;
	}

	public int getAge() {
		if (birthDate == null)
			return 0;
		Calendar cal = Calendar.getInstance();
		cal.setTime(birthDate);
		return Years.yearsBetween(new LocalDate(birthDate), new LocalDate()).getYears();
	}

	public AgeType getAgeType() {
		if (birthDate == null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(birthDate);
		// hier ist ggf. nicht das exakte Alter gefragt, sondern, ob jemand in diesem Jahr das Alter erreicht
		int age = Calendar.getInstance().get(Calendar.YEAR) - cal.get(Calendar.YEAR);
		switch (age) {
		case 11:
			return AgeType.schuelerC;
		case 12:
			return AgeType.schuelerB;
		case 13:
			return AgeType.schuelerA;
		case 14:
		case 15:
			return AgeType.jugend;
		case 16:
		case 17:
			return AgeType.junioren;
		}
		if (age < 32)
			return AgeType.lk;
		if (age < 40)
			return AgeType.akA;
		if (age < 50)
			return AgeType.akB;
		if (age < 60)
			return AgeType.akC;
		if (age < 70)
			return AgeType.akD;
		if (age < 80)
			return AgeType.akE;
		throw new IllegalArgumentException("sorry, this age is not implemented");
	}

}
