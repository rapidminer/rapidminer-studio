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
package com.rapidminer.connection.gui.actions;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.rapidminer.connection.gui.InjectParametersDialog;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.connection.gui.model.InjectParametersModel;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * Inject Parameters Action
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class InjectParametersAction extends ResourceAction {

	private final Window parent;
	private final String type;
	private final transient Consumer<List<ConnectionParameterModel>> save;
	private final Supplier<List<ConnectionParameterModel>> injectableParameters;
	private final List<ValueProvider> valueProviders;


	/**
	 * Creates a new InjectParametersAction
	 *
	 * @param parent
	 * 		the parent window
	 * @param type
	 * 		the connection type
	 * @param injectableParameters
	 * 		supplier of the injectable parameters. Asked when the dialog is created
	 * @param valueProviders
	 * 		names of the available value providers
	 */
	public InjectParametersAction(Window parent, String type, Supplier<List<ConnectionParameterModel>> injectableParameters, List<ValueProvider> valueProviders, Consumer<List<ConnectionParameterModel>> saveCallback) {
		super(true, "inject_connection_parameter");
		this.parent = parent;
		this.type = type;
		this.save = saveCallback;
		this.injectableParameters = injectableParameters;
		this.valueProviders = valueProviders;
	}


	@Override
	protected void loggedActionPerformed(ActionEvent e) {
		InjectParametersModel data = new InjectParametersModel(injectableParameters.get(), valueProviders);
		InjectParametersDialog dialog = new InjectParametersDialog(parent, type, getKey(), data);
		dialog.setSize(new Dimension(650, 400));
		dialog.setVisible(true);
		if (dialog.wasConfirmed()) {
			save.accept(data.getParameters());
		}
	}
}
