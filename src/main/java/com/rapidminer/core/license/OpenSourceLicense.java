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

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import com.rapidminer.license.Constraints;
import com.rapidminer.license.DefaultConstraints;
import com.rapidminer.license.License;
import com.rapidminer.license.LicenseConstants;
import com.rapidminer.license.LicenseStatus;
import com.rapidminer.license.LicenseUser;
import com.rapidminer.license.StudioLicenseConstants;


/**
 * The license returned by the {@link OpenSourceLicenseManager}. It is a valid Basic edition with a
 * default user.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public class OpenSourceLicense implements License {

	@Override
	public int compareTo(License o) {
		return 0;
	}

	@Override
	public int getPrecedence() {
		return 0;
	}

	@Override
	public String getProductId() {
		return StudioLicenseConstants.PRODUCT_ID;
	}

	@Override
	public String getProductEdition() {
		return LicenseConstants.STARTER_EDITION;
	}

	@Override
	public LicenseUser getLicenseUser() {
		return new OpenSourceUser();
	}

	@Override
	public Constraints getConstraints() {
		DefaultConstraints defaultConstraints = new DefaultConstraints();
		defaultConstraints.addConstraint(LicenseConstants.LOGICAL_PROCESSOR_CONSTRAINT,
				StudioLicenseConstants.FREE_LOGICAL_PROCESSORS);
		defaultConstraints.addConstraint(LicenseConstants.DATA_ROW_CONSTRAINT, StudioLicenseConstants.FREE_DATA_ROWS);
		return defaultConstraints;
	}

	@Override
	public LocalDate getStartDate() {
		return null;
	}

	@Override
	public LocalDate getExpirationDate() {
		return null;
	}

	@Override
	public LicenseStatus getStatus() {
		return LicenseStatus.VALID;
	}

	@Override
	public LicenseStatus validate(LocalDate today) {
		return LicenseStatus.VALID;
	}

	@Override
	public boolean isStarterLicense() {
		return true;
	}

	@Override
	public String getLicenseID() {
		return "832c6f9a-7ed3-408f-8c6b-749900952766";
	}

	@Override
	public License copy() {
		return new OpenSourceLicense();
	}

	@Override
	public Set<String> getVersions() {
		return Collections.unmodifiableSet(Collections.singleton(StudioLicenseConstants.VERSION));
	}

	@Override
	public String getAnnotations() {
		return null;
	}

}
