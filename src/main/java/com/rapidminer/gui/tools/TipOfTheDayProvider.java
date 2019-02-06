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
package com.rapidminer.gui.tools;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;


/**
 * This class loads all available tips and provides them as string. Each invocation of
 * {@link #nextTip()} returns a new randomly chosen tip string.
 * 
 * @author Ingo Mierswa
 */
public class TipOfTheDayProvider {

	private List<String> allTips = new ArrayList<String>();

	private Random random;

	public TipOfTheDayProvider() {
		this.random = new Random();

		// load all tips
		try {
			StringBuffer current = new StringBuffer();
			URL totdURL = Tools.getResource("totd.txt");
			if (totdURL != null) {
				BufferedReader in = null;
				try {
					in = new BufferedReader(new InputStreamReader(totdURL.openStream()));
					String line = null;
					while ((line = in.readLine()) != null) {
						line = line.trim();
						if (line.startsWith("#")) {
							continue;
						}
						if (line.length() == 0) { // start new tip
							String tip = current.toString();
							if (tip.length() > 0) {
								allTips.add(tip);
							}
							current = new StringBuffer();
						} else {
							current.append(line + "<lb>");
						}
					}
				} finally {
					if (in != null) {
						in.close();
					}
				}
			} else {
				// LogService.getGlobal().logWarning("Cannot show Tip of the Day: resource 'totd.txt' not found...");
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.tools.TipOfTheDayProvider.showing_tip_of_the_day_error_ressource_not_found");
			}
		} catch (java.io.IOException e) {
			// LogService.getGlobal().logWarning("Cannot show Tip of the Day: cannot load tip file.");
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.tools.TipOfTheDayProvider.showing_tip_of_the_day_error_loading_tip_file");
		}
	}

	public String nextTip() {
		if (allTips.size() == 0) {
			return "No tips available.";
		} else {
			return allTips.get(random.nextInt(allTips.size()));
		}
	}
}
