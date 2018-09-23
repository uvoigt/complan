package org.planner.eo;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;

import org.planner.util.NLSBundle;
import org.planner.util.Visible;

@Entity
@Access(AccessType.FIELD)
@NLSBundle("roles")
public class Role extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Column(length = 32, nullable = false, unique = true)
	@Visible
	private String role;

	@Visible(depth = 0)
	private String description;

	@ManyToMany
	private List<Role> roles = new ArrayList<>();

	@Visible(depth = 0, initial = false)
	private boolean internal;

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Role> getRoles() {
		return roles;
	}

	public boolean isInternal() {
		return internal;
	}

	public void setInternal(boolean internal) {
		this.internal = internal;
	}
}
