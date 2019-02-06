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

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.io.process.ProcessLayoutXMLFilter;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.tools.XMLException;


/**
 * Handles the list of undo states for the current {@link Process} in the {@link MainFrame}.
 * Operations are <i>not</i> synchronized. Is a repurposed class of an older implementation
 * ({@link ProcessUndoManager}).
 *
 * @author Jan Czogalla
 * @since 7.5
 */
public class NewProcessUndoManager {

	/** Regex to filter operator's height attribute from XML */
	private static final Pattern REGEX_OPERATOR_PATTERN = Pattern.compile("(?i)(<operator.*?)(height=\"\\d+\"\\s)");
	private static final String REGEX_OPERATOR_REPLACEMENT = "$1";

	/** Comparator for process XMLs, ignoring height attributes of operators */
	private static final Comparator<String> XML_COMPARE_WIHOUT_HEIGHT = (a, b) -> {
		a = REGEX_OPERATOR_PATTERN.matcher(a).replaceAll(REGEX_OPERATOR_REPLACEMENT);
		b = REGEX_OPERATOR_PATTERN.matcher(b).replaceAll(REGEX_OPERATOR_REPLACEMENT);
		return a.compareTo(b);
	};

	/**
	 * Simple storage construct for a {@link Process} state.
	 *
	 * @author Jan Czogalla
	 * @since 7.5
	 */
	private static class ProcessUndoState {

		private String processXML;
		private String displayedChain;
		private List<String> selectedOperators;
		private List<String> viewUserData;

	}

	private final List<ProcessUndoState> undoList = new ArrayList<>();
	private ProcessUndoState lastSnapshot;
	private ProcessUndoState snapshot;

	/** Resets the list of undo states, removes all stored states. */
	public void reset() {
		undoList.clear();
		clearSnapshot();
	}

	/** Returns the number of currently stored states. */
	public int getNumberOfUndos() {
		return undoList.size();
	}

	/**
	 * Returns the XML string representing the {@link Process} at the given undo index.
	 *
	 * @param index
	 * @return the XML or {@code null} if no undo step existed for the given index
	 */
	public String getXML(int index) {
		try {
			return undoList.get(index).processXML;
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/** Removes the last undo step. If there is none, does nothing. */
	public void removeLast() {
		if (undoList.isEmpty()) {
			return;
		}
		undoList.remove(undoList.size() - 1);
	}

	/** Removes the first undo step. If there is none, does nothing. */
	public void removeFirst() {
		if (undoList.isEmpty()) {
			return;
		}
		undoList.remove(0);
	}

	/** Returns whether the process has changed between snapshots. */
	public boolean snapshotDiffers() {
		ProcessUndoState last = lastSnapshot;
		ProcessUndoState current = snapshot;
		return last != null && last != current
				&& XML_COMPARE_WIHOUT_HEIGHT.compare(last.processXML, current.processXML) != 0;
	}

	/**
	 * Adds an undo step that was previously created by
	 * {@link #takeSnapshot(String, OperatorChain, Collection, Collection)}. Will add the current
	 * snapshot if so indicated; usually, the current snapshot will be used when a view change
	 * occurred,and the last snapshot if the XML of the process changed in some way
	 *
	 * @param useCurrent
	 *            indicates whether the current or last snapshot should be used
	 * @return if the state was actually added
	 * @see ProcessRendererModel#addToUndoList(boolean)
	 */
	public boolean add(boolean useCurrent) {
		ProcessUndoState state = useCurrent ? snapshot : lastSnapshot;
		if (state != null && state.processXML != null) {
			undoList.add(state);
			return true;
		}
		return false;
	}

	/**
	 * Takes a snapshot, consisting of the (non-null) {@link Process} XML, the currently displayed
	 * {@link OperatorChain}, the list of currently selected {@link Operator Operators} and a list
	 * of all {@link Operator Operators} present in the process.
	 */
	public void takeSnapshot(String processXML, OperatorChain displayedChain, Collection<Operator> selectedOperators,
			Collection<Operator> allOperators) {
		if (processXML == null) {
			throw new IllegalArgumentException("processXML must not be null!");
		}
		ProcessUndoState state = new ProcessUndoState();
		state.processXML = processXML;
		state.displayedChain = displayedChain == null ? null : displayedChain.getName();
		state.selectedOperators = selectedOperators.stream().map(Operator::getName).collect(Collectors.toList());
		state.viewUserData = extractUserData(allOperators);
		lastSnapshot = snapshot;
		snapshot = state;
	}

	/** Clears both tiers of snapshots. */
	public void clearSnapshot() {
		snapshot = lastSnapshot = null;
	}

	/**
	 * Restores a {@link Process} from the given undo index if possible, setting the user data from
	 * the stored state. May skip unreadable user data. Throws an Exception if an error occurs while
	 * parsing the XML string ({@link Process#Process(String)}).
	 *
	 * @return the restored process or null if the index is invalid
	 *
	 */
	public Process restoreProcess(int index) throws IOException, XMLException {
		ProcessUndoState state;
		try {
			state = undoList.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
		Process p = new Process(state.processXML);
		if (state.viewUserData == null) {
			return p;
		}
		restoreUserData(state, p);
		return p;
	}

	/**
	 * Restores the viewed {@link OperatorChain} from the given undo index and {@link Process}. Will
	 * return null if the index is invalid or the process does not contain the stored chain.
	 */
	public OperatorChain restoreDisplayedChain(Process p, int index) {
		ProcessUndoState state;
		try {
			state = undoList.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
		restoreUserData(state, p);
		return (OperatorChain) p.getOperator(state.displayedChain);
	}

	/**
	 * Restores the list of {@link Operator Operators} from the given undo index and
	 * {@link Process}. Will return null if the index is invalid or no operator from the stored
	 * state is present in the process.
	 */
	public List<Operator> restoreSelectedOperators(Process p, int index) {
		ProcessUndoState state;
		try {
			state = undoList.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
		List<Operator> selected = state.selectedOperators.stream().map(p::getOperator).filter(Objects::nonNull).collect(Collectors.toList());
		return selected.isEmpty() ? null : selected;
	}

	/** Restores the user data from the given state in the given {@link Process}. */
	private void restoreUserData(ProcessUndoState state, Process p) {
		if (state.viewUserData == null) {
			return;
		}
		state.viewUserData.forEach(vud -> setUserData(p, vud));
	}

	/** Set the user data encoded by the given string. */
	private void setUserData(Process p, String vud) {
		try {
			String[] vudSplit = vud.split(" ", 3);
			OperatorChain opChain = (OperatorChain) p.getOperator(vudSplit[2]);
			if (opChain == null) {
				return;
			}
			Object ud = userDataFrom(vudSplit[0]);
			if (vudSplit[1].equals(ProcessLayoutXMLFilter.KEY_OPERATOR_CHAIN_POSITION)) {
				ProcessLayoutXMLFilter.setOperatorChainPosition(opChain, (Point) ud);
			} else {
				ProcessLayoutXMLFilter.setOperatorChainZoom(opChain, (Double) ud);
			}
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * Creates the object represented by the given String. Will be either a {@link Point} or a
	 * {@link Double}
	 */
	private Object userDataFrom(String string) {
		if (string.contains(",")) {
			String[] point = string.split(",");
			return new Point(Integer.parseInt(point[0]), Integer.parseInt(point[1]));
		}
		return Double.parseDouble(string);
	}

	/**
	 * Extracts relevant view user data from the {@link OperatorChain OperatorChains} from the given
	 * list. Returns a list of the form "value key name".
	 */
	private List<String> extractUserData(Collection<Operator> allOperators) {
		List<String> userData = new ArrayList<>();
		for (Operator op : allOperators) {
			if (!(op instanceof OperatorChain)) {
				continue;
			}
			OperatorChain opChain = (OperatorChain) op;
			String name = opChain.getName();
			Point position = ProcessLayoutXMLFilter.lookupOperatorChainPosition(opChain);
			if (position != null) {
				userData.add(position.x + "," + position.y + " " + ProcessLayoutXMLFilter.KEY_OPERATOR_CHAIN_POSITION + " "
						+ name);
			}
			Double zoom = ProcessLayoutXMLFilter.lookupOperatorChainZoom(opChain);
			if (zoom != null) {
				userData.add(zoom + " " + ProcessLayoutXMLFilter.KEY_OPERATOR_CHAIN_ZOOM + " " + name);
			}

		}
		return userData.isEmpty() ? null : userData;
	}

}
