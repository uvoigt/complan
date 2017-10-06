package org.planner.eo;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Access(AccessType.FIELD)
public class Permission implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum Right {
		// 0, 1, 2 implizit
		none, read, write;
	}
	// siehe org.jboss.security.acl.ACLImpl

	@Id
	@GeneratedValue
	private Long id;

	private int rights;

	private long subject_id;

	private long object_id;

	public int getRights() {
		return rights;
	}

	public void setRights(int rights) {
		this.rights = rights;
	}

	public long getSubject_id() {
		return subject_id;
	}

	public void setSubject_id(long subject_id) {
		this.subject_id = subject_id;
	}

	public long getObject_id() {
		return object_id;
	}

	public void setObject_id(long object_id) {
		this.object_id = object_id;
	}

}
