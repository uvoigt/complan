package org.planner.model;

import java.io.Serializable;

public interface IResultProvider {

	<T extends Serializable> Suchergebnis<T> search(Class<T> entityType, Suchkriterien criteria);

	<T extends Serializable> T getObject(Class<T> type, long id, int fetchDepth);
}
