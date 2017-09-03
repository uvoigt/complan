package org.planner.model;

import org.planner.eo.AbstractEntity;

public interface IResultProvider {

	<T extends AbstractEntity> Suchergebnis<T> search(Class<T> entityType, Suchkriterien criteria);

	<T extends AbstractEntity> T getObject(Class<T> type, long id);
}
