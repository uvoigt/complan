package org.planner.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.metamodel.Attribute;

public class FetchInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Attribute<?, ?> attribute;
	private final boolean fetch;
	private List<FetchInfo> children;

	public FetchInfo(Attribute<?, ?> attribute, boolean fetch) {
		this.attribute = attribute;
		this.fetch = fetch;
	}

	public Attribute<?, ?> getAttribute() {
		return attribute;
	}

	public boolean isFetch() {
		return fetch;
	}

	@SuppressWarnings("unchecked")
	public List<FetchInfo> getChildren() {
		return children != null ? children : Collections.EMPTY_LIST;
	}

	public FetchInfo add(FetchInfo... fetchInfo) {
		if (children == null)
			children = new ArrayList<>();
		for (FetchInfo info : fetchInfo) {
			children.add(info);
		}
		return this;
	}
}
