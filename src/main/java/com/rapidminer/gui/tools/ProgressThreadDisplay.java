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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.rapidminer.tools.I18N;


/**
 * UI component which displays the name, dependencies, progress and a cancel button.
 *
 * @author Marco Boeck
 *
 */
class ProgressThreadDisplay extends JPanel {

	private static final long serialVersionUID = 1L;

	/** dimension of the cancel button */
	private static final Dimension CANCEL_DIMENSION = new Dimension(60, 30);

	/** the {@link ProgressThread} behind this view */
	private ProgressThread pg;

	/** the progress bar */
	private JProgressBar progressBar;

	private boolean isQueued;

	/**
	 * Creates a new {@link ProgressThreadDisplay} instance for the {@link ProgressThread} .
	 *
	 * @param pg
	 */
	public ProgressThreadDisplay(ProgressThread pg, boolean isQueued) {
		this.pg = pg;
		this.isQueued = isQueued;

		initGUI();
	}

	/**
	 * Inits the GUI.
	 */
	private void initGUI() {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 15, 0, 15);
		// add name label (including ID and dependencies)
		String threadLabel = createDisplayLabel();
		JLabel nameLabel = new JLabel(threadLabel);
		add(nameLabel, gbc);

		gbc.gridy += 1;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 15, 0, 5);
		if (pg.isIndeterminate()) {
			progressBar = new JProgressBar();
			progressBar.setIndeterminate(true);
		} else {
			progressBar = new JProgressBar(0, pg.getDisplay().getTotal());
			progressBar.setValue(pg.getDisplay().getCompleted());
		}
		progressBar.setOpaque(false);
		add(progressBar, gbc);

		gbc.gridx += 1;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 5, 0, 15);

		JButton cancelButton = new JButton(new ResourceAction(true, "stop_progress") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				pg.cancel();
			}
		});
		cancelButton.setMinimumSize(CANCEL_DIMENSION);
		cancelButton.setMaximumSize(CANCEL_DIMENSION);
		cancelButton.setPreferredSize(CANCEL_DIMENSION);
		cancelButton.setEnabled(pg.isCancelable());
		add(cancelButton, gbc);

		setBackground(Color.WHITE);
		setOpaque(true);
	}

	/**
	 * Creates a HTML formatted label which contains the name, ID and any dependencies of a
	 * {@link ProgressThread}.
	 *
	 * @return
	 */
	private String createDisplayLabel() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append(pg.getName());
		if (isQueued) {
			sb.append(" (Waiting)");
		}
		sb.append("<br/>");
		if (isQueued) {
			List<String> dependencies = pg.getDependencies();

			if (!dependencies.isEmpty()) {
				ArrayList<ProgressThread> all = new ArrayList<>(ProgressThread.getQueuedThreads());
				all.addAll(ProgressThread.getCurrentThreads());
				Map<String, String> keyToName = new HashMap<>();
				for (ProgressThread progressThread : all) {
					keyToName.put(progressThread.getID(), progressThread.getName());
				}
				sb.append("<font color=\"#6E6E6E\" size=\"-2\">");
				sb.append("Depends on: ");
				for (int i = 0; i < dependencies.size(); i++) {
					String dependency = dependencies.get(i);
					sb.append(keyToName.getOrDefault(dependency, I18N.getGUIMessage("gui.progress." + dependency + ".label")));
					if (i < dependencies.size() - 1) {
						sb.append(", ");
					}
					// add <br/> after every third dependency to avoid too long dependency lists
					if (i % 3 == 0) {
						sb.append("<br/>");
					}
				}
			}
			sb.append("</font>");
		}
		sb.append("</html>");

		return sb.toString();
	}

	/**
	 * Sets the progress of the progess bar.
	 */
	public void setProgress(int progress) {
		// discard updates which would indicate backwards progress
		if (progress > progressBar.getValue()) {
			progressBar.setValue(progress);
		}
	}

	/**
	 * Sets the progress message.
	 *
	 * @param message
	 */
	public void setMessage(String message) {
		if (!progressBar.isStringPainted()) {
			progressBar.setStringPainted(true);
		}
		progressBar.setString(message);
	}

}
