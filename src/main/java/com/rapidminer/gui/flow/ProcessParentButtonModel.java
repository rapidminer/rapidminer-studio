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
package com.rapidminer.gui.flow;

import com.rapidminer.Process;
import com.rapidminer.gui.tools.ParentButtonModel;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;

import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;


/**
 * 
 * @author Simon Fischer
 */
public class ProcessParentButtonModel implements ParentButtonModel<Operator> {

	private Process process;

	public ProcessParentButtonModel(Process process) {
		this.process = process;
	}

	private List<Operator> getChildren(OperatorChain chain) {
		List<Operator> children = new LinkedList<>();
		for (ExecutionUnit executionUnit : chain.getSubprocesses()) {
			children.addAll(executionUnit.getOperators());
		}
		return children;
	}

	@Override
	public Operator getChild(Operator node, int index) {
		if (node instanceof OperatorChain) {
			return getChildren((OperatorChain) node).get(index);
		} else {
			return null;
		}
	}

	@Override
	public int getNumberOfChildren(Operator node) {
		if (node instanceof OperatorChain) {
			return getChildren((OperatorChain) node).size();
		} else {
			return 0;
		}
	}

	@Override
	public Operator getParent(Operator child) {
		return child.getParent();
	}

	@Override
	public Operator getRoot() {
		if (process != null) {
			return process.getRootOperator();
		} else {
			return null;
		}
	}

	@Override
	public String toString(Operator node) {
		return node.getName();
	}

	public void setProcess(Process process) {
		this.process = process;
	}

	@Override
	public Icon getIcon(Operator node) {
		return node.getOperatorDescription().getSmallIcon();
	}
}
