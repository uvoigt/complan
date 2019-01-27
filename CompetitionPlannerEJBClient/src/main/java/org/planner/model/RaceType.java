package org.planner.model;

import org.planner.util.CommonMessages;

public enum RaceType implements LocalizedEnum {
	heat, semiFinal, finalA, finalB;

	@Override
	public String getText() {
		return CommonMessages.getEnumText(this);
	}

	public boolean isFinal() {
		return this == finalA || this == finalB;
	}
}
