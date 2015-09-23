/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.io;

import static com.rapidminer.operator.io.OutputTypes.OUTPUT_TYPES;
import static com.rapidminer.operator.io.OutputTypes.OUTPUT_TYPE_BINARY;
import static com.rapidminer.operator.io.OutputTypes.OUTPUT_TYPE_XML;
import static com.rapidminer.operator.io.OutputTypes.OUTPUT_TYPE_XML_ZIPPED;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;


/**
 * Generic writer for all types of IOObjects. Writes one of the input objects into a given file.
 * 
 * @author Ingo Mierswa
 */
public class IOObjectWriter extends Operator {

	private InputPort objectInput = getInputPorts().createPort("object", IOObject.class);
	private OutputPort objectOutput = getOutputPorts().createPort("object");
	/** The parameter name for &quot;Filename of the object file.&quot; */
	public static final String PARAMETER_OBJECT_FILE = "object_file";

	// /** The parameter name for &quot;The class of the object(s) which should be saved.&quot; */
	// public static final String PARAMETER_IO_OBJECT = "io_object";
	//
	// /** The parameter name for &quot;Defines which input object should be written.&quot; */
	// public static final String PARAMETER_WRITE_WHICH = "write_which";
	//
	/** The parameter name for &quot;Indicates the type of the output&quot; */
	public static final String PARAMETER_OUTPUT_TYPE = "output_type";

	public static final String PARAMETER_CONTINUE_ON_ERROR = "continue_on_error";

	public IOObjectWriter(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new PassThroughRule(objectInput, objectOutput, false));
	}

	/** Writes the attribute set to a file. */
	@Override
	public void doWork() throws OperatorException {
		IOObject object = objectInput.getData(IOObject.class);
		File objectFile = getParameterAsFile(PARAMETER_OBJECT_FILE, true);

		int outputType = getParameterAsInt(PARAMETER_OUTPUT_TYPE);
		switch (outputType) {
			case OUTPUT_TYPE_XML:
				OutputStream out = null;
				try {
					out = new FileOutputStream(objectFile);
					object.write(out);
				} catch (IOException e) {
					if (!getParameterAsBoolean(PARAMETER_CONTINUE_ON_ERROR)) {
						throw new UserError(this, e, 303, new Object[] { objectFile, e.getMessage() });
					} else {
						logError("Could not write IO Object to file " + objectFile + ": " + e.getMessage());
					}
				} finally {
					if (out != null) {
						try {
							out.close();
						} catch (IOException e) {
							logError("Cannot close stream to file " + objectFile);
						}
					}
				}
				break;
			case OUTPUT_TYPE_XML_ZIPPED:
				out = null;
				try {
					out = new GZIPOutputStream(new FileOutputStream(objectFile));
					object.write(out);
				} catch (IOException e) {
					if (!getParameterAsBoolean(PARAMETER_CONTINUE_ON_ERROR)) {
						throw new UserError(this, e, 303, new Object[] { objectFile, e.getMessage() });
					} else {
						logError("Could not write IO Object to file " + objectFile + ": " + e.getMessage());
					}
				} finally {
					if (out != null) {
						try {
							out.close();
						} catch (IOException e) {
							logError("Cannot close stream to file " + objectFile);
						}
					}
				}
				break;
			case OUTPUT_TYPE_BINARY:
				ObjectOutputStream objectOut = null;
				try {
					objectOut = new ObjectOutputStream(new FileOutputStream(objectFile));
					objectOut.writeObject(object);
				} catch (IOException e) {
					if (!getParameterAsBoolean(PARAMETER_CONTINUE_ON_ERROR)) {
						throw new UserError(this, e, 303, new Object[] { objectFile, e.getMessage() });
					} else {
						logError("Could not write IO Object to file " + objectFile + ": " + e.getMessage());
					}
				} finally {
					if (objectOut != null) {
						try {
							objectOut.close();
						} catch (IOException e) {
							logError("Cannot close stream to file " + objectFile);
						}
					}
				}
				break;
			default:
				break;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_OBJECT_FILE, "Filename of the object file.", "ioo", false));
		// types.add(new ParameterTypeCategory(PARAMETER_IO_OBJECT,
		// "The class of the object(s) which should be saved.", getIOObjectNames(), 0));
		// types.add(new ParameterTypeInt(PARAMETER_WRITE_WHICH,
		// "Defines which input object should be written.", 1, Integer.MAX_VALUE, 1));
		types.add(new ParameterTypeCategory(PARAMETER_OUTPUT_TYPE, "Indicates the type of the output", OUTPUT_TYPES,
				OutputTypes.OUTPUT_TYPE_XML_ZIPPED));
		types.add(new ParameterTypeBoolean(PARAMETER_CONTINUE_ON_ERROR, "Defines behavior on errors", false));
		return types;
	}
}
