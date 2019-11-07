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
package com.rapidminer.repository.internal.remote.model;

import com.rapidminer.gui.tools.VersionNumber;


/**
 * Model to hold data from the response of /api/rest/instance
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class InstanceData {
	// the data
	private int responseCode;
	private boolean fullyInitialized;
	private VersionNumber serverVersion;
	private VersionNumber serverCoreVersion;
	private String productEdition;

	public void setProductEdition(String productEdition) {
		this.productEdition = productEdition;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public boolean isFullyInitialized() {
		return fullyInitialized;
	}

	public VersionNumber getServerVersion() {
		return serverVersion;
	}

	public VersionNumber getServerCoreVersion() {
		return serverCoreVersion;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public void setFullyInitialized(boolean fullyInitialized) {
		this.fullyInitialized = fullyInitialized;
	}

	public void setServerVersion(VersionNumber serverVersion) {
		this.serverVersion = serverVersion;
	}

	public void setServerCoreVersion(VersionNumber serverCoreVersion) {
		this.serverCoreVersion = serverCoreVersion;
	}

	public String getProductEdition() {
		return productEdition;
	}

	@Override
	public String toString() {
		return "InstanceData{" +
				"responseCode=" + responseCode +
				", fullyInitialized=" + fullyInitialized +
				", serverVersion=" + serverVersion +
				", serverCoreVersion=" + serverCoreVersion +
				", productEdition=" + productEdition +
				'}';
	}
}
