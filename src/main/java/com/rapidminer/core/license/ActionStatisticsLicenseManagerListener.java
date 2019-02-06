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

import java.util.List;

import com.rapidminer.license.License;
import com.rapidminer.license.LicenseEvent;
import com.rapidminer.license.LicenseEvent.LicenseEventType;
import com.rapidminer.license.LicenseManagerListener;
import com.rapidminer.license.annotation.LicenseLevel;
import com.rapidminer.license.product.Constraint;
import com.rapidminer.license.violation.LicenseConstraintViolation;
import com.rapidminer.license.violation.LicenseLevelViolation;
import com.rapidminer.license.violation.LicenseViolation;
import com.rapidminer.license.violation.LicenseViolation.ViolationType;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * The {@link LicenseManagerListener} that listens for {@link LicenseEvent} to collection action
 * statistics.
 *
 * @author Nils Woehler
 *
 */
public enum ActionStatisticsLicenseManagerListener implements LicenseManagerListener {

	INSTANCE;

	@Override
	public <S, C> void handleLicenseEvent(LicenseEvent<S, C> event) {
		LicenseEventType type = event.getType();
		switch (type) {
			case LICENSE_VIOLATED:
				licenseViolated(event.getLicenseViolations());
				break;
			case LICENSE_EXPIRED:
				break;
			case ACTIVE_LICENSE_CHANGED:
				break;
			case LICENSE_STORED:
				break;
			default:
				throw new RuntimeException("Unknown license event type: " + type);
		}
	}

	/**
	 * Logs violation statistics.
	 */
	private void licenseViolated(List<LicenseViolation> licenseViolations) {

		// Only log license constraint violations in case of multiple violations
		if (licenseViolations.size() > 1) {
			for (LicenseViolation violation : licenseViolations) {
				if (violation.getViolationType() == ViolationType.LICENSE_CONSTRAINT_VIOLATED) {
					logConstraintViolation((LicenseConstraintViolation<?, ?>) violation);
				}
			}
		} else {

			// At least one violation is present, otherwise the method wouldn't be called
			LicenseViolation violation = licenseViolations.get(0);
			switch (violation.getViolationType()) {
				case LICENSE_CONSTRAINT_VIOLATED:
					logConstraintViolation((LicenseConstraintViolation<?, ?>) violation);
					break;
				case LICENSE_LEVEL_VIOLATED:
					logLicenseLevelViolation((LicenseLevelViolation) violation);
					break;
				default:
					throw new RuntimeException("Unknown violation type " + violation.getViolationType());

			}
		}

	}

	/**
	 * Logs a license constraint violation.
	 *
	 * @param constraintViolation
	 *            the {@link LicenseConstraintViolation} to log
	 */
	private void logConstraintViolation(LicenseConstraintViolation<?, ?> constraintViolation) {

		License license = constraintViolation.getLicense();
		Constraint<?, ?> constraint = constraintViolation.getConstraint();

		// only count violations for licenses that are not null
		if (license != null && constraint != null) {
			ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_CONSTRAINT, constraint.getKey(),
					license.getProductId() + ":" + license.getProductEdition());
		}
	}

	/**
	 * Logs a license level violation.
	 *
	 * @param violation
	 *            the {@link LicenseLevelViolation} to log
	 */
	private void logLicenseLevelViolation(LicenseLevelViolation violation) {
		LicenseLevel annotation = violation.getLicenseAnnotation();
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_LICENSE_LEVEL, annotation.productId(),
				annotation.comparison() + ":" + String.valueOf(annotation.precedence()));
	}
}
