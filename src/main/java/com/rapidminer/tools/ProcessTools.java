/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.tools;

import java.util.List;

import com.rapidminer.Process;
import com.rapidminer.io.process.ProcessOriginProcessXMLFilter;
import com.rapidminer.io.process.ProcessOriginProcessXMLFilter.ProcessOriginState;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.InputMissingMetaDataError;
import com.rapidminer.operator.preprocessing.filter.attributes.SubsetAttributeFilter;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.resource.ResourceRepository;
import com.rapidminer.tools.container.Pair;


/**
 * This class contains utility methods related to {@link com.rapidminer.Process}es.
 *
 * @author Marco Boeck
 * @since 6.5.0
 *
 */
public final class ProcessTools {

	/**
	 * Private constructor which throws if called.
	 */
	private ProcessTools() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Checks whether the given process has at least one connected result port, i.e. if the process
	 * would generate results in the result perspective.
	 *
	 * @param process
	 *            the process in question
	 * @return {@code true} if the process has at least one connected result port; {@code false} if
	 *         it does not
	 */
	public static boolean isProcessConnectedToResultPort(final Process process) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}

		return process.getRootOperator().getSubprocess(0).getInnerSinks().getNumberOfConnectedPorts() > 0;
	}

	/**
	 * Extracts the last executed operator of the {@link ProcessRootOperator} of the provided
	 * process.
	 *
	 * @param process
	 *            the process to extract the operator from
	 * @return the last executed child operator of the {@link ProcessRootOperator} or {@code null}
	 *         if process contains no operators
	 */
	public static Operator getLastExecutedRootChild(final Process process) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}

		List<Operator> enabledOps = process.getRootOperator().getSubprocess(0).getEnabledOperators();
		return enabledOps.isEmpty() ? null : enabledOps.get(enabledOps.size() - 1);
	}

	/**
	 * Checks whether the given process contains at least one operator with a mandatory input port
	 * which is not connected. The port is then returned. If no such port can be found, returns
	 * {@code null}.
	 * <p>
	 * This method explicitly only checks for unconnected ports because metadata alone could lead to
	 * a false positive. That would prevent process execution, so a false positive has to be
	 * prevented under any circumstances.
	 * </p>
	 *
	 * @param process
	 *            the process in question
	 * @return the first {@link Port} along with it's error that is found if the process contains at least one operator with an
	 *         input port which is not connected; {@code null} otherwise
	 */
	public static Pair<Port, ProcessSetupError> getPortWithoutMandatoryConnection(final Process process) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}

		for (Operator op : process.getAllOperators()) {
			// / if operator or one of its parents is disabled, we don't care
			if (isSuperOperatorDisabled(op)) {
				continue;
			}

			// look for matching errors. We can only identify this via metadata errors
			for (ProcessSetupError error : op.getErrorList()) {
				// the error list of an OperatorChain contains all errors of its children
				// we only want errors for the current operator however, so skip otherwise
				if (!op.equals(error.getOwner().getOperator())) {
					continue;
				}
				if (error instanceof InputMissingMetaDataError) {
					InputMissingMetaDataError err = (InputMissingMetaDataError) error;
					// as we don't know what will be sent at runtime, we only look for unconnected
					if (!err.getPort().isConnected()) {
						return new Pair<>(err.getPort(), err);
					}
				}
			}
		}

		// no port with missing input and no connection found
		return null;
	}

	/**
	 * Checks whether the given operator or one of its suboperators has a mandatory input port which
	 * is not connected. The port is then returned. If no such port can be found, returns
	 * {@code null}.
	 * <p>
	 * This method explicitly only checks for unconnected ports because metadata alone could lead to
	 * a false positive.
	 * </p>
	 *
	 * @param operator
	 *            the operator for which to check for unconnected mandatory ports
	 * @return the first {@link Port} along with it's error that is found if the operator has at least one input port which is not
	 *         connected; {@code null} otherwise
	 */
	public static Pair<Port, ProcessSetupError> getMissingPortConnection(Operator operator) {
		// look for matching errors. We can only identify this via metadata errors
		for (ProcessSetupError error : operator.getErrorList()) {
			if (error instanceof InputMissingMetaDataError) {
				InputMissingMetaDataError err = (InputMissingMetaDataError) error;
				// as we don't know what will be sent at runtime, we only look for unconnected
				if (!err.getPort().isConnected()) {
					return new Pair<>(err.getPort(), err);
				}
			}
		}
		return null;
	}

	/**
	 * Checks whether the given process contains at least one operator with a mandatory parameter
	 * which has no value and no default value. Both the operator and the parameter are then
	 * returned. If no such operator can be found, returns {@code null}.
	 *
	 * @param process
	 *            the process in question
	 * @return the first {@link Operator} found if the process contains at least one operator with a
	 *         mandatory parameter which is neither set nor has a default value; {@code null}
	 *         otherwise
	 */
	public static Pair<Operator, ParameterType> getOperatorWithoutMandatoryParameter(final Process process) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}

		for (Operator op : process.getAllOperators()) {
			// if operator or one of its parents is disabled, we don't care
			if (isSuperOperatorDisabled(op)) {
				continue;
			}

			// check all parameters and see if they have no value and are non optional
			ParameterType param = getMissingMandatoryParameter(op);
			if (param != null) {
				return new Pair<>(op, param);
			}
		}

		// no operator with missing mandatory parameter found
		return null;
	}

	/**
	 * Checks whether the given operator or one of its sub-operators has a mandatory parameter which
	 * has no value and no default value. Both the operator and the parameter are then returned. If
	 * no such operator can be found, returns {@code null}.
	 *
	 * @param operator
	 *            the operator in question
	 * @return the first {@link Operator} found if the operator or one of its sub-operators has a
	 *         mandatory parameter which is neither set nor has a default value; {@code null}
	 *         otherwise
	 */
	public static Pair<Operator, ParameterType> getOperatorWithoutMandatoryParameter(final Operator operator) {
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null!");
		}

		// check the operator first
		ParameterType param = getMissingMandatoryParameter(operator);
		if (param != null) {
			return new Pair<>(operator, param);
		}

		// if it has children check them
		if (operator instanceof OperatorChain) {
			for (Operator op : ((OperatorChain) operator).getAllInnerOperators()) {
				// if operator or one of its parents is disabled, we don't care
				if (isSuperOperatorDisabled(op)) {
					continue;
				}

				// check all parameters and see if they have no value and are non optional
				param = getMissingMandatoryParameter(op);
				if (param != null) {
					return new Pair<>(op, param);
				}
			}
		}

		// no operator with missing mandatory parameter found
		return null;
	}

	/**
	 * Makes the "subset" parameter of the attribute selector the primary parameter. If the given list does not contain that parameter type, nothing is done.
	 *
	 * @param parameterTypes
	 * 		the list of parameter types which contain the {@link AttributeSubsetSelector#getParameterTypes()}. If {@code null} or empty, the input is returned
	 * @param primary
	 * 		if {@code true} the subset parameter will become a primary parameter type, otherwise it will become a non-primary parameter type
	 * @return the original input
	 * @since 8.2.0
	 */
	public static List<ParameterType> setSubsetSelectorPrimaryParameter(final List<ParameterType> parameterTypes, final boolean primary) {
		if (parameterTypes == null || parameterTypes.isEmpty()) {
			return parameterTypes;
		}

		// look for attribute "subset" parameter, and make it primary if found
		for (ParameterType type : parameterTypes) {
			if (SubsetAttributeFilter.PARAMETER_ATTRIBUTES.equals(type.getKey())) {
				type.setPrimary(primary);
				break;
			}
		}

		return parameterTypes;
	}

	/**
	 * Tags the given process with an {@link ProcessOriginState origin} if possible. If the {@link Process} is not stored
	 * in a {@link Repository}, this does nothing. If it is stored in a {@link ResourceRepository}, it will be tagged
	 * with {@link ProcessOriginState#GENERATED_SAMPLE}. Otherwise a lookup of
	 * {@link RepositoryManager#getSpecialRepositoryOrigin(Repository)} is performed.
	 *
	 * @param process
	 * 		the process to be tagged with an origin
	 * @since 9.0.0
	 */
	public static void setProcessOrigin(Process process) {
		RepositoryLocation repositoryLocation = process.getRepositoryLocation();
		if (repositoryLocation == null) {
			return;
		}
		Repository repository = null;
		try {
			repository = repositoryLocation.getRepository();
		} catch (RepositoryException e) {
			// nothing to do here
			return;
		}
		if (repository == null) {
			return;
		}
		ProcessOriginState origin;
		// resource based repos cannot be created in user interface; tag as sample
		if (repository instanceof ResourceRepository) {
			origin = ProcessOriginState.GENERATED_SAMPLE;
		} else {
			origin = RepositoryManager.getInstance(null).getSpecialRepositoryOrigin(repository);
		}
		ProcessOriginProcessXMLFilter.setProcessOriginState(process, origin);
	}

	/**
	 * Checks whether the given operator has a mandatory parameter which has no value and no default
	 * value and returns the parameter. If no such parameter can be found, returns {@code null}.
	 *
	 * @param operator
	 *            the operator in question
	 * @return the first mandatory parameter which is neither set nor has a default value;
	 *         {@code null} otherwise
	 */
	private static ParameterType getMissingMandatoryParameter(Operator operator) {
		for (String key : operator.getParameters().getKeys()) {
			ParameterType param = operator.getParameterType(key);
			if (!param.isOptional() && (operator.getParameters().getParameterOrNull(key) == null
					|| param instanceof ParameterTypeAttribute && "".equals(operator.getParameters().getParameterOrNull(key)))) {
				return param;
			}
		}
		return null;
	}

	/**
	 * Recursively checks if the operator or one of its (grant) parents is disabled.
	 *
	 * @param operator
	 *            the operator to check
	 * @return {@code true} if the operator or one of the operators it is contained in is disabled
	 */
	private static boolean isSuperOperatorDisabled(Operator operator) {
		return !operator.isEnabled() || operator.getParent() != null && isSuperOperatorDisabled(operator.getParent());
	}
}
