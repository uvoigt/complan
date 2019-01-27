package org.planner.model;

import java.io.Serializable;

import org.planner.eo.HasId;

public interface IResultProvider {

	<T extends Serializable> Suchergebnis<T> search(Class<T> entityType, Suchkriterien criteria);

	<T extends HasId> T getObject(Class<T> type, long id, FetchInfo... fetchInfo);
}
