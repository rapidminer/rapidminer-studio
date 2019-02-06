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
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaDataFactory;
import com.rapidminer.parameter.IOObjectCacheSuggestionProvider;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeSuggestion;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * This operator can be used to retrieve the IOObject which was previously stored in the cache under
 * the specified name. In order to store an object to make it again accessible, you can use the
 * operator {@link IOPublishToAppOperator}. The combination of those two operators can be used
 * to cache {@link IOObject}s during a RapidMiner Server App session and use those cached
 * {@link IOObject}s multiple times in the app without executing the same process several times.
 *
 * This operator is used to manipulate a RapidMiner Server App. If the operator is executed in
 * RapidMiner Studio, the {@link IOObject} is retrieved from the cache in
 * {@link RapidMiner#getGlobalIOObjectCache()}.
 *
 * @author Sabrina Kirstein
 *
 */
public class IORecallfromAppOperator extends Operator {

	public static final String PARAMETER_NAME = "name";

	public static final String PARAMETER_IO_OBJECT = "io_object";

	public static final String PARAMETER_REMOVE_FROM_APP = "remove_from_app";

	private final OutputPort resultOutput = getOutputPorts().createPort("result");

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	/**
	 * @param description
	 */
	public IORecallfromAppOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				try {
					String name = getParameterAsString(PARAMETER_NAME);
					if (name != null && !name.isEmpty()) {
						IOObject object = getProcess().getIOObjectCache().get(name);
						if (object != null) {
							resultOutput.deliverMD(MetaDataFactory.getInstance().createMetaDataforIOObject(object, false));
						}
					}
				} catch (UndefinedParameterError e) {
					getLogger().fine("Cannot transform meta data: " + e);
				}
			}
		});

		dummyPorts.start();
		getTransformer().addRule(dummyPorts.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		IOObject object = null;
		String name = getParameterAsString(PARAMETER_NAME);

		object = getProcess().getIOObjectCache().get(name);

		if (object == null) {
			throw new UserError(this, "io.retrieve.dashboard.not_possible", name);
		}

		if (getParameterAsBoolean(PARAMETER_REMOVE_FROM_APP)) {
			object = getProcess().getIOObjectCache().remove(name);
			resultOutput.deliver(object);
		} else {
			resultOutput.deliver(object.copy());
		}

		dummyPorts.passDataThrough();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeSuggestion(PARAMETER_NAME,
				"The name under which the specified object was published to the app and should now be recalled.",
				new IOObjectCacheSuggestionProvider(), false));

		types.add(new ParameterTypeBoolean(PARAMETER_REMOVE_FROM_APP,
				"Indicates if the stored object should be removed from the app.", false, false));

		return types;
	}
}
