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
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;


/**
 * An Operator to rename repository entries. The user can select the entry to rename, a new name and
 * if an already existing entry should be overwritten or not. If overwriting is not allowed (default
 * case) a user error is thrown if there already exists another element with the new name.
 * 
 * @author Nils Woehler
 * 
 */
public class RepositoryEntryRenameOperator extends AbstractRepositoryManagerOperator {

	public static final String ELEMENT_TO_RENAME = "entry_to_rename";
	public static final String NEW_ELEMENT_NAME = "new_name";
	public static final String OVERWRITE = "overwrite";

	public RepositoryEntryRenameOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {

		super.doWork();

		// fetch parameters
		RepositoryLocation repoLoc = getParameterAsRepositoryLocation(ELEMENT_TO_RENAME);
		String newName = getParameterAsString(NEW_ELEMENT_NAME);
		boolean overwrite = getParameterAsBoolean(OVERWRITE);

		// name with length 0 is not allowed
		if (newName.length() == 0) {
			throw new UserError(this, "207", "", NEW_ELEMENT_NAME, "An empty new name is not allowed.");
		}

		// locate the entry that should be renamed
		Entry entry;
		try {
			entry = repoLoc.locateEntry();
			if (entry == null) {
				throw new UserError(this, "301", repoLoc);
			}
		} catch (RepositoryException e1) {
			throw new UserError(this, e1, "302", repoLoc, e1.getMessage());
		}

		try {
			// fetch the containing folder and check if another entry with the same name already
			// exists.
			Folder containingFolder = entry.getContainingFolder();
			if (containingFolder != null && containingFolder.containsEntry(newName)) {

				// if overwriting is allowed, try to delete the equally named entry
				if (overwrite) {
					List<DataEntry> dataEntries = containingFolder.getDataEntries();
					boolean deleted = false;
					for (DataEntry dataEntry : dataEntries) {
						if (dataEntry.getName().equals(newName)) {
							dataEntry.delete();
							deleted = true;
							break;
						}
					}

					if (!deleted) {
						List<Folder> subfolders = containingFolder.getSubfolders();
						for (Folder subfolder : subfolders) {
							if (subfolder.getName().equals(newName)) {
								subfolder.delete();
								deleted = true;
								break;
							}
						}
					}

					// if deleting was not successful, show an user error
					if (!deleted) {
						throw new RepositoryException("Could not delete already existing element " + newName
								+ " at move destination " + containingFolder);
					}
				} else {
					throw new RepositoryException("Could not rename entry: Element with name " + newName
							+ " already exists.");
				}
			}
		} catch (RepositoryException e) {
			throw new OperatorException("Renaming the repository entry " + entry + " is not possible: ", e);
		}

		// finally try to rename the repository entry
		try {
			entry.rename(newName);
		} catch (RepositoryException e) {
			throw new UserError(this, e, "repository_management.rename_repository_entry", repoLoc, newName, e.getMessage());
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeRepositoryLocation(ELEMENT_TO_RENAME, "Entry that should be renamed", true, true, false);
		type.setPrimary(true);
		types.add(type);
		types.add(new ParameterTypeString(NEW_ELEMENT_NAME, "New entry name", false, false));
		types.add(new ParameterTypeBoolean(OVERWRITE, "Overwrite already existing entry with same name?", false, false));

		return types;
	}

}
