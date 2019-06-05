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
package com.rapidminer.connection.gui.components;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * Deprecation warning panel
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class DeprecationWarning extends JPanel {

	private static final String DEPRECATION_PREFIX = "deprecation.warning";
	private static final Icon WARNING_ICON = SwingTools.createIcon("24/" + "sign_warning.png");

	/**
	 * The original content pane of the dialog
	 */
	private Container originalContentPane;

	/**
	 * Creates a deprecation panel for the given i18n key
	 *
	 * @param i18nKey
	 * 		the {i18nKey} part of gui.label.deprecation.warning.{i18nKey}.label
	 */
	public DeprecationWarning(String i18nKey) {
		super(new BorderLayout());
		JPanel warning = new JPanel(new GridBagLayout());
		JLabel infoIcon = new JLabel(WARNING_ICON);
		infoIcon.setVerticalAlignment(SwingConstants.CENTER);
		GridBagConstraints gbc = new GridBagConstraints();
		warning.setBackground(Colors.WARNING_COLOR);
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.BOTH;
		warning.add(infoIcon, gbc);
		gbc.weightx = gbc.weighty = 1;
		String fullKey = String.join(ConnectionI18N.KEY_DELIMITER, DEPRECATION_PREFIX, i18nKey, ConnectionI18N.LABEL_SUFFIX);
		warning.add(createMultiLineLabel(I18N.getGUILabel(fullKey)), gbc);
		add(warning, BorderLayout.NORTH);
	}

	/**
	 * Wraps the content pane of the dialog with this instance
	 *
	 * @param dialog
	 * 		a fully configured dialog
	 */
	public void addToDialog(JDialog dialog) {
		if (!(dialog.getContentPane() instanceof DeprecationWarning) && originalContentPane == null) {
			originalContentPane = dialog.getContentPane();
			add(originalContentPane, BorderLayout.CENTER);
			dialog.setContentPane(this);
		}
	}

	/**
	 * Restores the previous content pane of the dialog
	 *
	 * @param dialog
	 * 		the same dialog that was used for {@link #addToDialog(JDialog)}
	 */
	public void removeFromDialog(JDialog dialog) {
		Container contentPane = originalContentPane;
		if (equals(dialog.getContentPane()) && contentPane != null) {
			dialog.setContentPane(contentPane);
			originalContentPane = null;
		}
	}

	/**
	 * Creates a multiline text label, which does not support HTML
	 *
	 * @param text
	 * 		the text of the label
	 * @return a multi line label
	 */
	private static JComponent createMultiLineLabel(String text) {
		JTextArea flowLabel = new JTextArea(text, 1, 0);
		flowLabel.setDisabledTextColor(flowLabel.getForeground());
		flowLabel.setMinimumSize(new Dimension(20, 40));
		flowLabel.setBackground(null);
		flowLabel.setLineWrap(true);
		flowLabel.setWrapStyleWord(true);
		flowLabel.setBorder(BorderFactory.createEmptyBorder());
		flowLabel.setOpaque(false);
		flowLabel.setEnabled(false);
		return flowLabel;
	}
}
