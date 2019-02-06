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
package com.rapidminer.tools.io;

import com.rapidminer.RapidMiner;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.ParameterService;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.LinkedList;
import java.util.List;


/**
 * Collection of static helper methods to add and evaluate parameters to specify an encoding.
 * 
 * @author Sebastian Land
 * 
 */
public class Encoding {

	public static final String PARAMETER_ENCODING = "encoding";

	public static final String[] CHARSETS;
	static {
		CHARSETS = new String[Charset.availableCharsets().size() + 1];
		CHARSETS[0] = RapidMiner.SYSTEM_ENCODING_NAME;
		int i = 0;
		for (String charSet : Charset.availableCharsets().keySet()) {
			CHARSETS[i + 1] = charSet;
			i++;
		}
	}

	public static Charset getEncoding(Operator handler) throws UndefinedParameterError, UserError {
		String selectedCharsetName = handler.getParameterAsString(PARAMETER_ENCODING);
		if (RapidMiner.SYSTEM_ENCODING_NAME.equals(selectedCharsetName)) {
			return Charset.defaultCharset();
		}
		try {
			return Charset.forName(selectedCharsetName);
		} catch (IllegalCharsetNameException e) {
			throw new UserError(handler, 207, selectedCharsetName, PARAMETER_ENCODING, "No legal charset name.");
		} catch (UnsupportedCharsetException e) {
			throw new UserError(handler, 207, selectedCharsetName, PARAMETER_ENCODING,
					"Charset not supported on this Java VM.");
		} catch (IllegalArgumentException e) {
			throw new UserError(handler, 207, selectedCharsetName, PARAMETER_ENCODING, "Select different charset.");
		}
	}

	public static Charset getEncoding(String charsetName) {
		if (RapidMiner.SYSTEM_ENCODING_NAME.equals(charsetName)) {
			return Charset.defaultCharset();
		}
		return Charset.forName(charsetName);
	}

	public static List<ParameterType> getParameterTypes(ParameterHandler handler) {
		List<ParameterType> types = new LinkedList<ParameterType>();

		String encoding = RapidMiner.SYSTEM_ENCODING_NAME;
		String encodingProperty = ParameterService
				.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING);
		if (encodingProperty != null) {
			encoding = encodingProperty;
		}
		types.add(new ParameterTypeStringCategory(PARAMETER_ENCODING, "The encoding used for reading or writing files.",
				CHARSETS, encoding, false));

		return types;
	}
}
