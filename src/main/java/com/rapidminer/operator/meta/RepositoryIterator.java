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
package com.rapidminer.operator.meta;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.file.BinaryEntryFileObject;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.nio.file.RepositoryBlobObject;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;


/**
 * Operator to iterate over entries in a repository.
 *
 * @author Vaclav Uher
 *
 */
public class RepositoryIterator extends AbstractRepositoryIterator {

	public static final String PARAMETER_DIRECTORY = "repository_folder";

	private RepositoryLocation repositoryLocation;


	public RepositoryIterator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected void iterate(Object currentParent, Pattern filter, boolean recursive, int type) throws OperatorException {
		// transform Repository Entry
		Folder folder;
		if (currentParent instanceof RepositoryLocation) {
			repositoryLocation = (RepositoryLocation) currentParent;
		}
		try {
			folder = repositoryLocation.locateFolder();
			if (folder == null) {
				throw new UserError(this, 323, getParameterAsString(PARAMETER_DIRECTORY));
			}
		} catch (RepositoryException e) {
			throw new UserError(this, 323, getParameterAsString(PARAMETER_DIRECTORY));
		}
		// calculate total number of iterations
		getProgress().setTotal(countIterations(folder, recursive) + 1);
		getProgress().setCompleted(1);

		// start to iterate
		iterateFolder(folder, filter, recursive, type);

		getProgress().complete();
	}

	@Override
	public void doWork() throws OperatorException {
		repositoryLocation = getParameterAsRepositoryLocationFolder(PARAMETER_DIRECTORY);
		getProgress().setTotal(1);
		super.doWork();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();

		ParameterTypeRepositoryLocation folder = new ParameterTypeRepositoryLocation(PARAMETER_DIRECTORY,
				"Folder in the repository to iterate over", false, true, false);
		folder.setExpert(false);
		folder.setPrimary(true);
		types.add(folder);

		types.addAll(super.getParameterTypes());

		return types;
	}

	private int countIterations(Folder folder, boolean recursive) throws OperatorException {
		int iterations;
		try {
			if (!recursive) {
				iterations = folder.getDataEntries().size();
			} else {
				iterations = folder.getDataEntries().size();
				for (Folder subfolder : folder.getSubfolders()) {
					iterations += countIterations(subfolder, recursive);
				}
			}
		} catch (RepositoryException e) {
			throw new UserError(this, 312, folder.getLocation().getAbsoluteLocation(), e.getCause());
		}
		return iterations;
	}

	private void iterateFolder(Folder folder, Pattern filter, boolean recursive, int type) throws OperatorException {
		getProgress().step();
		try {
			List<DataEntry> entries = new ArrayList<>(folder.getDataEntries());

			for (DataEntry child : entries) {
				handleData(child, filter, type);
			}
			if (recursive) {
				for (Folder subfolder : folder.getSubfolders()) {
					iterateFolder(subfolder, filter, recursive, type);
				}
			}
		} catch (RepositoryException e) {
			throw new UserError(this, 312, folder.getLocation().getAbsoluteLocation(), e.getCause());
		}
	}

	private void handleData(DataEntry entry, Pattern filter, int type) throws OperatorException {
		Folder containingFolder = entry.getContainingFolder();
		try {
			if (entry instanceof IOObjectEntry && type == IO_OBJECT) {
				String fileName = entry.getName();
				String fullPath = entry.getLocation().getAbsoluteLocation();
				if (containingFolder != null) {
					String parentPath = containingFolder.getName();
					if (matchesFilter(filter, fileName, fullPath, parentPath)) {

						IOObject data = ((IOObjectEntry) entry).retrieveData(null);
						data.setSource(getName());
						data.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, entry.getLocation().toString());
						doWorkForSingleIterationStep(fileName, fullPath, parentPath, data);
					}
				}
			} else if ((entry instanceof BinaryEntry || entry instanceof BlobEntry) && type == BLOB) {
				String fileName = entry.getName();
				String fullPath = entry.getLocation().getAbsoluteLocation();
				if (containingFolder != null) {

					String parentPath = containingFolder.getName();
					if (matchesFilter(filter, fileName, fullPath, parentPath)) {
						RepositoryLocation location = entry.getLocation();

						FileObject result;
						if (entry instanceof BinaryEntry) {
							result = new BinaryEntryFileObject(location);
						} else {
							result = new RepositoryBlobObject(location);
						}
						result.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, location.getAbsoluteLocation());
						result.setSource(getName());

						doWorkForSingleIterationStep(fileName, fullPath, parentPath, result);
					}
				}
			}
		} catch (RepositoryException e) {
			throw new UserError(this, 312, containingFolder.getLocation().getAbsoluteLocation(), e.getCause());
		}
	}
}
