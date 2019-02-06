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
package com.rapidminer.operator.learner.tree;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.learner.meta.SimpleVoteModel;


/**
 *
 * This model simply extends the SimpleVoteModel to avoid naming problems. The functionality remains
 * unchanged.
 *
 * @author Sebastian Land
 * @deprecated Since 7.0.0, replaced by {@link ConfigurableRandomForestModel}.
 */
@Deprecated
public class RandomForestModel extends SimpleVoteModel {

	private static final long serialVersionUID = 1L;

	public RandomForestModel(ExampleSet exampleSet, List<TreeModel> baseModels) {
		super(exampleSet, baseModels);
	}

	@Override
	public String getName() {
		return "Random Forest Model";
	}
}
