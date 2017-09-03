package org.planner.eo;

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
import javax.xml.bind.annotation.XmlRootElement;

import org.planner.util.NLSBundle;
import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@XmlRootElement
@NLSBundle("users")
public class User extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Column(nullable = false, unique = true, length = 32)
	@Visible(initial = false)
	private String userId;
	private String password;
	private String token;
	private Date tokenExpires;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@Column(nullable = false)
	@Visible
	private String firstName;

	@Column(nullable = false)
	@Visible
	private String lastName;

	@Visible
	private String email;

	@ManyToOne /* (optional = false) */
	// aufgrund der Neuanlage bei Registrierung muss das bei der Erstanmeldung
	// gesetzt werden
	@Visible(initial = false)
	private Club club = new Club();

	@Visible(initial = false)
	private Date lastLogon;

	@Visible(initial = false)
	private Boolean locked;

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

	public Date getTokenExpires() {
		return tokenExpires;
	}

	public void setTokenExpires(Date tokenExpires) {
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
}
