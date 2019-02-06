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
package com.rapidminer.gui.license;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.license.ConstraintNotRestrictedException;
import com.rapidminer.license.License;
import com.rapidminer.license.LicenseConstants;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * Collection of utility functions for license handling.
 *
 * @author Michael Knopf, Sabrina Kirstein
 */
public class LicenseTools {

	private static final String LICENSE_PROPERTIES_PATH = "license.properties";

	private static final String LAST_ACTIVE_LICENSE_PRECEDENCE_FOR_PRODUCT = "license.last_active.%s.precedence";
	private static final String LAST_ACTIVE_LICENSE_PRODUCT_EDITION_FOR_PRODUCT = "license.last_active.%s.product_edition";
	private static final String LAST_ACTIVE_LICENSE_EXPIRATION_DATE_FOR_PRODUCT = "license.last_active.%s.expiration_date";

	private static final String FREE_TIER_PREFIX = "free";

	/**
	 * @deprecated use {@link #getPrecedenceKey(License)} instead.
	 */
	@Deprecated
	public static final String LAST_ACTIVE_LICENSE_PRECEDENCE = "license.last_active.precedence";
	/**
	 * @deprecated use {@link #getEditionKey(License)} instead.
	 */
	@Deprecated
	public static final String LAST_ACTIVE_LICENSE_PRODUCT_EDITION = "license.last_active.product_edition";
	/**
	 * @deprecated use {@link #getEditionKey(License)}, {@link #getExpirationDateKey(License)} or
	 *             {@link #getPrecedenceKey(License)} instead.
	 */
	@Deprecated
	public static final String LAST_ACTIVE_LICENSE_PRODUCT_ID = "license.last_active.product_id";
	/**
	 * @deprecated use {@link #getExpirationDateKey(License)} instead.
	 */
	@Deprecated
	public static final String LAST_ACTIVE_LICENSE_EXPIRATION_DATE = "license.last_active.expiration_date";

	/**
	 * Date formatter to generate ISO8601 compliant string representations in UTC time.
	 */
	public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern(ParameterTypeDateFormat.DATE_FORMAT_YYYY_MM_DD)
			.withLocale(Locale.UK).withZone(ZoneOffset.UTC);

	/**
	 * Date formatter format the license start and end date for UI labels. Format will look like this: 'July 07, 2018'
	 */
	public static final DateTimeFormatter UI_DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);

	/**
	 * Get the translated name of the product associated with the license.
	 *
	 * @param license
	 *            The {@link License}.
	 * @return The translated product name.
	 */
	public static String translateProductName(License license) {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.license." + license.getProductId() + ".label");
	}

	/**
	 * Get the translated edition of the product associated with the license.
	 *
	 * @param license
	 *            The {@link License}.
	 * @return The translated product edition.
	 */
	public static String translateProductEdition(License license) {
		return I18N.getMessage(I18N.getGUIBundle(),
				"gui.license." + license.getProductId() + "." + license.getProductEdition() + ".label");
	}

	/**
	 * Translates the data row constraint with the license.
	 *
	 * @param license
	 *            The {@link License}.
	 * @return The translated data row constraint.
	 */
	public static String translateDataRowConstraint(License license) {
		String limit;
		try {
			limit = license.getConstraints().getConstraintValue(ProductConstraintManager.INSTANCE.getDataRowConstraint());
			try {
				limit = NumberFormat.getInstance().format(Integer.parseInt(limit));
			} catch (NumberFormatException e) {
				// ignore
			}
		} catch (ConstraintNotRestrictedException e) {
			limit = I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.value.unlimited.label");
		}
		return I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.data_row.label", limit);
	}

	/**
	 * Translates the Hadoop node constraint with the license.
	 *
	 * @param license
	 *            The {@link License}.
	 * @return The translated Hadoop node constraint.
	 */
	public static String translateHadoopNodeConstraint(License license) {
		String limit;
		try {
			limit = license.getConstraints().getConstraintValue(LicenseConstants.NODES_CONSTRAINT);
			try {
				int numLimit = Integer.parseInt(limit);
				if (numLimit == -1) {
					limit = I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.value.unlimited.label");
				} else {
					limit = NumberFormat.getInstance().format(numLimit);
				}
			} catch (NumberFormatException e) {
				// ignore
			}
		} catch (ConstraintNotRestrictedException e) {
			limit = I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.value.unlimited.label");
		}
		return I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.hadoop_node.label", limit);
	}

	/**
	 * Translates the pushdown operator constraint with the license.
	 *
	 * @param license
	 *            The {@link License}.
	 * @return The translated pushdown operator constraint.
	 */
	public static String translatePushdownOperatorConstraint(License license) {
		Boolean rmOperatorsAllowed;
		try {
			rmOperatorsAllowed = Boolean
					.parseBoolean(license.getConstraints().getConstraintValue(LicenseConstants.RM_IN_HADOOP_CONSTRAINT));
		} catch (ConstraintNotRestrictedException e) {
			rmOperatorsAllowed = true;
		}

		String value;
		if (rmOperatorsAllowed) {
			value = I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.value.enabled.label");
		} else {
			value = I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.value.disabled.label");
		}
		return I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.pushdown_operator.label", value);
	}

	/**
	 * Translates the logical processor constraint with the license.
	 *
	 * @param license
	 *            The {@link License}.
	 * @return The translated logical processor constraint.
	 */
	public static String translateLogicalProcessorConstraint(License license) {
		String limit;
		try {
			limit = license.getConstraints()
					.getConstraintValue(ProductConstraintManager.INSTANCE.getLogicalProcessorConstraint());
		} catch (ConstraintNotRestrictedException e) {
			limit = I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.value.unlimited.label");
		}
		return I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.logical_processor.label", limit);
	}

	/**
	 * Translates the memory constraint with the license.
	 *
	 * @param license
	 *            The {@link License}.
	 * @return The translated memory constraint.
	 */
	public static String translateMemoryConstraint(License license) {
		String limit;
		try {
			limit = license.getConstraints()
					.getConstraintValue(ProductConstraintManager.INSTANCE.getMemoryLimitConstraint());
		} catch (ConstraintNotRestrictedException e) {
			return I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.unlimited_memory.label");
		}
		return I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.memory.label", limit);
	}

	/**
	 * Translates the web service constraint with the license.
	 *
	 * @param license
	 *            The {@link License}.
	 * @return The translated web service constraint.
	 */
	public static String translateWebServiceConstraint(License license) {
		String limit;
		try {
			limit = license.getConstraints()
					.getConstraintValue(ProductConstraintManager.INSTANCE.getWebServiceLimitConstraint());
			try {
				limit = NumberFormat.getInstance().format(Integer.parseInt(limit));
			} catch (NumberFormatException e) {
				// ignore
			}
		} catch (ConstraintNotRestrictedException e) {
			limit = I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.value.unlimited.label");
		}
		return I18N.getMessage(I18N.getGUIBundle(), "gui.license.constraint.web_service.label", limit);
	}

	/**
	 * Load last active license properties.
	 */
	public static Properties loadLastActiveLicenseProperties() {
		File licensePropertiesFile = FileSystemService.getUserConfigFile(LICENSE_PROPERTIES_PATH);
		Properties licenseProperties = new Properties();

		// try to load properties from file
		if (licensePropertiesFile.exists()) {
			try (FileInputStream in = new FileInputStream(licensePropertiesFile)) {
				licenseProperties.load(in);
				handleLegacyProperties(licenseProperties);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.license.RMLicenseManagerListener.loading_properties_failed", e);
			}
		}

		return licenseProperties;
	}

	private static void handleLegacyProperties(Properties licenseProperties) {
		String legacyProductId = licenseProperties.getProperty(LAST_ACTIVE_LICENSE_PRODUCT_ID);
		if (legacyProductId != null) {
			String legacyEdition = licenseProperties.getProperty(LAST_ACTIVE_LICENSE_PRODUCT_EDITION);
			String legacyExpiration = licenseProperties.getProperty(LAST_ACTIVE_LICENSE_EXPIRATION_DATE);
			String legacyPrecedence = licenseProperties.getProperty(LAST_ACTIVE_LICENSE_PRECEDENCE);

			if (legacyEdition != null) {
				licenseProperties.setProperty(
						String.format(LAST_ACTIVE_LICENSE_PRODUCT_EDITION_FOR_PRODUCT, legacyProductId), legacyEdition);
			}
			if (legacyExpiration != null) {
				licenseProperties.setProperty(
						String.format(LAST_ACTIVE_LICENSE_EXPIRATION_DATE_FOR_PRODUCT, legacyProductId), legacyExpiration);
			}
			if (legacyPrecedence != null) {
				licenseProperties.setProperty(String.format(LAST_ACTIVE_LICENSE_PRECEDENCE_FOR_PRODUCT, legacyProductId),
						legacyPrecedence);
			}
		}
		licenseProperties.remove(LAST_ACTIVE_LICENSE_PRODUCT_ID);
		licenseProperties.remove(LAST_ACTIVE_LICENSE_PRODUCT_EDITION);
		licenseProperties.remove(LAST_ACTIVE_LICENSE_EXPIRATION_DATE);
		licenseProperties.remove(LAST_ACTIVE_LICENSE_PRECEDENCE);
	}

	/**
	 * Store active license properties. Requires RapidMiner to be running ad standalone GUI
	 * application.
	 */
	public static void storeActiveLicenseProperties(License activeLicense) {
		// do nothing when RapidMiner is not executed as standalone GUI application
		if (RapidMiner.getExecutionMode() != ExecutionMode.UI) {
			return;
		}

		// load existing properties file
		File licensePropertiesFile = FileSystemService.getUserConfigFile(LICENSE_PROPERTIES_PATH);
		Properties licenseProperties = LicenseTools.loadLastActiveLicenseProperties();

		// store properties necessary to identify the license
		licenseProperties.setProperty(getEditionKey(activeLicense), activeLicense.getProductEdition());
		licenseProperties.setProperty(getPrecedenceKey(activeLicense), String.valueOf(activeLicense.getPrecedence()));
		if (activeLicense.getExpirationDate() != null) {
			String dateString = ISO_DATE_FORMATTER.format(activeLicense.getExpirationDate());
			licenseProperties.setProperty(getExpirationDateKey(activeLicense), dateString);
		} else {
			licenseProperties.remove(getExpirationDateKey(activeLicense));
		}

		// store properties
		try (FileOutputStream out = new FileOutputStream(licensePropertiesFile)) {
			licenseProperties.store(out, "RapidMiner Studio License Properties");
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.license.RMLicenseManagerListener.storing_properties_failed", e);
		}
	}

	public static String getPrecedenceKey(License license) {
		return String.format(LAST_ACTIVE_LICENSE_PRECEDENCE_FOR_PRODUCT, license.getProductId());
	}

	public static String getExpirationDateKey(License license) {
		return String.format(LAST_ACTIVE_LICENSE_EXPIRATION_DATE_FOR_PRODUCT, license.getProductId());
	}

	public static String getEditionKey(License license) {
		return String.format(LAST_ACTIVE_LICENSE_PRODUCT_EDITION_FOR_PRODUCT, license.getProductId());
	}

	public static boolean isLicenseFree(License license) {
		return isLicenseFree(license.getProductEdition());
	}

	public static boolean isLicenseFree(String edition) {
		// Compare the beginning of the edition String to the prefix of free tier licenses.
		return edition.startsWith(FREE_TIER_PREFIX);
	}
}
