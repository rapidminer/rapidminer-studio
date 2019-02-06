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
package com.rapidminer.tools.update.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;


/**
 * A factory that knows how to create {@link UpdateManager} instances.
 *
 * <p>
 * This is an internal interface and might be changed or removed without any further notice.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public interface UpdateManagerFactory {

	/**
	 * @return a new {@link UpdateManager} instance
	 */
	UpdateManager create() throws MalformedURLException, URISyntaxException, IOException;
}
