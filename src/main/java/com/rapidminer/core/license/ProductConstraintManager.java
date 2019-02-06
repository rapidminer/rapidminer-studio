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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rapidminer.license.AlreadyRegisteredException;
import com.rapidminer.license.InvalidProductException;
import com.rapidminer.license.License;
import com.rapidminer.license.LicenseConstants;
import com.rapidminer.license.LicenseManager;
import com.rapidminer.license.LicenseManagerListener;
import com.rapidminer.license.LicenseManagerRegistry;
import com.rapidminer.license.LicenseStatus;
import com.rapidminer.license.LicenseValidationException;
import com.rapidminer.license.StudioLicenseConstants;
import com.rapidminer.license.UnknownProductException;
import com.rapidminer.license.location.FileLicenseLocation;
import com.rapidminer.license.location.LicenseLoadingException;
import com.rapidminer.license.location.LicenseLocation;
import com.rapidminer.license.location.LicenseStoringException;
import com.rapidminer.license.product.Constraint;
import com.rapidminer.license.product.DefaultProduct;
import com.rapidminer.license.product.NumericalConstraint;
import com.rapidminer.license.product.Product;
import com.rapidminer.license.utils.Pair;
import com.rapidminer.license.violation.LicenseViolation;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * This class handles the interaction with the {@link LicenseManager} for RapidMiner. It installs
 * the {@link Product} that will be used to retrieve licenses. Furthermore it has convenience
 * methods to check RapidMiner constraints.
 *
 * @author Nils Woehler
 *
 */
public enum ProductConstraintManager {

	INSTANCE;

	/**
	 * folder name for licenses
	 */
	private static final String LICENSES_FOLDER_NAME = "licenses";

	/**
	 * the signature for the RapidMiner Studio product used for verification of our default product
	 */
	private static final String RAPIDMINER_STUDIO_PRODUCT_SIGNATURE = "Rmfgu6rDLgqPCIBl/WzEWmVW4O8cPHF2yPMQvTTAWZGDIwhMadeRmMK6e3V/VW+VOrdKKPHCHB3PtzNQAVGWHrKsv3tmKivQGNIQOSG8192araFXSGHpapQhWFf+8gjsDlf1Dbbt2ZRSf/Gmiinb2JcoT6x+NQiZfkXUFVeOEGyAJLUufKCAdvTu2bzkbexdfcJAvTSzqn2VwgFThg4zRzLxoO2hElT6DHWmr3pi2iLnzVgcM0ifJYdTYsTnAk0fhSijpVv3jMbL81ehUh8iJSQlXoutVcxYFAviMhlBlKb/3dgLhBlG8F12epF20WNSyewCRM8ysANZbzP9qcOf+w==";

	private static final Product DEFAULT_PRODUCT = new DefaultProduct(StudioLicenseConstants.PRODUCT_ID,
			StudioLicenseConstants.VERSION, false, RAPIDMINER_STUDIO_PRODUCT_SIGNATURE, LicenseConstants.DATA_ROW_CONSTRAINT,
			LicenseConstants.LOGICAL_PROCESSOR_CONSTRAINT, LicenseConstants.MEMORY_LIMIT_CONSTRAINT,
			LicenseConstants.WEB_SERVICE_LIMIT_CONSTRAINT);

	private static final Path MAIN_LICENSE_PATH = Paths
			.get(new File(FileSystemService.getUserRapidMinerDir(), LICENSES_FOLDER_NAME).toURI());

	/** The product that is used to retrieve RM licenses */
	private Product registeredProduct;

	private NumericalConstraint logicalProcessorConstraint;
	private NumericalConstraint dataRowConstraint;
	private NumericalConstraint memoryLimitConstraint;
	private NumericalConstraint webServiceLimitConstraint;

	private AtomicBoolean initialized = new AtomicBoolean(false);

	/**
	 *
	 * Initialized the {@link LicenseManager} with the provided {@link LicenseLocation}. Furthermore
	 * the provided product is registered. The provided product must contain all constraints defined
	 * in {@link RMConstraints}.
	 */
	public synchronized void initialize(LicenseLocation licenseLocation, Product product)
			throws IllegalAccessException, AlreadyRegisteredException, LicenseLoadingException, InvalidProductException {
		if (initialized.get()) {
			throw new UnsupportedOperationException("Cannot initialize the ProductConstraintManager twice");
		}

		// Install open-source license manager in case no available
		if (LicenseManagerRegistry.INSTANCE.get() == null) {
			LogService.getRoot().info("com.rapidminer.license.ConstraintManager.using_open_source_license_manager");
			LicenseManagerRegistry.INSTANCE.set(new OpenSourceLicenseManager());
		}

		LogService.getRoot().info("com.rapidminer.license.ConstraintManager.initializing_constraint_manager");

		if (licenseLocation == null) {
			LogService.getRoot().info("com.rapidminer.license.ConstraintManager.using_default_license_location");
			Path installationLicenseFolder = null;
			try {
				installationLicenseFolder = FileSystemService.getRapidMinerHome().toPath().resolve(LICENSES_FOLDER_NAME);
			} catch (IOException e) {
				LogService.getRoot()
						.info("com.rapidminer.license.ConstraintManager.cannot_use_installation_folder_licenses");
			}
			// Files.isDirectory follows symbolic links
			if (installationLicenseFolder != null && Files.isDirectory(installationLicenseFolder)) {
				licenseLocation = new FileLicenseLocation(MAIN_LICENSE_PATH, installationLicenseFolder);
				LogService.getRoot().info("com.rapidminer.license.ConstraintManager.found_installation_folder_licenses");
			} else {
				licenseLocation = new FileLicenseLocation(MAIN_LICENSE_PATH);
			}
		}
		LicenseManagerRegistry.INSTANCE.get().setLicenseLocation(licenseLocation);

		if (product == null) {
			product = DEFAULT_PRODUCT;
			LogService.getRoot().info("com.rapidminer.license.ConstraintManager.using_default_product");
		}
		// make sure that extensions CANNOT be used to init the RM Studio product constraints
		if (product.isExtension()) {
			throw new InvalidProductException("Cannot init RapidMiner Studio with extension product!",
					product.getProductId());
		}

		// make sure only core product ID is used
		if (!product.getProductId().matches(Product.RM_REGEX)) {
			throw new InvalidProductException(
					"Cannot init RapidMiner Studio. Only core product IDs (matching " + Product.RM_REGEX + ") are allowed!",
					product.getProductId());
		}

		// check if all default Studio constraint are defined for the product that should be
		// installed
		for (Constraint<?, ?> rmConstr : LicenseConstants.getDefaultConstraints()) {

			// get constraint from product
			String constraintId = rmConstr.getKey();
			Constraint<?, ?> productConstraint = product.findConstraint(constraintId);

			// check if product contains constraint
			if (productConstraint == null) {
				throw new RuntimeException(I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.license.ConstraintManager.no_constraint_defined", rmConstr.getKey()));
			}

			// also check if constraint is of correct type
			if (!productConstraint.getClass().isAssignableFrom(rmConstr.getClass())) {
				throw new RuntimeException("Constraint with constraintId " + rmConstr.getKey()
						+ " is of wrong type. Expected class: " + rmConstr.getClass());
			}

			// remember constraints in class variables
			switch (constraintId) {
				case LicenseConstants.DATA_ROW_CONSTRAINT_ID:
					dataRowConstraint = (NumericalConstraint) productConstraint;
					break;
				case LicenseConstants.LOGICAL_PROCESSORS_CONSTRAINT_ID:
					logicalProcessorConstraint = (NumericalConstraint) productConstraint;
					break;
				case LicenseConstants.MEMORY_LIMIT_CONSTRAINT_ID:
					memoryLimitConstraint = (NumericalConstraint) productConstraint;
					break;
				case LicenseConstants.WEB_SERVICE_LIMIT_CONSTRAINT_ID:
					webServiceLimitConstraint = (NumericalConstraint) productConstraint;
					break;
				default:
					throw new RuntimeException("Unknown constraint " + rmConstr.getKey());
			}
		}

		// register product to license manager
		LicenseManagerRegistry.INSTANCE.get().registerProduct(product);

		// remember registered product
		registeredProduct = product;

		// set initialized
		initialized.set(true);
	}

	/**
	 * @return <code>true</code> if the {@link ProductConstraintManager} has been initialized
	 */
	public boolean isInitialized() {
		return initialized.get();
	}

	/**
	 * @return the currently registered {@link Product}
	 */
	public Product getProduct() {
		return registeredProduct;
	}

	/**
	 * @return if a trial license is contained in the list of licenses <code>true</code> is
	 *         returned.
	 */
	public boolean wasTrialActivated() {
		List<License> licenses = LicenseManagerRegistry.INSTANCE.get().getLicenses(getProduct());
		for (License lic : licenses) {
			if (lic.getProductEdition().equals(StudioLicenseConstants.TRIAL_EDITION)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if there ever was a license activated which is better than a Free edition. Examples
	 * are either a trial edition or any other paid edition.
	 *
	 * @return <code>true</code> if at least one trial or any other editions except basic was
	 *         activated; <code>false</code> otherwise
	 */
	public boolean wasHigherThanFreeActivated() {
		List<License> licenses = LicenseManagerRegistry.INSTANCE.get().getLicenses(getProduct());
		for (License lic : licenses) {
			if (!lic.getProductEdition().equals(LicenseConstants.STARTER_EDITION)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if there ever was any license activated.
	 *
	 * @return <code>true</code> if at least one license was activated; <code>false</code> otherwise
	 */
	public boolean wasAnyLicenseActivated() {
		return LicenseManagerRegistry.INSTANCE.get().getLicenses(getProduct()).size() > 0;
	}

	/**
	 * @return returns <code>true</code> if it reasonable to offer trial to the user, based on
	 *         locally available information. This method does not ask the server whether trial is
	 *         still available. It checks if a trial license was installed and if trial is better
	 *         than the currently active license.
	 */
	public boolean shouldTrialBeOffered() {
		return !wasTrialActivated() && getActiveLicense().getPrecedence() < StudioLicenseConstants.TRIAL_LICENSE_PRECEDENCE;
	}

	/**
	 * @return returns <code>true</code> if the current active license is a trial license
	 */
	public boolean isTrialLicense() {
		return StudioLicenseConstants.TRIAL_EDITION
				.equals(LicenseManagerRegistry.INSTANCE.get().getActiveLicense(getProduct()).getProductEdition());
	}

	/**
	 * @return the current active license for the {@link Product} installed to the
	 *         {@link ProductConstraintManager}.
	 */
	public License getActiveLicense() {
		return LicenseManagerRegistry.INSTANCE.get().getActiveLicense(getProduct());
	}

	/**
	 * Checks whether the provided license text is valid for the {@link Product} installed.
	 *
	 * @param licenseText
	 *            the text of the license which should be validated
	 * @return <code>true</code> in case the license text is valid, <code>false</code> otherwise
	 */
	public boolean isLicenseValid(String licenseText) {
		Pair<Product, License> validateLicense = null;
		try {
			validateLicense = validateLicense(licenseText);
		} catch (LicenseValidationException | UnknownProductException e) {
			return false;
		}
		LicenseStatus status = validateLicense.getSecond().getStatus();
		return status == LicenseStatus.VALID || status == LicenseStatus.STARTS_IN_FUTURE;
	}

	/**
	 * Install a new license to the {@link LicenseManager}. Should only be called if
	 * {@link #isLicenseValid(String)} returns <code>true</code>.
	 *
	 * @param licenseText
	 *            the text if the license that should be installed
	 *
	 * @return the freshly installed license.
	 */
	public License installNewLicense(String licenseText)
			throws LicenseStoringException, UnknownProductException, LicenseValidationException {
		return LicenseManagerRegistry.INSTANCE.get().storeNewLicense(licenseText);
	}

	/**
	 * @param l
	 *            the license manager listener
	 */
	public void registerLicenseManagerListener(LicenseManagerListener l) {
		LicenseManagerRegistry.INSTANCE.get().registerLicenseManagerListener(l);
	}

	/**
	 * @param l
	 *            removes the provided license manager listener from the license manager
	 */
	public void removeLicenseManagerListener(LicenseManagerListener l) {
		LicenseManagerRegistry.INSTANCE.get().removeLicenseManagerListener(l);
	}

	/**
	 * @return the upcoming license for the product installed to the
	 *         {@link ProductConstraintManager}
	 */
	public License getUpcomingLicense() {
		return LicenseManagerRegistry.INSTANCE.get().getUpcomingLicense(getProduct());
	}

	/**
	 * @param enteredLicenseKey
	 *            the key that was entered and should be validated
	 * @return
	 * @throws UnknownProductException
	 *             if the licenses belongs to an unknown product
	 * @throws LicenseValidationException
	 *             if something goes wrong during validation this exception is thrown. <br/>
	 *             CAUTION: the returned license can still have an invalid license status even
	 *             though no exception was thrown.
	 */
	public Pair<Product, License> validateLicense(String enteredLicenseKey)
			throws LicenseValidationException, UnknownProductException {
		// LicenseManager accepts unknown(null) products
		return LicenseManagerRegistry.INSTANCE.get().validateLicense(null, enteredLicenseKey);
	}

	/**
	 * See {@link LicenseManager#checkAnnotationViolations(Object, boolean)} for more details.
	 */
	public List<LicenseViolation> checkAnnotationViolations(Operator op, boolean informListeners) {
		return LicenseManagerRegistry.INSTANCE.get().checkAnnotationViolations(op, informListeners);
	}

	/**
	 * See {@link LicenseManager#isAllowedByAnnotations(Object)} for more details.
	 */
	public boolean isAllowedByAnnotations(Operator op) {
		return LicenseManagerRegistry.INSTANCE.get().isAllowedByAnnotations(op);
	}

	/**
	 * Checks if free license or a higher license is installed.
	 *
	 * @return {@code true} if at least a free license is installed
	 */
	public boolean isFreeFeatureAllowed() {
		return !LicenseManagerRegistry.INSTANCE.get().getActiveLicense(ProductConstraintManager.INSTANCE.getProduct())
				.isStarterLicense();
	}

	/**
	 * @return the data row constraint object
	 */
	public NumericalConstraint getDataRowConstraint() {
		return dataRowConstraint;
	}

	/**
	 * @return the logical processor constraint object
	 */
	public NumericalConstraint getLogicalProcessorConstraint() {
		return logicalProcessorConstraint;
	}

	/**
	 * @return the memory limit constraint object
	 */
	public NumericalConstraint getMemoryLimitConstraint() {
		return memoryLimitConstraint;
	}

	/**
	 * @return the web service limit constraint object
	 */
	public NumericalConstraint getWebServiceLimitConstraint() {
		return webServiceLimitConstraint;
	}

}
