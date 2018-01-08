package org.planner.model;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.planner.util.CommonMessages;

public enum AgeType implements LocalizedEnum {
	schuelerC(6, 9), schuelerB(10, 12), schuelerA(13, 14), jugend(15, 16), junioren(17, 18), lk(19, 31), akA(32,
			39), akB(40, 49), akC(50, 59), akD(60, 69);

	public static AgeType getAgeType(Date birthDate) {
		if (birthDate == null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(birthDate);
		// hier ist nicht das exakte Alter gefragt, sondern, ob jemand in diesem Jahr das Alter erreicht
		int age = Calendar.getInstance().get(Calendar.YEAR) - cal.get(Calendar.YEAR);
		if (age <= 9)
			return AgeType.schuelerC;
		if (age <= 12)
			return AgeType.schuelerB;
		if (age <= 14)
			return AgeType.schuelerA;
		if (age <= 16)
			return AgeType.jugend;
		if (age <= 18)
			return AgeType.junioren;
		if (age < 32)
			return AgeType.lk;
		if (age < 40)
			return AgeType.akA;
		if (age < 50)
			return AgeType.akB;
		if (age < 60)
			return AgeType.akC;
		return AgeType.akD;
	}

	public static int[] getAgesForAgeType(List<AgeType> ageTypes) {
		int[] result = { 99, 0 };
		for (AgeType ageType : ageTypes) {
			int low = 0;
			int high = 0;
			switch (ageType) {
			case schuelerC:
				low = high = 11;
				break;
			case schuelerB:
				low = high = 12;
				break;
			case schuelerA:
				low = high = 13;
				break;
			case jugend:
				low = 14;
				high = 15;
				break;
			case junioren:
				low = 16;
				high = 17;
				break;
			case lk:
				low = 18;
				high = 31;
				break;
			case akA:
				low = 32;
				high = 39;
				break;
			case akB:
				low = 40;
				high = 49;
				break;
			case akC:
				low = 50;
				high = 59;
				break;
			case akD:
				low = 60;
				high = 120;
				break;
			}
			if (low < result[0])
				result[0] = low;
			if (high > result[1])
				result[1] = high;
		}
		return result;
	}

	private final int lowerBound;
	private final int upperBound;

	private AgeType(int lowerBound, int upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public int getLowerBound() {
		return lowerBound;
	}

	public int getUpperBound() {
		return upperBound;
	}

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
			return true;
		}
	}
}
