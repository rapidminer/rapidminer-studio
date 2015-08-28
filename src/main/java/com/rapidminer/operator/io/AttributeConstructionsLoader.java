/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
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
package com.rapidminer.operator.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.generator.GenerationException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParser;
import com.rapidminer.tools.expression.ExpressionParserBuilder;
import com.rapidminer.tools.expression.ExpressionRegistry;
import com.rapidminer.tools.expression.internal.ExpressionParserUtils;


/**
 * Loads an attribute set from a file and constructs the desired features. If keep_all is false,
 * original attributes are deleted before the new ones are created. This also means that a feature
 * selection is performed if only a subset of the original features was given in the file.
 *
 * @author Ingo Mierswa
 */
public class AttributeConstructionsLoader extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	/** The parameter name for &quot;Filename for the attribute constructions file.&quot; */
	public static final String PARAMETER_ATTRIBUTE_CONSTRUCTIONS_FILE = "attribute_constructions_file";

	/**
	 * The parameter name for &quot;If set to true, all the original attributes are kept, otherwise
	 * they are removed from the example set.&quot;
	 */
	public static final String PARAMETER_KEEP_ALL = "keep_all";

	public AttributeConstructionsLoader(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.SUPERSET) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				metaData.clearRegular();
				return metaData;
			}
		});
	}

	/** Loads the attribute set from a file and constructs desired features. */
	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		boolean keepAll = getParameterAsBoolean(PARAMETER_KEEP_ALL);
		List<Attribute> oldAttributes = new LinkedList<Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			oldAttributes.add(attribute);
		}

		File file = getParameterAsFile(PARAMETER_ATTRIBUTE_CONSTRUCTIONS_FILE);
		List<Attribute> generatedAttributes = new LinkedList<Attribute>();
		if (file != null) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				generatedAttributes = generateAll(this, exampleSet, in);
			} catch (java.io.IOException e) {
				throw new UserError(this, e, 302, new Object[] { file.getName(), e.getMessage() });
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						getLogger().warning("Cannot close stream to file " + file);
					}
				}
			}
		}

		if (!keepAll) {
			for (Attribute oldAttribute : oldAttributes) {
				if (!generatedAttributes.contains(oldAttribute)) {
					exampleSet.getAttributes().remove(oldAttribute);
				}
			}
		}

		exampleSetOutput.deliver(exampleSet);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_ATTRIBUTE_CONSTRUCTIONS_FILE,
				"Filename for the attribute constructions file.", "att", false, false));
		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_ALL,
				"If set to true, all the original attributes are kept, otherwise they are removed from the example set.",
				false, false));
		return types;
	}

	/**
	 * Parses all lines of the AttributeConstruction file and returns a list containing all newly
	 * generated attributes.
	 *
	 * @throws IOException
	 * @throws ProcessStoppedException
	 * @throws GenerationException
	 */
	private List<Attribute> generateAll(LoggingHandler logging, ExampleSet exampleSet, InputStream in) throws IOException,
			GenerationException, ProcessStoppedException {
		LinkedList<Attribute> generatedAttributes = new LinkedList<>();
		Document document = null;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		} catch (SAXException e1) {
			throw new IOException(e1.getMessage());
		} catch (ParserConfigurationException e1) {
			throw new IOException(e1.getMessage());
		}

		Element constructionsElement = document.getDocumentElement();
		if (!constructionsElement.getTagName().equals("constructions")) {
			throw new IOException("Outer tag of attribute constructions file must be <constructions>");
		}

		NodeList constructions = constructionsElement.getChildNodes();
		for (int i = 0; i < constructions.getLength(); i++) {
			Node node = constructions.item(i);
			if (node instanceof Element) {
				Element constructionTag = (Element) node;
				String tagName = constructionTag.getTagName();
				if (!tagName.equals("attribute")) {
					throw new IOException("Only <attribute> tags are allowed for attribute description files, but found "
							+ tagName);
				}
				String attributeName = constructionTag.getAttribute("name");
				String attributeConstruction = constructionTag.getAttribute("construction");
				if (attributeName == null) {
					throw new IOException("<attribute> tag needs 'name' attribute.");
				}
				if (attributeConstruction == null) {
					throw new IOException("<attribute> tag needs 'construction' attribute.");
				}
				if (attributeConstruction.equals(attributeName)) {
					Attribute presentAttribute = exampleSet.getAttributes().get(attributeName);
					if (presentAttribute != null) {
						generatedAttributes.add(presentAttribute);
						continue;
					} else {
						throw new GenerationException("No such attribute: " + attributeName);
					}
				} else {

					ExpressionParserBuilder builder = new ExpressionParserBuilder();

					// decide which functions should be available
					builder = builder.withModules(ExpressionRegistry.INSTANCE.getAll());
					ExampleResolver resolver = new ExampleResolver(exampleSet);
					builder = builder.withDynamics(resolver);

					ExpressionParser parser = builder.build();

					try {
						generatedAttributes.add(ExpressionParserUtils.addAttribute(exampleSet, attributeName,
								attributeConstruction, parser, resolver, null));
					} catch (ExpressionException e) {
						throw new GenerationException(e.getShortMessage());
					}
				}
			}
		}
		return generatedAttributes;
	}
}
