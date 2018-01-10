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
		if (age <= schuelerC.upperBound)
			return AgeType.schuelerC;
		if (age <= schuelerB.upperBound)
			return AgeType.schuelerB;
		if (age <= schuelerA.upperBound)
			return AgeType.schuelerA;
		if (age <= jugend.upperBound)
			return AgeType.jugend;
		if (age <= junioren.upperBound)
			return AgeType.junioren;
		if (age <= lk.upperBound)
			return AgeType.lk;
		if (age <= akA.upperBound)
			return AgeType.akA;
		if (age <= akB.upperBound)
			return AgeType.akB;
		if (age <= akC.upperBound)
			return AgeType.akC;
		return AgeType.akD;
	}

	public static int[] getAgesForAgeType(List<AgeType> ageTypes) {
		int[] result = { 99, 0 };
		for (AgeType ageType : ageTypes) {
			int low = ageType.lowerBound;
			int high = ageType.upperBound;
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
