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
import java.util.List;
import java.util.Objects;

import com.rapidminer.license.AlreadyRegisteredException;
import com.rapidminer.license.ConstraintNotRestrictedException;
import com.rapidminer.license.InvalidProductException;
import com.rapidminer.license.License;
import com.rapidminer.license.LicenseManager;
import com.rapidminer.license.LicenseManagerListener;
import com.rapidminer.license.LicenseValidationException;
import com.rapidminer.license.StudioLicenseConstants;
import com.rapidminer.license.UnknownProductException;
import com.rapidminer.license.location.LicenseLoadingException;
import com.rapidminer.license.location.LicenseLocation;
import com.rapidminer.license.location.LicenseStoringException;
import com.rapidminer.license.product.Constraint;
import com.rapidminer.license.product.Product;
import com.rapidminer.license.utils.Pair;
import com.rapidminer.license.violation.LicenseConstraintViolation;
import com.rapidminer.license.violation.LicenseViolation;


/**
 * The {@link LicenseManager} which is installed by the {@link ProductConstraintManager} in case no
 * other LicenseManager is available on {@link ProductConstraintManager} initialization. It has a
 * hard-coded basic edition license with a default user and cannot store any new licenses.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public class OpenSourceLicenseManager implements LicenseManager {

	private final License license = new OpenSourceLicense();

	@Override
	public void registerProduct(Product newProduct)
			throws AlreadyRegisteredException, LicenseLoadingException, InvalidProductException {
		if (!StudioLicenseConstants.PRODUCT_ID.equals(newProduct.getProductId())) {
			throw new InvalidProductException("LicenseManager does not allow to register products.",
					newProduct.getProductId());
		}
	}

	@Override
	public List<LicenseViolation> checkAnnotationViolations(Object obj, boolean informListeners) {
		return Collections.emptyList();
	}

	@Override
	public <S, C> LicenseConstraintViolation<S, C> checkConstraintViolation(Product product, Constraint<S, C> constraint,
			C checkedValue, boolean informListeners) {
		return checkConstraintViolation(product, constraint, checkedValue, null, informListeners);
	}

	@Override
	public <S, C> LicenseConstraintViolation<S, C> checkConstraintViolation(Product product, Constraint<S, C> constraint,
			C checkedValue, String i18nKey, boolean informListeners) {
		if (StudioLicenseConstants.PRODUCT_ID.equals(product.getProductId())) {
			return null; // ignore
		} else {
			// all other products are not allowed to be used
			try {
				return new LicenseConstraintViolation<S, C>(null, constraint, getConstraintValue(product, constraint), null,
						i18nKey);
			} catch (ConstraintNotRestrictedException e) {
				// cannot happen
				return null;
			}
		}
	}

	@Override
	public License getActiveLicense(Product product) {
		if (StudioLicenseConstants.PRODUCT_ID.equals(product.getProductId())) {
			return license;
		} else {
			// no license available for other products
			return null;
		}
	}

	@Override
	public <S, C> S getConstraintValue(Product product, Constraint<S, C> constraint)
			throws ConstraintNotRestrictedException {
		return constraint.getDefaultValue();
	}

	@Override
	public List<License> getLicenses(Product product) {
		return Collections.singletonList(license);
	}

	@Override
	public License getUpcomingLicense(License license) throws UnknownProductException {
		return null;
	}

	@Override
	public License getUpcomingLicense(Product product) {
		return null;
	}

	@Override
	public <S, C> boolean isAllowed(Product product, Constraint<S, C> constraint, C checkedValue) {
		return true;
	}

	@Override
	public boolean isAllowedByAnnotations(Object obj) {
		return true;
	}

	@Override
	public Pair<Product, License> validateLicense(Product product, String licenseText)
			throws UnknownProductException, LicenseValidationException {
		Objects.requireNonNull(product);
		if (StudioLicenseConstants.PRODUCT_ID.equals(product.getProductId())) {
			return new Pair<Product, License>(product, license);
		} else {
			// do not return licenses other than for Studio
			throw new UnknownProductException(product.getProductId());
		}
	}

	@Override
	public License storeNewLicense(String licenseText)
			throws LicenseStoringException, UnknownProductException, LicenseValidationException {
		throw new LicenseValidationException("Storing of licenses not supported by this LicenseManager.", null);
	}

	@Override
	public void registerLicenseManagerListener(LicenseManagerListener l) {
		// ignore
	}

	@Override
	public void removeLicenseManagerListener(LicenseManagerListener l) {
		// ignore
	}

	@Override
	public void setLicenseLocation(LicenseLocation location) {
		// ignore
	}

	@Override
	public List<License> getAllActiveLicenses() {
		return Collections.singletonList(license);
	}
}
