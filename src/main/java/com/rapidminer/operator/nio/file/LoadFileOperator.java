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
package com.rapidminer.operator.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.WebServiceTools;


/**
 * @author Nils Woehler, Marius Helf
 */
public class LoadFileOperator extends Operator {

	public static final String PARAMETER_FILENAME = "filename";
	public static final String PARAMETER_URL = "url";
	public static final String PARAMETER_REPOSITORY_LOCATION = "repository_entry";

	public static final String[] SOURCE_TYPES = new String[] { "file", "URL", "repository blob entry" };
	public static final String PARAMETER_SOURCE_TYPE = "resource_type";
	public static final int SOURCE_TYPE_FILE = 0;
	public static final int SOURCE_TYPE_URL = 1;
	public static final int SOURCE_TYPE_REPOSITORY = 2;

	public OutputPort fileOutputPort = getOutputPorts().createPort("file");

	public LoadFileOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(
				new GenerateNewMDRule(fileOutputPort, FileObject.class){
					@Override
					public void transformMD() {
						MetaData clone = getUnmodifiedMetaData();
						try {
							clone.setAnnotations(checkMetaDataAndGetAnnotations());
						} catch (UserError e) {
							addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "passthrough", e.getMessage()));
						}
						clone.addToHistory(fileOutputPort);
						fileOutputPort.deliverMD(modifyMetaData(clone));
					}
				}
		);
	}

	/**
	 * @throws UserError
	 *
	 */
	protected void checkMetaData() throws UserError {
		checkMetaDataAndGetAnnotations();
	}

	/**
	 * Checks the meta data and creates the {@link Annotations#KEY_SOURCE} and {@link Annotations#KEY_FILENAME} if possible
	 *
	 * @throws UserError in case the current configuration is not valid
	 * @return the annotations for the selected entry
	 * @since 9.6
	 */
	private Annotations checkMetaDataAndGetAnnotations() throws UserError {
		String source = null;
		String fileName = null;
		try {
			switch (getParameterAsInt(PARAMETER_SOURCE_TYPE)) {
				case SOURCE_TYPE_FILE:
					File file = getFile();
					source = file.getAbsolutePath();
					fileName =  file.getName();
					break;
				case SOURCE_TYPE_URL:
					// check only if url is valid, not if it's accessible for performance reasons
					source = getURL().toExternalForm();
					// it's hard to always get a good filename from an url
					fileName = null;
					break;
				case SOURCE_TYPE_REPOSITORY:
					// check if entry exists
					DataEntry entry = getEntry();
					source = entry.getLocation().getAbsoluteLocation();
					fileName = entry.getName();
					break;
			}
		} catch (UndefinedParameterError e) {
			// handled by parameter checks in super class
		}
		Annotations annotations = new Annotations();
		if (fileName != null) {
			annotations.setAnnotation(Annotations.KEY_FILENAME, fileName);
		}
		if (source != null) {
			annotations.setAnnotation(Annotations.KEY_SOURCE, source);
		}
		return annotations;
	}

	@Override
	public void doWork() throws OperatorException {
		final FileObject result;
		switch (getParameterAsInt(PARAMETER_SOURCE_TYPE)) {
			case SOURCE_TYPE_FILE:
				result = new SimpleFileObject(getFile());
				break;
			case SOURCE_TYPE_URL:
				try {
					byte[] fileBytes = Tools.readInputStream(WebServiceTools.openStreamFromURL(getURL()));
					result = new BufferedFileObject(fileBytes);
				} catch (IOException e) {
					throw new UserError(this, "314", getParameterAsString(PARAMETER_URL));
				}
				break;
			case SOURCE_TYPE_REPOSITORY:
				// check if entry exists
				DataEntry entry = getEntry();
				if (entry instanceof BinaryEntry) {
					RepositoryLocation binLoc = new RepositoryLocationBuilder().withExpectedDataEntryType(BinaryEntry.class).
							buildFromAbsoluteLocation(entry.getLocation().getAbsoluteLocation());
					result = new BinaryEntryFileObject(binLoc);
				} else {
					RepositoryLocation blobLoc = new RepositoryLocationBuilder().withExpectedDataEntryType(BlobEntry.class).
							buildFromAbsoluteLocation(entry.getLocation().getAbsoluteLocation());
					result = new RepositoryBlobObject(blobLoc);
				}
				break;
			default:
				// cannot happen
				throw new OperatorException("Illegal source type: " + getParameterAsString(PARAMETER_SOURCE_TYPE));
		}
		result.getAnnotations().addAll(checkMetaDataAndGetAnnotations());
		fileOutputPort.deliver(result);
	}

	/**
	 * @return the selected {@link #PARAMETER_FILENAME}
	 * @throws UserError if the parameter is not set or invalid
	 * @since 9.6
	 */
	private File getFile() throws UserError{
		File file = getParameterAsFile(PARAMETER_FILENAME);

		// check if file exists and is readable
		if (!file.exists()) {
			throw new UserError(this, "301", file);
		} else if (!file.canRead()) {
			throw new UserError(this, "302", file, "");
		}
		return file;
	}

	/**
	 * @return the selected {@link #PARAMETER_URL}
	 * @throws UserError if the parameter is not set or invalid
	 * @since 9.6
	 */
	private URL getURL() throws UserError{
		try {
			// create URL to check if the parameter string represents a valid url syntax
			return new URL(getParameterAsString(PARAMETER_URL));
		} catch (MalformedURLException e) {
			throw new UserError(this, e, "313", getParameterAsString(PARAMETER_URL));
		}
	}

	/**
	 * @return the selected {@link #PARAMETER_REPOSITORY_LOCATION}
	 * @throws UserError if the parameter is not set or invalid
	 * @since 9.6
	 */
	private DataEntry getEntry() throws UserError{
		RepositoryLocation location = getParameterAsRepositoryLocationData(PARAMETER_REPOSITORY_LOCATION, DataEntry.class);
		String absoluteLocation = location.getAbsoluteLocation();

		// check if entry exists
		DataEntry entry;
		try {
			entry = location.locateData();
		} catch (RepositoryException e) {
			throw new UserError(this, "319", e, absoluteLocation);
		}
		if (entry == null) {
			throw new UserError(this, "312", absoluteLocation, "entry does not exist");
		} else if (!(entry instanceof BlobEntry || entry instanceof BinaryEntry)) {
			throw new UserError(this, "942", absoluteLocation, "blob", entry.getType());
		}
		return entry;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();

		parameterTypes.add(new ParameterTypeCategory(PARAMETER_SOURCE_TYPE,
				"Choose whether to open a file, a URL or a repository entry.", SOURCE_TYPES, SOURCE_TYPE_FILE, true));

		ParameterTypeFile parameterTypeFile = new ParameterTypeFile(PARAMETER_FILENAME, "File to open", null, true, false);
		parameterTypeFile.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SOURCE_TYPE, SOURCE_TYPES,
				true, SOURCE_TYPE_FILE));
		parameterTypeFile.setPrimary(true);
		parameterTypes.add(parameterTypeFile);

		ParameterTypeString parameterTypeUrl = new ParameterTypeString(PARAMETER_URL, "URL to open", true, false);
		parameterTypeUrl.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SOURCE_TYPE, SOURCE_TYPES, true,
				SOURCE_TYPE_URL));
		parameterTypes.add(parameterTypeUrl);

		ParameterTypeRepositoryLocation parameterTypeRepositoryLocation = new ParameterTypeRepositoryLocation(
				PARAMETER_REPOSITORY_LOCATION, "repository entry to open", true);
		parameterTypeRepositoryLocation.setExpert(false);
		parameterTypeRepositoryLocation.setRepositoryFilter(RepositoryTools.ONLY_BLOB_AND_BINARY_ENTRIES);
		parameterTypeRepositoryLocation.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SOURCE_TYPE,
				SOURCE_TYPES, true, SOURCE_TYPE_REPOSITORY));
		parameterTypes.add(parameterTypeRepositoryLocation);

		return parameterTypes;
	}

}
