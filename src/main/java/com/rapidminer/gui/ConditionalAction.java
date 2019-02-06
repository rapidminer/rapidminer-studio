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
package com.rapidminer.gui;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.SwingUtilities;


/**
 * An action that must be enabled/disabled depending on certain conditions. These conditions can be
 * mandatory, disallowed, or irrelevant. All ConditionalActions created are added to a collection
 * and there status is automatically checked if the condition premises might have changed.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public abstract class ConditionalAction extends LoggedAbstractAction {

	/**
	 *
	 */
	private static final long serialVersionUID = -3581066203343247846L;

	private static final List<WeakReference<ConditionalAction>> ALL_ACTIONS = new LinkedList<>();

	/* The possible states. */
	public static final int DISALLOWED = -1;

	public static final int DONT_CARE = 0;

	public static final int MANDATORY = 1;

	/*
	 * The possible conditions. TODO: Make enum
	 */
	public static final int OPERATOR_SELECTED = 0;

	public static final int OPERATOR_CHAIN_SELECTED = 1;

	public static final int ROOT_SELECTED = 2;

	public static final int SIBLINGS_EXIST = 3;

	public static final int PROCESS_STOPPED = 4;

	public static final int PROCESS_PAUSED = 5;

	public static final int PROCESS_RUNNING = 6;

	public static final int PARENT_ENABLED = 7;

	/** TODO: Unused */
	public static final int EXECUTION_UNIT_SELECTED = 8;

	public static final int EDIT_IN_PROGRESS = 9;

	public static final int PROCESS_IS_ON_REMOTE_REPOSITORY = 10;

	/**
	 * Process is stored and editable
	 */
	public static final int PROCESS_SAVED = 11;

	public static final int PROCESS_RENDERER_IS_VISIBLE = 12;

	public static final int PROCESS_RENDERER_HAS_UNDO_STEPS = 13;

	public static final int PROCESS_RENDERER_HAS_REDO_STEPS = 14;

	public static final int PROCESS_HAS_BREAKPOINTS = 15;

	/**
	 * Process has a repository location
	 * @since 9.0.2
	 */
	public static final int PROCESS_HAS_REPOSITORY_LOCATION = 16;

	public static final int NUMBER_OF_CONDITIONS = 17;

	private final int[] conditions = new int[NUMBER_OF_CONDITIONS];

	private boolean isDisabledDueToFocusLost;

	public ConditionalAction(String name) {
		this(name, null);
	}

	public ConditionalAction(String name, Icon icon) {
		super(name, icon);
		conditions[EDIT_IN_PROGRESS] = DISALLOWED;
		if (SwingUtilities.isEventDispatchThread()) {
			ALL_ACTIONS.add(new WeakReference<>(this));
		} else {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					ALL_ACTIONS.add(new WeakReference<>(ConditionalAction.this));
				}
			});
		}
	}

	/**
	 * @param index
	 *            one out of OPERATOR_SELECTED, OPERATOR_CHAIN_SELECTED, ROOT_SELECTED,
	 *            CLIPBOARD_FILLED, and PROCESS_RUNNING
	 * @param condition
	 *            one out of DISALLOWED, DONT_CARE, and MANDATORY
	 */
	public void setCondition(int index, int condition) {
		conditions[index] = condition;
	}

	/** Updates all actions. */
	public static void updateAll(boolean[] states) {
		Iterator<WeakReference<ConditionalAction>> i = ALL_ACTIONS.iterator();
		while (i.hasNext()) {
			WeakReference<ConditionalAction> ref = i.next();
			ConditionalAction c = ref.get();
			if (c == null) {
				i.remove();
			} else {
				c.update(states);
			}
		}
	}

	/**
	 * Updates an action given the set of states that can be true or false. States refer to
	 * OPERATOR_SELECTED... An action is enabled iff for all states the condition is MANDATORY and
	 * state is true or DISALLOWED and state is false. If for all states the condition is DONT_CARE,
	 * the enabling status of the action is not touched.
	 */
	protected void update(boolean[] state) {
		// if this action is disabled due to a focus loss never change its enabled state here
		if (isDisabledDueToFocusLost) {
			return;
		}

		boolean ok = true;
		boolean ignore = true;
		for (int i = 0; i < conditions.length; i++) {
			if (conditions[i] != DONT_CARE) {
				ignore = false;
				if (((conditions[i] == MANDATORY) && (state[i] == false))
						|| ((conditions[i] == DISALLOWED) && (state[i] == true))) {
					ok = false;
					break;
				}
			}
		}
		if (!ignore) {
			setEnabled(ok);
		}
	}

	public boolean isDisabledDueToFocusLost() {
		return isDisabledDueToFocusLost;
	}

	/**
	 * If set to <code>true</code>, will not enable itself to condition changes.
	 *
	 * @param isDisabledDueToFocusLost
	 */
	public void setDisabledDueToFocusLost(boolean isDisabledDueToFocusLost) {
		this.isDisabledDueToFocusLost = isDisabledDueToFocusLost;
	}
}
