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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
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
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
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

	private List<File> myTempFiles = new LinkedList<File>();

	public LoadFileOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(fileOutputPort, FileObject.class));
		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				try {
					checkMetaData();
				} catch (UserError e) {
					addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "passthrough", e.getMessage()));
				}
			}
		});
	}

	/**
	 * @throws UserError
	 * 
	 */
	protected void checkMetaData() throws UserError {
		String source;
		try {
			switch (getParameterAsInt(PARAMETER_SOURCE_TYPE)) {
				case SOURCE_TYPE_FILE:
					File file = getParameterAsFile(PARAMETER_FILENAME);

					// check if file exists and is readable
					if (!file.exists()) {
						throw new UserError(this, "301", file);
					} else if (!file.canRead()) {
						throw new UserError(this, "302", file, "");
					}
					break;
				case SOURCE_TYPE_URL:
					// check only if url is valid, not if it's accessible for performance reasons
					try {
						// ignore this warning - only create URL to check if the parameter string
						// represents a valid url syntax
						new URL(getParameterAsString(PARAMETER_URL));
					} catch (MalformedURLException e) {
						throw new UserError(this, e, "313", getParameterAsString(PARAMETER_URL));
					}
					break;
				case SOURCE_TYPE_REPOSITORY:
					RepositoryLocation location = getParameterAsRepositoryLocation(PARAMETER_REPOSITORY_LOCATION);
					source = location.getAbsoluteLocation();

					// check if entry exists
					Entry entry;
					try {
						entry = location.locateEntry();
					} catch (RepositoryException e) {
						throw new UserError(this, "319", e, source);
					}
					if (entry == null) {
						throw new UserError(this, "312", source, "entry does not exist");
					} else if (!(entry instanceof BlobEntry)) {
						throw new UserError(this, "942", source, "blob", entry.getType());
					}
					break;
			}
		} catch (UndefinedParameterError e) {
			// handled by parameter checks in super class
		}
	}

	@Override
	public void doWork() throws OperatorException {
		String source;
		FileObject result;
		switch (getParameterAsInt(PARAMETER_SOURCE_TYPE)) {
			case SOURCE_TYPE_FILE:
				File file = getParameterAsFile(PARAMETER_FILENAME);

				// check if file exists and is readable
				if (!file.exists()) {
					throw new UserError(this, "301", file);
				} else if (!file.canRead()) {
					throw new UserError(this, "302", file, "");
				}

				source = file.getAbsolutePath();
				result = new SimpleFileObject(file);
				break;
			case SOURCE_TYPE_URL:
				try {
					URL url = new URL(getParameterAsString(PARAMETER_URL));
					source = url.toString();
					byte[] fileBytes = Tools.readInputStream(WebServiceTools.openStreamFromURL(url));
					result = new BufferedFileObject(fileBytes);
				} catch (MalformedURLException e) {
					throw new UserError(this, e, "313", getParameterAsString(PARAMETER_URL));
				} catch (IOException e) {
					throw new UserError(this, "314", getParameterAsString(PARAMETER_URL));
				}
				break;
			case SOURCE_TYPE_REPOSITORY:
				RepositoryLocation location = getParameterAsRepositoryLocation(PARAMETER_REPOSITORY_LOCATION);

				// check if entry exists
				Entry entry;
				try {
					entry = location.locateEntry();
				} catch (RepositoryException e) {
					throw new UserError(this, "319", e, location.getAbsoluteLocation());
				}
				if (entry == null) {
					throw new UserError(this, "312", location.getAbsoluteLocation(), "entry does not exist");
				} else if (!(entry instanceof BlobEntry)) {
					throw new UserError(this, "942", location.getAbsoluteLocation(), "blob", entry.getType());
				}

				source = location.getAbsoluteLocation();
				result = new RepositoryBlobObject(location);
				break;
			default:
				// cannot happen
				throw new OperatorException("Illegal source type: " + getParameterAsString(PARAMETER_SOURCE_TYPE));
		}
		result.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, source);
		fileOutputPort.deliver(result);
	}

	@Override
	public void processFinished() throws OperatorException {
		for (File file : myTempFiles) {
			file.delete();
		}
		myTempFiles.clear();
		super.processFinished();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();

		parameterTypes.add(new ParameterTypeCategory(PARAMETER_SOURCE_TYPE,
				"Choose wether to open a file, a URL or a repository entry.", SOURCE_TYPES, SOURCE_TYPE_FILE, true));

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
		parameterTypeRepositoryLocation.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SOURCE_TYPE,
				SOURCE_TYPES, true, SOURCE_TYPE_REPOSITORY));
		parameterTypes.add(parameterTypeRepositoryLocation);

		return parameterTypes;
	}

}
