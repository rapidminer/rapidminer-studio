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
package com.rapidminer.gui.flow.processrendering.event;

import java.util.Collection;
import java.util.EventListener;

import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.operator.Operator;


/**
 * An object listening for {@link ProcessRendererModelEvent}s and
 * {@link ProcessRendererOperatorEvent}s on the {@link ProcessRendererModel} as well as
 * {@link ProcessRendererAnnotationEvent}s must implement this interface and register itself as a
 * listener to the model.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public interface ProcessRendererEventListener extends EventListener {

	/**
	 * Called when something in the model has changed which is not directly related to
	 * {@link Operator}s.
	 *
	 * @param e
	 *            the event instance
	 */
	public void modelChanged(final ProcessRendererModelEvent e);

	/**
	 * Called when something in the model has changed which is directly related to {@link Operator}
	 * s.
	 *
	 * @param e
	 *            the event instance
	 * @param operators
	 *            a collection of affected operators
	 */
	public void operatorsChanged(final ProcessRendererOperatorEvent e, final Collection<Operator> operators);

	/**
	 * Called when workflow annotations have changed.
	 *
	 * @param e
	 *            the event instance
	 * @param annotations
	 *            a collection of affected annotations. Can contain {@code null}!
	 */
	public void annotationsChanged(final ProcessRendererAnnotationEvent e, final Collection<WorkflowAnnotation> annotations);

}
