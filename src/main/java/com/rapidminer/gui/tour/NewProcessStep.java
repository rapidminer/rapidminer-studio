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

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.operator.Operator;

import java.awt.Window;
import java.util.List;


/**
 * This Step creates a Bubble which appears at the New-Process-Button. The Bubble will disappears if
 * the Process is empty.
 * 
 * @author kamradt
 * 
 */
public class NewProcessStep extends Step {

	private String buttonKey = "new";
	private String i18nKey;
	private Object[] arguments;
	private String docKey = null;
	private Window owner = RapidMinerGUI.getMainFrame();
	private ProcessEditor myProcessEditor;

	public NewProcessStep(String i18nKey, Object... arguments) {
		this.i18nKey = i18nKey;
		this.arguments = arguments;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#createBubble()
	 */
	@Override
	boolean createBubble() {
		bubble = new ButtonBubble(owner, docKey, AlignedSide.BOTTOM, i18nKey, buttonKey, false, false, arguments);
		// add listener
		myProcessEditor = new ProcessEditor() {

			@Override
			public void setSelection(List<Operator> selection) {
				if (RapidMinerGUI.getMainFrame().getProcess().getAllOperators().size() == 1) {
					NewProcessStep.this.bubble.triggerFire();
					RapidMinerGUI.getMainFrame().removeProcessEditor(this);
				}
			}

			@Override
			public void processUpdated(Process process) {
				if (RapidMinerGUI.getMainFrame().getProcess().getAllOperators().size() == 1) {
					NewProcessStep.this.bubble.triggerFire();
					RapidMinerGUI.getMainFrame().removeProcessEditor(this);
				}
			}

			@Override
			public void processChanged(Process process) {
				if (RapidMinerGUI.getMainFrame().getProcess().getAllOperators().size() == 1) {
					NewProcessStep.this.bubble.triggerFire();
					RapidMinerGUI.getMainFrame().removeProcessEditor(this);
				}
			}
		};
		RapidMinerGUI.getMainFrame().addProcessEditor(myProcessEditor);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#stepCanceled()
	 */
	@Override
	protected void stepCanceled() {
		RapidMinerGUI.getMainFrame().removeProcessEditor(myProcessEditor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#getPreconditions()
	 */
	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}

}
