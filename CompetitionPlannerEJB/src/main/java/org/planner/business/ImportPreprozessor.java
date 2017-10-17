package org.planner.business;

import java.util.Map;

import javax.transaction.RollbackException;

import org.planner.eo.AbstractEntity;

public interface ImportPreprozessor {

	void preprocessDataImport(AbstractEntity entity, Map<String, Object> context);

	String handleItemRollback(AbstractEntity entity, RollbackException e);
}
