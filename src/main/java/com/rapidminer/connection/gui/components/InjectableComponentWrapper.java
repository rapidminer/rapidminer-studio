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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.rapidminer.connection.gui.model.ConnectionParameterModel;


/**
 * A wrapper for injectable component, displays {@link InjectedParameterPlaceholderLabel} instead of the component if the parameter is injected
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class InjectableComponentWrapper extends JPanel {

	/**
	 * Creates a {@link InjectableComponentWrapper}
	 *
	 * @param component the component to wrap
	 * @param parameter the parameter which is displayed by the {@code component}
	 */
	public InjectableComponentWrapper(JComponent component, ConnectionParameterModel parameter) {
		super(new GridBagLayout());
		JComponent placeholder = new InjectedParameterPlaceholderLabel(parameter);
		component.setVisible(!parameter.isInjected());
		placeholder.setVisible(parameter.isInjected());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(component, gbc);
		add(placeholder, gbc);
		parameter.injectorNameProperty().addListener((observable, oldValue, newValue) -> SwingUtilities.invokeLater(() -> {
			if (component.isVisible() && component.getWidth() != 0 && component.getHeight() != 0) {
				Dimension size = new Dimension(Math.max(placeholder.getWidth(), component.getWidth()),
						Math.max(placeholder.getHeight(), component.getHeight()));
				placeholder.setPreferredSize(size);
				placeholder.setMinimumSize(size);
			}
			component.setVisible(!parameter.isInjected());
			placeholder.setVisible(parameter.isInjected());
			revalidate();
			repaint();
		}));
		parameter.enabledProperty().addListener((observable, oldValue, newValue) -> placeholder.setEnabled(newValue));
		placeholder.setEnabled(parameter.isEnabled());
	}

}
