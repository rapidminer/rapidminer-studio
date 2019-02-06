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

import java.util.List;

import com.rapidminer.RapidMiner;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.IOObjectCacheSuggestionProvider;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeSuggestion;


/**
 * This operator can be used to store the given IOObject into a cache under the specified name (the
 * IOObject will be &quot;hidden&quot; and can not be directly accessed by following operators. In
 * order to retrieve the stored object and make it again accessible, you can use the operator
 * {@link IORecallfromAppOperator}. The combination of those two operators can be used to cache
 * {@link IOObject}s during a RapidMiner Server App session and use those cached {@link IOObject}s
 * multiple times in the app without executing the same process several times.
 *
 * This operator is used to manipulate a RapidMiner Server App. If the operator is executed in
 * RapidMiner Studio, the {@link IOObject} is stored in the cache in
 * {@link RapidMiner#getGlobalIOObjectCache()}.
 *
 * @author Sabrina Kirstein
 *
 */
public class IOPublishToAppOperator extends Operator {

	private final InputPort storeInput = getInputPorts().createPort("store");
	private final OutputPort storedOutput = getOutputPorts().createPort("stored");

	public static final String PARAMETER_NAME = "name";

	/**
	 * @param description
	 */
	public IOPublishToAppOperator(OperatorDescription description) {
		super(description);
		storeInput.addPrecondition(new SimplePrecondition(storeInput, new MetaData(IOObject.class)));
		getTransformer().addRule(new PassThroughRule(storeInput, storedOutput, false));
	}

	@Override
	public void doWork() throws OperatorException {
		IOObject object = storeInput.getData(IOObject.class);
		String name = getParameterAsString(PARAMETER_NAME);

		getProcess().getIOObjectCache().store(name, object.copy());
		storedOutput.deliver(object);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeSuggestion(PARAMETER_NAME,
				"The name under which the specified object is published to the app and can later be recalled.",
				new IOObjectCacheSuggestionProvider(), false));

		return types;
	}
}
