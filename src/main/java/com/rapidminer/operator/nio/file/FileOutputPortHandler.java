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

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.PortProvider;
import com.rapidminer.parameter.conditions.PortConnectedCondition;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Provides methods for creating and working with a file InputPort. Used by reading operators.
 * 
 * @author Dominik Halfkann
 */
public class FileOutputPortHandler {

	private OutputPort fileOutputPort;
	private String fileParameterName;
	private Operator operator;

	public FileOutputPortHandler(Operator operator, OutputPort fileOutputPort, String fileParameterName) {
		this.fileOutputPort = fileOutputPort;
		this.fileParameterName = fileParameterName;
		this.operator = operator;
	}

	/**
	 * Returns an OutputStream, depending on whether the {@link #fileOutputPort} is connected or a
	 * file name is given.
	 */
	public OutputStream openSelectedFile() throws OperatorException {
		if (!fileOutputPort.isConnected()) {
			try {
				return new FileOutputStream(operator.getParameterAsFile(fileParameterName, true));
			} catch (FileNotFoundException e) {
				throw new UserError(operator, e, 303, operator.getParameterAsFile(fileParameterName), e.getMessage());
			}
		} else {
			return new ByteArrayOutputStream() {

				@Override
				public void close() throws IOException {
					super.close();
					fileOutputPort.deliver(new BufferedFileObject(this.toByteArray()));
				}
			};
		}
	}

	/**
	 * Returns an OutputStream, depending on whether the {@link #fileOutputPort} is connected, a
	 * file name is given and it should be appended to the end of the file.
	 */
	public OutputStream openSelectedFile(boolean append) throws OperatorException {
		if (!fileOutputPort.isConnected()) {
			try {
				return new FileOutputStream(operator.getParameterAsFile(fileParameterName, true), append);
			} catch (FileNotFoundException e) {
				throw new UserError(operator, e, 303, operator.getParameterAsFile(fileParameterName), e.getMessage());
			}
		} else {
			return new ByteArrayOutputStream() {

				@Override
				public void close() throws IOException {
					super.close();
					fileOutputPort.deliver(new BufferedFileObject(this.toByteArray()));
				}
			};
		}
	}

	/**
	 * Returns either the selected file referenced by the value of the parameter with the name
	 * {@link #getFileParameterName()} or the file delivered at {@link #fileOutputPort}. Which of
	 * these options is chosen is determined by the parameter {@link #PARAMETER_DESTINATION_TYPE}.
	 * */
	/*
	 * public File getSelectedFile() throws OperatorException { if(!fileOutputPort.isConnected()){
	 * return operator.getParameterAsFile(fileParameterName); } else { return
	 * fileOutputPort.getData(FileObject.class).getFile(); } }
	 */

	/**
	 * Same as {@link #getSelectedFile()}, but opens the stream.
	 * */
	/*
	 * public InputStream openSelectedFile() throws OperatorException, IOException {
	 * if(!fileOutputPort.isConnected()){ return new
	 * FileInputStream(operator.getParameterAsFile(fileParameterName)); } else { return
	 * fileOutputPort.getData(FileObject.class).openStream(); } }
	 */

	/**
	 * Same as {@link #getSelectedFile()}, but returns true if file is specified (in the respective
	 * way).
	 * */
	public boolean isFileSpecified() {
		if (!fileOutputPort.isConnected()) {
			return operator.isParameterSet(fileParameterName);
		} else {
			try {
				return (fileOutputPort.getData(IOObject.class) instanceof FileObject);
			} catch (OperatorException e) {
				return false;
			}
		}

	}

	/**
	 * Creates the file parameter named by fileParameterName that depends on whether or not the port
	 * returned by the given PortProvider is connected.
	 */
	public static ParameterType makeFileParameterType(ParameterHandler parameterHandler, String parameterName,
			String fileExtension, PortProvider portProvider) {
		final ParameterTypeFile fileParam = new ParameterTypeFile(parameterName, "Name of the file to write the data in.",
				true, new String[] { fileExtension });
		fileParam.setExpert(false);
		fileParam.registerDependencyCondition(new PortConnectedCondition(parameterHandler, portProvider, true, false));
		return fileParam;
	}

	/**
	 * Creates the file parameter named by fileParameterName that depends on whether or not the port
	 * returned by the given PortProvider is connected.
	 */
	public static ParameterType makeFileParameterType(ParameterHandler parameterHandler, String parameterName,
			PortProvider portProvider, String... fileExtension) {
		final ParameterTypeFile fileParam = new ParameterTypeFile(parameterName, "Name of the file to write the data in.",
				true, fileExtension);
		fileParam.setExpert(false);
		fileParam.registerDependencyCondition(new PortConnectedCondition(parameterHandler, portProvider, true, false));
		return fileParam;
	}

	/**
	 * Adds a new (file-)OutputPortNotConnectedCondition for a given parameter.
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
	 * Returns the specified filename or "OutputFileObject" if the file OutputPort is connected.
	 * 
	 * @return
	 * @throws OperatorException
	 */
	public String getSelectedFileDescription() throws OperatorException {
		if (!fileOutputPort.isConnected()) {
			return operator.getParameterAsString(fileParameterName);
		} else {
			return "OutputFileObject";
		}
	}
}
