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
package com.rapidminer.gui.tools;

import com.rapidminer.gui.ApplicationFrame;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Some utils for the creation of a modal progress monitor dialog.
 * 
 * @author Santhosh Kumar, Ingo Mierswa
 */
public class ProgressUtils {

	static class MonitorListener implements ChangeListener, ActionListener {

		private String title;
		private ProgressMonitor monitor;
		private Window owner;
		private Timer timer;
		private boolean modal;

		public MonitorListener(Window owner, String title, ProgressMonitor monitor, boolean modal) {
			this.title = title;
			this.owner = owner;
			if (owner == null) {
				owner = ApplicationFrame.getApplicationFrame();
			}
			this.monitor = monitor;
			this.modal = modal;
		}

		@Override
		public void stateChanged(ChangeEvent ce) {
			ProgressMonitor monitor = (ProgressMonitor) ce.getSource();
			if (monitor.getCurrent() != monitor.getTotal()) {
				if (timer == null) {
					timer = new Timer(monitor.getWaitingTime(), this);
					timer.setRepeats(false);
					timer.start();
				}
			} else {
				if (timer != null && timer.isRunning()) {
					timer.stop();
				}
				monitor.removeChangeListener(this);
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			monitor.removeChangeListener(this);
			ProgressDialog dlg = owner instanceof Frame ? new ProgressDialog((Frame) owner, title, monitor, modal)
					: new ProgressDialog((Dialog) owner, title, monitor, modal);
			dlg.pack();
			dlg.setLocationRelativeTo(null);
			dlg.setVisible(true);
		}
	}

	/**
	 * Create a new (modal) progress monitor dialog. Please note the the value for total (the
	 * maximum number of possible steps) is greater then 0 even for indeterminate progresses. The
	 * value of waitingTime is used before the dialog is actually created and shown.
	 */
	public static ProgressMonitor createProgressMonitor(Component owner, int total, boolean indeterminate,
			int waitingTimeBeforeDialogAppears, boolean modal) {
		return createProgressMonitor(owner, "Progress", total, indeterminate, waitingTimeBeforeDialogAppears, modal);
	}

	/**
	 * Create a new (modal) progress monitor dialog. Please note the the value for total (the
	 * maximum number of possible steps) is greater then 0 even for indeterminate progresses. The
	 * value of waitingTime is used before the dialog is actually created and shown.
	 */
	public static ProgressMonitor createProgressMonitor(Component owner, String title, int total, boolean indeterminate,
			int waitingTimeBeforeDialogAppears, boolean modal) {
		ProgressMonitor monitor = new ProgressMonitor(total, indeterminate, waitingTimeBeforeDialogAppears);
		Window window = owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
		monitor.addChangeListener(new MonitorListener(window, title, monitor, modal));
		return monitor;
	}
}
