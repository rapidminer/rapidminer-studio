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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.operator.ports.InputPort;

import java.util.Collection;


/**
 * Checks whether a given port receives an object of a desired class. If more checks are desired,
 * override {@link #makeAdditionalChecks}.
 * 
 * @author Simon Fischer
 * 
 * */
public class SimplePrecondition extends AbstractPrecondition {

	private final MetaData desiredMetaData;
	private final boolean mandatory;

	public SimplePrecondition(InputPort inputPort, MetaData desiredMetaData) {
		this(inputPort, desiredMetaData, true);
	}

	public SimplePrecondition(InputPort inputPort, MetaData desiredMetaData, boolean mandatory) {
		super(inputPort);
		this.desiredMetaData = desiredMetaData;
		this.mandatory = mandatory;
	}

	@Override
	public final void check(MetaData metaData) {
		InputPort inputPort = getInputPort();
		if (metaData == null) {
			if (isMandatory()) {
				inputPort.addError(new InputMissingMetaDataError(inputPort, desiredMetaData.getObjectClass(), null));
			}
		} else {
			if (desiredMetaData != null) {
				Collection<MetaDataError> errors = desiredMetaData.getErrorsForInput(inputPort, metaData,
						CompatibilityLevel.VERSION_5);
				for (MetaDataError error : errors) {
					inputPort.addError(error);
				}
			}
			makeAdditionalChecks(metaData);
		}
	}

	@Override
	public String getDescription() {
		return (isMandatory() ? "<em>expects:</em> " : "<em>optional:</em> ") + desiredMetaData;
	}

	/** Override this method to make additional checks. The default implementation does nothing. */
	public void makeAdditionalChecks(MetaData received) {}

	@Override
	public boolean isCompatible(MetaData input, CompatibilityLevel level) {
		if (desiredMetaData != null) {
			return desiredMetaData.isCompatible(input, level);
		} else {
			return true;
		}
	}

	@Override
	public void assumeSatisfied() {
		if (mandatory && (desiredMetaData != null)) {
			getInputPort().receiveMD(desiredMetaData.clone());
		}
	}

	protected boolean isMandatory() {
		return mandatory;
	}

	@Override
	public MetaData getExpectedMetaData() {
		return desiredMetaData;
	}
}
