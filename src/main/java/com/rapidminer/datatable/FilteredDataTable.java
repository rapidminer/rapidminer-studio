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
package com.rapidminer.datatable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;


/**
 * This DataTable filters the contained rows using a stack of FilterConditions. Each time the stack
 * is modified, it informs it's DataTableFilteredListener that they need to update the table.
 * 
 * @author Sebastian Land, Marius Helf
 */
public class FilteredDataTable extends DataTableView {

	public static enum ConditionCombination {
		AND,	// all conditions must match
		OR		// at least one condition must match
	}

	public static interface DataTableFilteredListener {

		/**
		 * This method is called by a datatable, if its content is changed.
		 */
		public void informDataTableChange(DataTable dataTable);
	}

	private List<DataTableFilteredListener> listeners = new LinkedList<DataTableFilteredListener>();

	private ArrayList<DataTableFilterCondition> conditionStack = new ArrayList<DataTableFilterCondition>();

	private final ConditionCombination conditionCombination;

	public FilteredDataTable(DataTable parentDataTable) {
		super(parentDataTable);
		this.conditionCombination = ConditionCombination.AND;
	}

	public FilteredDataTable(DataTable parentDataTable, ConditionCombination conditionCombination) {
		super(parentDataTable);
		this.conditionCombination = conditionCombination;
	}

	/**
	 * Adds a new condition. null arguments will be ignored.
	 * 
	 * @param condition
	 */
	public void addCondition(DataTableFilterCondition condition) {
		if (condition == null) {
			return;
		}
		conditionStack.add(condition);
		setSelectedIndices(updateSelection());
		informDataTableFilteredListener();
	}

	/**
	 * Adds a batch of conditions. The listeners will be only informed after all of the conditions
	 * have been added. Null conditions will be ignored.
	 * 
	 * @param conditions
	 *            The collection of the conditions to be added. Must not be null.
	 */
	public void addConditions(Iterable<? extends DataTableFilterCondition> conditions) {
		boolean changed = false;
		for (DataTableFilterCondition condition : conditions) {
			if (condition != null) {
				conditionStack.add(condition);
				changed = true;
			}
		}
		if (changed) {
			setSelectedIndices(updateSelection());
			informDataTableFilteredListener();
		}
	}

	public void removeCondition() {
		if (conditionStack.size() > 0) {
			conditionStack.remove(conditionStack.size() - 1);
			setSelectedIndices(updateSelection());
			informDataTableFilteredListener();
		}
	}

	public void removeAllConditions() {
		if (conditionStack.size() > 0) {
			conditionStack.clear();
			setSelectedIndices(updateSelection());
			informDataTableFilteredListener();
		}
	}

	/**
	 * Replaces all conditions. See addConditions for parameter description.
	 */
	public void replaceConditions(Iterable<? extends DataTableFilterCondition> newConditions) {
		int oldSize = conditionStack.size();
		conditionStack.clear();
		if (!newConditions.iterator().hasNext() && oldSize > 0) {
			setSelectedIndices(updateSelection());
			informDataTableFilteredListener();
			return;
		}
		addConditions(newConditions);
	}

	/**
	 * @return a vector which contains the indices of all rows in the parent table that match the
	 *         condition stack.
	 */
	private Vector<Integer> updateSelection() {
		if (conditionStack.isEmpty()) {
			return null;
		}

		int parentRowIndex = 0;
		Vector<Integer> selectedIndices = new Vector<Integer>();
		for (DataTableRow row : getParentTable()) {
			// heuristic: if we have accesses to the row, cache the row in a SimpleDataTableRow
			if (conditionStack.size() / 2 > row.getNumberOfValues() && !(row instanceof SimpleDataTableRow)) {
				row = new SimpleDataTableRow(row);
			}

			switch (conditionCombination) {
				case AND:
					boolean keep = true;

					for (int conditionIndex = conditionStack.size() - 1; conditionIndex >= 0 && keep; conditionIndex--) {
						keep &= conditionStack.get(conditionIndex).keepRow(row);
						if (!keep) {
							break;
						}
					}
					if (keep) {
						selectedIndices.add(parentRowIndex);
					}
					break;
				case OR:
					for (int conditionIndex = conditionStack.size() - 1; conditionIndex >= 0; conditionIndex--) {
						if (conditionStack.get(conditionIndex).keepRow(row)) {
							selectedIndices.add(parentRowIndex);
							break;
						}
					}
					break;
			}
			parentRowIndex++;
		}
		return selectedIndices;
	}

	/*
	 * Listener Methods
	 */
	public void addDataTableFilteredListener(DataTableFilteredListener listener) {
		this.listeners.add(listener);
	}

	public void removeDataTableFilteredListewner(DataTableFilteredListener listener) {
		this.listeners.remove(listener);
	}

	private void informDataTableFilteredListener() {
		for (DataTableFilteredListener listener : listeners) {
			listener.informDataTableChange(this);
		}
	}

	@Override
	public void dataTableUpdated(DataTable source) {
		setSelectedIndices(updateSelection());
	}
}
