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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.connection.gui.actions.InjectParametersAction;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.tools.I18N;


/**
 * Injection Panel, contains out of a "Inject Parameter" button and a label
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class InjectionPanel extends JPanel{

	/**
	 * Creates a new injection panel
	 *
	 * @param parent
	 * 		the parent window
	 * @param type
	 * 		the connection type
	 * @param injectableParameters
	 * 		injectable parameter supplier
	 * @param valueProviders
	 * 		names of the available value providers
	 */
	public InjectionPanel(Window parent, String type, Supplier<List<ConnectionParameterModel>> injectableParameters, List<ValueProvider> valueProviders, Consumer<List<ConnectionParameterModel>> saveCallback){
		super(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(8, 10, 8, 16);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		JLabel injectLabel = new JLabel(I18N.getGUIMessage("gui.action.inject_connection_parameter.tip"));
		injectLabel.setHorizontalAlignment(JLabel.LEFT);
		// Copy parameters
		JButton injectButton = new JButton(new InjectParametersAction(parent, type, injectableParameters, valueProviders, saveCallback));
		injectLabel.setLabelFor(injectButton);
		add(injectButton, c);

		c.weightx = 1;
		add(injectLabel, c);
	}
}
