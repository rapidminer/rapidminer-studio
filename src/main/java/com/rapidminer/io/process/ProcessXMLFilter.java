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
package com.rapidminer.io.process;

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;

import org.w3c.dom.Element;


/**
 * Filter applied to operators and processes during import and export of process XML files. E.g.,
 * the ProcessRenderer uses this functionality to add GUI information to the process XML.
 * 
 * @author Simon Fischer
 * 
 */
public interface ProcessXMLFilter {

	public void operatorExported(Operator operator, Element element);

	public void operatorImported(Operator operator, Element element);

	public void executionUnitExported(ExecutionUnit unit, Element element);

	public void executionUnitImported(ExecutionUnit unit, Element element);

}
