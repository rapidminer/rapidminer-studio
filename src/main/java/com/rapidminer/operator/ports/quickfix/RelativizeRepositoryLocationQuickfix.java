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
package com.rapidminer.operator.ports.quickfix;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.repository.RepositoryLocation;


/**
 * Replaces an absolute reference to a repository entry by an entry resolved relative to the current
 * process.
 *
 * @author Simon Fischer
 *
 */
public class RelativizeRepositoryLocationQuickfix extends AbstractQuickFix {

	private String key;
	private Operator operator;

	public RelativizeRepositoryLocationQuickfix(Operator operator, String key, String value) {
		super(10, false, "relativize_repository_location", key, value);
		this.key = key;
		this.operator = operator;
	}

	@Override
	public void apply() {
		RepositoryLocation absLoc;
		try {
			absLoc = operator.getParameterAsRepositoryLocation(key);
			final RepositoryLocation processLoc = operator.getProcess().getRepositoryLocation() != null
					? operator.getProcess().getRepositoryLocation().parent() : null;
			if (processLoc == null) {
				SwingTools.showVerySimpleErrorMessage("quickfix_failed", "Process is not stored in repository.");
			} else {
				String relative = absLoc.makeRelative(processLoc);
				operator.setParameter(key, relative);
			}
		} catch (UserError e) {
			// Should not happen. Parameter should be set, otherwise we would not have created this
			// prefix.
			SwingTools.showVerySimpleErrorMessage("quickfix_failed", e.toString());
		}
	}

}
