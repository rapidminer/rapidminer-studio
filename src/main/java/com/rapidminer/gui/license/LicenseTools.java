/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.license.License;
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
	public static final String LAST_ACTIVE_LICENSE_PRECEDENCE = "license.last_active.precedence";
	public static final String LAST_ACTIVE_LICENSE_PRODUCT_EDITION = "license.last_active.product_edition";
	public static final String LAST_ACTIVE_LICENSE_PRODUCT_ID = "license.last_active.product_id";
	public static final String LAST_ACTIVE_LICENSE_EXPIRATION_DATE = "license.last_active.expiration_date";

	/**
	 * Date formatter to generate ISO8601 compliant string representations in UTC time.
	 */
	public static final ThreadLocal<DateFormat> ISO_DATE_FORMATTER = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX", Locale.UK);
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			return format;
		}
	};

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
	 * Load last active license properties.
	 */
	public static Properties loadLastActiveLicenseProperties() {
		File licensePropertiesFile = FileSystemService.getUserConfigFile(LICENSE_PROPERTIES_PATH);
		Properties licenseProperties = new Properties();

		// try to load properties from file
		if (licensePropertiesFile.exists()) {
			try (FileInputStream in = new FileInputStream(licensePropertiesFile)) {
				licenseProperties.load(in);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.license.RMLicenseManagerListener.loading_properties_failed", e);
			}
		}

		return licenseProperties;
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
		licenseProperties.setProperty(LAST_ACTIVE_LICENSE_PRODUCT_ID, activeLicense.getProductId());
		licenseProperties.setProperty(LAST_ACTIVE_LICENSE_PRODUCT_EDITION, activeLicense.getProductEdition());
		licenseProperties.setProperty(LAST_ACTIVE_LICENSE_PRECEDENCE, String.valueOf(activeLicense.getPrecedence()));
		if (activeLicense.getExpirationDate() != null) {
			String dateString = ISO_DATE_FORMATTER.get().format(activeLicense.getExpirationDate());
			licenseProperties.setProperty(LAST_ACTIVE_LICENSE_EXPIRATION_DATE, dateString);
		}

		// store properties
		try (FileOutputStream out = new FileOutputStream(licensePropertiesFile)) {
			licenseProperties.store(out, "RapidMiner Studio License Properties");
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.license.RMLicenseManagerListener.storing_properties_failed", e);
		}
	}
}
