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
package com.rapidminer.repository;

/**
 * 
 * @author Simon Fischer
 * 
 */
public enum AccessType {
	READ, WRITE, EXECUTE,
	/**
	 * This one is not an access type of its own, but is only used as a possible argument to when
	 * checking access rights. This is then mapped to WRITE unless the username equals the owner in
	 * which case access is immediately granted.
	 */
	CHANGE_PERMISSIONS;
}
