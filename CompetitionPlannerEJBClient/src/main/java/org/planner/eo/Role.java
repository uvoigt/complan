package org.planner.eo;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;

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
}
