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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.preprocessing.MaterializeDataInMemory;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * Abstract superclass of all operators modifying an example set, i.e. accepting an
 * {@link ExampleSet} as input and delivering an {@link ExampleSet} as output. The behavior is
 * delegated from the {@link #doWork()} method to {@link #apply(ExampleSet)}.
 *
 * @author Simon Fischer
 */
public abstract class AbstractExampleSetProcessing extends Operator {

	/**
	 * name of the example set input port
	 *
	 * @since 8.2.0
	 */
	public static final String EXAMPLE_SET_INPUT_PORT_NAME = "example set input";

	/**
	 * name of the example set output port
	 *
	 * @since 8.2.0
	 */
	public static final String EXAMPLE_SET_OUTPUT_PORT_NAME = "example set output";

	/**
	 * name of the original output port
	 *
	 * @since 8.2.0
	 */
	public static final String ORIGINAL_OUTPUT_PORT_NAME = "original";

	private final InputPort exampleSetInput = getInputPorts().createPort(EXAMPLE_SET_INPUT_PORT_NAME);
	private final OutputPort exampleSetOutput = getOutputPorts().createPort(EXAMPLE_SET_OUTPUT_PORT_NAME);
	private final OutputPort originalOutput = getOutputPorts().createPort(ORIGINAL_OUTPUT_PORT_NAME);

	public AbstractExampleSetProcessing(OperatorDescription description) {
		super(description);
		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, getRequiredMetaData()));
		getTransformer().addRule(new PassThroughRule(exampleSetInput, exampleSetOutput, false) {

			@Override
			public MetaData modifyMetaData(MetaData metaData) {
				if (metaData instanceof ExampleSetMetaData) {
					try {
						return AbstractExampleSetProcessing.this.modifyMetaData((ExampleSetMetaData) metaData);
					} catch (UndefinedParameterError e) {
						return metaData;
					}
				} else {
					return metaData;
				}
			}
		});
		getTransformer().addPassThroughRule(exampleSetInput, originalOutput);
	}

	/** Returns the example set input port, e.g. for adding errors. */
	protected final InputPort getInputPort() {
		return exampleSetInput;
	}

	/**
	 * Subclasses might override this method to define the meta data transformation performed by
	 * this operator.
	 *
	 * @throws UndefinedParameterError
	 */
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		return metaData;
	}

	/**
	 * Subclasses my override this method to define more precisely the meta data expected by this
	 * operator.
	 */
	protected ExampleSetMetaData getRequiredMetaData() {
		return new ExampleSetMetaData();
	}

	@Override
	public final void doWork() throws OperatorException {
		ExampleSet inputExampleSet = exampleSetInput.getData(ExampleSet.class);
		ExampleSet applySet = null;
		// check for needed copy of original exampleset
		if (writesIntoExistingData()) {
			int type = DataRowFactory.TYPE_DOUBLE_ARRAY;
			if (inputExampleSet.getExampleTable() instanceof MemoryExampleTable) {
				DataRowReader dataRowReader = inputExampleSet.getExampleTable().getDataRowReader();
				if (dataRowReader.hasNext()) {
					type = dataRowReader.next().getType();
				}
			}
			// check if type is supported to be copied
			if (type >= 0) {
				applySet = MaterializeDataInMemory.materializeExampleSet(inputExampleSet, type);
			}
		}

		if (applySet == null) {
			applySet = (ExampleSet) inputExampleSet.clone();
		}

		// we apply on the materialized data, because writing can't take place in views anyway.
		ExampleSet result = apply(applySet);
		originalOutput.deliver(inputExampleSet);
		exampleSetOutput.deliver(result);
	}

	/**
	 * Delegate for the apply method. The given ExampleSet is already a clone of the input example
	 * set so that changing this examples set does not affect the original one. Subclasses should
	 * avoid cloning again unnecessarily.
	 */
	public abstract ExampleSet apply(ExampleSet exampleSet) throws OperatorException;

	/**
	 * This method indicates whether the operator will perform a write operation on a cell in an
	 * existing column of the example set's {@link ExampleTable}. If yes, the original example will
	 * be completely copied in memory if the original port is used.
	 *
	 * <strong>Note: </strong> Subclasses must implement this method. The safe implementation would
	 * be to return true, however, for backwards compatibility, the default implementation returns
	 * false.
	 */
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == originalOutput) {
			return getParameterAsBoolean("keep_example_set");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	public InputPort getExampleSetInputPort() {
		return exampleSetInput;
	}

	public OutputPort getExampleSetOutputPort() {
		return exampleSetOutput;
	}

	/**
	 * Used for backward compatibility only.
	 *
	 * @since 7.2.0
	 * @return
	 */
	public boolean isOriginalOutputConnected() {
		return originalOutput.isConnected();
	}
}
