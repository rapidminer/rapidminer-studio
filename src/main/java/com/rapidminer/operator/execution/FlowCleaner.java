/**
 * Copyright (C) 2001-2016 RapidMiner GmbH
 */
package com.rapidminer.operator.execution;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.parameter.ParameterChangeListener;


/**
 * Cleans up {@link ExampleSet}s between operators if possible.
 *
 * @author Gisa Schaefer
 * @since 7.3
 */
enum FlowCleaner {

	INSTANCE;

	/** the cleanup is only possible if beta features are activated */
	private boolean cleanupPossible = Boolean
			.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES));

	private FlowCleaner() {
		// register listener for (de)activation of beta features
		ParameterService.registerParameterChangeListener(new ParameterChangeListener() {

			@Override
			public void informParameterSaved() {
				// not necessary
			}

			@Override
			public void informParameterChanged(String key, String value) {
				if (RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES.equals(key)) {
					cleanupPossible = Boolean.parseBoolean(value);
				}
			}
		});
	}

	/**
	 * Checks if the data is a {@link ExampleSet} that can be cleaned via
	 * {@link ExampleSet#cleanup()}. Does the cleanup if it is necessary.
	 *
	 * @param data
	 *            the current data at the inputPort
	 * @param inputPort
	 *            the input port to which the data belongs
	 * @return the cleaned up data or the unchanged data
	 */
	IOObject checkCleanup(IOObject data, InputPort inputPort) {
		if (cleanupPossible && data instanceof ExampleSet) {
			ExampleSet exampleSet = (ExampleSet) data;
			// do cleanup if there are unused columns
			if (exampleSet.getAttributes().allSize() < exampleSet.getExampleTable().getAttributeCount()) {
				ExampleSet clone = (ExampleSet) exampleSet.clone();
				clone.cleanup();
				inputPort.receive(clone);
				return clone;
			}
		}
		return data;
	}

}
