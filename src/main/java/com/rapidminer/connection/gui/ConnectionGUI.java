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

import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.model.ConnectionModel;
import com.rapidminer.connection.gui.model.ConnectionParameterGroupModel;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;


/**
 * Root interface for a GUI that allows users to edit a {@link ConnectionInformation}. Every connection type creator
 * must register his own UI for editing said connection type, see {@link ConnectionGUIRegistry}.
 * <p>
 * <strong>Note:</strong> Do not extend this interface directly, but rather extending {@link AbstractConnectionGUI} and
 * register that to the registry!
 * </p>
 *
 * @author Jonas Wilms-Pfau, Marco Boeck
 * @since 9.3
 */
public interface ConnectionGUI {

	/**
	 * This returns a component that is used to edit the entire connection. Only called once during construction time of
	 * the dialog.
	 *
	 * @return the component, never {@code null}
	 */
	JComponent getConnectionEditComponent();

	/**
	 * Returns the current state of the {@link ConnectionInformation}, based on the changes in the GUI the user did.
	 * Unless the user hits save, this object is not persisted anywhere, e.g. pressing cancel on the dialog will discard
	 * this.
	 *
	 * @return the configured configuration.
	 * @see com.rapidminer.connection.gui.model.ConnectionModelConverter#applyConnectionModel(ConnectionInformation,
	 * ConnectionModel) Converter between model and CI
	 */
	ConnectionInformation getConnection();

	/**
	 * Returns the injectable parameters for the given group.
	 *
	 * @param group
	 * 		the group that is displayed
	 * @return the injectable parameters for the group
	 */
	List<ConnectionParameterModel> getInjectableParameters(ConnectionParameterGroupModel group);

	/**
	 * Called when the validation (see {@link com.rapidminer.connection.ConnectionHandler#validate(Object)}) of the
	 * current connection has happened. This should be used to indicate the fields that failed validation to the user, to help
	 * fix the problems.
	 *
	 * @param parameterErrorMap
	 * 		the map that contains the validation errors. Format: {@code group.key - errorI18N}. To get the human readable,
	 * 		i18n string for the error, see {@link com.rapidminer.connection.util.ConnectionI18N#getValidationErrorMessage(String,
	 *        String, String, String)}.
	 */
	void validationResult(Map<String, String> parameterErrorMap);

	/**
	 * Hook for additional checks before saving a connection. If this returns {@code false}, the saving is aborted. Can
	 * be used to display dialogs with warnings.
	 *
	 * @return whether to go on with the saving process
	 */
	boolean preSaveCheck();
}
