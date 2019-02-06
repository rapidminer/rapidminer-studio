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

import java.util.List;

import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;


/**
 * Registry for {@link DataImportWizardFactory}s. Factories cannot be removed once they are added.
 *
 * @author Michael Knopf
 * @since 6.5
 * @deprecated use {@link DataSourceFactoryRegistry} instead
 */
@Deprecated
public interface DataImportWizardRegistry {

	/**
	 * Registers a new {@link DataImportWizardFactory}.
	 *
	 * @param factory
	 *            the new factory
	 * @throws IllegalArgumentException
	 *             if the given factory is {@code null}
	 */
	void register(DataImportWizardFactory factory);

	/**
	 * Returns all registered factories as unmodifiable list.
	 *
	 * @return list of registered factories
	 */
	List<DataImportWizardFactory> getFactories();
}
