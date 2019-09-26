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

import java.util.function.Predicate;

import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.parameter.ParameterTypeProcessLocation;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryLocationChooser;


/**
 * Repository location cell editor that is specialized for {@link ProcessEntry ProcessEntries} and adds a second button
 * that allows to open the selected process.
 *
 * @author Marcel Seifert, Nils Woehler, Jan Czogalla
 *
 */
public class ProcessLocationValueCellEditor extends RepositoryLocationWithExtraValueCellEditor {

	private static final long serialVersionUID = 1L;

	public ProcessLocationValueCellEditor(final ParameterTypeProcessLocation type) {
		super(type);
	}

	@Override
	protected String getExtraActionKey() {
		return "execute_process.open_process";
	}

	@Override
	protected void doExtraAction(RepositoryLocation repositoryLocation) {
		if (RapidMinerGUI.getMainFrame().close()) {
			RepositoryProcessLocation repositoryProcessLocation = new RepositoryProcessLocation(repositoryLocation);
			OpenAction.open(repositoryProcessLocation, true);
		}
	}

	@Override
	protected Class<ProcessEntry> getExpectedEntryClass() {
		return ProcessEntry.class;
	}

	@Override
	protected Predicate<Entry> getRepositoryFilter() {
		return RepositoryLocationChooser.ONLY_PROCESSES;
	}
}
