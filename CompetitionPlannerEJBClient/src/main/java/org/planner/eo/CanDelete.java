package org.planner.eo;

import javax.persistence.EntityManager;

public interface CanDelete {
	void delete(EntityManager em);
}
