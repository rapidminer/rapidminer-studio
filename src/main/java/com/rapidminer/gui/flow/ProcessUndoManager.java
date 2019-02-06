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
package com.rapidminer.gui.flow;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.tools.container.Triple;

import java.util.LinkedList;


/**
 * Handles the undo system for the current {@link MainFrame} {@link Process}. Operations are
 * <i>not</i> synchronized.
 * 
 * @author Marco Boeck
 * @deprecated since 7.5
 * @see com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel ProcessRendererModel
 * @see NewProcessUndoManager
 */
@Deprecated
public class ProcessUndoManager {

	private final LinkedList<Triple<String, OperatorChain, Operator>> undoList;

	/**
	 * Standard constructor.
	 */
	public ProcessUndoManager() {
		undoList = new LinkedList<Triple<String, OperatorChain, Operator>>();
	}

	/**
	 * Resets the undo list and discards all stored entries.
	 */
	public void reset() {
		undoList.clear();
	}

	/**
	 * Gets the number of undos currently stored.
	 * 
	 * @return
	 */
	public int getNumberOfUndos() {
		return undoList.size();
	}

	/**
	 * Gets the undo step with the given index or <code>null</code>.
	 * 
	 * @param index
	 * @return
	 */
	public String getXml(int index) {
		try {
			return undoList.get(index).getFirst();
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Gets the {@link OperatorChain} associated with the undo step with the given index or
	 * <code>null</code>. If the index is >= {@link #getNumberOfUndos()}, will return the last item.
	 * If the index is < 0, will return the first item.
	 * 
	 * @param index
	 * @return
	 */
	public OperatorChain getOperatorChain(int index) {
		if (index >= getNumberOfUndos()) {
			index = getNumberOfUndos() - 1;
		}
		if (index < 0) {
			index = 0;
		}
		try {
			return undoList.get(index).getSecond();
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Gets the selected {@link Operator} associated with the undo step with the given index or
	 * <code>null</code>. If the index is >= {@link #getNumberOfUndos()}, will return the last item.
	 * If the index is < 0, will return the first item.
	 * 
	 * @param index
	 * @return
	 */
	public Operator getSelectedOperator(int index) {
		if (index >= getNumberOfUndos()) {
			index = getNumberOfUndos() - 1;
		}
		if (index < 0) {
			index = 0;
		}
		try {
			return undoList.get(index).getThird();
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Removes the last undo step. If there is none, does nothing.
	 */
	public void removeLast() {
		if (undoList.size() > 0) {
			undoList.removeLast();
		}
	}

	/**
	 * Removes the first undo step. If there is none, does nothing.
	 */
	public void removeFirst() {
		if (undoList.size() > 0) {
			undoList.removeFirst();
		}
	}

	/**
	 * Adds an undo step.
	 * 
	 * @param processXml
	 * @param currentlyShownOperatorChain
	 * @param selectedOperator
	 */
	public void add(String processXml, OperatorChain currentlyShownOperatorChain, Operator selectedOperator) {
		if (processXml == null) {
			throw new IllegalArgumentException("processXml must not be null!");
		}
		undoList.add(new Triple<String, OperatorChain, Operator>(processXml, currentlyShownOperatorChain, selectedOperator));
	}
}
