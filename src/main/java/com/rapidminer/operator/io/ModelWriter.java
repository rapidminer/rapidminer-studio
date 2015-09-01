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
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
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
 * <p>
 * Writes the input model in the file specified by the corresponding parameter. Since models are
 * often written into files and loaded and applied in other processes or applications, this operator
 * offers three different writing modes for models:
 * </p>
 * 
 * <ul>
 * <li><em>XML</em>: in this mode, the models are written as plain text XML files. The file size is
 * usually the biggest in this mode (might be several hundred mega bytes so you should be cautious)
 * but this model type has the advantage that the user can inspect and change the files.</li>
 * <li><em>XML Zipped (default)</em>: In this mode, the models are written as zipped XML files.
 * Users can simply unzip the files and read or change the contents. The file sizes are smallest for
 * most models. For these reasons, this mode is the default writing mode for models although the
 * loading times are the longest due to the XML parsing and unzipping.</li>
 * <li><em>Binary</em>: In this mode, the models are written in an proprietary binary format. The
 * resulting model files cannot be inspected by the user and the file sizes are usually slightly
 * bigger then for the zipped XML files. The loading time, however, is smallers than the time needed
 * for the other modes.</li>
 * </ul>
 * 
 * <p>
 * This operator is also able to keep old files if the overwriting flag is set to false. However,
 * this could also be achieved by using some of the parameter macros provided by RapidMiner like
 * %{t} or %{a} (please refer to the tutorial section about macros).
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class ModelWriter extends AbstractWriter<Model> {

	/** The parameter name for &quot;Filename for the model file.&quot; */
	public static final String PARAMETER_MODEL_FILE = "model_file";

	/**
	 * The parameter name for &quot;Overwrite an existing file. If set to false then an index is
	 * appended to the filename.&quot;
	 */
	public static final String PARAMETER_OVERWRITE_EXISTING_FILE = "overwrite_existing_file";

	/** The parameter name for &quot;Indicates the type of the output&quot; */
	public static final String PARAMETER_OUTPUT_TYPE = "output_type";

	public ModelWriter(OperatorDescription description) {
		super(description, Model.class);
	}

	/** Writes the attribute set to a file. */
	@Override
	public Model write(Model model) throws OperatorException {
		File modelFile = getParameterAsFile(PARAMETER_MODEL_FILE, true);

		if (!getParameterAsBoolean(PARAMETER_OVERWRITE_EXISTING_FILE)) {
			if (modelFile.exists()) {
				File newFile = null;
				String fileName = modelFile.getAbsolutePath();
				int counter = 1;
				while (true) {
					// create the new file name
					String[] extension = fileName.split("\\.");
					extension[extension.length - 2] += "_" + counter + ".";
					String newFileName = stringArrayToString(extension);
					newFile = new File(newFileName);
					if (!newFile.exists()) {
						break;
					}
					counter++;
				}
				modelFile = newFile;
			}
		}
		int outputType = getParameterAsInt(PARAMETER_OUTPUT_TYPE);
		switch (outputType) {
			case OUTPUT_TYPE_XML:
				OutputStream out = null;
				try {
					out = new FileOutputStream(modelFile);
					model.write(out);
				} catch (IOException e) {
					throw new UserError(this, e, 303, new Object[] { modelFile, e.getMessage() });
				} finally {
					if (out != null) {
						try {
							out.close();
						} catch (IOException e) {
							logError("Cannot close stream to file " + modelFile);
						}
					}
				}
				break;
			case OUTPUT_TYPE_XML_ZIPPED:
				out = null;
				try {
					out = new GZIPOutputStream(new FileOutputStream(modelFile));
					model.write(out);
				} catch (IOException e) {
					throw new UserError(this, e, 303, new Object[] { modelFile, e.getMessage() });
				} finally {
					if (out != null) {
						try {
							out.close();
						} catch (IOException e) {
							logError("Cannot close stream to file " + modelFile);
						}
					}
				}
				break;
			case OUTPUT_TYPE_BINARY:
				ObjectOutputStream objectOut = null;
				try {
					objectOut = new ObjectOutputStream(new FileOutputStream(modelFile));
					objectOut.writeObject(model);
				} catch (IOException e) {
					throw new UserError(this, e, 303, new Object[] { modelFile, e.getMessage() });
				} finally {
					if (objectOut != null) {
						try {
							objectOut.close();
						} catch (IOException e) {
							logError("Cannot close stream to file " + modelFile);
						}
					}
				}
				break;
			default:
				break;
		}

		return model;
	}

	private String stringArrayToString(String[] filenameParts) {
		StringBuffer newString = new StringBuffer();
		for (int i = 0; i < filenameParts.length; i++) {
			newString.append(filenameParts[i]);
		}
		return newString.toString();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_MODEL_FILE, "Filename for the model file.", "mod", false));
		types.add(new ParameterTypeBoolean(PARAMETER_OVERWRITE_EXISTING_FILE,
				"Overwrite an existing file. If set to false then an index is appended to the filename.", true));
		types.add(new ParameterTypeCategory(PARAMETER_OUTPUT_TYPE, "Indicates the type of the output", OUTPUT_TYPES,
				OutputTypes.OUTPUT_TYPE_XML_ZIPPED));
		return types;
	}
}
