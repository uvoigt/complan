package org.planner.dao;

import javax.persistence.EntityManager;

public interface IOperation<T> {

	T execute(EntityManager em);

}
