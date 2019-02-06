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
package com.rapidminer.gui.processeditor.results;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.IOObject;
import com.rapidminer.tools.ParameterService;


/**
 * Summarizes the results recent process executions.
 *
 * @author Simon Fischer, Marco Boeck
 *
 */
public class ResultOverview extends JPanel {

	private static final int HISTORY_LENGTH = 20;

	private static final long serialVersionUID = 1L;

	private final LinkedList<ProcessExecutionResultOverview> processOverviews = new LinkedList<ProcessExecutionResultOverview>();

	protected final Action CLEAR_HISTORY_ACTION = new ResourceAction("resulthistory.clear_history") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			Iterator<ProcessExecutionResultOverview> i = processOverviews.iterator();
			while (i.hasNext()) {
				ProcessExecutionResultOverview o = i.next();
				i.remove();
				ResultOverview.this.remove(o);
			}
			ResultOverview.this.repaint();
		}
	};

	private GridBagConstraints gbc;

	public ResultOverview() {
		setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = HISTORY_LENGTH + 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		// add a filler at the bottom so results start at top
		add(new JLabel(), gbc);

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				showContextMenu(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				showContextMenu(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				showContextMenu(e);
			}

			private void showContextMenu(MouseEvent e) {
				if (e.isPopupTrigger()) {
					JPopupMenu m = new JPopupMenu();
					m.add(CLEAR_HISTORY_ACTION);
					m.show(ResultOverview.this, e.getX(), e.getY());
				}
			}
		});

		// reset y grid
		gbc.gridy = 0;
	}

	/**
	 * Adds a new result entry for the results of the given process.
	 *
	 * @param process
	 * @param results
	 * @param statusMessage
	 */
	public void addResults(Process process, List<IOObject> results, String statusMessage) {
		if (process.getProcessState() != Process.PROCESS_STATE_PAUSED
				|| "true".equals(ParameterService
						.getParameterValue(RapidMinerGUI.PROPERTY_ADD_BREAKPOINT_RESULTS_TO_HISTORY))) {
			final ProcessExecutionResultOverview newOverview = new ProcessExecutionResultOverview(this, process, results,
					statusMessage);

			gbc.weightx = 1.0;
			gbc.weighty = 0.0;
			gbc.insets = new Insets(10, 10, 10, 10);
			gbc.fill = GridBagConstraints.HORIZONTAL;

			// Swing calls need to be done in EDT to avoid freezing up of the Result Overview
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					processOverviews.add(newOverview);
					gbc.gridy += 1;
					add(newOverview, gbc);

					while (processOverviews.size() > HISTORY_LENGTH) {
						ProcessExecutionResultOverview first = processOverviews.removeFirst();
						remove(first);
					}
				}

			});
		}
	}

	/**
	 * Remove the given result from the overview.
	 * 
	 * @param processExecutionResultOverview
	 */
	void removeProcessOverview(ProcessExecutionResultOverview processExecutionResultOverview) {
		remove(processExecutionResultOverview);
		processOverviews.remove(processExecutionResultOverview);

		revalidate();
		repaint();
	}
}
