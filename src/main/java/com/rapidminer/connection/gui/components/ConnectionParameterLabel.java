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

import java.awt.Font;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.tools.SwingTools;


/**
 * Connection Parameter Label
 *
 * @since 9.3
 * @author Jonas Wilms-Pfau
 */
public class ConnectionParameterLabel extends JLabel {

	private static final ImageIcon WARNING_ICON = SwingTools.createIcon("16/" + ConnectionI18N.getConnectionGUIMessage("validation.warning.icon"));

	/**
	 * Creates a new {@link ConnectionParameterLabel}
	 * <p>
	 * Displays a warning icon if an {@link ConnectionParameterModel#getValidationError() validation error} exists
	 * </p>
	 *
	 * @param type
	 * 		the {@link com.rapidminer.connection.gui.model.ConnectionModel#getType() connection type} (unused)
	 * @param parameter
	 * 		the parameter for which this label is displayed
	 * @deprecated since 9.3.1, use {@link #ConnectionParameterLabel(ConnectionParameterModel)} instead
	 */
	@Deprecated
	public ConnectionParameterLabel(String type, ConnectionParameterModel parameter) {
		this(parameter);
	}

	/**
	 * Creates a new {@link ConnectionParameterLabel}
	 * <p>
	 * Displays a warning icon if an {@link ConnectionParameterModel#getValidationError() validation error} exists
	 * </p>
	 *
	 * @param parameter
	 * 		the parameter for which this label is displayed
	 */
	public ConnectionParameterLabel(ConnectionParameterModel parameter) {
		this(parameter.getType(), parameter.getGroupName(), parameter.getName());
		parameter.validationErrorProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue == null && newValue != null) {
				setFont(getFont().deriveFont(Font.BOLD));
				setIcon(WARNING_ICON);
			} else if (oldValue != null && newValue == null) {
				setFont(getFont().deriveFont(Font.PLAIN));
				setIcon(null);
			}
		});
		parameter.enabledProperty().addListener((observable, oldValue, newValue) -> setEnabled(newValue));
		setEnabled((parameter.isEnabled()));
	}

	/**
	 * Creates an {@link ConnectionParameterLabel} for given parameter
	 *
	 * @param type the {@link com.rapidminer.connection.gui.model.ConnectionModel#getType() connection type}
	 * @param groupKey {@link com.rapidminer.connection.gui.model.ConnectionParameterGroupModel#getName() parameter group name}
	 * @param parameterKey {@link ConnectionParameterModel#getName() parameter name}
	 */
	public ConnectionParameterLabel(String type, String groupKey, String parameterKey) {
		super(ConnectionI18N.getParameterName(type, groupKey, parameterKey, parameterKey));
	}
}
