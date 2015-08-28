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

import java.awt.Window;
import java.util.ArrayList;


/**
 * @author kamradt
 * 
 */
public class ConnectOperatorStep extends Step {

	private Class<? extends Operator> operatorClass1;
	private Class<? extends Operator> operatorClass2;
	private Operator myOperator1;
	private Operator myOperator2;
	private int split1;
	private int split2;
	private int inputPort;
	private int outputPort;
	private Object[] arguments;
	private String i18nKey;
	private AlignedSide preferredSide;
	private Window owner = RapidMinerGUI.getMainFrame();
	private ProcessSetupListener setupListener;

	public ConnectOperatorStep(AlignedSide preferredAlignment, Class<? extends Operator> startOperatorClass, int startPort,
			int startSplit, Class<? extends Operator> targetOperatorClass, int targetPort, int targetSplit, String i18nKey,
			Object... arguments) {
		this.preferredSide = preferredAlignment;
		this.operatorClass1 = startOperatorClass;
		this.split1 = startSplit;
		this.outputPort = startPort;
		this.operatorClass2 = targetOperatorClass;
		this.split2 = targetSplit;
		this.inputPort = targetPort;
		this.i18nKey = i18nKey;
		this.arguments = arguments;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#createBubble()
	 */
	@Override
	boolean createBubble() {
		// ensure that the required Operators are available
		if (operatorClass1.equals(operatorClass2)) {
			Operator[] matched = this.findMatchingOperators(operatorClass1);
			// operators are from the same class
			if (split1 < split2) {
				myOperator1 = matched[((split1 < matched.length) ? split1 : matched.length - 1) - 1];
				myOperator2 = matched[((split2 <= matched.length) ? split2 : matched.length) - 1];
			} else if (split1 == split2) {
				// Operator will be connected to itself
				myOperator1 = matched[((split1 <= matched.length) ? split1 : matched.length) - 1];
				myOperator2 = myOperator1;
			} else if (split1 > split2) {
				myOperator1 = matched[((split1 <= matched.length) ? split1 : matched.length) - 1];
				myOperator2 = matched[((split2 < matched.length) ? split2 : matched.length - 1) - 1];
			}
		} else {
			Operator[] matched1 = this.findMatchingOperators(operatorClass1);
			Operator[] matched2 = this.findMatchingOperators(operatorClass2);
			myOperator1 = matched1[((split1 <= matched1.length) ? split1 : matched1.length) - 1];
			myOperator2 = matched2[((split2 <= matched2.length) ? split2 : matched2.length) - 1];
		}
		if (this.isConnected()) {
			return false;
		}
		// Operator can be connected
		bubble = new OperatorBubble(owner, preferredSide, i18nKey, operatorClass1, split1, arguments);
		setupListener = new ProcessSetupListener() {

			@Override
			public void operatorRemoved(Operator operator, int oldIndex, int oldIndexAmongEnabled) {
				// do not care
			}

			@Override
			public void operatorChanged(Operator operator) {
				if (operator.equals(myOperator1) || operator.equals(myOperator2)) {
					if (ConnectOperatorStep.this.isConnected()) {
						bubble.triggerFire();
						RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
					}
				}
			}

			@Override
			public void operatorAdded(Operator operator) {
				// do not care

			}

			@Override
			public void executionOrderChanged(ExecutionUnit unit) {
				// do not care

			}
		};
		RapidMinerGUI.getMainFrame().getProcess().addProcessSetupListener(setupListener);
		return true;
	}

	private Operator[] findMatchingOperators(Class<? extends Operator> operatorClass) {
		ArrayList<Operator> matching = new ArrayList<Operator>();
		// List<Operator> operatorsOnScreen =
		// RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().getDisplayedChain().getAllInnerOperators();
		ArrayList<Operator> operatorsOnScreen = new ArrayList<Operator>(RapidMinerGUI.getMainFrame().getProcess()
				.getAllOperators());
		for (Operator operator : operatorsOnScreen) {
			if (operatorClass.isAssignableFrom(operator.getClass())) {
				matching.add(operator);
			}
		}
		return matching.toArray(new Operator[0]);
	}

	private boolean isConnected() {
		if (myOperator1 == null || myOperator2 == null) {
			return false;
			// throw new
			// NullPointerException("Operators of the ConnectOperatorStep can not be null");
		}
		// check whether the needed port exists
		if (myOperator1.getOutputPorts().getAllPorts().size() < outputPort
				|| myOperator2.getInputPorts().getAllPorts().size() < inputPort) {
			return false;
		}
		// check whether the ports are connected
		if (!myOperator1.getOutputPorts().getPortByIndex(outputPort - 1).isConnected()
				|| !myOperator2.getInputPorts().getPortByIndex(inputPort - 1).isConnected()) {
			return false;
		}
		// check whether the ports are connected with each other
		if (myOperator1.getOutputPorts().getPortByIndex(outputPort - 1).getDestination()
				.equals(myOperator2.getInputPorts().getPortByIndex(inputPort - 1))) {
			return true;
		} else {
			return false;
		}
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
		return new Step[] { new PerspectivesStep(1), new NotShowingStep(ProcessPanel.PROCESS_PANEL_DOCK_KEY),
				new CheckOperatorsStep(operatorClass1, split1, operatorClass2, split2) };
	}

}
