package org.planner.model;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public class Suchergebnis<T> implements Serializable, Iterable<T> {
	private static final long serialVersionUID = 1L;

	private List<T> liste;
	private int gesamtgroesse;
	
	public Suchergebnis(List<T> liste, int gesamtgroesse) {
		this.liste = liste;
		this.gesamtgroesse = gesamtgroesse;
	}

	public List<T> getListe() {
		return liste;
	}

	@Override
	public Iterator<T> iterator() {
		return liste.iterator();
	}

	public int getGesamtgroesse() {
		return gesamtgroesse;
	}
}
