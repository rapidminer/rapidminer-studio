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

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.ProcessInteractionListener;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.Port;

import java.awt.Window;

import javax.swing.JPopupMenu;


/**
 * This subclass of {@link Step} will be closed if the a Subprocess was opened.
 * 
 * @author Kersting and Thilo Kamradt
 * 
 */

public class OpenSubprocessStep extends Step {

	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private String i18nKey;
	private Class<? extends OperatorChain> operatorClass;
	private ProcessInteractionListener listener = null;
	private BubbleType element;
	private String dockableKey = ProcessPanel.PROCESS_PANEL_DOCK_KEY;

	/**
	 * should be used to align to the operator which can be entered or the {@link ProcessPanel}
	 * 
	 * @param element
	 *            indicates to which element the Bubble will be aligned (BubbleTo.BUTTON will throw
	 *            an IllegalArgumentException)
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param operator
	 *            the class of the Operator which the user should enter.
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public OpenSubprocessStep(BubbleType element, AlignedSide preferredAlignment, String i18nKey,
			Class<? extends OperatorChain> operator, Object... arguments) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operator;
		this.element = element;
		this.arguments = arguments;
		if (element == BubbleType.BUTTON) {
			throw new IllegalArgumentException("can not align to a button for entering a subprocess");
		}
	}

	@Override
	boolean createBubble() {
		switch (element) {
			case DOCKABLE:
				bubble = new DockableBubble(owner, alignment, i18nKey, dockableKey, arguments);
				break;
			case OPERATOR:
				bubble = new OperatorBubble(owner, alignment, i18nKey, operatorClass, arguments);
				break;
			default:
				throw new IllegalArgumentException("can only align to Dockable or Operator");
		}

		listener = new ProcessInteractionListener() {

			@Override
			public void portContextMenuWillOpen(JPopupMenu menu, Port port) {
				// do not care

			}

			@Override
			public void operatorMoved(Operator op) {
				// do not care

			}

			@Override
			public void operatorContextMenuWillOpen(JPopupMenu menu, Operator operator) {
				// do not care

			}

			@Override
			public void displayedChainChanged(OperatorChain displayedChain) {
				if (displayedChain != null
						&& (OpenSubprocessStep.this.operatorClass == null || displayedChain.getClass().equals(
								OpenSubprocessStep.this.operatorClass))) {
					bubble.triggerFire();
					RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer()
							.removeProcessInteractionListener(this);
				}
			}
		};
		RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().addProcessInteractionListener(listener);
		return true;
	}

	@Override
	protected void stepCanceled() {
		if (listener != null) {
			RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().removeProcessInteractionListener(listener);
		}
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] { new PerspectivesStep(1), new NotShowingStep(dockableKey) };
	}
}
