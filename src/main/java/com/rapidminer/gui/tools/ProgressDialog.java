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

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.HeadlessException;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * A (modal) progress monitor dialog which is also able to show state texts and also provides an
 * interemediate mode.
 * 
 * @author Santhosh Kumar, Ingo Mierswa
 */
public class ProgressDialog extends JDialog implements ChangeListener {

	private static final long serialVersionUID = -8792339176006884719L;

	private JLabel statusLabel = new JLabel();
	private JProgressBar progressBar;
	private transient ProgressMonitor monitor;

	public ProgressDialog(Frame owner, String title, ProgressMonitor monitor, boolean modal) throws HeadlessException {
		super(owner, title, modal);
		init(monitor);
	}

	public ProgressDialog(Dialog owner, String title, ProgressMonitor monitor, boolean modal) throws HeadlessException {
		super(owner, title, modal);
		init(monitor);
	}

	private void init(ProgressMonitor monitor) {
		this.monitor = monitor;

		progressBar = new JProgressBar(0, monitor.getTotal());
		if (monitor.isIndeterminate()) {
			progressBar.setIndeterminate(true);
		} else {
			progressBar.setValue(monitor.getCurrent());
		}
		statusLabel.setText(monitor.getStatus());

		JPanel contents = (JPanel) getContentPane();
		contents.setBorder(BorderFactory.createEmptyBorder(11, 11, 11, 11));
		contents.add(statusLabel, BorderLayout.NORTH);
		contents.add(progressBar);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		monitor.addChangeListener(this);
	}

	@Override
	public void stateChanged(final ChangeEvent ce) {
		// ensure EDT thread
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					stateChanged(ce);
				}
			});
			return;
		}

		statusLabel.setText(monitor.getStatus());
		if (!monitor.isIndeterminate()) {
			progressBar.setValue(monitor.getCurrent());
		}

		if (monitor.getCurrent() >= monitor.getTotal()) {
			dispose();
		}
	}
}
