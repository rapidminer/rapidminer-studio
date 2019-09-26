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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.PortProvider;
import com.rapidminer.parameter.conditions.PortConnectedCondition;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.WebServiceTools;


/**
 * Provides methods for creating and working with a file InputPort. Used by reading operators.
 *
 * @author Dominik Halfkann
 *
 */
public class FileInputPortHandler {

	private InputPort fileInputPort;
	private String fileParameterName;
	private Operator operator;

	/**
	 *
	 * @param fileParameterName
	 *            has to be a path to a file or a URL. If it is a URL the file will be downloaded
	 *            and temporary saved.
	 */
	public FileInputPortHandler(Operator operator, InputPort fileInputPort, String fileParameterName) {
		this.fileInputPort = fileInputPort;
		this.fileParameterName = fileParameterName;
		this.operator = operator;

		SimplePrecondition precondition = new SimplePrecondition(fileInputPort, new MetaData(FileObject.class)) {

			@Override
			protected boolean isMandatory() {
				return false;
			}
		};
		fileInputPort.addPrecondition(precondition);
	}

	private URL fileCachedForURL;
	private File cachedFile;

	/**
	 * Returns either the selected file referenced by the value of the parameter with the name
	 * {@link #getFileParameterName()} or the file delivered at {@link #fileInputPort}. Which of
	 * these options is chosen is determined by the parameter {@link #PARAMETER_DESTINATION_TYPE}.
	 * */
	public File getSelectedFile() throws OperatorException {
		if (!(fileInputPort.isConnected() || fileInputPort.getPorts().getOwner().getOperator().getProcess() == null
				&& fileInputPort.getRawData() != null)) {
			String fileParameter = operator.getParameterAsString(fileParameterName);
			try {
				URL url = new URL(fileParameter);
				// Check file:// manually to avoid copying to temp file
				if ("file".equals(url.getProtocol())) {
					return new File(url.getFile());
				} else if (fileCachedForURL != null && fileCachedForURL.equals(url)) {
					// for other URL protocols, download and return temp file,
					// but use cache in case
					// method is called twice.
					return cachedFile;
				} else {
					try {
						cachedFile = File.createTempFile("rm_file_", ".dump");
						cachedFile.deleteOnExit();
						try (InputStream urlStream = WebServiceTools.openStreamFromURL(url);
								FileOutputStream fos = new FileOutputStream(cachedFile)) {
							Tools.copyStreamSynchronously(urlStream, fos, true);
						}
					} catch (IOException e) {
						throw new OperatorException("Failed to access URL: " + url, e);
					}
					fileCachedForURL = url;
					return cachedFile;
				}
			} catch (MalformedURLException e) {
				File file = operator.getParameterAsFile(fileParameterName);
				return file;
			}
		} else {
			return fileInputPort.getData(FileObject.class).getFile();
		}
	}

	/**
	 * Same as {@link #getSelectedFile()}, but opens the stream.
	 * */
	public InputStream openSelectedFile() throws OperatorException, IOException {
		if (!(fileInputPort.isConnected() || fileInputPort.getPorts().getOwner().getOperator().getProcess() == null
				&& fileInputPort.getRawData() != null)) {
			return new FileInputStream(getSelectedFile());
		} else {
			return fileInputPort.getData(FileObject.class).openStream();
		}
	}

	/**
	 * Same as {@link #getSelectedFile()}, but returns true if file is specified (in the respective
	 * way).
	 * */
	public boolean isFileSpecified() {
		if (!(fileInputPort.isConnected() || fileInputPort.getPorts().getOwner().getOperator().getProcess() == null
				&& fileInputPort.getRawData() != null)) {
			return operator.isParameterSet(fileParameterName);
		} else {
			try {
				return fileInputPort.getDataOrNull(FileObject.class) != null;
			} catch (OperatorException e) {
				return false;
			}
		}

	}

	/**
	 * Creates the file parameter named by fileParameterName that depends on whether or not the port
	 * returned by the given PortProvider is connected.
	 *
	 * @param parameterHandler
	 *            used to check dependencies
	 * @param parameterName
	 *            Name of the parameter that is created
	 * @param description
	 *            Description of the parameter
	 * @param portProvider
	 *            port which accepts the FileObject. If this port is connected, the parameter will
	 *            be hidden.
	 * @param fileExtensions
	 *            allowed file types.
	 * */
	public static ParameterType makeFileParameterType(ParameterHandler parameterHandler, String parameterName,
			String description, PortProvider portProvider, String... fileExtensions) {
		return makeFileParameterType(parameterHandler, parameterName, description, portProvider, false, fileExtensions);
	}

	/**
	 * Creates the file parameter named by fileParameterName that depends on whether or not the port
	 * returned by the given PortProvider is connected.
	 *
	 * @param parameterHandler
	 *            used to check dependencies
	 * @param parameterName
	 *            Name of the parameter that is created
	 * @param description
	 *            Description of the parameter
	 * @param portProvider
	 *            port which accepts the FileObject. If this port is connected, the parameter will
	 *            be hidden.
	 * @param addAllFileFormatsFilter
	 *            defines whether a filter for all file extension should be added as default filter
	 *            for the file chooser dialog. This makes most sense for file reading operations
	 *            that allow to read files with multiple file extensions. For file writing
	 *            operations it is not recommended as the new filter will not add the correct file
	 *            ending when entering the path of a file that does not exist.
	 * @param fileExtensions
	 *            allowed file types.
	 * */
	public static ParameterType makeFileParameterType(ParameterHandler parameterHandler, String parameterName,
			String description, PortProvider portProvider, boolean addAllFileExtensionsFilter, String... fileExtensions) {
		String[] fileExtArray = new String[fileExtensions.length];
		int i = 0;
		for (String fileExtension : fileExtensions) {
			fileExtArray[i++] = fileExtension;
		}
		final ParameterTypeFile fileParam = new ParameterTypeFile(parameterName, description, true, fileExtArray);
		fileParam.setExpert(false);
		fileParam.setAddAllFileExtensionsFilter(addAllFileExtensionsFilter);
		fileParam.registerDependencyCondition(new PortConnectedCondition(parameterHandler, portProvider, true, false));
		return fileParam;
	}

	/**
	 * Uses a default description and a single file extension.
	 *
	 * @see #makeFileParameterType(ParameterHandler, String, String, PortProvider, String...)
	 * */
	public static ParameterType makeFileParameterType(ParameterHandler parameterHandler, String parameterName,
			String fileExtension, PortProvider portProvider) {
		return makeFileParameterType(parameterHandler, parameterName, "Name of the file to read the data from.",
				fileExtension, portProvider);
	}

	/**
	 * Uses a single allowed file extension.
	 *
	 * @see #makeFileParameterType(ParameterHandler, String, String, PortProvider, String...)
	 * */
	public static ParameterType makeFileParameterType(ParameterHandler parameterHandler, String parameterName,
			String description, String fileExtension, PortProvider portProvider) {
		return makeFileParameterType(parameterHandler, parameterName, description, portProvider, fileExtension);
	}

	/**
	 * Adds a new (file-)InputPortNotConnectedCondition for a given parameter.
	 *
	 * @param parameter
	 * @param parameterHandler
	 * @param portProvider
	 */
	public static void addFileDependencyCondition(ParameterType parameter, ParameterHandler parameterHandler,
			PortProvider portProvider) {
		parameter.registerDependencyCondition(new PortConnectedCondition(parameterHandler, portProvider, true, false));
	}

	/**
	 * Returns the specified filename or "InputFileObject" if the file OutputPort is connected.
	 *
	 * @return
	 * @throws OperatorException
	 */
	public String getSelectedFileDescription() throws OperatorException {
		if (!fileInputPort.isConnected()) {
			return operator.getParameterAsString(fileParameterName);
		} else {
			return "InputFileObject";
		}
	}
}
