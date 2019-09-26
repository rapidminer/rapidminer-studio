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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.connection.gui.listener.TextChangedDocumentListener;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.tools.SwingTools;


/**
 * Connection Parameter TextField
 * <p>
 * Displays an simple input field. With support for:
 * <ul>
 * <li>{@link ConnectionParameterModel#isInjected() Injection: Displays an injection icon}</li>
 * <li>{@link ConnectionParameterModel#isEnabled() Enabled/Disabled: Disables the element if needed}</li>
 * <li>{@link ConnectionParameterModel#isEncrypted() Encryption: Displays an password field if needed}</li>
 * <li>{@link ConnectionParameterModel#setValue(String) Updates the underlying parameter model}</li>
 * <li>{@link ConnectionParameterModel#valueProperty() Listens to parameter values from outside}</li>
 * </ul>
 * </p>
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class ConnectionParameterTextField extends JPanel {

	public static final Border DISABLED_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);
	private JTextComponent editComponent;
	private final String type;
	private transient ConnectionParameterModel parameter;


	/**
	 * Creates a new {@link ConnectionParameterTextField text field} for the {@link ConnectionParameterModel parameter}
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.gui.model.ConnectionModel#getType() connection type} (unused)
	 * @param parameter
	 * 		the parameter
	 * @deprecated since 9.3.1, use {@link #ConnectionParameterTextField(ConnectionParameterModel)} instead
	 */
	@Deprecated
	public ConnectionParameterTextField(String type, ConnectionParameterModel parameter) {
		this(parameter);
	}

	/**
	 * Creates a new {@link ConnectionParameterTextField text field} for the {@link ConnectionParameterModel parameter}
	 *
	 * @param parameter
	 * 		the parameter
	 */
	public ConnectionParameterTextField(ConnectionParameterModel parameter) {
		super(new GridBagLayout());
		this.type = parameter.getType();
		this.parameter = parameter;
		editComponent = createEditComponent();

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(new InjectableComponentWrapper(editComponent, parameter), gbc);
		parameter.enabledProperty().addListener((observable, oldValue, newValue) -> toggleDisabledState());
		toggleDisabledState();
		if (!parameter.isEditable()) {
			editComponent.setBorder(DISABLED_BORDER);
			editComponent.setEditable(false);
			editComponent.setBackground(new Color(255, 255, 255, 0));
			editComponent.setOpaque(false);
			// Set to ********, in case the password is set and in view mode
			if (parameter.isEncrypted() && !StringUtils.isEmpty(parameter.getValue())) {
				editComponent.setText(ConnectionI18N.getConnectionGUILabel("placeholder_encrypted"));
			}
			toggleDisabledState();
			return;
		}
		parameter.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (!editComponent.getText().equals(newValue)) {
				SwingUtilities.invokeLater(() -> editComponent.setText(newValue));
			}
		});
	}

	private void toggleDisabledState() {
		editComponent.setEnabled((parameter.isEnabled()));
	}

	/**
	 * @return the component that is used for encrypted parameters
	 */
	protected JTextComponent getEncryptedTextComponent() {
		return new JPasswordField(15);
	}

	/**
	 * @return the editable text component
	 */
	protected JTextComponent getTextComponent() {
		return new JTextField(15);
	}

	private JTextComponent createEditComponent() {
		JTextComponent textComponent = parameter.isEncrypted() ? getEncryptedTextComponent() : getTextComponent();
		textComponent.setText(parameter.getValue());
		if (parameter.isEditable()) {
			textComponent.getDocument().addDocumentListener(new TextChangedDocumentListener(parameter.valueProperty()));
			SwingTools.setPrompt(ConnectionI18N.getParameterPrompt(type, parameter.getGroupName(), parameter.getName(), null), textComponent);
		}
		return textComponent;
	}
}
