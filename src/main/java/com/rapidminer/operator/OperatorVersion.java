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
package com.rapidminer.operator;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.tools.plugin.Plugin;


/**
 * Operators can change their behaviour from one version to another. Hence, their version is stored
 * in the process XML file. If the behaviour of an operator changes from, say, version 5.0.003 to
 * 5.0.004, we can notify the user in version 5.0.004. To that end, the method
 * {@link Operator#getCompatibilityLevel()} will return 5.0.003, whereas
 * {@link Operator#getIncompatibleVersionChanges()} will return [5.0.003] (or a superset thereof) in
 * version [5.0.004], so that we can detect that the behavior changed.
 * 
 * 
 * <strong>Note:</strong> The version numbers always refer to the plugin the operator is loaded
 * from. If it is not loaded from a plugin, it refers to the RapidMiner version.
 * 
 * @author Simon Fischer
 * 
 */
public class OperatorVersion extends VersionNumber {

	/**
	 * Parses a version string of the form x.xx.xxx
	 * 
	 * @throws VersionNumberException
	 *             for malformed strings.
	 */
	public OperatorVersion(String versionString) {
		super(versionString);
	}

	public OperatorVersion(int major, int minor, int buildNumber) {
		super(major, minor, buildNumber);
	}

	/**
	 * Creates an {@link OperatorVersion} from any other {@link VersionNumber}.
	 * 
	 * @since 7.6
	 */
	public static OperatorVersion asNewOperatorVersion(VersionNumber vn) {
		try {
			return new OperatorVersion(vn.getLongVersion());
		} catch (VersionNumberException vne) {
			return new OperatorVersion(vn.getMajorNumber(), vn.getMinorNumber(), vn.getPatchLevel());
		}
	}

	public static OperatorVersion getLatestVersion(OperatorDescription desc) {
		Plugin plugin = desc.getProvider();
		if (plugin == null) {
			return asNewOperatorVersion(RapidMiner.getVersion());
		} else {
			try {
				return new OperatorVersion(plugin.getVersion());
			} catch (VersionNumberException vne) {
				// returning current version
				return asNewOperatorVersion(RapidMiner.getVersion());
			}
		}
	}
}
