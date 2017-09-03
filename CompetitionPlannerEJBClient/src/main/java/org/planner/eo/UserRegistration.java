package org.planner.eo;

import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

public class UserRegistration implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	protected Long id;

	private String ip;
	private String userId;
}
