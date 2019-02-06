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
package com.rapidminer.operator.filesystem;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.tools.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;


/**
 * 
 * This operator copies a file to another location. If the destination is a directory, the file is
 * copied in there. If the target already exists and overwriting is enabled, the existing file will
 * be overwritten. Else, an exception is thrown.
 * 
 * @author Philipp Kersting
 * 
 */

public class CopyFileOperator extends Operator {

	public static final String PARAMETER_SOURCE_FILE = "source_file";
	public static final String PARAMETER_NEW_FILE = "new_file";
	public static final String PARAMETER_OVERWRITE = "overwrite";

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public CopyFileOperator(OperatorDescription description) {
		super(description);
		dummyPorts.start();
		getTransformer().addRule(dummyPorts.makePassThroughRule());
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeFile(PARAMETER_SOURCE_FILE, "The source file to be copied.", "*", false, false));
		types.add(new ParameterTypeFile(PARAMETER_NEW_FILE, "The file that should be created.", "*", false, false));
		types.add(new ParameterTypeBoolean(PARAMETER_OVERWRITE,
				"Defines whether an already existing file should be overwritten.", false, false));

		return types;
	}

	@Override
	public void doWork() throws OperatorException {
		String sourceFileName = getParameterAsString(PARAMETER_SOURCE_FILE);
		String newFileName = getParameterAsString(PARAMETER_NEW_FILE);
		Boolean overwrite = getParameterAsBoolean(PARAMETER_OVERWRITE);

		File newFile = new File(newFileName);
		if (newFile.exists()) {
			copyFile(newFile, overwrite, sourceFileName, newFileName);
		} else {

			File parentFile = newFile.getParentFile();
			if (!parentFile.exists()) {
				boolean success = parentFile.mkdirs();
				if (!success) {
					throw new UserError(this, "create_directory.failure", parentFile);
				}
			}

			copyFile(newFile, overwrite, sourceFileName, newFileName);

		}

		dummyPorts.passDataThrough();

	}

	private void copyFile(File newFile, boolean overwrite, String sourceFileName, String newFileName) throws UserError {
		if (newFile.isDirectory()) {
			newFile = new File(newFile.getAbsoluteFile() + File.separator + new File(sourceFileName).getName());
		}
		if ((!newFile.exists() || overwrite) && new File(sourceFileName).exists()) {
			try (InputStream in = new FileInputStream(sourceFileName); OutputStream out = new FileOutputStream(newFile)) {
				Tools.copyStreamSynchronously(in, out, true);
			} catch (IOException e) {
				throw new UserError(this, e, "copy_file.ioerror", sourceFileName, newFileName, e.getLocalizedMessage());
			}
		} else if (newFile.exists() && !overwrite) {
			throw new UserError(this, "copy_file.exists", newFileName);
		} else if (new File(sourceFileName).exists() == false) {
			throw new UserError(this, "301", new File(sourceFileName));
		}
	}
}
