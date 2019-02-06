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

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.nio.file.SimpleFileObject;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDirectory;


/**
 * This operator iterates over the files in the specified directory (and subdirectories if the
 * corresponding parameter is set to true).
 *
 * @author Sebastian Land, Ingo Mierswa, Marius Helf
 */
@Deprecated
public class FileIterator extends AbstractFileIterator {

	public static final String PARAMETER_DIRECTORY = "directory";

	private File directory;

	public static final OperatorVersion CHANGE_6_4_0_ERROR_WHEN_DIRECTORY_NOT_EXISTS = new OperatorVersion(6, 4, 0);

	public FileIterator(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		directory = getParameterAsFile(PARAMETER_DIRECTORY);

		super.doWork();
	}

	private List<EntryContainer> calcObjectsOfIntrest(File dir, Pattern filter, boolean iterateSubDirs, boolean iterateFiles,
			boolean recursive, List<EntryContainer> toFill) throws OperatorException {

		File[] directoryListFiles = dir.listFiles();
		if (dir.isDirectory() && directoryListFiles != null) {
			for (File child : dir.listFiles()) {
				String fileName = child.getName();
				String fullPath = child.getAbsolutePath();
				String parentPath = child.getParent();
				if (iterateSubDirs && child.isDirectory() || iterateFiles && child.isFile()) {
					if (matchesFilter(filter, fileName, fullPath, parentPath)) {
						FileObject fileObject = new SimpleFileObject(child);
						toFill.add(new EntryContainer(fileName, fullPath, parentPath, fileObject));
					}
				}

				if (recursive && child.isDirectory()) {
					calcObjectsOfIntrest(child, filter, iterateSubDirs, iterateFiles, recursive, toFill);
				}
			}
		} else if (getCompatibilityLevel().isAbove(CHANGE_6_4_0_ERROR_WHEN_DIRECTORY_NOT_EXISTS)
				|| dir.isDirectory() && directoryListFiles == null) {
			throw new UserError(this, 324, getParameterAsString(PARAMETER_DIRECTORY));
		}
		return toFill;
	}

	@Override
	protected void iterate(Object currentParent, Pattern filter, boolean iterateSubDirs, boolean iterateFiles,
			boolean recursive) throws OperatorException {
		if (currentParent == null) {
			currentParent = directory;

		}
		// init Operator progress and compute objects which meet all criteria
		List<EntryContainer> objectsOfIntrest = this.calcObjectsOfIntrest((File) currentParent, filter, iterateSubDirs,
				iterateFiles, recursive, new LinkedList<EntryContainer>());
		getProgress().setTotal(objectsOfIntrest.size());

		// do the actual work
		for (EntryContainer entry : objectsOfIntrest) {
			doWorkForSingleIterationStep(entry.fileName, entry.fullPath, entry.parentPath, entry.fileObject);
			getProgress().step();
		}

	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();

		ParameterType type = new ParameterTypeDirectory(PARAMETER_DIRECTORY, "Specifies the directory to iterate over.",
				false);
		type.setExpert(false);
		types.add(type);

		types.addAll(super.getParameterTypes());

		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] changes = super.getIncompatibleVersionChanges();
		changes = Arrays.copyOf(changes, changes.length + 1);
		changes[changes.length - 1] = CHANGE_6_4_0_ERROR_WHEN_DIRECTORY_NOT_EXISTS;
		return changes;
	}
}
