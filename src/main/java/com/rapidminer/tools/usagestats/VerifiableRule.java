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

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * Verifiable Rule which is a mechanism to check if the passed {@link Rule} is fulfilled. If so, the
 * message of the rule is displayed and its user interaction is stored in the CTA database.
 *
 * @author Jonas Wilms-Pfau, Marco Boeck
 * @since 7.5.0
 *
 */
class VerifiableRule {

	/** Conversion factor to convert a second into a millisecond */
	private static final long SECOND_TO_MILLISECOND = 1000;

	/** Timestamp of the last execution */
	private long lastRun = 0l;

	/** Only run once at a time */
	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	/** flag to indicate if CTA is currently displayed to user */
	private final AtomicBoolean isDisplaying = new AtomicBoolean(false);

	/** the rule parsed from JSON */
	private final Rule rule;

	/**
	 * Creates a verifiable rule based on the given {@link Rule}.
	 *
	 * @param rule
	 */
	VerifiableRule(Rule rule) {
		this.rule = rule;
	}

	/**
	 * Check if the rule should be verified, if yes execute the queries against the CTA Database, if
	 * they are all fulfilled display the message
	 *
	 * @return {@code true} if the rule is not running/displaying and all rule queries return true;
	 *         {@code false} otherwise
	 */
	public boolean isRuleFulfilled() {
		// Check if the rule should be executed
		if (isDisplaying.get() == false && isRunning.get() == false
				&& System.currentTimeMillis() >= lastRun + rule.getInterval() * SECOND_TO_MILLISECOND) {
			// Try to get the lock
			if (isRunning.compareAndSet(false, true)) {
				try {
					lastRun = System.currentTimeMillis();
					return CtaDao.INSTANCE.verify(rule);
				} catch (SQLException e) {
					// Don't run this rule again today
					lastRun += TimeUnit.DAYS.toMillis(1);
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.usagestats.VerifiableRule.verification.failed", rule.getId()), e);
				} finally {
					isRunning.set(false);
				}
			}
		}

		return false;
	}

	/**
	 * Returns the {@link Rule} for this rule.
	 *
	 * @return the rule, never {@code null}
	 */
	public Rule getRule() {
		return rule;
	}

	/**
	 * Set the flag that this rule is displaying. Rules which are displaying cannot return
	 * {@code true} for {@link #isRuleFulfilled()}.
	 */
	public void setDisplaying(boolean displaying) {
		isDisplaying.set(displaying);
	}

	/**
	 * Returns whether this rule is currently displaying its CTA.
	 *
	 * @return {@code true} if the rule is currently displaying; {@code false} otherwise
	 */
	public boolean isDisplaying() {
		return isDisplaying.get();
	}

	@Override
	public String toString() {
		return rule.toString() + " Last run: " + lastRun;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (rule == null ? 0 : rule.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		VerifiableRule other = (VerifiableRule) obj;
		if (rule == null) {
			if (other.rule != null) {
				return false;
			}
		} else if (!rule.equals(other.rule)) {
			return false;
		}
		return true;
	}

}
