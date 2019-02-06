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
package com.rapidminer.gui.flow.processrendering.view;

/**
 * These are the different render phases which are used to determine drawing order of the process as
 * well as the event handling order.
 * <p>
 * Phases in order:
 * <ol>
 * <li>{@link #BACKGROUND}: first to be drawn, last to receive events</li>
 * <li>{@link #ANNOTATIONS}: drawn after {@link #BACKGROUND}, receives events before
 * {@link #BACKGROUND}</li>
 * <li>{@link #OPERATOR_BACKGROUND}: drawn after {@link #ANNOTATIONS}, receives events before
 * {@link #ANNOTATIONS}</li>
 * <li>{@link #CONNECTIONS}: drawn after {@link #ANNOTATIONS}, receives events before
 * {@link #ANNOTATIONS}</li>
 * <li>{@link #OPERATOR_ANNOTATIONS}: drawn after {@link #CONNECTIONS}, receives events before
 * {@link #CONNECTIONS}</li>
 * <li>{@link #OPERATORS}: drawn after {@link #OPERATOR_ANNOTATIONS}, receives events before
 * {@link #OPERATOR_ANNOTATIONS}</li>
 * <li>{@link #OPERATOR_ADDITIONS}: drawn after {@link #OPERATORS}, receives events before
 * {@link #OPERATORS}</li>
 * <li>{@link #OVERLAY}: drawn after {@link #OPERATOR_ADDITIONS}, receives events before
 * {@link #OPERATOR_ADDITIONS}</li>
 * <li>{@link #FOREGROUND}: last to be drawn, first to receive events</li>
 * </ol>
 * </p>
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public enum RenderPhase {

	/** the first phase during rendering. Last to be notified of events. */
	BACKGROUND,

	/** the second phase during rendering. Eighth to be notified of events. */
	ANNOTATIONS,

	/** the third phase during rendering. Seventh to be notified of events. */
	OPERATOR_ANNOTATIONS,

	/** the fourth phase during rendering. Sixth to be notified of events. */
	OPERATOR_BACKGROUND,

	/** the fifth phase during rendering. Fifth to be notified of events. */
	CONNECTIONS,

	/** the sixth phase during rendering. Fourth to be notified of events. */
	OPERATORS,

	/** the seventh phase during rendering. Third to be notified of events. */
	OPERATOR_ADDITIONS,

	/** the eighth phase during rendering. Second to be notified of events. */
	OVERLAY,

	/** the last phase during rendering. First to be notified of events. */
	FOREGROUND;

	/**
	 * Returns the phases in drawing order (first to last).
	 *
	 * @return the array containing the sorted phases for drawing
	 */
	public static RenderPhase[] drawOrder() {
		return new RenderPhase[] { BACKGROUND, ANNOTATIONS, OPERATOR_ANNOTATIONS, OPERATOR_BACKGROUND, CONNECTIONS,
				OPERATORS, OPERATOR_ADDITIONS, OVERLAY, FOREGROUND };
	}

	/**
	 * Returns the phases in event processing order (first to last).
	 *
	 * @return the array containing the sorted phases for event handling
	 */
	public static RenderPhase[] eventOrder() {
		return new RenderPhase[] { FOREGROUND, OVERLAY, OPERATOR_ADDITIONS, OPERATORS, CONNECTIONS, OPERATOR_BACKGROUND,
				OPERATOR_ANNOTATIONS, ANNOTATIONS, BACKGROUND };
	}
}
