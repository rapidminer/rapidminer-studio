/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import javax.swing.AbstractListModel;


/**
 * A filterable list model for JList components. The method {@link #valueChanged(String)} updates
 * the filter, i.e. the visible list is filtered according to the given filter value. If the value
 * is null, the view is set back to include all actual kept list values.
 * 
 * @author Tobias Malbrecht
 */
public class FilterableListModel extends AbstractListModel implements FilterListener {

	public abstract static class FilterCondition {

		public abstract boolean matches(Object o);
	}

	private static final long serialVersionUID = 552254394780900171L;

	private LinkedList<Object> list;

	private LinkedList<Object> filteredList;

	private Comparator<Object> comparator;

	private String filterValue;

	private LinkedList<FilterCondition> conditions = new LinkedList<FilterCondition>();

	public FilterableListModel() {
		list = new LinkedList<Object>();
		filteredList = new LinkedList<Object>();
		comparator = new Comparator<Object>() {

			@Override
			public int compare(Object o1, Object o2) {
				return o1.toString().compareTo(o2.toString());
			}

		};
	}

	@Override
	public void valueChanged(String value) {
		filteredList.clear();
		if (value == null || value.trim().length() == 0 || value.equals("")) {
			for (Object o : list) {
				if (!filteredByCondition(o)) {
					filteredList.add(o);
				}
			}
		} else {
			for (Object o : list) {
				if (o.toString().contains(value)) {
					if (!filteredByCondition(o)) {
						filteredList.add(o);
					}
				}
			}
		}
		fireContentsChanged(this, 0, filteredList.size() - 1);
		filterValue = value;
	}

	public void addElement(Object o) {
		list.add(o);
		Collections.sort(list, comparator);
		if (filterValue == null) {
			filteredList.add(o);
		} else {
			if (o.toString().contains(filterValue)) {
				filteredList.add(o);
			}
		}
		Collections.sort(filteredList, comparator);
		fireContentsChanged(this, 0, filteredList.size() - 1);
	}

	public void removeElement(Object o) {
		list.remove(o);
		Collections.sort(list, comparator);
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
	public Object getElementAt(int index) {
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

	public void setComparator(Comparator<Object> comparator) {
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
			for (Object o : list) {
				if (!filteredByCondition(o)) {
					filteredList.add(o);
				}
			}
		} else {
			for (Object o : list) {
				if (o.toString().contains(filterValue)) {
					if (!filteredByCondition(o)) {
						filteredList.add(o);
					}
				}
			}
		}
		fireContentsChanged(this, 0, filteredList.size() - 1);
	}
}
