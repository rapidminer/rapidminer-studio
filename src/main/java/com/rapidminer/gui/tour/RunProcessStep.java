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
import com.rapidminer.ProcessListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.operator.Operator;

import java.awt.Window;


/**
 * @author kamradt
 * 
 */
public class RunProcessStep extends Step {

	private final String buttonKey = "run";
	private final String i18nKey;
	private final Object[] arguments;
	private final String docKey = null;
	private final Window owner = RapidMinerGUI.getMainFrame();
	private ProcessListener processListener;

	public RunProcessStep(final String i18nKey, final Object... arguments) {
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
		// If this is called multiple times, kill any previous bubbles
		if (bubble != null) {
			bubble.fireEventCloseClicked();
		}
		bubble = new ButtonBubble(owner, docKey, AlignedSide.BOTTOM, i18nKey, buttonKey, false, false, arguments);
		// add listener
		processListener = new ProcessListener() {

			@Override
			public void processStarts(final Process process) {
				bubble.triggerFire();
				RunProcessStep.this.stepCanceled();
			}

			@Override
			public void processStartedOperator(final Process process, final Operator op) {
				// nothing to do
			}

			@Override
			public void processFinishedOperator(final Process process, final Operator op) {
				// nothing to do
			}

			@Override
			public void processEnded(final Process process) {
				// nothing to do
			}
		};
		RapidMinerGUI.getMainFrame().getProcess().getRootOperator().addProcessListener(processListener);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#stepCanceled()
	 */
	@Override
	protected void stepCanceled() {
		RapidMinerGUI.getMainFrame().getProcess().getRootOperator().removeProcessListener(processListener);
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
