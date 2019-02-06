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
import java.net.URISyntaxException;
import java.util.List;


/**
 *
 * Interface for a class that queries the update server and knows how to install extensions via the
 * RapidMiner Studio UI.
 * <p>
 * This is an internal interface and might be changed or removed without any further notice.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public interface UpdateManager {

	public static final String PACKAGE_TYPE_RAPIDMINER_PLUGIN = "RAPIDMINER_PLUGIN";
	public static final String PACKAGE_TYPE_STAND_ALONE = "STAND_ALONE";

	public static final String PARAMETER_UPDATE_INCREMENTALLY = "rapidminer.update.incremental";
	public static final String PARAMETER_UPDATE_URL = "rapidminer.update.url";
	public static final String UPDATESERVICE_URL = "https://marketplace.rapidminer.com/UpdateServer";
	public static final String OLD_UPDATESERVICE_URL = "http://marketplace.rapid-i.com:80/UpdateServer";
	public static final String PACKAGEID_RAPIDMINER = "rapidminer-studio-6";
	public static final String COMMERCIAL_LICENSE_NAME = "RIC";
	public static final String NEVER_REMIND_INSTALL_EXTENSIONS_FILE_NAME = "ignored_extensions.xml";

	/**
	 * Opens the Install extension dialog with a preselected extensions.
	 *
	 * @param selectedPackages
	 *            a list with extension IDs that should be preselected
	 */
	void installSelectedPackages(List<String> selectedPackages);

	/**
	 * Opens the Update dialog. Marks the provided extensions as "selected for installation".
	 *
	 * @param selectUpdateTab
	 *            if {@code true} the "Update" tab will be shown
	 * @param preselectedExtensions
	 *            a list with extension IDs that should be marked for installation
	 */
	void showUpdateDialog(final boolean selectUpdateTab, final String... preselectedExtensions);

	/**
	 * Takes an operator prefix and looks up the extension ID for the provided operator prefix.
	 *
	 * @param operatorPrefix
	 *            the operator prefix
	 *
	 * @return the extension ID. Returns {@code null} in case the extension is unknown.
	 * @throws URISyntaxException
	 *             in case the UpdateManager URI has wrong syntax
	 * @throws IOException
	 *             in case querying the update server fails
	 */
	String getExtensionIdForOperatorPrefix(String operatorPrefix) throws IOException, URISyntaxException;

	/**
	 * Queries the update server for the latest version of the extension of the specified extension
	 * ID.
	 *
	 * @param extensionId
	 *            the extension ID
	 * @param string
	 *            the target platform (e.g. "ANY" for extensions)
	 * @param rapidMinerVersion
	 *            the current RapidMiner Studio version
	 * @return the latest version for the provided extension
	 *
	 * @throws URISyntaxException
	 *             in case the UpdateManager URI has wrong syntax
	 * @throws IOException
	 *             in case querying the update server fails
	 */
	String getLatestVersion(String extensionId, String targetPlatform, String rapidMinerVersion) throws IOException,
			URISyntaxException;

	/**
	 * Queries the update server for the extension name of the extension for the provided extension
	 * ID and version.
	 *
	 * @param extensionId
	 *            the extension ID
	 * @param latestVersion
	 *            the latest version
	 * @param targetPlatform
	 *            the target platform
	 *
	 * @return the extension name or {@code null} in case the extension ID is unknown
	 *
	 * @throws URISyntaxException
	 *             in case the UpdateManager URI has wrong syntax
	 * @throws IOException
	 *             in case querying the update server fails
	 */
	String getExtensionName(String extensionId, String latestVersion, String targetPlatform) throws IOException,
	URISyntaxException;

}
