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
package com.rapidminer.parameter;

import java.util.logging.Level;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.cipher.CipherException;
import com.rapidminer.tools.cipher.CipherTools;


/**
 * A parameter for passwords. The parameter is written with asteriks in the GUI but can be read in
 * process configuration file. Please make sure that noone but the user can read the password from
 * such a file.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class ParameterTypePassword extends ParameterTypeString {

	private static final long serialVersionUID = 384977559199162363L;

	public ParameterTypePassword(String key, String description) {
		super(key, description, true);
		setExpert(false);
	}

	@Override
	public String getRange() {
		return "password";
	}

	/**
	 * This method will be invoked by the Parameters after a parameter was set and will decrypt the
	 * given value.
	 */
	@Override
	public String transformNewValue(String value) {
		if (value == null) {
			return null;
		} else {
			return decryptPassword(value);
		}
	}

	private String encryptPassword(String value) {
		if (CipherTools.isKeyAvailable()) {
			try {
				return CipherTools.encrypt(value);
			} catch (CipherException e) {
				LogService.getRoot().log(Level.SEVERE,
				        "com.rapidminer.parameter.ParameterTypePassword.encrypting_password_error");
				return value;
			}
		} else {
			return value;
		}
	}

	private String decryptPassword(String value) {
		if (CipherTools.isKeyAvailable()) {
			try {
				return CipherTools.decrypt(value);
			} catch (CipherException e) {
				LogService.getRoot().log(Level.FINE,
				        "com.rapidminer.parameter.ParameterTypePassword.password_looks_like_unencrypted_plain_text");
			}
		}
		return value;
	}

	@Override
	public String toString(Object value) {
		if (value == null) {
			return "";
		} else {
			return encryptPassword(super.toString(value));
		}
	}

	@Override
	public String getXML(String indent, String key, String value, boolean hideDefault) {
		if (value != null && (!hideDefault || !value.equals(getDefaultValue()))) {
			return indent + "<parameter key=\"" + key + "\"\tvalue=\"" + toXMLString(value) + "\"/>"
			        + Tools.getLineSeparator();
		} else {
			return "";
		}
	}

	@Override
	public String toXMLString(String value) {
		return encryptPassword(value);
	}
}
