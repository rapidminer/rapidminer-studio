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

import com.rapidminer.RapidMiner;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.postprocessing.Threshold;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.tools.io.Encoding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;


/**
 * Writes the given threshold into a file. The first line holds the threshold, the second the value
 * of the first class, and the second the value of the second class. This file can be read in
 * another process using the {@link ThresholdLoader}.
 * 
 * @author Ingo Mierswa
 */
public class ThresholdWriter extends AbstractWriter<Threshold> {

	/** The parameter name for &quot;Filename for the threshold file.&quot; */
	public static final String PARAMETER_THRESHOLD_FILE = "threshold_file";

	public ThresholdWriter(OperatorDescription description) {
		super(description, Threshold.class);
	}

	/** Writes the threshold to a file. */
	@Override
	public Threshold write(Threshold threshold) throws OperatorException {
		File thresholdFile = getParameterAsFile(PARAMETER_THRESHOLD_FILE, true);
		PrintWriter out = null;
		try {
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(thresholdFile), Encoding.getEncoding(this)));
			out.println("<?xml version=\"1.0\" encoding=\"" + Encoding.getEncoding(this) + "\"?>");
			out.println("<threshold version=\"" + RapidMiner.getShortVersion() + "\" value=\"" + threshold.getThreshold()
					+ "\" first=\"" + threshold.getZeroClass() + "\" second=\"" + threshold.getOneClass() + "\"/>");
			out.close();
		} catch (IOException e) {
			throw new UserError(this, e, 303, new Object[] { thresholdFile, e.getMessage() });
		} finally {
			if (out != null) {
				out.close();
			}
		}

		return threshold;
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeFile(PARAMETER_THRESHOLD_FILE, "Filename for the threshold file.", "thr", false));
		types.addAll(super.getParameterTypes());
		return types;
	}
}
