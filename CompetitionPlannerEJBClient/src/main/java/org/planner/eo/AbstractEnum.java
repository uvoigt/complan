package org.planner.eo;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.planner.util.Visible;

@Entity
@Table(name = "ENUM")
@DiscriminatorColumn(discriminatorType = DiscriminatorType.INTEGER, name = "TYPE")
public class AbstractEnum extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Visible
	private String name;
	private int ordinal;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getOrdinal() {
		return ordinal;
	}

	public void setOrdinal(int ordinal) {
		this.ordinal = ordinal;
	}
}
