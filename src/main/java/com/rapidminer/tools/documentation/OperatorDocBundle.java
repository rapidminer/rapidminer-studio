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
package com.rapidminer.tools.documentation;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;


/**
 * A resource bundle that maps operator names to {@link OperatorDocumentation} instances. Instances
 * of this class always return {@link OperatorDocumentation}s from their {@link #getObject(String)}
 * methods.
 * 
 * @author Sebastian Land
 * 
 */
public class OperatorDocBundle extends ResourceBundle {

	public static final String GROUP_PREFIX = "group.";

	public static final String OPERATOR_PREFIX = "operator.";

	private final Map<String, OperatorDocumentation> operatorKeyDescriptionMap;

	private final Map<String, GroupDocumentation> groupMap;

	public OperatorDocBundle() {
		this.operatorKeyDescriptionMap = new HashMap<String, OperatorDocumentation>();
		this.groupMap = new HashMap<String, GroupDocumentation>();
	}

	public OperatorDocBundle(Map<String, OperatorDocumentation> operatorKeyDescriptionMap,
			Map<String, GroupDocumentation> groupMap) {
		this.operatorKeyDescriptionMap = operatorKeyDescriptionMap;
		this.groupMap = groupMap;
	}

	protected void addOperatorDoc(String key, OperatorDocumentation documentation) {
		this.operatorKeyDescriptionMap.put(key, documentation);
	}

	protected void addGroupDoc(String key, GroupDocumentation documentation) {
		this.groupMap.put(key, documentation);
	}

	@Override
	public Enumeration<String> getKeys() {
		return Collections.enumeration(operatorKeyDescriptionMap.keySet());
	}

	@Override
	protected Object handleGetObject(String key) {
		if (key.startsWith(OPERATOR_PREFIX)) {
			key = key.substring(OPERATOR_PREFIX.length());
			OperatorDocumentation doc = operatorKeyDescriptionMap.get(key);
			if (doc == null) {
				doc = new OperatorDocumentation(key);
				operatorKeyDescriptionMap.put(key, doc);
				// LogService.getRoot().fine("Creating new empty documentation for operator " +
				// key);
				LogService.getRoot().log(Level.FINE,
						"com.rapidminer.tools.documentation.OperatorDocBundle.creating_empty_documentation_for_operator",
						key);
			}
			return doc;
		} else if (key.startsWith(GROUP_PREFIX)) {
			key = key.substring(GROUP_PREFIX.length());
			GroupDocumentation groupDocumentation = groupMap.get(key);
			if (groupDocumentation == null) {
				groupDocumentation = new GroupDocumentation(key);
				groupMap.put(key, groupDocumentation);
				// LogService.getRoot().fine("Creating new empty documentation for group " + key);
				LogService.getRoot().log(Level.FINE,
						"com.rapidminer.tools.documentation.OperatorDocBundle.creating_empty_documentation_for_group", key);
			}
			return groupDocumentation;
		}
		return null;
	}

	/** Checks for empty documentation and documentation that has no associated operator. */
	public void check() {
		// LogService.getRoot().info("Checking operator documentation");
		LogService.getRoot().log(Level.INFO,
				"com.rapidminer.tools.documentation.OperatorDocBundle.checking_operator_documentation");
		int missing = 0;
		int same = 0;
		int deprecation = 0;
		int different = 0;
		int empty = 0;
		for (Map.Entry<String, OperatorDocumentation> entry : operatorKeyDescriptionMap.entrySet()) {
			String key = entry.getKey();
			OperatorDocumentation doc = entry.getValue();
			if (key.startsWith("W-")) {
				continue;
			}
			if (doc.getDocumentation().trim().isEmpty()) {
				// LogService.getRoot().warning("Empty documentation for " + key);
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.documentation.OperatorDocBundle.empty_documentation", key);
				empty++;
			}
			OperatorDescription desc = OperatorService.getOperatorDescription(key);
			if (desc == null) {
				missing++;
				// LogService.getRoot().warning("Documentation for nonexistent operator " + key);
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.documentation.OperatorDocBundle.documentation_for_nonexistent_operator", key);
			}
			String replacement = OperatorService.getReplacementForDeprecatedClass(key);
			if (replacement != null) {
				deprecation++;
				String string;
				OperatorDocumentation otherDoc = operatorKeyDescriptionMap.get(replacement);
				if (otherDoc != null) {
					if (otherDoc.getDocumentation().equals(doc.getDocumentation())) {
						string = replacement + " has the same documentation entry.";
						same++;
					} else {
						string = replacement + " has a different documentation entry.";
						different++;
					}
				} else {
					string = replacement + " has no documentation entry.";
				}
				// LogService.getRoot().warning("Documentation for deprecated operator " + key +
				// " replaced by " + replacement + ". " + string);
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.documentation.OperatorDocBundle.documentation_for_deprecated_operator",
						new Object[] { key, replacement, string });
			}
		}
		// LogService.getRoot().info("Found " + empty +
		// " empty documentations. Found documentation for " + missing + " nonexistent and " +
		// (deprecation) + " replaced operators. Out of these, " + same +
		// " documentations are identical to the documentation of the replacement and "
		// + (deprecation - same - different) + " replacements have no documentation.");
		LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.I18N.operator_doc_bundle_warning4",
				new Object[] { empty, missing, (deprecation), same, (deprecation - same - different) });
	}
}
