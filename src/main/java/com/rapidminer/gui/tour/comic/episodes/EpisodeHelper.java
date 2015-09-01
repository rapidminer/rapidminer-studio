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
package com.rapidminer.gui.tour.comic.episodes;

import java.util.Collection;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.DockingDesktop;


/**
 * Helper methods which can be used by {@link AbstractEpisode} implementations.
 *
 * @author Marcin Skirzynski, Marco Boeck
 *
 */
public class EpisodeHelper {

	/**
	 * Returns the first operator in the collection of operators which has the specified class or
	 * <code>null</code> if none could be found.
	 *
	 * @param operators
	 * @param operatorClass
	 */
	public static <T extends Operator> T getOperator(final Collection<Operator> operators, final Class<T> operatorClass) {
		if (operators == null) {
			throw new IllegalArgumentException("operators must not be null!");
		}
		if (operatorClass == null) {
			throw new IllegalArgumentException("operatorClass must not be null!");
		}

		for (Operator operator : operators) {
			// no supertypes, no subtypes permitted
			if (operator.getClass().isAssignableFrom(operatorClass) && operatorClass.isAssignableFrom(operator.getClass())) {
				return operatorClass.cast(operator);
			}
		}
		return null;
	}

	/**
	 * Returns <code>true</code> if the collection of operators contains an operator which is a
	 * either the specified class or any subclass of the given operator class.
	 *
	 * @param operators
	 * @param operatorClass
	 */
	public static boolean containsOperator(final Collection<Operator> operators,
			final Class<? extends Operator> operatorClass) {
		return getOperator(operators, operatorClass) != null;
	}

	/**
	 * Returns <code>true</code> if the given operator is selected in the current process.
	 *
	 * @param operator
	 * @return
	 */
	public static boolean isOperatorSelected(final Operator operator) {
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null!");
		}

		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		if (mainFrame != null) {
			for (Operator selectedOperator : mainFrame.getProcessPanel().getProcessRenderer().getModel()
					.getSelectedOperators()) {
				if (selectedOperator.equals(operator)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the number of split process panels, e.g. <code>2</code> for an open x-validation.
	 * Returns -1 if something goes wrong.
	 *
	 * @return
	 */
	public static int getNumberOfDisplayedSplitPanels() {
		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		if (mainFrame != null) {
			return mainFrame.getProcessPanel().getProcessRenderer().getModel().getProcesses().size();
		}
		return -1;
	}

	/**
	 * Returns the first retrieve operator in the collection of operators which loads the data from
	 * the specified repository location.
	 *
	 * @param operators
	 * @param repositoryLocation
	 */
	public static RepositorySource getRetrieveOperator(final Collection<Operator> operators, final String repositoryLocation) {
		if (operators == null) {
			throw new IllegalArgumentException("operators must not be null!");
		}
		if (repositoryLocation == null || "".equals(repositoryLocation.trim())) {
			throw new IllegalArgumentException("repositoryLocation must not be null or empty!");
		}

		for (Operator operator : operators) {
			try {
				if (operator instanceof RepositorySource
						&& ((RepositorySource) operator)
						.getParameterAsRepositoryLocation(RepositorySource.PARAMETER_REPOSITORY_ENTRY)
						.getAbsoluteLocation().equals(repositoryLocation)) {
					return (RepositorySource) operator;
				}
			} catch (UserError e) {
			}
		}
		return null;
	}

	/**
	 * Returns <code>true</code> if the given to ports are connected.
	 *
	 * @param fromPort
	 * @param toPort
	 */
	public static boolean areConnected(final OutputPort fromPort, final InputPort toPort) {
		if (fromPort == null) {
			throw new IllegalArgumentException("fromPort must not be null!");
		}
		if (toPort == null) {
			throw new IllegalArgumentException("toPort must not be null!");
		}

		if (fromPort.getDestination() == null) {
			return false;
		}

		return fromPort.getDestination().equals(toPort);
	}

	/**
	 * Returns <code>true</code> if the two operator are connected with the specified ports.
	 *
	 * @param from
	 * @param fromPortIndex
	 * @param to
	 * @param toPortIndex
	 */
	public static boolean areConnected(final Operator from, final int fromPortIndex, final Operator to, final int toPortIndex) {
		if (from == null) {
			throw new IllegalArgumentException("from must not be null!");
		}
		if (to == null) {
			throw new IllegalArgumentException("to must not be null!");
		}

		// prevent IndexOutOfBounds
		if (from.getOutputPorts().getNumberOfPorts() <= fromPortIndex) {
			return false;
		}
		if (to.getInputPorts().getNumberOfPorts() <= toPortIndex) {
			return false;
		}
		OutputPort fromPort = from.getOutputPorts().getPortByIndex(fromPortIndex);
		InputPort toPort = to.getInputPorts().getPortByIndex(toPortIndex);

		return areConnected(fromPort, toPort);
	}

	/**
	 * Returns <code>true</code> if the two operator are connected with the specified ports.
	 *
	 * @param from
	 * @param fromPortName
	 * @param to
	 * @param toPortName
	 */
	public static boolean areConnected(final Operator from, final String fromPortName, final Operator to,
			final String toPortName) {
		if (from == null) {
			throw new IllegalArgumentException("from must not be null!");
		}
		if (fromPortName == null || "".equals(fromPortName.trim())) {
			throw new IllegalArgumentException("fromPortName must not be null or empty!");
		}
		if (to == null) {
			throw new IllegalArgumentException("to must not be null!");
		}
		if (toPortName == null || "".equals(toPortName.trim())) {
			throw new IllegalArgumentException("toPortName must not be null or empty!");
		}

		OutputPort fromPort = from.getOutputPorts().getPortByName(fromPortName);
		InputPort toPort = to.getInputPorts().getPortByName(toPortName);

		return areConnected(fromPort, toPort);
	}

	/**
	 * Returns <code>true</code> if the given operator is connected with the specified port to the
	 * specifed sink.
	 *
	 * @param operator
	 * @param outputPortIndex
	 * @param sinkPortIndex
	 */
	public static boolean isConnected(final Operator operator, final int outputPortIndex, final int sinkPortIndex) {
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null!");
		}

		// prevent IndexOutOfBounds
		if (operator.getOutputPorts().getNumberOfPorts() <= outputPortIndex) {
			return false;
		}
		if (operator.getExecutionUnit().getInnerSinks().getNumberOfPorts() <= sinkPortIndex) {
			return false;
		}
		OutputPort fromPort = operator.getOutputPorts().getPortByIndex(outputPortIndex);
		InputPort toPort = operator.getExecutionUnit().getInnerSinks().getPortByIndex(sinkPortIndex);

		return areConnected(fromPort, toPort);
	}

	/**
	 * Returns <code>true</code> if the given operator is connected with the specified port from the
	 * specifed source port.
	 *
	 * @param operator
	 * @param inputPortIndex
	 * @param sourcePortIndex
	 */
	public static boolean isConnectedFrom(final Operator operator, final int inputPortIndex, final int sourcePortIndex) {
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null!");
		}

		// prevent IndexOutOfBounds
		if (operator.getInputPorts().getNumberOfPorts() <= inputPortIndex) {
			return false;
		}
		if (operator.getExecutionUnit().getInnerSources().getNumberOfPorts() <= sourcePortIndex) {
			return false;
		}
		InputPort toPort = operator.getInputPorts().getPortByIndex(inputPortIndex);
		OutputPort fromPort = operator.getExecutionUnit().getInnerSources().getPortByIndex(sourcePortIndex);

		return areConnected(fromPort, toPort);
	}

	/**
	 * Returns <code>true</code> if the given operator is connected with the specified port to the
	 * specifed sink.
	 *
	 * @param operator
	 * @param outputPortName
	 * @param sinkPortName
	 */
	public static boolean isConnected(final Operator operator, final String outputPortName, final String sinkPortName) {
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null!");
		}
		if (outputPortName == null || "".equals(outputPortName.trim())) {
			throw new IllegalArgumentException("outputPortName must not be null or empty!");
		}
		if (sinkPortName == null || "".equals(sinkPortName.trim())) {
			throw new IllegalArgumentException("sinkPortName must not be null or empty!");
		}

		OutputPort fromPort = operator.getOutputPorts().getPortByName(outputPortName);
		InputPort toPort = operator.getExecutionUnit().getInnerSinks().getPortByName(sinkPortName);

		return areConnected(fromPort, toPort);
	}

	/**
	 * Returns <code>true</code> if the collection of operators contains an retrieve operator with
	 * the given repository location.
	 *
	 * @param operators
	 * @param repositoryLocation
	 */
	public static boolean containsRetrieveOperator(final Collection<Operator> operators, final String repositoryLocation) {
		return getRetrieveOperator(operators, repositoryLocation) != null;
	}

	/**
	 * Returns <code>true</code> if the view with the given key is closed.
	 *
	 * @param viewKey
	 * @throws IllegalStateException
	 *             if no MainFrame was registred
	 */
	public static boolean isViewClosed(final String viewKey) {
		if (viewKey == null || "".equals(viewKey.trim())) {
			throw new IllegalArgumentException("viewKey must not be null or empty!");
		}

		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		if (mainFrame == null) {
			throw new IllegalStateException("No MainFrame registered in RapidMinerGUI");
		}
		return isViewClosed(viewKey, mainFrame);
	}

	/**
	 * Returns <code>true</code> if the view with the given key is closed.
	 *
	 * @param viewKey
	 * @param mainFrame
	 * @return
	 */
	public static boolean isViewClosed(final String viewKey, final MainFrame mainFrame) {
		if (viewKey == null || "".equals(viewKey.trim())) {
			throw new IllegalArgumentException("viewKey must not be null or empty!");
		}
		if (mainFrame == null) {
			throw new IllegalArgumentException("mainFrame must not be null or empty!");
		}

		DockableState state = getDockableState(viewKey, mainFrame);
		return state == null || state.isClosed() || state.isHidden();
	}

	/**
	 * Returns the {@link DockableState} object for the view with the given key.
	 *
	 * @param viewKey
	 * @param mainFrame
	 * @throws IllegalArgumentException
	 *             if no view with the key exists.
	 */
	public static DockableState getDockableState(final String viewKey, final MainFrame mainFrame) {
		if (viewKey == null || "".equals(viewKey.trim())) {
			throw new IllegalArgumentException("viewKey must not be null or empty!");
		}
		if (mainFrame == null) {
			throw new IllegalArgumentException("mainFrame must not be null or empty!");
		}

		DockingDesktop dockingDesktop = mainFrame.getDockingDesktop();
		Dockable dockable = dockingDesktop.getContext().getDockableByKey(viewKey);
		if (dockable == null) {
			throw new IllegalArgumentException("There is no dockable with the key " + viewKey);
		}

		return dockingDesktop.getDockableState(dockable);
	}

	/**
	 * Returns the {@link DockableState} object for the view with the given key.
	 *
	 * Will return <code>null</code>, if the {@link MainFrame} is not initialized.
	 *
	 * @param viewKey
	 * @throws IllegalStateException
	 *             if no MainFrame was registred
	 * @throws IllegalArgumentException
	 *             if no view with the key exists.
	 */
	public static DockableState getDockableState(final String viewKey) {
		if (viewKey == null || "".equals(viewKey.trim())) {
			throw new IllegalArgumentException("viewKey must not be null or empty!");
		}

		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		if (mainFrame == null) {
			throw new IllegalStateException("No MainFrame registerer in RapidMinerGUI");
		}
		return getDockableState(viewKey, mainFrame);
	}
}
