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

import java.util.Objects;
import javax.swing.JCheckBox;

import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.StaticButtonModel;

/**
 * A {@link JCheckBox} representing a boolean {@link ConnectionParameterModel}. Looks active but is inert in view mode.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ConnectionParameterCheckBox extends JCheckBox {

	public ConnectionParameterCheckBox(String type, ConnectionParameterModel parameter) {
		this(type, parameter.getGroupName(), parameter.getName());
		boolean editable = parameter.isEditable();
		setEnabled((parameter.isEnabled() && editable));
		setSelected(Boolean.parseBoolean(parameter.getValue()));
		if (!editable) {
			setModel(new StaticButtonModel(isSelected()));
			setFocusPainted(false);
			return;
		}
		parameter.enabledProperty().addListener((observable, oldValue, newValue) -> setEnabled(newValue));
		parameter.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (Objects.equals(oldValue, newValue)) {
				return;
			}
			setSelected(Boolean.parseBoolean(newValue));
		});
		addChangeListener(e -> parameter.setValue(Boolean.toString(isSelected())));
	}

	public ConnectionParameterCheckBox(String type, String groupKey, String parameterKey) {
		super(ConnectionI18N.getParameterName(type, groupKey, parameterKey, parameterKey));
	}
}
