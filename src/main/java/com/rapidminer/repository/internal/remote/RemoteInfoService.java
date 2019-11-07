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
package com.rapidminer.repository.internal.remote;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.rapidminer.repository.internal.remote.model.PluginInfo;


/**
 * The remote info service can be used to query RapidMiner Server system information.
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public interface RemoteInfoService {

	/**
	 * @return the free memory available
	 */
	public String getFreeMemory();

	/**
	 * @return the current system load average
	 */
	public double getSystemLoadAverage();

	/**
	 * @return the total memory available
	 */
	public String getTotalMemory();

	/**
	 * @return the maximum memory available
	 */
	public String getMaxMemory();

	/**
	 * @return the server version
	 */
	public String getVersionNumber();

	/**
	 * @return the time since the server has been booted
	 */
	public XMLGregorianCalendar getUpSince();

	/**
	 * @return a list of installed extensions
	 */
	public List<PluginInfo> getInstalledExtensions();

}
