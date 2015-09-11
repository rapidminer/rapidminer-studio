/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tour;

import com.rapidminer.ProcessSetupListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;

import java.util.ArrayList;


/**
 * This Step is a Prestep which ensures that two Operators are available.
 * 
 * @author kamradt
 * 
 */
public class CheckOperatorsStep extends Step {

	private Class<? extends Operator> operatorClass2, operatorClass1;
	private int split1, split2;
	private ProcessSetupListener setupListener;
	private final String textKey = "missingOperators";

	public CheckOperatorsStep(Class<? extends Operator> startOperatorClass, int startSplit,
			Class<? extends Operator> targetOperatorClass, int targetSplit) {
		this.operatorClass1 = startOperatorClass;
		this.operatorClass2 = targetOperatorClass;
		this.split1 = startSplit;
		this.split2 = targetSplit;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#createBubble()
	 */
	@Override
	boolean createBubble() {
		if (!this.areOperatorsAvailable()) {
			bubble = new DockableBubble(RapidMinerGUI.getMainFrame(), AlignedSide.MIDDLE, textKey,
					ProcessPanel.PROCESS_PANEL_DOCK_KEY);
			setupListener = new ProcessSetupListener() {

				@Override
				public void operatorRemoved(Operator operator, int oldIndex, int oldIndexAmongEnabled) {
					if (CheckOperatorsStep.this.areOperatorsAvailable()) {
						CheckOperatorsStep.this.bubble.triggerFire();
						CheckOperatorsStep.this.stepCanceled();
					}
				}

				@Override
				public void operatorChanged(Operator operator) {
					if (CheckOperatorsStep.this.areOperatorsAvailable()) {
						CheckOperatorsStep.this.bubble.triggerFire();
						CheckOperatorsStep.this.stepCanceled();
					}
				}

				@Override
				public void operatorAdded(Operator operator) {
					if (CheckOperatorsStep.this.areOperatorsAvailable()) {
						CheckOperatorsStep.this.bubble.triggerFire();
						CheckOperatorsStep.this.stepCanceled();
					}
				}

				@Override
				public void executionOrderChanged(ExecutionUnit unit) {
					if (CheckOperatorsStep.this.areOperatorsAvailable()) {
						CheckOperatorsStep.this.bubble.triggerFire();
						CheckOperatorsStep.this.stepCanceled();
					}
				}
			};
			RapidMinerGUI.getMainFrame().getProcess().addProcessSetupListener(setupListener);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks whether the given Operators are available
	 * 
	 * @return true if the given Operators are available or false if not
	 */
	private boolean areOperatorsAvailable() {
		if (operatorClass1.equals(operatorClass2)) {
			Operator[] matched = this.findMatchingOperators(operatorClass1);
			// operators are from the same class
			if (((matched.length < 2) && (split1 != split2)) || ((split1 == split2) && (matched.length < 1))) {
				// not enough operators available for this step
				return false;
			} else {
				return true;
			}
		} else {
			Operator[] matched1 = this.findMatchingOperators(operatorClass1);
			Operator[] matched2 = this.findMatchingOperators(operatorClass2);
			if (matched1.length == 0) {
				return false;
			} else if (matched2.length == 0) {
				return false;
			}
			return true;
		}
	}

	/**
	 * this method looks for the given Operatorclass in the hole current process.
	 * 
	 * @param operatorClass
	 *            class of the wanted Operator
	 * @return a Array of matching Operators (also Subclasses of the given Class will match)
	 */
	private Operator[] findMatchingOperators(Class<? extends Operator> operatorClass) {
		ArrayList<Operator> matching = new ArrayList<Operator>();
		ArrayList<Operator> operatorsOnScreen = new ArrayList<Operator>(RapidMinerGUI.getMainFrame().getProcess()
				.getAllOperators());
		for (Operator operator : operatorsOnScreen) {
			if (operatorClass.isAssignableFrom(operator.getClass())) {
				matching.add(operator);
			}
		}
		return matching.toArray(new Operator[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#stepCanceled()
	 */
	@Override
	protected void stepCanceled() {
		RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(setupListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#getPreconditions()
	 */
	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}

}
