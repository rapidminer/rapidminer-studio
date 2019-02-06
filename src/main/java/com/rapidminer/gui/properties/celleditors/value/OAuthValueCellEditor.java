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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.rapidminer.gui.properties.DefaultRMCellEditor;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.OAuthMechanism;
import com.rapidminer.parameter.ParameterTypeOAuth;


/**
 * Cell editor consisting of a text field which stores the access token and a small button for
 * opening the authentication dialog.
 *
 * @author Marcel Michel
 */
public class OAuthValueCellEditor extends DefaultRMCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = 8761334706751374047L;

	private JPanel container;

	private JButton authButton;

	public OAuthValueCellEditor(final ParameterTypeOAuth type) {
		super(new JPasswordField());
		this.container = new JPanel(new GridBagLayout());
		this.container.setToolTipText(type.getDescription());

		GridBagConstraints gbc = new GridBagConstraints();

		editorComponent.setToolTipText(type.getDescription());
		editorComponent.putClientProperty("JPasswordField.cutCopyAllowed", true);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.weighty = 1;
		gbc.weightx = 1;
		container.add(editorComponent, gbc);

		((JTextField) editorComponent).getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				fireEditingStopped();

			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				fireEditingStopped();

			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				fireEditingStopped();

			}
		});

		authButton = new JButton(new ResourceAction(true, "generate_auth_key") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {

				OAuthMechanism oAuth = type.getOAuthMechanism();
				OAuthDialog dialog = new OAuthDialog(SwingUtilities.getWindowAncestor(getTableCellEditorComponent(null,
						null, false, 0, 0)), oAuth);
				dialog.setVisible(true);
				String code = oAuth.getToken();
				if (code != null) {
					((JTextField) editorComponent).setText(code);
				}
			}
		});
		gbc.gridx += 1;
		gbc.weightx = 0;
		gbc.insets = new Insets(0, 5, 0, 0);
		container.add(authButton, gbc);

	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (value != null) {
			((JTextField) editorComponent).setText(String.valueOf(value));
		}
		return container;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if (value != null) {
			((JTextField) editorComponent).setText(String.valueOf(value));
		}
		return container;
	}

	@Override
	public Object getCellEditorValue() {
		return ((JTextField) editorComponent).getText();
	}

	@Override
	public void setOperator(Operator operator) {}

	@Override
	public void activate() {
		authButton.doClick();
	}
}
