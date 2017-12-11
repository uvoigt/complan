package org.planner.model;

import org.planner.util.CommonMessages;

public enum Gender implements LocalizedEnum {
	m, f, mixed;

	@Override
	public String getText() {
		return CommonMessages.getEnumText(this);
	}

	public String getAgeFriendlyText(AgeType ageType) {
		String key = getClass().getSimpleName() + "." + name();
		if (this != mixed)
			key += (ageType.isMature() ? "m" : "c");
		return CommonMessages.getMessage(key);
	}
}
