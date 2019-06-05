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
package com.rapidminer.tools.usagestats;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.RapidMinerVersion;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.studio.internal.RuleProvider;
import com.rapidminer.studio.internal.RuleProviderRegistry;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * This class loads the CTA rules either from a local test file or from Nexus.
 *
 * @author Jonas Wilms-Pfau, Marco Boeck
 * @since 7.5
 *
 */
enum RuleService {

	INSTANCE;

	private final Pattern prohibitedKeywords;

	private final ObjectReader reader;

	private final Set<VerifiableRule> rules = new HashSet<>();

	private RuleService() {
		List<String> prohibitedKeywords  = new ArrayList<>();
		prohibitedKeywords.add("JOIN");
		prohibitedKeywords.add("CREATE");
		prohibitedKeywords.add("INSERT");
		prohibitedKeywords.add("UPDATE");
		prohibitedKeywords.add("DELETE");
		prohibitedKeywords.add("DROP");
		prohibitedKeywords.add("ALTER");
		prohibitedKeywords.add("MERGE");
		prohibitedKeywords.add("TRUNCATE");
		prohibitedKeywords.add("SET");
		prohibitedKeywords.add("SHUTDOWN");
		prohibitedKeywords.add("COMMIT");
		prohibitedKeywords.add("GRANT");
		prohibitedKeywords.add("CHECKPOINT");
		prohibitedKeywords.add("SAVEPOINT");
		prohibitedKeywords.add("PREPARE");
		prohibitedKeywords.add("REVOKE");
		prohibitedKeywords.add("ROLLBACK");
		prohibitedKeywords.add("CONSTRAINT");
		prohibitedKeywords.add("RUNSCRIPT");
		prohibitedKeywords.add("BACKUP");
		prohibitedKeywords.add("CALL");
		prohibitedKeywords.add("SCRIPT");
		prohibitedKeywords.add("ANALYZE");
		prohibitedKeywords.add("COMMENT");
		prohibitedKeywords.add("EXPLAIN");
		prohibitedKeywords.add("SHOW");

		// Create a "\b(JOIN|CREATE|...)\b" regex, where \b matches word boundaries
		this.prohibitedKeywords = Pattern.compile("\\b(" + String.join("|", prohibitedKeywords) + ")\\b", Pattern.CASE_INSENSITIVE);
		ObjectMapper mapper = new ObjectMapper();
		this.reader = mapper.reader(mapper.getTypeFactory().constructCollectionType(List.class, Rule.class));
		reloadRules();
	}

	public Set<VerifiableRule> getRules() {
		return rules;
	}

	/**
	 * Reloads the CTA rules from either local file or Nexus.
	 */
	public void reloadRules() {
		if (!RapidMiner.getExecutionMode().equals(ExecutionMode.UI)) {
			return;
		}

		List<VerifiableRule> loadedRules = null;
		Iterator<RuleProvider> ruleProvider = RuleProviderRegistry.INSTANCE.getRuleProvider().iterator();

		while (ruleProvider.hasNext() && loadedRules == null) {
			RuleProvider provider = ruleProvider.next();
			try (InputStream ruleJson = provider.getRuleJson()) {
				if (ruleJson != null) {
					loadedRules = checkAndConvertRules(reader.readValue(ruleJson));
				} else {
					LogService.getRoot().log(Level.FINE, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.usagestats.RuleService.load.empty", provider.getClass().getSimpleName()));
				}
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.tools.usagestats.RuleService.load.failure", provider.getClass().getSimpleName()), e);
				break;
			}
		}

		if (loadedRules != null) {
			RapidMinerVersion coreVersion = new RapidMinerVersion();
			List<VerifiableRule> newRules = new LinkedList<>();
			for (VerifiableRule rule : loadedRules) {
				// rules that have neither min nor max Studio version are automatically added
				if (rule.getRule().getMinStudioVersion() == null && rule.getRule().getMaxStudioVersion() == null) {
					newRules.add(rule);
					continue;
				}

				// skip rules that require a minimum Studio version which is above our current version
				if (rule.getRule().getMinStudioVersion() != null) {
					try {
						if (coreVersion.isAtLeast(new VersionNumber(rule.getRule().getMinStudioVersion()))) {
							newRules.add(rule);
						}
					} catch (VersionNumber.VersionNumberException e) {
						// malformed version number in JSON, skip rule
						LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.tools.usagestats.RuleService.load.invalid_min_version", rule.getRule().getMinStudioVersion()));
						continue;
					}
				}
				// skip rules that require a maximum Studio version which is below our current version
				if (rule.getRule().getMaxStudioVersion() != null) {
					try {
						if (coreVersion.isAtMost(new VersionNumber(rule.getRule().getMaxStudioVersion()))) {
							newRules.add(rule);
						}
					} catch (VersionNumber.VersionNumberException e) {
						// malformed version number in JSON, skip rule
						LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.tools.usagestats.RuleService.load.invalid_max_version", rule.getRule().getMinStudioVersion()));
					}
				}
			}

			rules.retainAll(newRules);
			rules.addAll(newRules);
		}

	}

	/**
	 * Converts JSON rules list to a {@link VerifiableRule} list. Also checks that rules do not
	 * violate the {@link #prohibitedKeywords} SQL blacklist. If any rules does, it is skipped.
	 *
	 * @param jsonRules
	 *            the input rule list
	 * @return the output verifiable rule list
	 */
	private List<VerifiableRule> checkAndConvertRules(List<Rule> jsonRules) {
		List<VerifiableRule> newRules = new ArrayList<>(jsonRules.size());
		for (Rule rule : jsonRules) {
			Matcher matcher = prohibitedKeywords.matcher(String.join(" ", rule.getQueries()));
			if (matcher.find()) {
				// prohibited keyword found, skip this rule
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.usagestats.RuleService.load_invalid_sql",
						new Object[]{rule.getId(), matcher.group()});
			} else {
				newRules.add(new VerifiableRule(rule));
			}
		}

		return newRules;
	}

}
