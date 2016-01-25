/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.operator.clustering.clusterer;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;

import java.util.List;


/**
 * Adds a parameter by which the user can choose to generate a cluster attribute. All clusterers
 * other than the Weka clusterers should extends this class rather than AbstractClusterer directly.
 * 
 * @author Simon Fischer
 * 
 */
public abstract class RMAbstractClusterer extends AbstractClusterer {

	/**
	 * The parameter name for &quot;Indicates if a cluster id is generated as new special
	 * attribute.&quot;
	 */
	public static final String PARAMETER_ADD_CLUSTER_ATTRIBUTE = "add_cluster_attribute";
	public static final String PARAMETER_REMOVE_UNLABELED = "remove_unlabeled";
	public static final String PARAMETER_ADD_AS_LABEL = "add_as_label";

	public RMAbstractClusterer(OperatorDescription description) {
		super(description);
	}

	@Override
	protected boolean addsClusterAttribute() {
		return getParameterAsBoolean(PARAMETER_ADD_CLUSTER_ATTRIBUTE);
	}

	@Override
	protected boolean addsIdAttribute() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeBoolean(
				PARAMETER_ADD_CLUSTER_ATTRIBUTE,
				"If enabled, a cluster id is generated as new special attribute directly in this operator, otherwise this operator does not add an id attribute. In the latter case you have to use the Apply Model operator to generate the cluster attribute.",
				true, false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_ADD_AS_LABEL,
				"If true, the cluster id is stored in an attribute with the special role 'label' instead of 'cluster'.",
				false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_REMOVE_UNLABELED, "Delete the unlabeled examples.", false);
		type.setExpert(false);
		types.add(type);

		return types;
	}

}
