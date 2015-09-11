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

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.postprocessing.Threshold;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * Reads a threshold from a file. The first line must hold the threshold, the second the value of
 * the first class, and the second the value of the second class. This file can be written in
 * another process using the {@link ThresholdWriter}.
 * 
 * @author Ingo Mierswa
 */
public class ThresholdLoader extends AbstractReader<Threshold> {

	/** The parameter name for &quot;Filename for the threshold file.&quot; */
	public static final String PARAMETER_THRESHOLD_FILE = "threshold_file";

	public ThresholdLoader(OperatorDescription description) {
		super(description, Threshold.class);
	}

	/** Loads the threshold from a file. */
	@Override
	public Threshold read() throws OperatorException {
		File thresholdFile = getParameterAsFile(PARAMETER_THRESHOLD_FILE);
		Threshold threshold = null;
		try {
			InputStream in = new FileInputStream(thresholdFile);
			Document document = null;
			try {
				document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
			} catch (SAXException e1) {
				throw new IOException(e1.getMessage());
			} catch (ParserConfigurationException e1) {
				throw new IOException(e1.getMessage());
			}

			Element thresholdElement = document.getDocumentElement();
			if (!thresholdElement.getTagName().equals("threshold")) {
				throw new IOException("Outer tag of threshold file must be <threshold>");
			}

			String thresholdValueString = thresholdElement.getAttribute("value");
			String thresholdFirst = thresholdElement.getAttribute("first");
			String thresholdSecond = thresholdElement.getAttribute("second");
			double thresholdValue = Double.parseDouble(thresholdValueString);
			threshold = new Threshold(thresholdValue, thresholdFirst, thresholdSecond);
			in.close();
		} catch (IOException e) {
			throw new UserError(this, e, 303, new Object[] { thresholdFile, e.getMessage() });
		}

		return threshold;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_THRESHOLD_FILE, "Filename for the threshold file.", "thr", false));
		return types;
	}
}
