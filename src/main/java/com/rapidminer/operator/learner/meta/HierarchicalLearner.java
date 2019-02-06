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
package com.rapidminer.operator.learner.meta;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Condition;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.example.set.ConditionedExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.RandomGenerator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Tobias Malbrecht, Sebastian Land
 */
@Deprecated
public class HierarchicalLearner extends AbstractMetaLearner {

	public static final String PARAMETER_HIERARCHY = "hierarchy";

	public static final String PARAMETER_PARENT_CLASS = "parent_class";

	public static final String PARAMETER_CHILD_CLASS = "child_class";

	public HierarchicalLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet inputSet) throws OperatorException {
		Attribute classLabel = inputSet.getAttributes().getLabel();
		if (classLabel.getMapping().size() == 2) {
			return applyInnerLearner(inputSet);
		}

		// create model hierarchy / tree
		List<String[]> hierarchyEntries = getParameterList(PARAMETER_HIERARCHY);
		Map<String, HierarchicalModel.Node> nodeMap = new HashMap<String, HierarchicalModel.Node>();
		Set<HierarchicalModel.Node> innerNodes = new HashSet<HierarchicalModel.Node>();
		for (String[] entries : hierarchyEntries) {
			String parentClass = entries[0];
			String childClass = entries[1];
			HierarchicalModel.Node parentNode = nodeMap.get(parentClass);
			if (parentNode == null) {
				parentNode = new HierarchicalModel.Node(parentClass);
			}
			HierarchicalModel.Node childNode = nodeMap.get(childClass);
			if (childNode == null) {
				childNode = new HierarchicalModel.Node(childClass);
			}
			parentNode.addChild(childNode);
			nodeMap.put(parentClass, parentNode);
			nodeMap.put(childClass, childNode);
			innerNodes.add(childNode);
		}
		HierarchicalModel.Node root = new HierarchicalModel.Node("_ROOT_");
		for (HierarchicalModel.Node node : nodeMap.values()) {
			if (!innerNodes.contains(node)) {
				root.addChild(node);
			}
		}

		try {
			// compute model (by DFS)
			computeModel(root, inputSet, classLabel);
		} catch (ConditionCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new HierarchicalModel(inputSet, root);
	}

	private void computeModel(HierarchicalModel.Node node, ExampleSet eSet, Attribute originalLabel)
			throws ConditionCreationException, OperatorException {
		// create child->parent label replacement map (by DFS)
		Map<String, String> classesMap = new HashMap<String, String>();
		for (HierarchicalModel.Node child : node.getChildren()) {
			if (child.getChildrenClasses().size() > 0) {
				for (String childClass : child.getChildrenClasses()) {
					classesMap.put(childClass, child.getClassName());
				}
			} else {
				classesMap.put(child.getClassName(), child.getClassName());
			}
		}

		// create working label with parent class labels
		eSet.getAttributes().setSpecialAttribute(originalLabel, "label_original");
		Attribute workingLabel = AttributeFactory.createAttribute(originalLabel.getName() + "_" + node.getClassName(),
				originalLabel.getValueType());
		eSet.getExampleTable().addAttribute(workingLabel);
		eSet.getAttributes().addRegular(workingLabel);
		for (Example example : eSet) {
			double index = example.getValue(originalLabel);
			if (!Double.isNaN(index)) {
				String value = originalLabel.getMapping().mapIndex((int) index);
				String mapVl = classesMap.get(value);
				if (mapVl != null) {
					example.setValue(workingLabel, workingLabel.getMapping().mapString(mapVl));
				} else {
					example.setValue(workingLabel, Double.NaN);
				}
			} else {
				example.setValue(workingLabel, Double.NaN);
			}
		}

		eSet.getAttributes().setLabel(workingLabel);
		Model model = applyInnerLearner(eSet);
		node.setModel(model);

		// compute models for child nodes
		for (HierarchicalModel.Node child : node.getChildren()) {
			if (child.getChildren().size() > 0) {
				Condition c = ConditionedExampleSet.createCondition(
						ConditionedExampleSet.KNOWN_CONDITION_NAMES[ConditionedExampleSet.CONDITION_ATTRIBUTE_VALUE_FILTER],
						eSet, workingLabel.getName() + "=" + child.getClassName());
				ExampleSet trainingSet = new ConditionedExampleSet(eSet, c);
				computeModel(child, trainingSet, originalLabel);
			}
		}
		eSet.getAttributes().setLabel(originalLabel);
		eSet.getAttributes().remove(workingLabel);
		eSet.getExampleTable().removeAttribute(workingLabel);
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NUMERICAL_LABEL:
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
				return false;
			default:
				return true;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeList(PARAMETER_HIERARCHY, "The hierarchy...", new ParameterTypeString(
				PARAMETER_PARENT_CLASS, "The parent class.", false), new ParameterTypeString(PARAMETER_CHILD_CLASS,
				"The child class.", false)));
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

}
