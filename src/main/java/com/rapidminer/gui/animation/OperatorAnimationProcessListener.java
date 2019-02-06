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
package com.rapidminer.gui.animation;

import com.rapidminer.Process;
import com.rapidminer.gui.GeneralProcessListener;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.operator.Operator;


/**
 * {@link GeneralProcessListener} that ensures that {@link Animation}s for {@link Operator}s are
 * added to the {@link ProcessAnimationManager} when the operator starts and removed if the operator
 * or the process finished. Also ensures that a {@link AnimationTimerProcessLister} is associated to
 * a running process.
 *
 * @author Gisa Schaefer
 * @since 7.1.0
 */
public class OperatorAnimationProcessListener extends GeneralProcessListener {

	private AnimationTimerProcessListener timerListener;

	public OperatorAnimationProcessListener(MainFrame mainFrame) {
		super(mainFrame);
	}

	public OperatorAnimationProcessListener(ProcessRendererModel processModel) {
		super(processModel);
	}

	@Override
	public void processStarts(Process process) {
		if (timerListener == null) {
			timerListener = new AnimationTimerProcessListener();
		}
		process.addProcessStateListener(timerListener);
		// need to start timerListener here since it is added after the event
		timerListener.startTimer();
	}

	@Override
	public void processStartedOperator(Process process, Operator op) {
		ProcessAnimationManager.INSTANCE.addAnimationForOperator(op);
	}

	@Override
	public void processFinishedOperator(Process process, Operator op) {
		if (!op.isAnimating()) {
			ProcessAnimationManager.INSTANCE.removeAnimationForOperator(op);
		}
	}

	@Override
	public void processEnded(Process process) {
		// need to remove operators here in case the process was stopped
		for (Operator operator : process.getAllOperators()) {
			ProcessAnimationManager.INSTANCE.removeAnimationForOperator(operator);
		}

		// need to stop the timer here since only processEnded tells if every operator is done
		timerListener.stopTimer();
		process.removeProcessStateListener(timerListener);

		// needed to remove the OperatorAnimation when the process ended via checkForStop
		RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().getModel().fireMiscChanged();
	}

}
