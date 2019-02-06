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
package com.rapidminer.core.license;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.license.violation.LicenseViolation;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.I18N;


/**
 * General exception that is thrown if an operator cannot be executed because of
 * {@link LicenseViolation}s.
 *
 * @author Nils Woehler
 */
public class LicenseViolationException extends OperatorException {

	private static final long serialVersionUID = 1L;

	private final Operator op;

	private final List<LicenseViolation> violations;

	/**
	 * @param op
	 *            the causing operator
	 * @param violation
	 *            the corresponding {@link LicenseViolation}
	 */
	public LicenseViolationException(Operator op, LicenseViolation violation) {
		this(op, Arrays.asList(violation));
	}

	/**
	 * @param op
	 *            the causing operator
	 * @param cause
	 *            Corresponding list of {@link LicenseViolation}s.
	 */
	public LicenseViolationException(Operator op, List<LicenseViolation> violations) {
		super(null);
		this.op = op;
		this.violations = violations;
	}

	/**
	 * @return the operator name or <code>null</code> if no operator was provided
	 */
	public String getOperatorName() {
		return op == null ? null : op.getName();
	}

	/**
	 * @return the list of {@link LicenseViolation}s.
	 */
	public List<LicenseViolation> getLicenseViolations() {
		return new LinkedList<>(violations);
	}

	@Override
	public String toString() {
		return I18N.getErrorMessage("process.error.operator_license_violation", getOperatorName());
	}
}
