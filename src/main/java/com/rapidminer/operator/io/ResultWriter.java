/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.operator.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.tools.ResultService;
import com.rapidminer.tools.io.Encoding;


/**
 * This operator can be used at each point in an operator chain. It returns all input it receives
 * without any modification. Every input object which implements the
 * {@link com.rapidminer.operator.ResultObject} interface (which is the case for almost all objects
 * generated by the core RapidMiner operators) will write its results to the file specified by the
 * parameter <var>result_file</var>. If the definition of this parameter is ommited then the global
 * result file parameter with the same name of the ProcessRootOperator (the root of the process)
 * will be used. If this file is also not specified the results are simply written to the console
 * (standard out).
 * 
 * @author Ingo Mierswa
 */
public class ResultWriter extends Operator {

	/**
	 * The parameter name for &quot;Appends the descriptions of the input objects to this file. If
	 * empty, use the general file defined in the process root operator.&quot;
	 */
	public static final String PARAMETER_RESULT_FILE = "result_file";
	private boolean firstRun = true;

	private PortPairExtender portExtender = new PortPairExtender("input", getInputPorts(), getOutputPorts());

	public ResultWriter(OperatorDescription description) {
		super(description);
		portExtender.start();
		getTransformer().addRule(portExtender.makePassThroughRule());
	}

	/**
	 * Use the ResultService to write the results of all input ResultObjects into the result file.
	 */
	@Override
	public void doWork() throws OperatorException {
		IOObject[] input = portExtender.getData(IOObject.class).toArray(new IOObject[0]);
		File file = getParameterAsFile(PARAMETER_RESULT_FILE, true);
		if (file != null) {
			PrintWriter out = null;
			try {
				out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, !firstRun),
						Encoding.getEncoding(this)));
				firstRun = false;
			} catch (IOException e) {
				throw new UserError(this, 301, file);
			}
			if (out != null) {
				ResultService.logResult("Results of ResultWriter '" + getName() + "' [" + getApplyCount() + "]: ", out);
				for (int i = 0; i < input.length; i++) {
					if (input[i] instanceof ResultObject) {
						ResultService.logResult((ResultObject) input[i], out);
					}
				}
				out.close();
			}
		} else {
			ResultService.logResult("Results of ResultWriter '" + getName() + "' [" + getApplyCount() + "]: ");
			for (int i = 0; i < input.length; i++) {
				if (input[i] instanceof ResultObject) {
					ResultService.logResult((ResultObject) input[i]);
				}
			}
		}
		portExtender.passDataThrough();
	}

	@Override
	public void processStarts() throws OperatorException {
		super.processStarts();
		this.firstRun = true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeFile(
				PARAMETER_RESULT_FILE,
				"Appends the descriptions of the input objects to this file. If empty, use the general file defined in the process root operator.",
				"res", true);
		type.setExpert(false);
		types.add(type);

		types.addAll(Encoding.getParameterTypes(this));
		return types;
	}
}
