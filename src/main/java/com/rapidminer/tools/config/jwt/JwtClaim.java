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
package com.rapidminer.tools.config.jwt;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Contains information about the repository user
 *
 * @since 8.1.0
 * @author Jonas Wilms-Pfau
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtClaim {

	private boolean admin;

	private String sub;

	/* @since 8.2.1 */
	private List<String> grp;

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}

	/** @since 8.2.1 */
	public List<String> getGrp() {
		return grp;
	}

	/** @since 8.2.1 */
	public void setGrp(List<String> grp) {
		this.grp = grp;
	}
}
