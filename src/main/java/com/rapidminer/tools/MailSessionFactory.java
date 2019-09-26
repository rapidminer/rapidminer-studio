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

import java.util.function.UnaryOperator;
import javax.mail.Session;


/**
 * Creates mail sessions.
 * 
 * @author Simon Fischer
 * 
 */
public interface MailSessionFactory {

	/** Create a {@link Session} from the properties in the {@link ParameterService} */
	Session makeSession();

	/**
	 * Create a session from the given properties. If a password decoder is necessary, it can be provided as the second parameter.
	 *
	 * @param properties
	 * 		The property lookup; must not be {@code null}
	 * @param pwDecoder
	 * 		a password decoder; can be {@code null}
	 * @return the session
	 * @since 9.4.1
	 */
	default Session makeSession(UnaryOperator<String> properties, UnaryOperator<String> pwDecoder) {
		return makeSession();
	}
}
