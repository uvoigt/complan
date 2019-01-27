package org.planner.ui.beans;

import org.planner.model.FetchInfo;

public interface ITarget {

	/**
	 * Wenn null zurückgegeben wird, dann wird das Objekt nicht geladen, sondern nur die ID propagiert.
	 * 
	 * @see SearchBean#bearbeiten(String, String, Object, ITarget)
	 * 
	 * @return Fetch-Infos
	 */
	FetchInfo[] getFetchInfo();

	void setItem(Object item);

	Object prepareForCopy(Object item);
}
