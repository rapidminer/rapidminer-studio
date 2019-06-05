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
package com.rapidminer.tools;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.rapidminer.Process;
import com.rapidminer.io.process.ProcessOriginProcessXMLFilter;
import com.rapidminer.io.process.ProcessOriginProcessXMLFilter.ProcessOriginState;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.UndefinedParameterSetupError;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.InputMissingMetaDataError;
import com.rapidminer.operator.preprocessing.filter.attributes.SubsetAttributeFilter;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
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
		return process.getAllOperators().stream()
				// if operator or one of its parents is disabled, we don't care
				.filter(operator -> !isSuperOperatorDisabled(operator))
				.map(ProcessTools::getMissingPortConnection).filter(Objects::nonNull)
				.findFirst().orElse(null);
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
		return operator.getErrorList().stream()
				// the error list of an OperatorChain contains all errors of its children
				// we only want errors for the current operator however, so skip otherwise
				.filter(e -> e.getOwner().getOperator() == operator)
				// look for matching errors. We can only identify this via metadata errors
				.filter(e -> e instanceof InputMissingMetaDataError)
				// as we don't know what will be sent at runtime, we only look for unconnected
				.filter(e -> !((InputMissingMetaDataError) e).getPort().isConnected())
				.findFirst().map(e -> new Pair<>(((InputMissingMetaDataError) e).getPort(), e)).orElse(null);
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
		return getOperatorWithoutMandatoryParameter(process.getAllOperators());
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
			return getOperatorWithoutMandatoryParameter(((OperatorChain) operator).getAllInnerOperators());
		}

		// no operator with missing mandatory parameter found
		return null;
	}

	/**
	 * Checks whether one of the given operators has a mandatory parameter which
	 * has no value and no default value. Both the operator and the parameter are then returned. If
	 * no such operator can be found, returns {@code null}.
	 *
	 * @param operators
	 *            the operators in question
	 * @return the first {@link Operator} found if one of the given operators has a
	 *         mandatory parameter which is neither set nor has a default value; {@code null} otherwise
	 * @since 9.3
	 */
	private static Pair<Operator, ParameterType> getOperatorWithoutMandatoryParameter(Collection<Operator> operators) {
		return operators.stream()
				// if operator or one of its parents is disabled, we don't care
				.filter(operator -> !isSuperOperatorDisabled(operator))
				.map(op -> {
					// check all parameter related setup errors
					ParameterType param = getMissingMandatoryParameter(op);
					return param == null ? null : new Pair<>(op, param);
				}).filter(Objects::nonNull)
				.findFirst().orElse(null);
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
	 * Calculates a new name based on the already known names. Will append a number in parenthesis if it is a duplicate.
	 *
	 * @param knownNames
	 * 		the collection of already known/used names; must not be {@code null}
	 * @param name
	 * 		the name to check; must not be {@code null}
	 * @return the new name; possibly the same as the input; never {@code null}
	 * @since 9.3
	 */
	public static String getNewName(Collection<String> knownNames, String name) {
		if (!knownNames.contains(name)) {
			return name;
		}
		String baseName = name;
		int index = baseName.lastIndexOf(" (");
		int i = 2;
		if (index >= 0 && baseName.endsWith(")")) {
			String suffix = baseName.substring(index + 2, baseName.length() - 1);
			try {
				i = Integer.parseInt(suffix) + 1;
				baseName = baseName.substring(0, index);
				if (!knownNames.contains(baseName)) {
					return baseName;
				}
			} catch (NumberFormatException e) {
				// not a number; ignore, go with 2
			}
		}
		String newName;
		do {
			newName = baseName + " (" + i++ + ')';
		} while (knownNames.contains(newName));
		return newName;
	}

	/**
	 * Calculates new names based on the already known names and returns a map of the renaming for all names that actually changed.
	 *
	 * @param knownNames
	 * 		the collection of already known/used names; must not be {@code null}
	 * @param names
	 * 		the names to check; must not be {@code null} or contain {@code null}
	 * @return the new names, mapped from old to new; might be empty; never {@code null}
	 * @since 9.3
	 * @see #getNewName(Collection, String)
	 */
	public static Map<String, String> getNewNames(Collection<String> knownNames, Collection<String> names) {
		// prevent side effects
		knownNames = new HashSet<>(knownNames);
		Map<String, String> nameMap = new LinkedHashMap<>();
		for (String name : names) {
			String newName = getNewName(knownNames, name);
			if (!name.equals(newName)) {
				nameMap.put(name, newName);
			}
			knownNames.add(newName);
		}
		return nameMap;
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
		List<ProcessSetupError> errorList = operator.getErrorList();
		if (errorList.isEmpty()) {
			// make sure that setup errors are calculated
			operator.checkProperties();
			errorList = operator.getErrorList();
		}
		return errorList.stream()
				// the error list of an OperatorChain contains all errors of its children
				// we only want errors for the current operator however, so skip otherwise
				.filter(pse -> pse.getOwner().getOperator() == operator)
				// look for matching errors. We can identify this via undefined parameter errors
				.filter(pse -> pse instanceof UndefinedParameterSetupError)
				.map(pse -> ((UndefinedParameterSetupError) pse).getKey())
				.map(operator::getParameterType).findFirst().orElse(null);
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
