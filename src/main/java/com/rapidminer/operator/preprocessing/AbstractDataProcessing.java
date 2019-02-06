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
package com.rapidminer.operator.preprocessing;

import com.rapidminer.operator.AbstractExampleSetProcessing;
import com.rapidminer.operator.OperatorDescription;


/**
 * Abstract super class of the {@link AbstractExampleSetProcessing} hierarchy in the preprocessing
 * package. At this stage it is not yet clear which additional subclasses we should add and what the
 * differences are between this package and the features.* packages.
 * 
 * TODO: Clarify relation to features.*
 * 
 * @author Simon Fischer
 */
public abstract class AbstractDataProcessing extends AbstractExampleSetProcessing {

	public AbstractDataProcessing(OperatorDescription description) {
		super(description);
	}

}
