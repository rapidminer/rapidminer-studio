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
package com.rapidminer.gui.security;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.processeditor.ExtendedProcessEditor;
import com.rapidminer.gui.tools.MultiSwingWorker;
import com.rapidminer.gui.tools.NotificationPopup;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.OperatorService;

/**
 * This class listens for blacklisted operators constraints. Whenever an operator is added to
 * the current {@link Process}, the operator is checked and if it is blacklisted, a notification is
 * shown.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0
 */
public final class BlacklistedOperatorProcessEditor implements ExtendedProcessEditor {

	/** the icon for the operator blacklisted notification */
	private static final Icon NOTIFICATION_ICON = SwingTools.createIcon("48/" + I18N.getGUIMessage("gui.notification.blacklisted_operator.icon"));

	/** list of already checked operators */
	private final Set<Operator> alreadyCheckedOperators = Collections.synchronizedSet(new HashSet<>());

	@Override
	public void processChanged(final Process process) {
		alreadyCheckedOperators.clear();
		checkConstraint(process);
	}

	@Override
	public void setSelection(final List<Operator> selection) {
		// don't care
	}

	@Override
	public void processViewChanged(Process process) {
		// don't care
	}

	@Override
	public void processUpdated(final Process process) {
		checkConstraint(process);
	}

	/**
	 * Check if the constraint is violated, show notification if it is.
	 */
	private void checkConstraint(Process process) {
		if (!OperatorService.hasBlacklistedOperators() || !RapidMinerGUI.getMainFrame().getProcessPanel().isShowing()) {
			return;
		}
		new MultiSwingWorker<Void, Operator>() {

			@Override
			protected Void doInBackground() {
				// iterate over all operators in the process
				process.getRootOperator().getAllInnerOperators().stream().
						// we only care about enabled operators
						filter(Operator::isEnabled).
						// do only check operators that have not yet been checked
						filter(alreadyCheckedOperators::add).
						// check if operator is blacklisted
						filter(this::isBlacklisted).forEach(this::publish);
				return null;
			}

			/**
			 * Check if an operator is blacklisted
			 *
			 * @param operator
			 * 		the operator to check
			 * @return {@code true} if the operator is blacklisted
			 */
			private boolean isBlacklisted(Operator operator) {
				return OperatorService.isOperatorBlacklisted(operator.getOperatorDescription().getKey());
			}

			@Override
			protected void process(List<Operator> chunks) {
				chunks.forEach(BlacklistedOperatorProcessEditor.this::showNotification);
			}
		}.start();
	}


	/**
	 * Shows the notification popup that this operator is blacklisted by the administrator
	 *
	 * @param op the blacklisted operator
	 */
	private void showNotification(final Operator op) {
		// setup notification
		JPanel notificationPanel = new JPanel();
		notificationPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 3;
		gbc.insets = new Insets(0, 10, 0, 10);

		JLabel iconLabel = new JLabel(NOTIFICATION_ICON);
		notificationPanel.add(iconLabel, gbc);

		JLabel notificationLabel = new JLabel(I18N.getGUIMessage("gui.notification.blacklisted_operator.label"));
		gbc.gridheight = 1;
		gbc.gridx += 1;
		gbc.insets = new Insets(10, 0, 0, 10);
		notificationPanel.add(notificationLabel, gbc);
		notificationLabel.setVerticalAlignment(JLabel.BOTTOM);

		LinkLocalButton linkButton = new LinkLocalButton(new ResourceAction("show_offending_operator", op.getName()) {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				MainFrame mainFrame = RapidMinerGUI.getMainFrame();
				mainFrame.selectAndShowOperator(mainFrame.getProcess().getOperator(op.getName()), true);
			}
		});
		gbc.gridy += 1;
		gbc.insets = new Insets(0, 0, 0, 10);
		gbc.anchor = GridBagConstraints.NORTHWEST;
		notificationPanel.add(linkButton, gbc);

		JButton closeButton = new JButton(new ResourceAction("close_blacklisted_operator_notification") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				// do nothing
			}
		});
		// close on click
		closeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				Arrays.asList(notificationPanel.getMouseListeners()).forEach(l -> l.mousePressed(e));
			}
		});
		closeButton.setFont(closeButton.getFont().deriveFont(Font.BOLD));
		gbc.gridy += 1;
		gbc.insets = new Insets(0, 0, 10, 10);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.weightx = 1.0;
		notificationPanel.add(closeButton, gbc);

		NotificationPopup.showFadingPopup(notificationPanel, RapidMinerGUI.getMainFrame().getProcessPanel()
						.getProcessRenderer(), NotificationPopup.PopupLocation.LOWER_RIGHT, (int) TimeUnit.MINUTES.toMillis(5), 45, 30,
				BorderFactory.createLineBorder(Color.GRAY, 1, false));
	}
}
