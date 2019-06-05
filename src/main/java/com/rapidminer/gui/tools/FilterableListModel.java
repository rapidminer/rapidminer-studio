/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.gui.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractListModel;

import com.rapidminer.external.alphanum.AlphanumComparator;
import com.rapidminer.external.alphanum.AlphanumComparator.AlphanumCaseSensitivity;


/**
 * A filterable list model for JList components. The method {@link #valueChanged(String)} updates
 * the filter, i.e. the visible list is filtered according to the given filter value. If the value
 * is null, the view is set back to include all actual kept list values.
 *
 * @author Tobias Malbrecht
 */
public class FilterableListModel<E> extends AbstractListModel<E> implements FilterListener {

	public interface FilterCondition {

		boolean matches(Object o);

	}

	/** @since 9.2.1 */
	public static final AlphanumComparator STRING_COMPARATOR = new AlphanumComparator(AlphanumCaseSensitivity.INSENSITIVE);

	private static final long serialVersionUID = 552254394780900171L;

	private List<E> list;

	private List<E> filteredList;

	private Comparator<E> comparator;

	private String filterValue;

	private List<FilterCondition> conditions = new LinkedList<>();

	public FilterableListModel() {
		this(true);
	}

	/**
	 * Can sort if desired.
	 *
	 * @param sort
	 * 		if {@code true}, will sort alpha-numerically; if {@code false} will not sort at all
	 * @since 9.2.0
	 */
	public FilterableListModel(boolean sort) {
		this(new ArrayList<>(), sort);

	}

	/**
	 * Can sort if desired and starts out with the specified elements.
	 *
	 * @param elements
	 * 		the elements present in the model; assumed to be already sorted if {@code sorted} is {@code true}
	 * @param sort
	 * 		if {@code true}, will sort alpha-numerically; if {@code false} will not sort at all
	 * @since 9.2.1
	 */
	@SuppressWarnings("unchecked")
	public FilterableListModel(List<E> elements, boolean sort) {
		list = new ArrayList<>(elements);
		filteredList = new ArrayList<>(elements);
		if (sort) {
			comparator = Comparator.comparing(Object::toString, STRING_COMPARATOR);
		}
	}

	@Override
	public void valueChanged(String value) {
		filteredList.clear();
		if (value == null || value.trim().length() == 0 || value.equals("")) {
			for (E e : list) {
				if (!filteredByCondition(e)) {
					filteredList.add(e);
				}
			}
		} else {
			for (E e : list) {
				if (e.toString().toLowerCase().contains(value.toLowerCase()) && !filteredByCondition(e)) {
					filteredList.add(e);
				}
			}
		}
		fireContentsChanged(this, 0, filteredList.size() - 1);
		filterValue = value;
	}

	public void addElement(E e) {
		list.add(e);
		if (comparator != null) {
			list.sort(comparator);
		}
		if (filterValue == null) {
			filteredList.add(e);
		} else {
			if (e.toString().contains(filterValue)) {
				filteredList.add(e);
			}
		}
		if (comparator != null) {
			filteredList.sort(comparator);
		}
		fireContentsChanged(this, 0, filteredList.size() - 1);
	}

	public void removeElement(Object o) {
		list.remove(o);
		if (comparator != null) {
			list.sort(comparator);
		}
		if (filteredList.contains(o)) {
			filteredList.remove(o);
		}
		fireContentsChanged(this, filteredList.size() - 2, filteredList.size() - 1);
	}

	public boolean contains(Object o) {
		return list.contains(o);
	}

	public void removeElementAt(int index) {
		list.remove(filteredList.remove(index));
		fireContentsChanged(this, index, index);
	}

	@Override
	public E getElementAt(int index) {
		return filteredList.get(index);
	}

	public int indexOf(Object o) {
		return filteredList.indexOf(o);
	}

	@Override
	public int getSize() {
		return filteredList.size();
	}

	private boolean filteredByCondition(Object o) {
		for (FilterCondition c : conditions) {
			if (c.matches(o)) {
				return true;
			}
		}
		return false;
	}

	public void addCondition(FilterCondition c) {
		conditions.add(c);
		filterConditionsChanged();
	}

	public void addConditions(Collection<FilterCondition> c) {
		conditions.addAll(c);
		filterConditionsChanged();
	}

	public void setComparator(Comparator<E> comparator) {
		this.comparator = comparator;
	}

	public void removeCondition(FilterCondition c) {
		conditions.remove(c);
		filterConditionsChanged();
	}

	public void removeAllConditions() {
		conditions.clear();
		filterConditionsChanged();
	}

	private void filterConditionsChanged() {
		filteredList.clear();
		if (filterValue == null || filterValue.trim().length() == 0 || filterValue.equals("")) {
			for (E e : list) {
				if (!filteredByCondition(e)) {
					filteredList.add(e);
				}
			}
		} else {
			for (E e : list) {
				if (e.toString().contains(filterValue) && !filteredByCondition(e)) {
					filteredList.add(e);
				}
			}
		}
		fireContentsChanged(this, 0, filteredList.size() - 1);
	}
}
