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
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.properties.OperatorPropertyPanel;
import com.rapidminer.gui.tour.AddBreakpointStep.Position;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;

import java.awt.Window;


/**
 * This subclass of {@link Step} will open a {@link BubbleWindow} which closes if a breakpoint on
 * the given {@link Operator} is removed.
 * 
 * @author Kersting and Thilo Kamradt
 * 
 */

public class RemoveBreakpointStep extends Step {

	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private String i18nKey;
	private BubbleType element;
	private Class<? extends Operator> operatorClass;
	private String attachToKey = "breakpoint_after";
	private Position positionOnOperator = Position.DONT_CARE;
	private ProcessSetupListener listener = null;
	private String dockableKey = OperatorPropertyPanel.PROPERTY_EDITOR_DOCK_KEY;

	/**
	 * chooses the breakpoint_after-Button as default shoould be used to align to the
	 * breakpoint-button or the operator
	 * 
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param owner
	 *            the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass
	 *            the Class or Superclass of the {@link Operator} to which the breakpoint should be
	 *            added.
	 * @param position
	 *            indicates whether the Step listens to a breakpoint before, after or any breakpoint
	 *            which will be removed from the given operatorClass.
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public RemoveBreakpointStep(BubbleType element, AlignedSide preferredAlignment, String i18nKey,
			Class<? extends Operator> operatorClass, Position position, Object... arguments) {
		super();
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operatorClass;
		this.positionOnOperator = position;
		this.element = element;
	}

	@Override
	boolean createBubble() {
		switch (element) {
			case BUTTON:
				bubble = new ButtonBubble(owner, dockableKey, alignment, i18nKey, attachToKey, false, arguments);
				break;
			case DOCKABLE:
				bubble = new DockableBubble(owner, alignment, i18nKey, dockableKey, arguments);
				break;
			case OPERATOR:
				bubble = new OperatorBubble(owner, alignment, i18nKey, operatorClass, arguments);
		}
		listener = new ProcessSetupListener() {

			@Override
			public void operatorRemoved(Operator operator, int oldIndex, int oldIndexAmongEnabled) {
				// don't care

			}

			@Override
			public void operatorChanged(Operator operator) {
				if (RemoveBreakpointStep.this.operatorClass.isInstance(operator) && !operator.hasBreakpoint()) {
					bubble.triggerFire();
					RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
				}
				if (RemoveBreakpointStep.this.operatorClass.isInstance(operator) && operator.hasBreakpoint()) {
					if (positionOnOperator == Position.BEFORE
							&& !operator.hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)) {
						bubble.triggerFire();
						RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
					} else if (positionOnOperator == Position.AFTER
							&& !operator.hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER)) {
						bubble.triggerFire();
						RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
					}
				}

			}

			@Override
			public void operatorAdded(Operator operator) {
				// don't care
			}

			@Override
			public void executionOrderChanged(ExecutionUnit unit) {
				// don't care
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
		if (this.element == BubbleType.OPERATOR) {
			return new Step[] { new PerspectivesStep(1), new NotShowingStep(ProcessPanel.PROCESS_PANEL_DOCK_KEY) };
		} else {
			return new Step[] { new PerspectivesStep(1), new NotShowingStep(dockableKey),
					new NotViewableStep(alignment, owner, attachToKey, dockableKey) };
		}
	}
}
