package org.planner.model;

import org.planner.util.CommonMessages;

public enum BoatClass implements LocalizedEnum {
	k1, k2, k4, c1, c2, c4, c8;

	@Override
	public String getText() {
		return CommonMessages.getEnumText(this);
	}
}
