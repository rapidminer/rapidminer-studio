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
package com.rapidminer.operator.learner;

import com.rapidminer.operator.OperatorCapability;


/**
 * @author Sebastian Land
 * 
 */
public interface CapabilityProvider {

	/**
	 * The property name for &quot;Indicates if only a warning should be made if learning
	 * capabilities are not fulfilled (instead of breaking the process).&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_GENERAL_CAPABILITIES_WARN = "rapidminer.general.capabilities.warn";

	/**
	 * Checks for Learner capabilities. Should return true if the given capability is supported.
	 */
	public boolean supportsCapability(OperatorCapability capability);

}
