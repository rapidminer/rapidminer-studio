/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.operator;

import com.rapidminer.operator.meta.branch.ProcessBranch;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.tools.io.Encoding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;


/**
 * This operator simply writed the specified text into the specified file. This can be useful in
 * combination with the {@link ProcessBranch} operator. For example, one could write the success or
 * non-success of a process into the same file depending on the condition specified by a process
 * branch.
 * 
 * @author Ingo Mierswa
 */
public class FileEchoOperator extends Operator {

	public static final String PARAMETER_FILE = "file";

	public static final String PARAMETER_TEXT = "text";

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public FileEchoOperator(OperatorDescription description) {
		super(description);

		dummyPorts.start();

		getTransformer().addRule(dummyPorts.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		File file = getParameterAsFile(PARAMETER_FILE, true);
		String text = getParameterAsString(PARAMETER_TEXT);

		PrintWriter out = null;
		try {
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Encoding.getEncoding(this)));
			out.println(text);
		} catch (IOException e) {
			throw new UserError(this, 303, file.getName(), e);
		} finally {
			if (out != null) {
				out.close();
			}
		}

		dummyPorts.passDataThrough();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeFile(PARAMETER_FILE,
				"The file into which this operator should write the specified text.", "out", false);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeText(PARAMETER_TEXT, "The text which should be written into the file.", TextType.PLAIN,
				false);
		type.setExpert(false);
		types.add(type);
		types.addAll(Encoding.getParameterTypes(this));
		return types;
	}
}
