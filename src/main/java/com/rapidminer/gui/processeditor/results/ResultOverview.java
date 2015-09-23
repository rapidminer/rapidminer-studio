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
package com.rapidminer.gui.processeditor.results;

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.IOObject;
import com.rapidminer.tools.ParameterService;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;


/**
 * Summarizes the results recent process executions.
 * 
 * @author Simon Fischer
 * 
 */
public class ResultOverview extends JPanel {

	private static final int HISTORY_LENGTH = 50;

	private static final long serialVersionUID = 1L;

	private final LinkedList<ProcessExecutionResultOverview> processOverviews = new LinkedList<ProcessExecutionResultOverview>();

	protected final Action CLEAR_HISTORY_ACTION = new ResourceAction("resulthistory.clear_history") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			Iterator<ProcessExecutionResultOverview> i = processOverviews.iterator();
			while (i.hasNext()) {
				ProcessExecutionResultOverview o = i.next();
				i.remove();
				ResultOverview.this.remove(o);
			}
			ResultOverview.this.repaint();
		}
	};

	public ResultOverview() {
		setLayout(null);
		setBackground(Color.WHITE);
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
	}

	@Override
	public void doLayout() {
		int y = 0;
		for (ProcessExecutionResultOverview overview : processOverviews) {
			overview.recomputeLayout();
			overview.setBounds(0, y, (int) overview.getPreferredSize().getWidth(), (int) overview.getPreferredSize()
					.getHeight());
			y += overview.getPreferredSize().getHeight();
		}
		if (y != getHeight()) {
			Dimension total = new Dimension(getWidth(), y);
			setPreferredSize(total);
			setMaximumSize(total);
			setMinimumSize(total);
		}
		getParent().doLayout();
	}

	public void addResults(Process process, List<IOObject> results, String statusMessage) {
		if (process.getProcessState() != Process.PROCESS_STATE_PAUSED
				|| "true".equals(ParameterService
						.getParameterValue(RapidMinerGUI.PROPERTY_ADD_BREAKPOINT_RESULTS_TO_HISTORY))) {
			final ProcessExecutionResultOverview newOverview = new ProcessExecutionResultOverview(this, process, results,
					statusMessage);
			// Swing calls need to be done in EDT to avoid freezing up of the Result Overview
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					processOverviews.add(newOverview);
					add(newOverview);

					while (processOverviews.size() > HISTORY_LENGTH) {
						ProcessExecutionResultOverview first = processOverviews.removeFirst();
						remove(first);
					}
				}

			});
		}
	}

	public void removeProcessOverview(ProcessExecutionResultOverview processExecutionResultOverview) {
		remove(processExecutionResultOverview);
		processOverviews.remove(processExecutionResultOverview);
		doLayout();
		repaint();
	}
}
