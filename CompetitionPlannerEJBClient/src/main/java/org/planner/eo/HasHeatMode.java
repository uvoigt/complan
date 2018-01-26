package org.planner.eo;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

// falls inheritance nicht mehr die richtige Lösung ist, auf custom-type umstellen
@MappedSuperclass
@Access(AccessType.FIELD)
public class HasHeatMode implements Serializable {

	private static final long serialVersionUID = 1L;

	// ;-separierter String
	// <Platzierungen ins Finale>;<Platzierungen in Zwischenlauf>
	// Bsp.: 1;2 bedeutet Platz 1 ins Finale, Platz 2 - 3 ins Semifinale
	// Bsp.: 2;2 bedeutet Platz 1 - 2 ins Finale, Platz 3 - 4 ins Semifinale
	// Bsp.: 3 bedeutet Platz 1 - 3 ins Finale, keine Semifinale
	// Bsp.: ;3 bedeutet Platz 1 - 3 ins Semifinale, keiner direkt ins Finale
	private String heatMode;

	@Transient
	private transient Integer intoFinal;
	@Transient
	private transient Integer intoSemiFinal;

	private void parseHeatMode() {
		if (heatMode != null) {
			String[] split = heatMode.split(";");
			intoFinal = Integer.parseInt(split[0]);
			intoSemiFinal = Integer.parseInt(split[1]);
		} else {
			intoFinal = 0;
			intoSemiFinal = 0;
		}
	}

	private void updateHeatMode() {
		heatMode = intoFinal + ";" + intoSemiFinal;
	}

	// für das Setzen aus einer nativen Query
	public void setHeatMode(String heatMode) {
		this.heatMode = heatMode;
	}

	public int getIntoFinal() {
		if (intoFinal == null)
			parseHeatMode();
		return intoFinal;
	}

	public void setIntoFinal(int intoFinal) {
		this.intoFinal = intoFinal;
		updateHeatMode();
	}

	public int getIntoSemiFinal() {
		if (intoSemiFinal == null)
			parseHeatMode();
		return intoSemiFinal;
	}

	public void setIntoSemiFinal(int intoSemiFinal) {
		this.intoSemiFinal = intoSemiFinal;
		updateHeatMode();
	}
}
