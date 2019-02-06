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

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;


/**
 * Copies an entry to a new parent folder. If destinationLocation references a folder, the entry at
 * oldLocation is copied to that folder. If it references an existing entry and overwriting is not
 * enabled (default case), an exception is raised. If overwriting is enabled the existing entry will
 * be overwritten.
 * <p>
 * If it references a location which does not exist, say, "/root/folder/leaf", but the parent exists
 * (in this case "/root/folder"), a new entry named by the last path component (in this case "leaf")
 * is created.
 * 
 * @author Nils Woehler
 * 
 */
public class RepositoryEntryCopyOperator extends AbstractRepositoryEntryRelocationOperator {

	public RepositoryEntryCopyOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		super.doWork();

		RepositoryManager repoMan = RepositoryManager.getInstance(null);

		// try to copy the repository element to the new destination
		try {
			repoMan.copy(getFromRepositoryLocation(), getDestinationFolder(), getDestinationName(), null);
		} catch (RepositoryException e) {
			throw new UserError(this, e, "repository_management.copy_repository_entry", getFromRepositoryLocation(),
					e.getMessage());
		}
	}

}
