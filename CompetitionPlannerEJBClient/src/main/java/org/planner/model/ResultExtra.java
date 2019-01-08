package org.planner.model;

import org.planner.util.CommonMessages;

public enum ResultExtra implements LocalizedEnum {
	dns, dnf, dq;

	@Override
	public String getText() {
		return CommonMessages.getEnumText(this);
	}
}
