package org.planner.business;

import java.util.Map;

import org.planner.eo.AbstractEntity;

public interface ImportPreprozessor {

	void preprocessDataImport(AbstractEntity entity, Map<String, Object> context);
}
