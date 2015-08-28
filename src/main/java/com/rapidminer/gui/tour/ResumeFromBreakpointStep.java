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
import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.OperatorService;

import java.awt.Window;


/**
 * This Subclass of {@link Step} will open a {@link BubbleWindow} when a Process reaches a
 * breakpoint and closes if the user resume the Process.
 * 
 * @author Philipp Kersting and Thilo Kamradt
 * 
 */

public class ResumeFromBreakpointStep extends Step {

	private String i18nKey;
	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private AddBreakpointStep.Position position;
	private Class<? extends Operator> operatorClass;
	private BubbleType element = BubbleType.BUTTON;
	private String elementKey = "run";
	private BreakpointListener listener = null;
	private boolean reachedBreakpoint = false;

	/**
	 * Bubble which will attach to the run button in the head menu
	 * 
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass
	 *            Class or Superclass of the {@link Operator} which owns the breakpoint.
	 * @param position
	 *            indicates to which position of a breakpoint the {@link Step} listens.
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public ResumeFromBreakpointStep(AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass,
			AddBreakpointStep.Position position, Object... arguments) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operatorClass;
		this.position = position;
		this.arguments = arguments;
	}

	/**
	 * Bubble which will attach to an element of the given Type with the given key
	 * 
	 * @param element
	 *            decides to which kind of Object the Bubble will be attached to
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param owner
	 *            the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass
	 *            Class or Superclass of the {@link Operator} which owns the breakpoint.
	 * @param elementKey
	 *            key of the element you want to attach to (can be null if element = Operator, in
	 *            this case the first Operator with the given Operator-Class will be attached)
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public ResumeFromBreakpointStep(BubbleType element, AlignedSide preferredAlignment, String i18nKey,
			Class<? extends Operator> operatorClass, String elementKey, AddBreakpointStep.Position position, Window owner,
			Object... arguments) {
		this.alignment = preferredAlignment;
		this.arguments = arguments;
		this.i18nKey = i18nKey;
		this.operatorClass = operatorClass;
		this.position = position;
		this.owner = owner;

	}

	@Override
	boolean createBubble() {
		switch (element) {
			case BUTTON:
				bubble = new ButtonBubble(owner, null, alignment, i18nKey, elementKey, false, false, arguments);
				break;
			case DOCKABLE:
				bubble = new DockableBubble(owner, alignment, i18nKey, elementKey, arguments);
				break;
			case OPERATOR:
				if (elementKey == null) {
					bubble = new OperatorBubble(owner, alignment, i18nKey, operatorClass, arguments);
				} else {
					bubble = new OperatorBubble(owner, alignment, i18nKey, OperatorService
							.getOperatorDescription(elementKey).getOperatorClass(), arguments);
				}
				break;
		}
		listener = new BreakpointListener() {

			@Override
			public void resume() {
				if (reachedBreakpoint) {
					ResumeFromBreakpointStep.this.conditionComplied();
				}
			}

			@Override
			public void breakpointReached(Process process, Operator op, IOContainer iocontainer, int location) {
				if (operatorClass.isInstance(op)
						&& ((location == 1 && position == AddBreakpointStep.Position.AFTER)
								|| (location == 0 && position == AddBreakpointStep.Position.BEFORE) || (position == AddBreakpointStep.Position.DONT_CARE))) {
					reachedBreakpoint = true;
					bubble.setVisible(true);
				}

			}
		};
		RapidMinerGUI.getMainFrame().getProcess().addBreakpointListener(listener);
		return true;
	}

	@Override
	protected void stepCanceled() {
		if (listener != null) {
			RapidMinerGUI.getMainFrame().getProcess().removeBreakpointListener(listener);
		}
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}

}
