/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tour;

import com.rapidminer.BreakpointListener;
import com.rapidminer.ProcessSetupListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.properties.OperatorPropertyPanel;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;

import java.awt.Window;


/**
 * 
 * This subclass of {@link Step} will open a {@link BubbleWindow} which closes if a Breakpoint is
 * set.
 * 
 * @author Philipp Kersting and Thilo Kamradt
 * 
 */

public class AddBreakpointStep extends Step {

	/** indicates on which position of a Breakpoint */
	public enum Position {
		BEFORE, AFTER, DONT_CARE
	}

	private Class<? extends Operator> operator;
	private String i18nKey;
	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private Position position;
	private String elementKey = "breakpoint_after";
	private ProcessSetupListener listener = null;
	private BubbleType element;
	private final String dockableKey = OperatorPropertyPanel.PROPERTY_EDITOR_DOCK_KEY;

	/**
	 * creates a Bubble which aligns to the addBreakpoint button
	 * 
	 * @param element
	 *            decides to which Component this Step will point
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param operator
	 *            the class of the Operator at which the Breakpoint should be added
	 * @param position
	 *            position on which the Breakpoint should be set.
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public AddBreakpointStep(BubbleType element, AlignedSide preferredAlignment, String i18nKey,
			Class<? extends Operator> operator, Position position, Object... arguments) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.position = position;
		this.operator = operator;
		this.element = element;
		this.arguments = arguments;
	}

	@Override
	boolean createBubble() {
		switch (element) {
			case BUTTON:
				bubble = new ButtonBubble(owner, dockableKey, alignment, i18nKey, elementKey, false, arguments);
				break;
			case DOCKABLE:
				bubble = new DockableBubble(owner, alignment, i18nKey, dockableKey, arguments);
				break;
			case OPERATOR:
				bubble = new OperatorBubble(owner, alignment, i18nKey, operator, arguments);
		}
		listener = new ProcessSetupListener() {

			@Override
			public void operatorRemoved(Operator operator, int oldIndex, int oldIndexAmongEnabled) {
				// do not care

			}

			@Override
			public void operatorChanged(Operator operator) {
				if (AddBreakpointStep.this.operator.isInstance(operator) && operator.hasBreakpoint()) {
					if (position == Position.BEFORE && operator.hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)) {
						conditionComplied();
					} else if (position == Position.AFTER && operator.hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER)) {
						conditionComplied();
					} else if (position == Position.DONT_CARE) {
						conditionComplied();
					}
				}

			}

			@Override
			public void operatorAdded(Operator operator) {
				// do not care

			}

			@Override
			public void executionOrderChanged(ExecutionUnit unit) {
				// do not care
			}
		};
		RapidMinerGUI.getMainFrame().getProcess().addProcessSetupListener(listener);
		return true;
	}

	@Override
	protected void stepCanceled() {
		if (listener != null) {
			RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(listener);
		}
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] { new PerspectivesStep(1), new NotShowingStep(dockableKey),
				new NotViewableStep(alignment, owner, elementKey, dockableKey) };
	}
}
