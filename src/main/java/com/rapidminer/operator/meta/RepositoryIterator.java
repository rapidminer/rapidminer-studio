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
import com.rapidminer.operator.nio.file.RepositoryBlobObject;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
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
		Entry entry = null;
		if (currentParent != null && currentParent instanceof RepositoryLocation) {
			repositoryLocation = (RepositoryLocation) currentParent;
		}
		try {
			entry = repositoryLocation.locateEntry();
			if (entry == null) {
				throw new UserError(this, 323, getParameterAsString(PARAMETER_DIRECTORY));
			}
		} catch (RepositoryException e) {
			throw new UserError(this, 323, getParameterAsString(PARAMETER_DIRECTORY));
		}
		// calculate total number of iterations
		getProgress().setTotal(countIterations(entry, recursive) + 1);
		getProgress().setCompleted(1);
		// start to iterate
		this.iterate(entry, filter, recursive, type);
		getProgress().complete();
	}

	private int countIterations(Entry entry, boolean recursive) throws OperatorException {
		int iterations = 0;
		try {
			if (!recursive) {
				iterations = ((Folder) entry).getDataEntries().size();
			} else {
				iterations = ((Folder) entry).getDataEntries().size();
				for (Entry child : ((Folder) entry).getSubfolders()) {
					if (child.getType().equals(Folder.TYPE_NAME)) {
						iterations += countIterations(child, recursive);
					} else {
						iterations++;
					}
				}

			}
		} catch (RepositoryException e) {
			throw new UserError(this, 312, entry.getLocation().getAbsoluteLocation(), e.getCause());
		}
		return iterations;
	}

	private void iterate(Entry currentParent, Pattern filter, boolean recursive, int type) throws OperatorException {
		getProgress().step();
		try {
			Entry entry = currentParent;
			String entryType = entry.getType();
			if (entryType.equals(Folder.TYPE_NAME)) {

				List<DataEntry> entries = new ArrayList<>(((Folder) entry).getDataEntries());

				for (Entry child : entries) {
					iterate(child, filter, recursive, type);
				}
				if (recursive) {
					for (Entry child : ((Folder) entry).getSubfolders()) {
						iterate(child, filter, recursive, type);
					}
				}

			}
			Folder containingFolder = entry.getContainingFolder();
			if (entryType.equals(IOObjectEntry.TYPE_NAME) && type == IO_OBJECT) {
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
			}
			if (entryType.equals(BlobEntry.TYPE_NAME) && type == BLOB) {
				String fileName = entry.getName();
				String fullPath = entry.getLocation().getAbsoluteLocation();
				if (containingFolder != null) {

					String parentPath = containingFolder.getName();
					if (matchesFilter(filter, fileName, fullPath, parentPath)) {
						RepositoryLocation location = entry.getLocation();
						String source = location.getAbsoluteLocation();
						RepositoryBlobObject result2 = new RepositoryBlobObject(location);
						result2.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, source);
						result2.setSource(getName());
						result2.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, location.toString());
						doWorkForSingleIterationStep(fileName, fullPath, parentPath, result2);
					}
				}
			}
		} catch (RepositoryException e) {
			throw new UserError(this, 312, currentParent.getLocation().getAbsoluteLocation(), e.getCause());
		}
	}

	@Override
	public void doWork() throws OperatorException {
		repositoryLocation = getParameterAsRepositoryLocation(PARAMETER_DIRECTORY);
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

}
