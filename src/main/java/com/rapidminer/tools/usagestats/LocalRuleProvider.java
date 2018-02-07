/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.tools.usagestats;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import com.rapidminer.studio.internal.RuleProvider;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * Read cta rules from local file
 *
 * @author Jonas Wilms-Pfau
 * @since 7.5
 *
 */
class LocalRuleProvider implements RuleProvider {

	/** the name of the CTA test file which is used to test CTA rules locally */
	private static final String LOCAL_RULE_OVERRIDE_FILE = I18N.getGUIMessageOrNull("gui.label.cta.json.local");

	@Override
	public InputStream getRuleJson() {
		File localTestFile = FileSystemService.getUserConfigFile(LOCAL_RULE_OVERRIDE_FILE);
		InputStream result = null;
		if (localTestFile.exists()) {
			// always warning because that is a test mechanism only
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.usagestats.LocalRuleProvider.start",
					LOCAL_RULE_OVERRIDE_FILE);
			try {
				result = new FileInputStream(localTestFile);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.usagestats.LocalRuleProvider.failure", e);
			}
		}
		return result;
	}

}
