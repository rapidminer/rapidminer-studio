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
package com.rapidminer.gui.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;


/**
 * Displays information about all pending {@link ProgressThread}s.
 *
 * @author Simon Fischer, Marco Boeck
 */
public class ProgressThreadDialog extends ButtonDialog {

	private static final long serialVersionUID = 1L;

	/** the singleton instance */
	private static ProgressThreadDialog INSTANCE;

	/** Create dialog in EDT */
	static {
		if (!RapidMiner.getExecutionMode().isHeadless()) {
			SwingTools.invokeLater(new Runnable() {

				@Override
				public void run() {
					INSTANCE = new ProgressThreadDialog();
				}
			});
		}
	}

	/** mapping between ProgressThread and the UI panel for it */
	private static final Map<ProgressThread, ProgressThreadDisplay> MAPPING_PG_TO_UI = new ConcurrentHashMap<>();

	/** the panel displaying the running and waiting threads */
	private JPanel threadPanel;

	/** if true, the user opened the dialog; false otherwise */
	private boolean openedByUser;

	/**
	 * Private default constructor.
	 */
	private ProgressThreadDialog() {
		super(ApplicationFrame.getApplicationFrame(), "progress_dialog", ModalityType.APPLICATION_MODAL, new Object[] {});

		// listener to be notified when the ProgressThread tasks change
		ProgressThread.addProgressThreadStateListener(new ProgressThreadStateListener() {

			@Override
			public void progressThreadStarted(ProgressThread pg) {
				updatePanelInEDT();
				updateUI();
			}

			@Override
			public void progressThreadQueued(ProgressThread pg) {
				updatePanelInEDT();
				updateUI();
			}

			@Override
			public void progressThreadFinished(ProgressThread pg) {
				updatePanelAndRemoveInEDT(pg);
				updateUI();

				MAPPING_PG_TO_UI.remove(pg);
			}

			@Override
			public void progressThreadCancelled(ProgressThread pg) {
				updatePanelAndRemoveInEDT(pg);
				updateUI();

				MAPPING_PG_TO_UI.remove(pg);
			}

			/**
			 * Updates the ProgressThread panel in the EDT.
			 */
			private void updatePanelInEDT() {
				SwingUtilities.invokeLater(() -> updateThreadPanel(false));
			}

			private void updatePanelAndRemoveInEDT(ProgressThread pg) {
				SwingUtilities.invokeLater(() -> {
					updateThreadPanel(false);
					// remove pg here again, otherwise it can happen that it is first removed by progressThreadFinished
					// and then added again by updateThreadPanel, leading to a memory leak
					MAPPING_PG_TO_UI.remove(pg);
				});
			}

		});

		SwingUtilities.invokeLater(ProgressThreadDialog.this::initGUI);
	}

	/**
	 * Inits the GUI.
	 */
	private void initGUI() {
		JPanel outerPanel = new JPanel(new BorderLayout());
		outerPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

		threadPanel = new JPanel(new GridBagLayout());
		threadPanel.setOpaque(true);
		threadPanel.setBackground(Color.WHITE);

		// add thread panel to outer panel
		JScrollPane scrollPane = new ExtendedJScrollPane(threadPanel);
		scrollPane.setBorder(null);
		outerPanel.add(scrollPane, BorderLayout.CENTER);

		setDefaultSize(ButtonDialog.NORMAL);
		layoutDefault(outerPanel, makeCancelButton("hide"));
		setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
		setModalityType(ModalityType.APPLICATION_MODAL);
	}

	/**
	 * Updates the thread display panel which shows running/queued threads.
	 *
	 * @param forceUpdate
	 *            will update even when the dialog is not visible
	 */
	private synchronized void updateThreadPanel(boolean forceUpdate) {
		if (!forceUpdate && !isVisible()) {
			return;
		}

		threadPanel.removeAll();
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.ipady = 10;
		// add currently running tasks
		for (ProgressThread currentThread : ProgressThread.getCurrentThreads()) {
			ProgressThreadDisplay pgPanel = new ProgressThreadDisplay(currentThread, false);
			threadPanel.add(pgPanel, gbc);
			MAPPING_PG_TO_UI.put(currentThread, pgPanel);
			updateProgressMessage(currentThread);
			updateProgress(currentThread);
			gbc.gridy += 1;
		}
		// add pending tasks
		for (ProgressThread queuedThread : ProgressThread.getQueuedThreads()) {
			ProgressThreadDisplay pgPanel = new ProgressThreadDisplay(queuedThread, true);
			threadPanel.add(pgPanel, gbc);
			MAPPING_PG_TO_UI.put(queuedThread, pgPanel);
			gbc.gridy += 1;
		}

		// add filler component
		gbc.gridy += 1;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		threadPanel.add(new JLabel(), gbc);

		threadPanel.revalidate();
		threadPanel.repaint();
	}

	/**
	 * Updates the status bar in the ApplicationFrame and refreshes the dialog if need be.
	 */
	private void updateUI() {
		if (ProgressThread.isEmpty()) {
			// close dialog if not opened by user
			if (!openedByUser) {
				SwingTools.invokeLater(ProgressThreadDialog.this::dispose);
			}

			// hide status bar
			if (ApplicationFrame.getApplicationFrame() != null) {
				ApplicationFrame.getApplicationFrame().getStatusBar().setProgress("", 100, 100);
			}
		} else {
			if (!ProgressThread.isForegroundRunning()) {
				// close dialog if not opened by user
				if (!openedByUser) {
					SwingTools.invokeLater(ProgressThreadDialog.this::dispose);
				}
			}

			Iterator<ProgressThread> iterator = ProgressThread.getCurrentThreads().iterator();
			if (iterator.hasNext()) {
				ProgressThread pg = iterator.next();
				// while there is at least one running, take first one and display in status
				// bar
				if (ApplicationFrame.getApplicationFrame() != null) {

					StatusBar statusBar = ApplicationFrame.getApplicationFrame().getStatusBar();
					if (pg.isIndeterminate()) {
						statusBar.setIndeterminateProgress(pg.getName(), 0, 100);
					} else {
						statusBar.setProgress(pg.getName(), pg.getDisplay().getCompleted(), pg.getDisplay().getTotal());
					}
				}
			} else {
				// no task running, hide status bar
				if (ApplicationFrame.getApplicationFrame() != null) {
					ApplicationFrame.getApplicationFrame().getStatusBar().setProgress("", 100, 100);
				}
			}
		}
	}

	/**
	 * Updates the progress of the specified ProgressThread.
	 *
	 * @param pg
	 */
	public void updateProgress(final ProgressThread pg) {
		final ProgressThreadDisplay ui = MAPPING_PG_TO_UI.get(pg);

		if (ui != null) {
			SwingTools.invokeLater(() -> ui.setProgress(pg.getDisplay().getCompleted()));
		}
		updateUI();
	}

	/**
	 * Updates the progress message of the specified ProgressThread.
	 *
	 * @param pg
	 */
	public void updateProgressMessage(final ProgressThread pg) {
		final ProgressThreadDisplay ui = MAPPING_PG_TO_UI.get(pg);

		if (ui != null) {
			SwingTools.invokeLater(() -> ui.setMessage(pg.getDisplay().getMessage()));
		}
		updateUI();
	}

	/**
	 * Sets the dialog to visible. If this was invoked by a user action, set openedByUser to
	 * <code>true</code>; <code>false</code> otherwise. This needed so that the dialog will not
	 * close itself if opened by the user.
	 *
	 * @param openedByUser
	 * @param visible
	 */
	public void setVisible(boolean openedByUser, boolean visible) {
		this.openedByUser = openedByUser;
		if (visible) {
			updateThreadPanel(true);
			setIconImage(ApplicationFrame.getApplicationFrame().getIconImage());
			setLocationRelativeTo(ApplicationFrame.getApplicationFrame());
		}
		super.setVisible(visible);
	}

	@Override
	public void setVisible(boolean b) {
		setVisible(false, b);
	}

	/**
	 * Singleton access to the {@link ProgressThreadDialog} or {@code null} if running in headless mode
	 */
	public static ProgressThreadDialog getInstance() {
		return INSTANCE;
	}
}
