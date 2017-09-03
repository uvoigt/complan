package org.planner.eo;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Access(AccessType.FIELD)
@XmlRootElement
public class Properties extends AbstractEntity {
	private static final long serialVersionUID = 1L;

	private String name;

	private String value;

	@Column(length = 32)
	private String userId;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public <T> T getTypedValue(Class<T> type) {
		return internalGetTypedValue(type, value);
	}

	@SuppressWarnings("unchecked")
	private <T> T internalGetTypedValue(Class<T> type, String value) {
		if (value == null)
			return null;
		if (Integer.class.equals(type))
			return (T) Integer.valueOf(value);
		if (Short.class.equals(type))
			return (T) Short.valueOf(value);
		if (Long.class.equals(type))
			return (T) Long.valueOf(value);
		if (Double.class.equals(type))
			return (T) Double.valueOf(value);
		if (Float.class.equals(type))
			return (T) Float.valueOf(value);
		if (String.class.equals(type))
			return (T) value;
		if (Date.class.equals(type))
			return (T) new Date(Long.valueOf(value));
		if (type != null && type.isArray())
			return (T) createArray(value, type.getComponentType());
		throw new IllegalArgumentException("not supported: " + type);
	}

	private Object createArray(String s, Class<?> componentType) {
		String[] split = s.split("~~");
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			if (split[i].length() > 0)
				list.add(internalGetTypedValue(componentType, split[i]));
		}
		return list.toArray((Object[]) Array.newInstance(componentType, list.size()));
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setValue(Object value) {
		if (value.getClass().isArray()) {
			StringBuilder sb = new StringBuilder();
			int length = Array.getLength(value);
			for (int i = 0; i < length; i++) {
				if (i > 0)
					sb.append("~~");
				sb.append(Array.get(value, i));
			}
			this.value = sb.toString();
		} else {
			setValue(value.toString());
		}
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public Long getId() {
		return id;
	}
}
