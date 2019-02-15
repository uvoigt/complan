package org.planner.eo;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Access(AccessType.FIELD)
public class Token implements Serializable {
	public enum TokenType {
		email, login
	}

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	private TokenType type;

	private String value;

	private long tokenExpires;

	public TokenType getType() {
		return type;
	}

	public void setType(TokenType type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public long getTokenExpires() {
		return tokenExpires;
	}

	public void setTokenExpires(long tokenExpires) {
		this.tokenExpires = tokenExpires;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
