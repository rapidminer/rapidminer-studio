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
package com.rapidminer.operator.repository;

import java.util.List;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;


/**
 * An operator to delete repository entries within a process. If the entry does not exists it won't
 * throw an error but continue silently.
 * 
 * 
 * @author Nils Woehler
 * 
 */
public class RepositoryEntryDeleteOperator extends AbstractRepositoryManagerOperator {

	public static final String ELEMENT_TO_DELETE = "entry_to_delete";

	public RepositoryEntryDeleteOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		super.doWork();

		RepositoryLocation repoLoc = getParameterAsRepositoryLocation(ELEMENT_TO_DELETE);

		Entry locateEntry;
		try {
			locateEntry = repoLoc.locateEntry();
		} catch (RepositoryException e1) {
			throw new UserError(this, e1, "302", repoLoc, e1.getMessage());
		}
		if (locateEntry == null) {
			return;
		}
		try {
			locateEntry.delete();
		} catch (RepositoryException e) {
			throw new UserError(this, e, "io.delete_file", repoLoc);
		}
		super.doWork();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeRepositoryLocation(ELEMENT_TO_DELETE, "Element that should be deleted", true, true, false);
		type.setPrimary(true);
		types.add(type);

		return types;
	}

}
