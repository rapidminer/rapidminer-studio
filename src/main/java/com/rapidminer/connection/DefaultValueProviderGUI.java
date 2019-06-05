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
package com.rapidminer.connection;

import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.rapidminer.connection.gui.AbstractConnectionGUI;
import com.rapidminer.connection.gui.ValueProviderGUIProvider;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;
import com.rapidminer.tools.I18N;


/**
 * The default GUI provider to configure the parameters of a {@link ValueProvider}
 *
 * @author Jonas Wilms-Pfau, Andreas Timm
 * @since 9.3
 */
public class DefaultValueProviderGUI implements ValueProviderGUIProvider {

	/**
	 * Generic text which states that no config is needed
	 */
	protected static final String NO_CONFIG_NEEDED_LABEL = I18N.getGUIMessage("gui.dialog.connection.valueprovider.needs_no_configuration.label");
	/**
	 * Some stars for encrypted values
	 */
	private static final String ENCRYPTED_PLACEHOLDER = ConnectionI18N.getConnectionGUILabel("placeholder_encrypted");

	@Override
	public JComponent createConfigurationComponent(JDialog parent, ValueProvider provider, ConnectionInformation connection, boolean editmode) {
		if (provider.getParameters().isEmpty()) {
			JLabel noConfigLabel = new JLabel(NO_CONFIG_NEEDED_LABEL);
			return addInformationIcon(noConfigLabel, provider.getType(), "no_configuration", parent);
		}

		final JPanel panel = new JPanel(new GridLayout(0, 2, 50, 8));

		for (ValueProviderParameter parameter : provider.getParameters()) {
			panel.add(new JLabel(ConnectionI18N.getParameterName(provider.getType(), "valueprovider",
					parameter.getName(), parameter.getName())));
			panel.add(addInformationIcon(getEditComponent(parameter, editmode), provider.getType(), parameter.getName(), parent));
		}

		return panel;
	}

	/**
	 * Creates an edit component for a value provider parameter
	 *
	 * @param parameter
	 * 		the parameter
	 * @param editmode
	 * 		if the parameter is editable
	 * @return the component for the parameter
	 */
	protected static JComponent getEditComponent(ValueProviderParameter parameter, boolean editmode) {
		final JComponent comp;
		if (editmode) {
			final JTextField textField = parameter.isEncrypted() ? new JPasswordField() : new JTextField();
			textField.setText(parameter.getValue());
			textField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					parameter.setValue(textField.getText());
				}
			});
			textField.setColumns(20);
			comp = textField;
		} else {
			comp = new JLabel(parameter.isEncrypted() ? ENCRYPTED_PLACEHOLDER : parameter.getValue());
		}
		comp.setEnabled(parameter.isEnabled());
		return comp;
	}

	/**
	 * Wraps the given input component in a panel and adds an information icon with a tooltip. The tooltip i18n is
	 * derived from the type, group and parameter name as {@code gui.label.connection.parameter.{type}.valueprovider.{
	 * parameterName}.tip}
	 *
	 * @param parameterInputComponent
	 * 		the component to wrap
	 * @param type
	 * 		the type of the value provider
	 * @param parameterName
	 * 		the name of the value provider parameter
	 * @param parent
	 * 		the parent dialog
	 * @return a new panel containing the old and an additional information icon with tooltip
	 */
	protected static JPanel addInformationIcon(JComponent parameterInputComponent, String type,
											String parameterName, JDialog parent) {
		return AbstractConnectionGUI.addInformationIcon(parameterInputComponent, ConnectionI18N.getParameterTooltip(type,
				"valueprovider", parameterName, null), parent);
	}
}
