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
package com.rapidminer.operator.ports.metadata;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.quickfix.QuickFix;


/**
 * Filters the {@link QuickFix quick fixes} of a {@link MetaDataError}
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0
 * @see com.rapidminer.operator.ports.quickfix.BlacklistedOperatorQuickFixFilter BlacklistedOperatorQuickFixFilter
 */
public class MetaDataErrorQuickFixFilter implements MetaDataError {

	private final MetaDataError error;
	private final Predicate<? super QuickFix> keepQuickFix;

	/**
	 * Filters the QuickFixes of a MetaDataError with the given filter function
	 *
	 * @param metaDataError The metaDataError to filter
	 * @param keepQuickFix The {@link QuickFix} -> {@code boolean} filter function, should return {@code true} to keep a {@link QuickFix}
	 */
	public MetaDataErrorQuickFixFilter(MetaDataError metaDataError, Predicate<? super QuickFix> keepQuickFix){
		this.error = metaDataError;
		this.keepQuickFix = keepQuickFix;
	}

	@Override
	public Port getPort() {
		return error.getPort();
	}

	@Override
	public String getMessage() {
		return error.getMessage();
	}

	@Override
	public PortOwner getOwner() {
		return error.getOwner();
	}

	@Override
	public List<? extends QuickFix> getQuickFixes() {
		return error.getQuickFixes().stream().filter(keepQuickFix).collect(Collectors.toList());
	}

	@Override
	public Severity getSeverity() {
		return error.getSeverity();
	}
}
