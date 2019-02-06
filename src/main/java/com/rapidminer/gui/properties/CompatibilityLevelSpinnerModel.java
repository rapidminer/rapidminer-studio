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
package com.rapidminer.gui.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractSpinnerModel;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorVersion;


/**
 * Displays the {@link OperatorVersion}s as returned by
 * {@link Operator#getIncompatibleVersionChanges()}. and {@link Operator#getCompatibilityLevel()} .
 *
 * @author Simon Fischer
 *
 */
public class CompatibilityLevelSpinnerModel extends AbstractSpinnerModel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private int index = 0;
	private Operator operator;
	private List<OperatorVersion> versions = new LinkedList<>();

	protected void setOperator(Operator operator) {
		this.operator = operator;
		this.versions = new LinkedList<>(Arrays.asList(operator.getIncompatibleVersionChanges()));
		OperatorVersion latest = OperatorVersion.getLatestVersion(operator.getOperatorDescription());
		if (!versions.contains(latest)) {
			versions.add(latest);
		}

		// sort list to have a ascending order
		Collections.sort(versions);

		setValue(operator.getCompatibilityLevel());
	}

	@Override
	public Object getNextValue() {
		if (index + 1 >= versions.size()) {
			return null;
		} else {
			return versions.get(index + 1);
		}
	}

	@Override
	public Object getPreviousValue() {
		if (index <= 0) {
			return null;
		} else {
			return versions.get(index - 1);
		}
	}

	@Override
	public Object getValue() {
		if (operator != null) {
			return operator.getCompatibilityLevel();
		} else {
			return "-------";
		}
	}

	@Override
	public void setValue(Object value) {
		if (operator != null) {
			if (value instanceof String) {
				value = new OperatorVersion((String) value);
			}
			if (value == null) {
				value = OperatorVersion.getLatestVersion(operator.getOperatorDescription());
			}
			operator.setCompatibilityLevel((OperatorVersion) value);
			index = versions.indexOf(value);
			if (index == -1) {
				versions.add((OperatorVersion) value);
				Collections.sort(versions);
				index = versions.indexOf(value);
			}
			fireStateChanged();
		}
	}
}
