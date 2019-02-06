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
package com.rapidminer.studio.internal;

import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.core.concurrency.ExecutionStoppedException;
import com.rapidminer.operator.ProcessStoppedException;


/**
 * Unchecked variant of a {@link ProcessStoppedException}. May be thrown by
 * {@link ConcurrencyContext#checkStatus()} or other places, that only allow to throw unchecked
 * exceptions, if the corresponding process should stop.
 *
 * @author Michael Knopf
 * @since 6.2.0
 */
public class ProcessStoppedRuntimeException extends ExecutionStoppedException {

	private static final long serialVersionUID = 1L;

}
