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
package com.rapidminer.operator.nio.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.tools.Tools;


/**
 * Operator to write a {@link FileObject} to a file or repository blob.
 * 
 * @author Simon Fischer
 * 
 */
public class WriteFileOperator extends Operator {

	public static final String PARAMETER_FILENAME = "filename";
	public static final String PARAMETER_REPOSITORY_LOCATION = "repository_entry";

	public static final String[] DESTINATION_TYPES = new String[] { "file", "repository blob entry" };
	public static final String PARAMETER_DESTINATION_TYPE = "resource_type";
	public static final int DESTINATION_TYPE_FILE = 0;
	public static final int DESTINATION_TYPE_REPOSITORY = 1;
	public static final String PARAMETER_MIME_TYPE = "mime_type";

	private static final String MIME_TYPE_OCTESTSTREAM = "application/octet-stream";
	private static final String[] MIME_TYPES = new String[] { MIME_TYPE_OCTESTSTREAM, "application/xml", "application/zip",
			"application/vnd.ms-excel", "text/html", "text/csv" };

	public InputPort fileInputPort = getInputPorts().createPort("file", FileObject.class);
	public OutputPort fileOutputPort = getOutputPorts().createPort("file");

	public WriteFileOperator(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(fileInputPort, fileOutputPort);
	}

	@Override
	public void doWork() throws OperatorException {
		FileObject fileObject = fileInputPort.getData(FileObject.class);
		OutputStream out = null;
		String destName;

		try {

			switch (getParameterAsInt(PARAMETER_DESTINATION_TYPE)) {
				case DESTINATION_TYPE_FILE:
					File file = getParameterAsFile(PARAMETER_FILENAME, true);
					destName = file.getAbsolutePath();
					try {
						out = new FileOutputStream(file);
					} catch (FileNotFoundException e) {
						throw new UserError(this, 303, file, e);
					}
					break;
				case DESTINATION_TYPE_REPOSITORY:
					RepositoryLocation location = getParameterAsRepositoryLocation(PARAMETER_REPOSITORY_LOCATION);
					destName = location.toString();
					try {
						BlobEntry blob = RepositoryManager.getInstance(getProcess().getRepositoryAccessor())
								.getOrCreateBlob(location);
						out = blob.openOutputStream(getParameterAsString(PARAMETER_MIME_TYPE));
					} catch (RepositoryException e) {
						throw new UserError(this, 315, location, e);
					}
					break;
				default:
					// cannot happen
					throw new OperatorException("Illegal destination type: "
							+ getParameterAsString(PARAMETER_DESTINATION_TYPE));
			}
			try {
				Tools.copyStreamSynchronously(fileObject.openStream(), out, true);
			} catch (IOException e) {
				throw new UserError(this, 322, destName, e);
			}
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// nothing we can do
				}
			}
		}

		fileOutputPort.deliver(fileObject);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();

		parameterTypes
				.add(new ParameterTypeCategory(PARAMETER_DESTINATION_TYPE,
						"Choose whether to open a file, a URL or a repository entry.", DESTINATION_TYPES,
						DESTINATION_TYPE_FILE, true));

		ParameterTypeFile parameterTypeFile = new ParameterTypeFile(PARAMETER_FILENAME, "File to save to.", null, true,
				false);
		parameterTypeFile.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_DESTINATION_TYPE,
				DESTINATION_TYPES, true, DESTINATION_TYPE_FILE));
		parameterTypeFile.setPrimary(true);
		parameterTypes.add(parameterTypeFile);

		ParameterTypeRepositoryLocation parameterTypeRepositoryLocation = new ParameterTypeRepositoryLocation(
				PARAMETER_REPOSITORY_LOCATION, "Repository entry to open. This must point to a blob.", true, false, false,
				true, true, true);
		parameterTypeRepositoryLocation.setExpert(false);
		parameterTypeRepositoryLocation.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_DESTINATION_TYPE,
				DESTINATION_TYPES, true, DESTINATION_TYPE_REPOSITORY));
		parameterTypes.add(parameterTypeRepositoryLocation);

		ParameterType mimeType = new ParameterTypeStringCategory(PARAMETER_MIME_TYPE,
				"If saved to the repository, this specifies the mime type to assign to the blob.", MIME_TYPES,
				MIME_TYPE_OCTESTSTREAM);
		mimeType.setExpert(true);
		mimeType.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_DESTINATION_TYPE, DESTINATION_TYPES,
				false, DESTINATION_TYPE_REPOSITORY));
		parameterTypes.add(mimeType);

		return parameterTypes;

	}
}
