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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import com.rapidminer.Process;
import com.rapidminer.ProcessStateListener;
import com.rapidminer.gui.RapidMinerGUI;


/**
 * A {@link ProcessStateListener} that periodically checks for redraws for animations if the process
 * is running.
 *
 * @author Gisa Schaefer
 * @since 7.1.0
 */
class AnimationTimerProcessListener implements ProcessStateListener {

	/**
	 * The interval for redrawing the animations (in milliseconds).
	 */
	private static final int REPAINT_INTERVAL = 60;

	private Timer timer;

	AnimationTimerProcessListener() {
		ActionListener timerListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (ProcessAnimationManager.INSTANCE.isRepaintRequired()) {
					RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().getModel().fireMiscChanged();
				}
			}
		};
		timer = new Timer(REPAINT_INTERVAL, timerListener);
	}

	@Override
	public void stopped(Process process) {
		// do nothing here, this is called if the process was stopped but an operator is still
		// running
	}

	@Override
	public void started(Process process) {
		startTimer();
	}

	@Override
	public void resumed(Process process) {
		startTimer();
	}

	@Override
	public void paused(Process process) {
		stopTimer();
	}

	/**
	 * Starts the timer.
	 */
	void startTimer() {
		timer.start();
	}

	/**
	 * Stops the timer.
	 */
	void stopTimer() {
		timer.stop();
	}

}
