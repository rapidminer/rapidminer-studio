/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
import java.awt.event.ItemEvent;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.gui.tools.ExtendedJComboBox;


/**
 * A wrapper for injectable component, displays {@link InjectedParameterPlaceholderLabel} instead of the component if the parameter is injected
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class InjectableComponentWrapper extends JPanel {

	/**
	 * Creates a {@link InjectableComponentWrapper} with a default {@link InjectedParameterPlaceholderLabel placeholder}.
	 * Same as {@link #InjectableComponentWrapper(JComponent, JComponent, ConnectionParameterModel)
	 * InjectableComponentWrapper(component, new InjectedParameterPlaceholderLabel(parameter), parameter)}
	 *
	 * @param component
	 * 		the component to wrap
	 * @param parameter
	 * 		the parameter which is displayed by the {@code component}
	 */
	public InjectableComponentWrapper(JComponent component, ConnectionParameterModel parameter) {
		this(component, new InjectedParameterPlaceholderLabel(parameter), parameter);
	}

	/**
	 * Creates a {@link InjectableComponentWrapper} with a custom placeholder
	 *
	 * @param component
	 * 		the component to wrap
	 * @param parameter
	 * 		the parameter which is displayed by the {@code component}
	 * @since 9.6
	 */
	public InjectableComponentWrapper(JComponent component, JComponent placeholder, ConnectionParameterModel parameter) {
		super(new GridBagLayout());
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

	/**
	 * Creates a combobox that can be injected and updates with regards to the given parameter.
	 *
	 * @param parameter
	 * 		the parameter the combobox should represent
	 * @param categories
	 * 		the categories for the combobox; must not be {@code null}
	 * @param initialValue
	 * 		the initial value of the combobox; can be {@code null}
	 * @return the wrapped combobox
	 * @since 9.6
	 */
	public static JComponent getInjectableCombobox(ConnectionParameterModel parameter, String[] categories, String initialValue) {
		JComboBox<String> comboBox = new ExtendedJComboBox<>(categories);
		if (initialValue != null ) {
			comboBox.setSelectedItem(initialValue);
		}
		if (parameter.getValue() != null) {
			comboBox.setSelectedItem(parameter.getValue());
		}

		comboBox.addItemListener(event -> {
			if (event.getStateChange() == ItemEvent.SELECTED) {
				parameter.setValue(event.getItem().toString());
			}
		});

		return new InjectableComponentWrapper(comboBox, parameter);
	}

}
