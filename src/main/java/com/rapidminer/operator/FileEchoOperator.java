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
package com.rapidminer.operator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import com.rapidminer.operator.meta.branch.ProcessBranch;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.tools.io.Encoding;


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

	private static final String PARAMETER_MODE = "mode";

	private static final String REPLACE = "replace";
	private static final String APPEND = "append";

	private static final String[] INSERT_MODES = { REPLACE, APPEND };
	/** Replace was the default and only option before version 7.6 */
	private static final int REPLACE_INDEX = Arrays.asList(INSERT_MODES).indexOf(REPLACE);

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
		String modeString = getParameterAsString(PARAMETER_MODE);
		OpenOption mode = APPEND.equals(modeString) ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING;

		try {
			Files.write(file.toPath(), Arrays.asList(text), Encoding.getEncoding(this), StandardOpenOption.CREATE, mode);
		} catch (IOException e) {
			throw new UserError(this, 303, file.getName(), e);
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
		type.setPrimary(true);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_MODE,
				"The text insertion mode, replace any existing file content, or append the text to the end of the file.",
				INSERT_MODES, REPLACE_INDEX, false);
		types.add(type);
		types.addAll(Encoding.getParameterTypes(this));
		return types;
	}
}
