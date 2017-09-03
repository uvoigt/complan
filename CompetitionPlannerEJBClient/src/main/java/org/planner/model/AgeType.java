package org.planner.model;

import org.planner.util.CommonMessages;

public enum AgeType implements LocalizedEnum {
	schuelerA, schuelerB, schuelerC, jugend, junioren, lk, akA, akB, akC, akD, ak;

	@Override
	public String getText() {
		return CommonMessages.getEnumText(this);
	}
}
