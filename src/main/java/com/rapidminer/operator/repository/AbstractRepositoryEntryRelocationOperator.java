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
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;

import java.util.List;


/**
 * @author Nils Woehler
 * 
 */
public class AbstractRepositoryEntryRelocationOperator extends AbstractRepositoryManagerOperator {

	public static final String SOURCE = "source entry";
	public static final String DESTINATION = "destination";
	public static final String OVERWRITE = "overwrite";
	private Folder destinationFolder;
	private String destinationName;
	private boolean overwrite;
	private RepositoryLocation sourceRepoLoc;

	public AbstractRepositoryEntryRelocationOperator(OperatorDescription description) {
		super(description);
	}

	protected Folder getDestinationFolder() {
		return destinationFolder;
	}

	protected String getDestinationName() {
		return destinationName;
	}

	protected RepositoryLocation getFromRepositoryLocation() {
		return sourceRepoLoc;
	}

	@Override
	public void doWork() throws OperatorException {

		super.doWork();

		sourceRepoLoc = getParameterAsRepositoryLocation(SOURCE);
		RepositoryLocation destRepoLoc = getParameterAsRepositoryLocation(DESTINATION);
		overwrite = getParameterAsBoolean(OVERWRITE);

		checkIfSourceEntryExists();

		// fetch destination entry
		Entry destEntry;
		try {
			destEntry = destRepoLoc.locateEntry();
		} catch (RepositoryException e1) {
			throw new UserError(this, e1, "302", destRepoLoc, e1.getMessage());
		}

		destinationName = destRepoLoc.getName();

		if (destEntry == null) {
			// if destination does not exists..
			destinationEntryIsNull(destRepoLoc);
		} else {
			// if destination entry already exists..
			destinationEntryAlreadyExists(destRepoLoc, destEntry);
		}

	}

	private void destinationEntryIsNull(RepositoryLocation destRepoLoc) throws UserError {

		// check if parent for destination location exists
		RepositoryLocation parentLoc = destRepoLoc.parent();

		Entry parentEntry;
		try {
			parentEntry = parentLoc.locateEntry();
		} catch (RepositoryException e1) {
			throw new UserError(this, e1, "302", parentLoc, e1.getMessage());
		}

		if (parentEntry == null) {

			// if parentEntry does not exists, create folders recursively as new parent
			try {
				parentLoc.createFoldersRecursively();
			} catch (RepositoryException e1) {
				throw new UserError(this, e1, "311", parentLoc);
			}

			try {
				destinationFolder = (Folder) parentLoc.locateEntry();
			} catch (RepositoryException e1) {
				throw new UserError(this, e1, "302", parentLoc, e1.getMessage());
			}

		} else {

			// if parentEntry exists, check if it is a folder
			if (parentEntry instanceof Folder) {
				destinationFolder = (Folder) parentEntry;
			} else {
				throw new UserError(this, "repository_management.dest_not_in_folder", destinationName, parentEntry);
			}
		}

	}

	private void destinationEntryAlreadyExists(RepositoryLocation destRepoLoc, Entry destEntry) throws UserError {
		// if destination entry already exists

		// check if it is a folder
		if (destEntry instanceof Folder) {
			destinationFolder = (Folder) destEntry;
			destinationName = sourceRepoLoc.getName();

			boolean containsEntry = false;
			try {
				containsEntry = destinationFolder.containsEntry(sourceRepoLoc.getName());
			} catch (RepositoryException e) {
				throw new UserError(this, e, "302", destinationFolder, e.getMessage());
			}

			if (containsEntry) {
				if (overwrite) {
					RepositoryLocation existingDestRepoLoc;
					try {
						existingDestRepoLoc = new RepositoryLocation(destinationFolder.getLocation(),
								sourceRepoLoc.getName());
					} catch (MalformedRepositoryLocationException e) {
						throw new UserError(this, e, "313", destinationFolder.getLocation());
					}

					Entry exsistingSourceEntry;
					try {
						exsistingSourceEntry = existingDestRepoLoc.locateEntry();
					} catch (RepositoryException e) {
						throw new UserError(this, e, "302", destinationFolder, e.getMessage());
					}

					try {
						exsistingSourceEntry.delete();
					} catch (RepositoryException e) {
						throw new UserError(this, e, "io.delete_file", destEntry);
					}
				} else {
					throw new UserError(this, "repository_management.relocate_repository_entry", destRepoLoc,
							"Entry already exsists but overwriting is disabled.");
				}
			}

		} else {
			// else check if it should be overwritten
			if (overwrite) {
				// delete old entry
				try {
					destEntry.delete();
				} catch (RepositoryException e) {
					throw new UserError(this, e, "io.delete_file", destEntry);
				}
				RepositoryLocation parentLoc = destRepoLoc.parent();
				try {
					destinationFolder = (Folder) parentLoc.locateEntry();
				} catch (RepositoryException e1) {
					throw new UserError(this, e1, "302", parentLoc, e1.getMessage());
				}
			} else {
				throw new UserError(this, "repository_management.relocate_repository_entry", destRepoLoc,
						"Entry already exsists but overwriting is disabled.");
			}
		}

	}

	/**
	 * @throws UserError
	 */
	private void checkIfSourceEntryExists() throws UserError {
		// check if "from" entry exists
		Entry sourceEntry;
		try {
			sourceEntry = sourceRepoLoc.locateEntry();
			if (sourceEntry == null) {
				throw new UserError(this, "301", sourceRepoLoc);
			}
		} catch (RepositoryException e1) {
			throw new UserError(this, e1, "302", sourceRepoLoc, e1.getMessage());
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeRepositoryLocation(SOURCE, "Entry that should be copied", true, true, false));
		types.add(new ParameterTypeRepositoryLocation(DESTINATION, "Copy destination", true, true, false, false, true));
		types.add(new ParameterTypeBoolean(OVERWRITE, "Overwrite entry at copy destination?", false, false));

		return types;
	}

}
