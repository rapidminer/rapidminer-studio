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
package com.rapidminer.studio.io.gui.internal;

import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.repository.RepositoryLocation;


/**
 * Callback handler for the {@link DataImportWizard} to execute context specific implementation and end up with the data
 * and in the view of choice.
 *
 * @author Andreas Timm
 * @since 9.0.0
 */
public interface DataImportWizardCallback {

	/**
	 * Method to execute after storing results. Can access the wizard and the newly created entry location.
	 *
	 * @param wizard
	 * 		the wizard that was run
	 * @param location
	 * 		the new location
	 */
	public void execute(ImportWizard wizard, RepositoryLocation location);
}
