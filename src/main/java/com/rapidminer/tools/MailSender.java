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
package com.rapidminer.tools;

import java.util.Map;
import java.util.function.UnaryOperator;


/**
 * This is the interface for all mail sending techniques.
 * 
 * @author Simon Fischer
 */
public interface MailSender {

	/** Sends an email with the specified mail parameters and retrieving sender information from {@link ParameterService} */
	void sendEmail(String address, String subject, String content, Map<String, String> headers) throws Exception;

	/**
	 * Sends an email with the specified mail parameters and retrieving sender information from the properties.
	 *
	 * @since 9.4.1
	 */
	default void sendEmail(String address, String subject, String content, Map<String, String> headers, UnaryOperator<String> properties) throws Exception {
		sendEmail(address, subject, content, headers);
	}

}
