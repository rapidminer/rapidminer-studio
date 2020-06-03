/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.repository.RepositoryLocationType;


/**
 * <p>
 * Since version 9.7: To cover situations where both a folder and a file exist with the same name, they will prefer the
 * file. They will only work on folder level if no file with that name exists.
 * </p>
 *
 * @author Nils Woehler
 */
public class AbstractRepositoryEntryRelocationOperator extends AbstractRepositoryManagerOperator {

	public static final String SOURCE = "source entry";
	public static final String DESTINATION = "destination";
	public static final String OVERWRITE = "overwrite";
	private Folder destinationFolder;
	private String destinationName;
	private boolean overwrite;
	private RepositoryLocation sourceRepoLoc;
	protected boolean sourceIsData;

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

		sourceRepoLoc = getParameterAsRepositoryLocationData(SOURCE, DataEntry.class);
		// could also be a folder, so change location type to UNKNOWN
		sourceRepoLoc.setLocationType(RepositoryLocationType.UNKNOWN);

		RepositoryLocation destRepoLoc = getParameterAsRepositoryLocationData(DESTINATION, DataEntry.class);
		// could also be a folder, so change location type to UNKNOWN
		destRepoLoc.setLocationType(RepositoryLocationType.UNKNOWN);
		overwrite = getParameterAsBoolean(OVERWRITE);

		checkIfSourceEntryExists();
		destinationName = destRepoLoc.getName();

		// fetch destination data entry
		try {
			// we need to match the source
			if (sourceIsData) {
				DataEntry destEntry = destRepoLoc.locateData();
				if (destEntry == null) {
					// if destination does not exists..
					destinationDataIsNull(destRepoLoc);
				} else {
					// if destination data already exists..
					destinationDataAlreadyExists(destRepoLoc, destEntry);
				}
			} else {
				Folder destFolder = destRepoLoc.locateFolder();
				if (destFolder == null) {
					// if destination does not exists..
					destinationFolderIsNull(destRepoLoc);
				} else {
					// if destination folder already exists..
					destinationFolderAlreadyExists(destRepoLoc, destFolder);
				}
			}
		} catch (RepositoryException e1) {
			throw new UserError(this, e1, "302", destRepoLoc, e1.getMessage());
		}
	}

	private void destinationFolderIsNull(RepositoryLocation destRepoLoc) throws UserError {

		// check if parent for destination location exists
		RepositoryLocation parentLoc = destRepoLoc.parent();

		Folder parentFolder;
		try {
			parentFolder = parentLoc.locateFolder();
		} catch (RepositoryException e1) {
			throw new UserError(this, e1, "302", parentLoc, e1.getMessage());
		}

		if (parentFolder == null) {

			// if parentEntry does not exists, create folders recursively as new parent
			try {
				parentLoc.createFoldersRecursively();
			} catch (RepositoryException e1) {
				throw new UserError(this, e1, "311", parentLoc);
			}

			try {
				destinationFolder = parentLoc.locateFolder();
			} catch (RepositoryException e1) {
				throw new UserError(this, e1, "302", parentLoc, e1.getMessage());
			}

		} else {
			destinationFolder = parentFolder;
		}

	}

	private void destinationDataIsNull(RepositoryLocation destRepoLoc) throws UserError {
		// no matter if data or folder, we need to create the parent structure here
		destinationFolderIsNull(destRepoLoc);
	}

	private void destinationFolderAlreadyExists(RepositoryLocation destRepoLoc, Folder destinationFolder) throws UserError {
		// if destination folder already exists

		destinationName = sourceRepoLoc.getName();

		boolean containsEntry = false;
		try {
			containsEntry = destinationFolder.containsFolder(sourceRepoLoc.getName());
		} catch (RepositoryException e) {
			throw new UserError(this, e, "302", destinationFolder, e.getMessage());
		}

		if (containsEntry) {
			if (overwrite) {
				RepositoryLocation existingDestRepoLoc;
				try {
					existingDestRepoLoc = new RepositoryLocationBuilder().withLocationType(RepositoryLocationType.UNKNOWN).buildFromParentLocation(destinationFolder.getLocation(),
							sourceRepoLoc.getName());
				} catch (MalformedRepositoryLocationException e) {
					throw new UserError(this, e, "313", destinationFolder.getLocation());
				}

				Entry existingTargetEntry;
				try {
					existingTargetEntry = existingDestRepoLoc.locateData();
					if (existingTargetEntry == null) {
						existingTargetEntry = existingDestRepoLoc.locateFolder();
					}
				} catch (RepositoryException e) {
					throw new UserError(this, e, "302", destinationFolder, e.getMessage());
				}

				try {
					existingTargetEntry.delete();
				} catch (RepositoryException e) {
					throw new UserError(this, e, "io.delete_file", destinationFolder);
				}
			} else {
				throw new UserError(this, "repository_management.relocate_repository_entry", destRepoLoc,
						"Entry already exsists but overwriting is disabled.");
			}
		}
	}

	private void destinationDataAlreadyExists(RepositoryLocation destRepoLoc, DataEntry destEntry) throws UserError {
		// if destination data already exists

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
				destinationFolder = parentLoc.locateFolder();
			} catch (RepositoryException e1) {
				throw new UserError(this, e1, "302", parentLoc, e1.getMessage());
			}
		} else {
			throw new UserError(this, "repository_management.relocate_repository_entry", destRepoLoc,
					"Entry already exsists but overwriting is disabled.");
		}
	}

	private void checkIfSourceEntryExists() throws UserError {
		// check if "from" entry exists
		Entry sourceEntry;
		try {
			sourceEntry = sourceRepoLoc.locateData();
			if (sourceEntry == null) {
				sourceEntry = sourceRepoLoc.locateFolder();
			}
			if (sourceEntry == null) {
				throw new UserError(this, "301", sourceRepoLoc);
			}
			sourceIsData = sourceEntry instanceof DataEntry;
			sourceRepoLoc.setLocationType(sourceEntry.getLocation().getLocationType());
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
