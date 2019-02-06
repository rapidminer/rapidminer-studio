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

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.components.AbstractLinkButton;
import com.rapidminer.gui.tools.components.LinkRemoteButton;
import com.rapidminer.license.violation.LicenseConstraintViolation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.RMUrlHandler;


/**
 * Banner that informs about the license limit for result displays.
 *
 * @author Marco Boeck, Gisa Schaefer
 */
class ResultLimitPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** URL for documentation article */
	private static final String DOCUMENTATION_LINK = I18N.getGUILabel("result_limit_panel.learn_more.url");

	/** URL for pricing page */
	private static final String LICENSE_LINK = I18N.getGUILabel("result_limit_panel.upgrade.url");

	/** font used for the result limit display */
	private static final Font BOLD_LABEL_FONT = new JLabel().getFont().deriveFont(Font.BOLD);

	/** action to open the {@link #DOCUMENTATION_LINK} */
	private static final ResourceAction DOCUMENTATION_ACTION = new ResourceAction(
			"too_much_data.results_banner.learn_more_limits") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			RMUrlHandler.openInBrowser(DOCUMENTATION_LINK);
		}

	};

	/** opens the pricing page */
	private static final ResourceAction UPGRADE_ACTION = new ResourceAction("too_much_data.results_banner.upgrade_license") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			new ProgressThread("opening_license_page") {

				@Override
				public void run() {
					RMUrlHandler.openInBrowser(LICENSE_LINK);
				}
			}.start();
		}

	};

	static {
		// bold font for link buttons
		DOCUMENTATION_ACTION.putValue(AbstractLinkButton.PROPERTY_BOLD, Boolean.TRUE);
		UPGRADE_ACTION.putValue(AbstractLinkButton.PROPERTY_BOLD, Boolean.TRUE);
	}

	private final int limit;
	private JPanel containerPanel;

	/**
	 * Creates a warning panel that notifies that the license limit was breached.
	 *
	 * @param backgroundColor
	 *            the background color for the panel
	 * @param violation
	 *            the violation that causes the warning panel
	 */
	ResultLimitPanel(Color backgroundColor, LicenseConstraintViolation<Integer, Integer> violation) {
		limit = violation.getConstraintValue();

		initGUI(backgroundColor, violation);
	}

	/**
	 * Initializes the banner panel.
	 */
	private void initGUI(Color backgroundColor, LicenseConstraintViolation<Integer, Integer> violation) {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		containerPanel = new JPanel(new GridBagLayout());
		containerPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Colors.PANEL_BORDER, 1, true), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		containerPanel.setBackground(Colors.WARNING_COLOR);

		ResourceLabel firstLabel = new ResourceLabel("too_much_data.display_license",
				NumberFormat.getIntegerInstance().format(violation.getConstraintValue()));
		firstLabel.setFont(BOLD_LABEL_FONT);

		gbc.gridx = 0;
		gbc.weightx = 0;
		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		containerPanel.add(firstLabel, gbc);

		LinkRemoteButton upgradeButton = new LinkRemoteButton(UPGRADE_ACTION);

		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.gridx += 1;
		containerPanel.add(upgradeButton, gbc);

		ResourceLabel secondLabel = new ResourceLabel("too_much_data.display_leverage");
		secondLabel.setFont(BOLD_LABEL_FONT);
		gbc.gridx += 1;
		containerPanel.add(secondLabel, gbc);

		gbc.gridx += 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.REMAINDER;
		containerPanel.add(new JLabel(), gbc);

		LinkRemoteButton moreButton = new LinkRemoteButton(DOCUMENTATION_ACTION);

		gbc.gridx += 1;
		gbc.anchor = GridBagConstraints.EAST;
		containerPanel.add(moreButton, gbc);

		gbc.gridx = 0;
		gbc.weightx = 1.0;
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		add(containerPanel, gbc);

		setBackground(backgroundColor);
	}

	/**
	 * Changes the text to inform the user that he should reopen the result.
	 *
	 * @param newlicenseLimit
	 *            the new license limit after the license changed
	 */
	void licenseUpdated(Integer newlicenseLimit) {
		if (newlicenseLimit == null || newlicenseLimit > limit) {
			containerPanel.removeAll();
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.weightx = 1.0;
			gbc.insets = new Insets(0, 5, 0, 0);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.WEST;

			ResourceLabel reopenLabel = new ResourceLabel("too_much_data.reopen_dataset");
			reopenLabel.setFont(BOLD_LABEL_FONT);
			containerPanel.add(reopenLabel, gbc);

			containerPanel.revalidate();
			containerPanel.repaint();
		}
	}

}
