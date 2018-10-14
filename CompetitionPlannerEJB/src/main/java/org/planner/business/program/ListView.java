package org.planner.business.program;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.planner.model.Change;

public class ListView<T> extends AbstractList<T> {

	private final class IndexIterator implements Iterator<T> {
		private int offset = 0;

		@Override
		public boolean hasNext() {
			return offset < indexes.size();
		}

		@Override
		public T next() {
			return list.get(indexes.get(offset));
		}
	}

	private List<T> list;
	private List<Integer> indexes;
	private List<Change> changes = new ArrayList<>();

	public ListView(List<T> list) {
		this.list = list;
		indexes = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			indexes.add(i);
		}
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public T get(int index) {
		return list.get(indexes.get(index));
	}

	@Override
	public T set(int index, T element) {
		Integer storedIndex = indexes.get(index);
		int indexOf = list.indexOf(element);
		if (indexOf != -1)
			indexes.set(indexOf, index);
		return list.get(storedIndex);
	}

	public void apply(Change change) {
		changes.add(change);
		change.applyTo(indexes);
	}
}
