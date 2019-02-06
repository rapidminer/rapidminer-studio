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
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.nio.file.ZipEntryObject;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.PortProvider;
import com.rapidminer.parameter.conditions.PortConnectedCondition;


/**
 * This operator loops over the entries of a zip file.
 *
 * @author Marius Helf
 *
 */
public class ZippedFileIterator extends AbstractFileIterator {

	public static final String PARAMETER_ZIPFILE = "filename";
	public static final String PARAMETER_INTERNAL_DIRECTORY = "internal_directory";

	private InputPort fileInputPort = getInputPorts().createPort("file");

	public ZippedFileIterator(OperatorDescription description) {
		super(description);

		fileInputPort.addPrecondition(new SimplePrecondition(fileInputPort, new MetaData(FileObject.class)) {

			@Override
			protected boolean isMandatory() {
				return false;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		super.doWork();
	}

	@Override
	protected void iterate(Object currentParent, Pattern filter, boolean iterateSubDirs, boolean iterateFiles,
			boolean recursive) throws OperatorException {

		ZipFile zipFile = null;
		File physicalZipFile = null;
		if (fileInputPort.isConnected()) {
			FileObject zipFileObject = fileInputPort.getDataOrNull(FileObject.class);
			if (zipFileObject == null) {
				throw new UserError(this, 122, FileObject.class.getName());
			}
			physicalZipFile = zipFileObject.getFile();
		} else {
			physicalZipFile = getParameterAsFile(PARAMETER_ZIPFILE);
		}

		try {
			zipFile = new ZipFile(physicalZipFile);
		} catch (ZipException e) {
			throw new UserError(this, 403, physicalZipFile.getAbsolutePath() + " (" + e.getMessage() + ").");
		} catch (IOException e) {
			throw new UserError(this, 301, physicalZipFile.getAbsolutePath());
		}
		try {
			String rootDirectory = getParameterAsString(PARAMETER_INTERNAL_DIRECTORY);

			// init Operator progress and store the entries which meet all criteria
			LinkedList<EntryContainer> entriesOfIntrest = new LinkedList<>();
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry currentEntry = entries.nextElement();
				String fullPath = currentEntry.getName();
				String[] pathParts = fullPath.split("/");
				String fileName = pathParts[pathParts.length - 1];
				StringBuilder builder = new StringBuilder();
				for (int i = 0; i < pathParts.length - 1; ++i) {
					builder.append(pathParts[i]);
					if (i != pathParts.length - 2) {
						builder.append("/");
					}
				}
				String parentPath = builder.toString();

				if (iterateFiles && !currentEntry.isDirectory() || iterateSubDirs && currentEntry.isDirectory()) {
					if (recursive && parentPath.startsWith(rootDirectory)
							|| !recursive && (rootDirectory.isEmpty() && parentPath.isEmpty()
									|| !rootDirectory.isEmpty() && parentPath.equals(rootDirectory))) {
						if (matchesFilter(filter, fileName, fullPath, parentPath)) {
							FileObject fileObject = new ZipEntryObject(currentEntry, zipFile);
							entriesOfIntrest.add(new EntryContainer(fileName, fullPath, parentPath, fileObject));
						}
					}
				}
			}
			getProgress().setTotal(entriesOfIntrest.size());

			// do actual work
			for (EntryContainer entry : entriesOfIntrest) {
				doWorkForSingleIterationStep(entry.fileName, entry.fullPath, entry.parentPath, entry.fileObject);
				getProgress().step();
			}
		} catch (Exception e) {
			try {
				zipFile.close();
			} catch (IOException ioe) {
				e.addSuppressed(ioe);
			}
			throw e;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();

		ParameterType type;

		type = new ParameterTypeFile(PARAMETER_ZIPFILE, "The zipfile over whose entries this operator iterates.", "zip",
				true, false);
		type.registerDependencyCondition(new PortConnectedCondition(this, new PortProvider() {

			@Override
			public Port getPort() {
				return fileInputPort;
			}
		}, true, false));
		type.setPrimary(true);
		types.add(type);

		type = new ParameterTypeString(PARAMETER_INTERNAL_DIRECTORY,
				"The directory inside the zipfile from which the entries should be taken.", "");
		type.setExpert(true);
		type.setOptional(true);
		types.add(type);

		types.addAll(super.getParameterTypes());
		return types;
	}

}
