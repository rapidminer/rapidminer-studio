/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.processeditor.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.learner.Learner;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.search.AbstractGlobalSearchManager;
import com.rapidminer.search.GlobalSearchDefaultField;
import com.rapidminer.search.GlobalSearchUtilities;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.documentation.OperatorDocBundle;


/**
 * Manages operator search for the Global Search feature. See {@link OperatorGlobalSearch}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
class OperatorGlobalSearchManager extends AbstractGlobalSearchManager implements OperatorService.OperatorServiceListener {

	private static final Map<String, String> ADDITIONAL_FIELDS;

	private static final String FIELD_TAG = "tag";
	private static final String FIELD_PARAMETER = "parameter";
	private static final String FIELD_INPUT_CLASS = "input";
	private static final String FIELD_OUTPUT_CLASS = "output";
	private static final String FIELD_CAPABILITIES = "capability";
	private static final String FIELD_SOURCE = "source";

	private static final String DUMMY_OPERATOR = "dummy";
	private static final String PROCESS_ROOT_OPERATOR = "process";
	private static final String IO_OBJECT = "IOObject";

	private static final float FIELD_BOOST_TAG = 0.5f;
	private static final float FIELD_BOOST_SOURCE = 0.25f;

	static {
		ADDITIONAL_FIELDS = new HashMap<>();
		ADDITIONAL_FIELDS.put(FIELD_TAG, "The tags of the operator");
		ADDITIONAL_FIELDS.put(FIELD_PARAMETER, "The parameter names of the operator");
		ADDITIONAL_FIELDS.put(FIELD_INPUT_CLASS, "The input port types of the operator");
		ADDITIONAL_FIELDS.put(FIELD_OUTPUT_CLASS, "The output port types of the operator");
		ADDITIONAL_FIELDS.put(FIELD_CAPABILITIES, "The capabilities of the operator (typically only specified for learners)");
		ADDITIONAL_FIELDS.put(FIELD_SOURCE, "The source of the operator, e.g. RapidMiner Studio Core or an extension");
	}


	protected OperatorGlobalSearchManager() {
		super(OperatorGlobalSearch.CATEGORY_ID, ADDITIONAL_FIELDS, new GlobalSearchDefaultField(FIELD_TAG, FIELD_BOOST_TAG), new GlobalSearchDefaultField(FIELD_SOURCE, FIELD_BOOST_SOURCE));
	}

	@Override
	public void operatorRegistered(final OperatorDescription description, final OperatorDocBundle bundle) {
		if (description.isDeprecated()) {
			return;
		}
		if (DUMMY_OPERATOR.equals(description.getKey()) || PROCESS_ROOT_OPERATOR.equals(description.getKey())) {
			return;
		}
		addDocumentToIndex(createDocument(description));
	}

	@Override
	public void operatorUnregistered(final OperatorDescription description) {
		removeDocumentFromIndex(createDocument(description));
	}

	@Override
	protected void init() {
		OperatorService.addOperatorServiceListener(this);
	}

	@Override
	protected List<Document> createInitialIndex(final ProgressThread progressThread) {
		// not needed
		return Collections.emptyList();
	}

	/**
	 * Creates an operator search document for the given operator description.
	 *
	 * @param opDesc
	 * 		the operator description for which to create the search document
	 * @return the document, never {@code null}
	 */
	private Document createDocument(final OperatorDescription opDesc) {
		List<Field> fields = new ArrayList<>();

		// add tags
		List<String> tags = opDesc.getTags();
		StringBuilder sb = new StringBuilder();
		if (!tags.isEmpty()) {
			for (String tag : tags) {
				sb.append(tag);
				sb.append(' ');
			}
		}
		sb.append(opDesc.getGroupName());
		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_TAG, sb.toString()));
		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_SOURCE, opDesc.getProviderName()));

		// add parameters and input/output port classes
		// as of 8.1, this operator creation does not cost any time, even though it looks scary
		// for over a dozen extensions, the whole try-block below with operator creation adds less than 1 second to Studio start
		try {
			Operator op = opDesc.createOperatorInstance();

			// store parameter keys
			createParameterField(fields, op);

			// store input port types
			createInputPortField(fields, op);

			// store output port types
			createOutputPortField(fields, op);

			// for learners, also store their capabilities
			if (op instanceof CapabilityProvider) {
				createCapabilitiesField(fields, (CapabilityProvider) op);
			}
		} catch (OperatorCreationException e) {
			// should not happen, if it does, ignore
		}
		return GlobalSearchUtilities.INSTANCE.createDocument(opDesc.getKey(), opDesc.getName(), fields.toArray(new Field[0]));
	}

	/**
	 * Creates the parameter field for operators.
	 *
	 * @param fields
	 * 		the list of fields to which the new field should be added
	 * @param op
	 * 		the operator instance
	 */
	private void createParameterField(final List<Field> fields, final Operator op) {
		StringBuilder sb;
		sb = new StringBuilder();
		for (ParameterType type : op.getParameterTypes()) {
			String key = type.getKey().replaceAll("_", " ");
			sb.append(key);
			sb.append(' ');
		}
		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_PARAMETER, sb.toString()));
	}

	/**
	 * Creates the input port type field for operators.
	 *
	 * @param fields
	 * 		the list of fields to which the new field should be added
	 * @param op
	 * 		the operator instance
	 */

	private void createInputPortField(final List<Field> fields, final Operator op) {
		StringBuilder sb;
		InputPorts inputPorts = op.getInputPorts();
		if (inputPorts != null && inputPorts.getNumberOfPorts() > 0) {
			sb = new StringBuilder();
			// there is a port? Every port input class extends IOObject, so add that
			sb.append(IO_OBJECT);
			sb.append(' ');
			for (InputPort inPort : inputPorts.getAllPorts()) {
				if (!inPort.getAllPreconditions().isEmpty()) {
					List<Precondition> preconditions = new LinkedList<>(inPort.getAllPreconditions());
					MetaData expectedMD = preconditions.get(0).getExpectedMetaData();
					String name = RendererService.getName(expectedMD.getObjectClass());
					if (name != null) {
						sb.append(name);
					} else {
						sb.append(inPort.getName());
					}
				} else {
					sb.append(inPort.getName());
				}
				sb.append(' ');
			}
			fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_INPUT_CLASS, sb.toString()));
		}
	}

	/**
	 * Creates the output port type field for operators.
	 *
	 * @param fields
	 * 		the list of fields to which the new field should be added
	 * @param op
	 * 		the operator instance
	 */
	private void createOutputPortField(final List<Field> fields, final Operator op) {
		StringBuilder sb;
		OutputPorts outputPorts = op.getOutputPorts();
		if (outputPorts != null && outputPorts.getNumberOfPorts() > 0) {
			sb = new StringBuilder();
			// there is a port? Every port output class extends IOObject, so add that
			sb.append(IO_OBJECT);
			sb.append(' ');

			// to prepare output MD, we need to trigger generation here
			try {
				op.transformMetaData();
			} catch (Exception e) {
				// some extensions may throw here, just ignore it and move on
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.processeditor.global_search.OperatorSearchManager.error.output_metadata_transform_failed",
						op.getOperatorDescription().getKey());
			}
			for (OutputPort outPort : outputPorts.getAllPorts()) {
				// we need to use the deprecated method because we don't want specific MD, but whatever MD is there
				MetaData resultMD = outPort.getMetaData();
				if (resultMD != null) {
					String name = RendererService.getName(resultMD.getObjectClass());
					if (name != null) {
						sb.append(name);
					} else {
						sb.append(outPort.getName());
					}
				} else {
					sb.append(outPort.getName());
				}
				sb.append(' ');
			}
			fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_OUTPUT_CLASS, sb.toString()));
		}
	}

	/**
	 * Create the capabilities field for {@link Learner}s.
	 *
	 * @param fields
	 * 		the list of fields to which the new field should be added
	 * @param op
	 * 		the learner instance
	 */
	private void createCapabilitiesField(final List<Field> fields, final CapabilityProvider op) {
		StringBuilder sb;
		sb = new StringBuilder();
		for (OperatorCapability capability : OperatorCapability.values()) {
			if (op.supportsCapability(capability)) {
				sb.append(capability.getDescription());
				sb.append(' ');
			}
		}

		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_CAPABILITIES, sb.toString()));
	}

}
