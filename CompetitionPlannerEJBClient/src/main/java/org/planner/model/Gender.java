package org.planner.model;

import org.planner.util.CommonMessages;

public enum Gender implements LocalizedEnum {
	m, f, mixed;

	@Override
	public String getText() {
		return CommonMessages.getEnumText(this);
	}

	public String getAgeFriendlyText(AgeType ageType) {
		return CommonMessages.getMessage(getClass().getSimpleName() + "." + name() + (ageType.isMature() ? "m" : "c"));
	}
}
