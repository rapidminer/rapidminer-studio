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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport;

import javax.swing.Action;

import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;


/**
 * Factory to create a {@link DataImportWizard} for a specific data source.
 *
 * @author Michael Knopf
 * @since 6.5
 * @deprecated use {@link DataSourceFactoryRegistry} instead
 */
@Deprecated
public interface DataImportWizardFactory {

	/**
	 * Creates a new {@link DataImportWizard}.
	 *
	 * @return the new data import wizard
	 * @throws WizardCreationException
	 *             if the wizard could not be created
	 */
	DataImportWizard createWizard() throws WizardCreationException;

	/**
	 * Creates a new {@link Action} that in return creates a new {@link DatabaseImportWizard}.
	 *
	 * @return the new action
	 */
	Action createAction();
}
