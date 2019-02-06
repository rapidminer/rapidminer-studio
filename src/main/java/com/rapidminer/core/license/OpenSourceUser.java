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
package com.rapidminer.core.license;

import java.util.Collections;
import java.util.Map;

import com.rapidminer.license.LicenseUser;


/**
 * The default license user which is returned by the {@link OpenSourceLicense}.
 * {@link OpenSourceLicense} is only active if the {@link OpenSourceLicense} has been installed by
 * the {@link ProductConstraintManager}.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public class OpenSourceUser implements LicenseUser {

	@Override
	public LicenseUser putProperty(String key, String value) {
		return this;
	}

	@Override
	public String getProperty(String key) {
		return "";
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.emptyMap();
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public String getEmail() {
		return "";
	}

	@Override
	public LicenseUser copy() {
		return new OpenSourceUser();
	}
}
