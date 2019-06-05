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
package com.rapidminer.connection.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.components.ConnectionParameterLabel;
import com.rapidminer.connection.gui.components.ConnectionParameterTextField;
import com.rapidminer.connection.gui.model.ConnectionModel;
import com.rapidminer.connection.gui.model.ConnectionParameterGroupModel;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.repository.RepositoryLocation;


/**
 * Default {@link ConnectionGUI} implementation in case no UI was registered to the {@link ConnectionGUIRegistry}.
 * Also can be extended for connection types that only require a simple UI.
 * More complex UIs should be implemented by extending {@link AbstractConnectionGUI} directly.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class DefaultConnectionGUI extends AbstractConnectionGUI {


	protected DefaultConnectionGUI(Window parent, ConnectionInformation connection, RepositoryLocation location, boolean editable) {
		super(parent, connection, location, editable);
	}

	@Override
	public JComponent getComponentForGroup(ConnectionParameterGroupModel groupModel, ConnectionModel connectionModel) {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.gridx = 0;
		gbc.insets = new Insets(15, 32, 0, 32);
		JPanel components = new JPanel(new GridBagLayout());
		GridBagConstraints left = new GridBagConstraints();
		GridBagConstraints right = new GridBagConstraints();
		right.weightx = 1;
		left.insets = new Insets(0, 0, VERTICAL_COMPONENT_SPACING, HORIZONTAL_COMPONENT_SPACING);
		right.fill = left.fill = GridBagConstraints.HORIZONTAL;
		right.insets = new Insets(0, 0, VERTICAL_COMPONENT_SPACING, 0);
		left.gridx = 0;
		right.gridx = 1;
		for (ConnectionParameterModel parameter : groupModel.getParameters()) {
			components.add(getParameterLabelComponent(connectionModel.getType(), parameter), left);
			JComponent parameterInputComponent = getParameterInputComponent(connectionModel.getType(), parameter);
			JPanel informationWrapper = addInformationIcon(parameterInputComponent, connectionModel.getType(),
					parameter, getParentDialog());
			components.add(informationWrapper, right);
		}
		panel.add(components, gbc);

		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.VERTICAL;
		// Add empty label to move stuff up
		panel.add(new JLabel(), gbc);

		return panel;
	}

	/**
	 * Creates the label component for the given {@link ConnectionParameterModel parameter} and type.
	 * Subclasses can override this to customize a simple UI. More complex UIs should be implemented by extending
	 * {@link AbstractConnectionGUI} directly.
	 *
	 * @param type
	 * 		the type of the connection (see {@link ConnectionModel#getType()})
	 * @param parameter
	 * 		the parameter
	 * @return a {@link ConnectionParameterLabel} by default
	 */
	protected JComponent getParameterLabelComponent(String type, ConnectionParameterModel parameter) {
		return new ConnectionParameterLabel(type, parameter);
	}

	/**
	 * Creates the input component for the given {@link ConnectionParameterModel parameter} and type.
	 * Subclasses can override this to customize a simple UI. If the parameter is injectable, the returned component
	 * should be wrapped inside a {@link com.rapidminer.connection.gui.components.InjectableComponentWrapper}.
	 * More complex UIs should be implemented by extending {@link AbstractConnectionGUI} directly.
	 *
	 * @param type
	 * 		the type of the connection (see {@link ConnectionModel#getType()})
	 * @param parameter
	 * 		the parameter
	 * @return a {@link ConnectionParameterTextField} by default
	 */
	protected JComponent getParameterInputComponent(String type, ConnectionParameterModel parameter) {
		return new ConnectionParameterTextField(type, parameter);
	}

}
