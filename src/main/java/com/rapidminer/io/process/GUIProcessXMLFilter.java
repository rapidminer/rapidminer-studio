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
package com.rapidminer.io.process;

import java.awt.geom.Rectangle2D;

import org.w3c.dom.Element;

import com.rapidminer.gui.flow.processrendering.annotations.model.OperatorAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.ProcessAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotations;
import com.rapidminer.gui.flow.processrendering.background.ProcessBackgroundImage;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.Port;


/**
 * {@link ProcessXMLFilter} to handle position data for operators and ports as well as operator
 * annotations and background images. The filter provides a set of static utility methods to work
 * with the loaded user data.
 *
 * It combines the logic of {@link AnnotationProcessXMLFilter},
 * {@link BackgroundImageProcessXMLFilter}, and {@link ProcessLayoutXMLFilter}.
 *
 * @author Michael Knopf, Marco Boeck, Nils Woehler
 */
public class GUIProcessXMLFilter implements ProcessXMLFilter {

	/** @deprecated use {@link AnnotationProcessXMLFilter#KEY_OPERATOR_ANNOTATION} instead */
	@Deprecated
	public static final String KEY_OPERATOR_ANNOTATION = AnnotationProcessXMLFilter.KEY_OPERATOR_ANNOTATION;

	/** @deprecated use {@link AnnotationProcessXMLFilter#KEY_PROCESS_ANNOTATION} instead */
	@Deprecated
	public static final String KEY_PROCESS_ANNOTATION = AnnotationProcessXMLFilter.KEY_PROCESS_ANNOTATION;

	/**
	 * @deprecated use {@link BackgroundImageProcessXMLFilter#KEY_PROCESS_BACKGROUND_IMAGE} instead
	 */
	@Deprecated
	public static final String KEY_PROCESS_BACKGROUND_IMAGE = BackgroundImageProcessXMLFilter.KEY_PROCESS_BACKGROUND_IMAGE;

	private final AnnotationProcessXMLFilter annotationProcessXMLFilter;
	private final ProcessLayoutXMLFilter layoutProcessXMLFilter;
	private final BackgroundImageProcessXMLFilter backgroundImageProcessXMLFilter;

	public GUIProcessXMLFilter() {
		layoutProcessXMLFilter = new ProcessLayoutXMLFilter();
		annotationProcessXMLFilter = new AnnotationProcessXMLFilter();
		backgroundImageProcessXMLFilter = new BackgroundImageProcessXMLFilter();
	}

	@Override
	public void operatorExported(final Operator op, final Element opElement) {
		layoutProcessXMLFilter.operatorExported(op, opElement);
		annotationProcessXMLFilter.operatorExported(op, opElement);
		backgroundImageProcessXMLFilter.operatorExported(op, opElement);
	}

	@Override
	public void executionUnitExported(final ExecutionUnit process, final Element element) {
		layoutProcessXMLFilter.executionUnitExported(process, element);
		annotationProcessXMLFilter.executionUnitExported(process, element);
		backgroundImageProcessXMLFilter.executionUnitExported(process, element);
	}

	@Override
	public void operatorImported(final Operator op, final Element opElement) {
		layoutProcessXMLFilter.operatorImported(op, opElement);
		annotationProcessXMLFilter.operatorImported(op, opElement);
		backgroundImageProcessXMLFilter.operatorImported(op, opElement);
	}

	@Override
	public void executionUnitImported(final ExecutionUnit process, final Element element) {
		layoutProcessXMLFilter.executionUnitImported(process, element);
		annotationProcessXMLFilter.executionUnitImported(process, element);
		backgroundImageProcessXMLFilter.executionUnitImported(process, element);
	}

	/**
	 * Looks up the spacing of the specified {@link Port}.
	 *
	 * @param port
	 *            The port.
	 * @return Additional spacing.
	 * @deprecated use {@link ProcessLayoutXMLFilter#lookupPortSpacing(Port)} instead
	 */
	@Deprecated
	public static int lookupPortSpacing(Port port) {
		return ProcessLayoutXMLFilter.lookupPortSpacing(port);
	}

	/**
	 * Sets the spacing of the specified {@link Port}.
	 *
	 * @param port
	 *            The port.
	 * @param spacing
	 *            The additional spacing.
	 * @deprecated use {@link ProcessLayoutXMLFilter#setPortSpacing(Port, Integer)} instead
	 */
	@Deprecated
	public static void setPortSpacing(Port port, Integer spacing) {
		ProcessLayoutXMLFilter.setPortSpacing(port, spacing);
	}

	/**
	 * Resets the spacing of the specified {@link Port}.
	 *
	 * @param port
	 *            The port.
	 * @deprecated use {@link ProcessLayoutXMLFilter#resetPortSpacing(Port)} instead
	 */
	@Deprecated
	public static void resetPortSpacing(Port port) {
		ProcessLayoutXMLFilter.resetPortSpacing(port);
	}

	/**
	 * Looks up the position rectangle of the specified {@link Operator}.
	 *
	 * @param operator
	 *            The operator.
	 * @return The rectangle or null.
	 * @deprecated use {@link ProcessLayoutXMLFilter#lookupOperatorRectangle(Operator)} instead
	 */
	@Deprecated
	public static Rectangle2D lookupOperatorRectangle(Operator operator) {
		return ProcessLayoutXMLFilter.lookupOperatorRectangle(operator);
	}

	/**
	 * Sets the position rectangle of the specified {@link Operator}.
	 *
	 * @param operator
	 *            The operator.
	 * @param rect
	 *            The rectangle.
	 * @deprecated use {@link ProcessLayoutXMLFilter#setOperatorRectangle(Operator, Rectangle2D)}
	 *             instead
	 */
	@Deprecated
	public static void setOperatorRectangle(Operator operator, Rectangle2D rect) {
		ProcessLayoutXMLFilter.setOperatorRectangle(operator, rect);
	}

	/**
	 * Resets the position rectangle of the specified {@link Operator}.
	 *
	 * @param operator
	 *            The operator.
	 * @deprecated use {@link ProcessLayoutXMLFilter#resetOperatorRectangle(Operator)} instead
	 */
	@Deprecated
	public static void resetOperatorRectangle(Operator operator) {
		ProcessLayoutXMLFilter.resetOperatorRectangle(operator);
	}

	/**
	 * Returns the operator annotations for the given operator.
	 *
	 * @param operator
	 *            the operator in question
	 * @return the annotations or {@code null} if there are none
	 * @deprecated use {@link AnnotationProcessXMLFilter#lookupOperatorAnnotations(Operator)}
	 *             instead
	 */
	@Deprecated
	public static WorkflowAnnotations lookupOperatorAnnotations(Operator operator) {
		return AnnotationProcessXMLFilter.lookupOperatorAnnotations(operator);
	}

	/**
	 * Adds a {@link OperatorAnnotation} to the {@link Operator}.
	 *
	 * @param annotation
	 *            the new annotation
	 * @deprecated use {@link AnnotationProcessXMLFilter#addOperatorAnnotation(OperatorAnnotation)}
	 *             instead
	 */
	@Deprecated
	public static void addOperatorAnnotation(OperatorAnnotation annotation) {
		AnnotationProcessXMLFilter.addOperatorAnnotation(annotation);
	}

	/**
	 * Removes the given {@link OperatorAnnotation}.
	 *
	 * @param annotation
	 *            the annotation to remove
	 * @deprecated use
	 *             {@link AnnotationProcessXMLFilter#removeOperatorAnnotation(OperatorAnnotation)}
	 *             instead
	 */
	@Deprecated
	public static void removeOperatorAnnotation(OperatorAnnotation annotation) {
		AnnotationProcessXMLFilter.removeOperatorAnnotation(annotation);
	}

	/**
	 * Returns the process annotations for the given execution unit.
	 *
	 * @param process
	 *            the execution unit in question
	 * @return the annotations or {@code null} if there are none
	 * @deprecated use {@link AnnotationProcessXMLFilter#lookupProcessAnnotations(ExecutionUnit)}
	 *             instead
	 */
	@Deprecated
	public static WorkflowAnnotations lookupProcessAnnotations(ExecutionUnit process) {
		return AnnotationProcessXMLFilter.lookupProcessAnnotations(process);
	}

	/**
	 * Adds a {@link ProcessAnnotation}.
	 *
	 * @param annotation
	 *            the new annotation
	 * @deprecated use {@link AnnotationProcessXMLFilter#addProcessAnnotation(ProcessAnnotation)}
	 *             instead
	 */
	@Deprecated
	public static void addProcessAnnotation(ProcessAnnotation annotation) {
		AnnotationProcessXMLFilter.addProcessAnnotation(annotation);
	}

	/**
	 * Removes the given {@link ProcessAnnotation}.
	 *
	 * @param annotation
	 *            the annotation to remove
	 * @deprecated use {@link AnnotationProcessXMLFilter#removeProcessAnnotation(ProcessAnnotation)}
	 *             instead
	 */
	@Deprecated
	public static void removeProcessAnnotation(ProcessAnnotation annotation) {
		AnnotationProcessXMLFilter.removeProcessAnnotation(annotation);
	}

	/**
	 * Returns the background image for the given execution unit.
	 *
	 * @param process
	 *            the execution unit in question
	 * @return the background image or {@code null} if there is none
	 * @deprecated use {@link BackgroundImageProcessXMLFilter#lookupBackgroundImage(ExecutionUnit)}
	 *             instead
	 */
	@Deprecated
	public static ProcessBackgroundImage lookupBackgroundImage(ExecutionUnit process) {
		return BackgroundImageProcessXMLFilter.lookupBackgroundImage(process);
	}

	/**
	 * Adds a {@link ProcessBackgroundImage}.
	 *
	 * @param image
	 *            the new background image
	 * @deprecated use
	 *             {@link BackgroundImageProcessXMLFilter#setBackgroundImage(ProcessBackgroundImage)}
	 *             instead
	 */
	@Deprecated
	public static void setBackgroundImage(ProcessBackgroundImage image) {
		BackgroundImageProcessXMLFilter.setBackgroundImage(image);
	}

	/**
	 * Removes the given {@link ProcessBackgroundImage}.
	 *
	 * @param process
	 *            the execution unit for which to remove the background image
	 * @deprecated use {@link BackgroundImageProcessXMLFilter#removeBackgroundImage(ExecutionUnit)}
	 *             instead
	 */
	@Deprecated
	public static void removeBackgroundImage(ExecutionUnit process) {
		BackgroundImageProcessXMLFilter.removeBackgroundImage(process);
	}
}
