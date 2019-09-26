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

import java.util.Arrays;
import java.util.function.Predicate;

import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.parameter.ParameterTypeConnectionLocation;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryLocation;


/**
 * Repository location cell editor that is specialized for {@link ConnectionEntry ConnectionEntries} and adds a second button
 * that allows to open/edit the selected connection.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ConnectionLocationValueCellEditor extends RepositoryLocationWithExtraValueCellEditor {

	// keep the connection types from the ParameterTypeConnectionLocation to create a Predicate for filtering the Repository
	private String[] conTypes;

	public ConnectionLocationValueCellEditor(ParameterTypeConnectionLocation type) {
		super(type);
		conTypes = type.getConnectionType();
	}

	@Override
	protected String getExtraActionKey() {
		return "connection.open_edit_connection";
	}

	@Override
	protected void doExtraAction(RepositoryLocation repositoryLocation) {
		OpenAction.open(repositoryLocation.getAbsoluteLocation(), true);
	}

	@Override
	protected Class<ConnectionEntry> getExpectedEntryClass() {
		return ConnectionEntry.class;
	}

	@Override
	protected Predicate<Entry> getRepositoryFilter() {
		return entry -> (((entry instanceof Repository) && ((Repository) entry).supportsConnections())
				|| (entry instanceof Folder && ((Folder) entry).isSpecialConnectionsFolder())
				|| (entry instanceof ConnectionEntry && (conTypes == null || conTypes.length == 0
							|| Arrays.stream(conTypes).anyMatch(ct -> ct != null && ct.equals(((ConnectionEntry) entry).getConnectionType())))));
	}
}
