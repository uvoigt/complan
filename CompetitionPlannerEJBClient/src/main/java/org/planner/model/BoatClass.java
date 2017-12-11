package org.planner.model;

import org.planner.util.CommonMessages;

public enum BoatClass implements LocalizedEnum {
	k1, k2, k4, c1, c2, c4, c8;

	@Override
	public String getText() {
		return CommonMessages.getEnumText(this);
	}

	public int getMinimalTeamSize() {
		switch (this) {
		default:
			throw new IllegalArgumentException(name());
		case c1:
		case k1:
			return 1;
		case c2:
		case k2:
			return 2;
		case c4:
		case k4:
			return 4;
		case c8:
			return 8;
		}
	}

	public int getMaximalTeamSize() {
		switch (this) {
		default:
			throw new IllegalArgumentException(name());
		case c1:
		case k1:
			return 1;
		case c2:
		case k2:
			return 2;
		case c4:
		case k4:
			return 4;
		case c8:
			return 8;
		}
	}

	public int getAllowedSubstitutes() {
		switch (this) {
		default:
			throw new IllegalArgumentException(name());
		case c1:
		case k1:
			return 1;
		case c2:
		case k2:
			return 2;
		case c4:
		case k4:
			return 4;
		case c8:
			return 8;
		}
	}
}
