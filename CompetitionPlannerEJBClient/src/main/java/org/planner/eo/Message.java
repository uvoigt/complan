package org.planner.eo;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Access(AccessType.FIELD)
@XmlRootElement
public class Message extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Column(nullable = false)
	private String name;

	@Lob
	private String text;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
