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
package com.rapidminer.gui.flow.processrendering.view.components;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessEventDecorator;
import com.rapidminer.gui.tools.ProcessGUITools;
import com.rapidminer.gui.tools.bubble.BubbleWindow;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.InvalidRepositoryEntryError;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.ProcessNotInRepositoryMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.ProcessTools;
import com.rapidminer.tools.container.Pair;


/**
 * Decorator that checks if the warning icon of an operator was clicked and displays operator
 * warnings in that case. Handles display of warning bubbles and events that show or hide them.
 *
 * @author Gisa Schaefer
 * @since 7.1.1
 */
public class OperatorWarningHandler implements ProcessEventDecorator {

	/** the bubble which is shown when the user clicks on the warning icon on an operator */
	private BubbleWindow operatorWarningBubble;

	/** the backing model */
	private final ProcessRendererModel model;

	/** the size of a warning icon */
	private static final int WARNING_ICON_SIZE = 16;

	/**
	 * the missing input port that caused the last warning bubble or {@code null} if it had another
	 * cause
	 */
	private Port lastMissingInputPort;

	/**
	 * the missing parameter that caused the last warning bubble or {@code null} if it had another
	 * cause
	 */
	private Pair<Operator, ParameterType> lastMissingParamPair;

	/**
	 * the setup error that caused the last warning bubble or {@code null} if it had another cause
	 */
	private ProcessSetupError lastProcessSetupError;

	/**
	 * Creates decorator that checks if the warning icon of an operator was clicked and displays
	 * operator warnings in that case.
	 */
	public OperatorWarningHandler(ProcessRendererModel model) {
		this.model = model;
	}

	@Override
	public void processMouseEvent(ExecutionUnit process, MouseEventType type, MouseEvent e) {
		if (type == MouseEventType.MOUSE_CLICKED) {
			// get the operator that was clicked on
			Operator operator = model.getHoveringOperator();

			// check if there is a warning icon for this operator
			if (operator != null && !operator.getErrorList().isEmpty() && !operator.isRunning()) {

				// calculate the bounding box of the warning icon as it is drawn by {@link
				Rectangle2D frame = model.getOperatorRect(operator);

				int iconX = (int) (frame.getX() + 3 + WARNING_ICON_SIZE);
				int iconY = (int) (frame.getY() + frame.getHeight() - WARNING_ICON_SIZE - 2);
				int size = WARNING_ICON_SIZE;

				Rectangle2D boundingBox = new Rectangle2D.Float(iconX, iconY, size, size);

				// check if the user clicked into the bounding box of the warning icon
				if (model.getMousePositionRelativeToProcess() != null
						&& boundingBox.contains(model.getMousePositionRelativeToProcess())) {
					showOperatorWarning(operator);
				}
			}

		}
	}

	@Override
	public void processKeyEvent(ExecutionUnit process, KeyEventType type, KeyEvent e) {
		// not needed
	}

	/**
	 * Kills the operator warning bubble.
	 */
	public void killWarningBubble() {
		if (operatorWarningBubble != null) {
			operatorWarningBubble.killBubble(true);
		}
	}

	/**
	 * Shows the first setup warning for the operator via an error bubble. Checks missing mandatory
	 * parameters and missing port connections for the operator and its sub-operators first, then
	 * displays the first {@link ProcessSetupError} from the operator error list. Hides the error
	 * bubble instead of showing the same bubble again.
	 *
	 * @param operator
	 *            the operator for which to show the warnings
	 */
	public void showOperatorWarning(Operator operator) {
		Pair<Operator, ParameterType> missingParamPair = ProcessTools.getOperatorWithoutMandatoryParameter(operator);
		if (missingParamPair != null) {
			if (shouldRefreshExistingBubble(missingParamPair.equals(lastMissingParamPair))) {
				return;
			}

			operatorWarningBubble = ProcessGUITools.displayPrecheckMissingMandatoryParameterWarning(
					missingParamPair.getFirst(), missingParamPair.getSecond(), false);

			// set last values
			lastMissingParamPair = missingParamPair;
			lastMissingInputPort = null;
			lastProcessSetupError = null;
			return;
		}
		lastMissingParamPair = null;

		Pair<Port, ProcessSetupError> missingInputPort = ProcessTools.getMissingPortConnection(operator);
		if (missingInputPort != null) {
			if (shouldRefreshExistingBubble(missingInputPort.equals(lastMissingInputPort))) {
				return;
			}
			operatorWarningBubble = ProcessGUITools.displayPrecheckInputPortDisconnectedWarning(missingInputPort, false);

			// set last values
			lastMissingInputPort = missingInputPort.getFirst();
			lastProcessSetupError = null;
			return;
		}
		lastMissingInputPort = null;

		if (!operator.getErrorList().isEmpty()) {

			ProcessSetupError processSetupError = operator.getErrorList().get(0);
			if (processSetupError != null) {
				if (operatorWarningBubble != null) {
					// if the required bubble is already shown kill it for toggle effect
					if (!operatorWarningBubble.isKilled() && processSetupError.equals(lastProcessSetupError)) {
						operatorWarningBubble.killBubble(true);
						return;
					}
					// kill wrong bubble and go on showing new bubble
					operatorWarningBubble.killBubble(true);
				}

				if (processSetupError instanceof InvalidRepositoryEntryError) {
					operatorWarningBubble = ProcessGUITools.displayPrecheckBrokenMandatoryParameterWarning(operator, ((InvalidRepositoryEntryError) processSetupError).getParameterKey());
				} else if (processSetupError instanceof ProcessNotInRepositoryMetaDataError) {
					operatorWarningBubble = ProcessGUITools.displayPrecheckProcessNotSavedWarning(operator, processSetupError);
				} else {
					operatorWarningBubble = ProcessGUITools.displayProcessSetupError(processSetupError);
				}
				lastProcessSetupError = processSetupError;
				return;
			}
		}
		lastProcessSetupError = null;
	}

	/**
	 * Checks whether a bubble is currently existing and shown. Returns {@code true} iff the current bubble
	 * was not killed and was showing, so that it only needs to be toggled and not created.
	 *
	 * @param isAlreadyShown if the current bubble is sufficient
	 * @return whether the bubble should eb refreshed or created anew
	 * @since 8.2
	 */
	private boolean shouldRefreshExistingBubble(boolean isAlreadyShown) {
		if (operatorWarningBubble == null) {
			return false;
		}
		boolean isAlreadyKilled = operatorWarningBubble.isKilled();
		// if the required bubble is already shown kill it for toggle effect
		// else kill wrong bubble and go on showing new bubble
		operatorWarningBubble.killBubble(true);
		return !isAlreadyKilled && isAlreadyShown;
	}
}
