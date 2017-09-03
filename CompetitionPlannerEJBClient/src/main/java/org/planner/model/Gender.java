package org.planner.model;

import org.planner.util.CommonMessages;

public enum Gender implements LocalizedEnum {
	m, w, mixed;

	@Override
	public String getText() {
		return CommonMessages.getEnumText(this);
	}
}
