package org.planner.model;

import org.planner.util.CommonMessages;

public enum AgeType implements LocalizedEnum {
	schuelerA, schuelerB, schuelerC, jugend, junioren, lk, akA, akB, akC, akD, akE, ak;

	@Override
	public String getText() {
		return CommonMessages.getEnumText(this);
	}

	public boolean isMature() {
		switch (this) {
		default:
			throw new IllegalArgumentException(name());
		case schuelerA:
		case schuelerB:
		case schuelerC:
		case jugend:
			return false;
		case junioren:
		case lk:
		case akA:
		case akB:
		case akC:
		case akD:
		case akE:
		case ak:
			return true;
		}
	}
}
