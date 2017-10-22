package org.planner.eo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.planner.util.Visible;

@Entity
@Table(name = "ENUM")
@Access(AccessType.FIELD)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.INTEGER, name = "TYPE")
public class AbstractEnum implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	@Column(length = 32, updatable = false)
	private String createUser;

	@Column(updatable = false)
	private Date createTime;

	@Visible
	private String name;

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
}
