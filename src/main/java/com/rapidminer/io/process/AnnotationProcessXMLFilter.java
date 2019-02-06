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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.rapidminer.gui.flow.processrendering.annotations.AnnotationDrawUtils;
import com.rapidminer.gui.flow.processrendering.annotations.model.OperatorAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.ProcessAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotations;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationAlignment;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationColor;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationStyle;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessRootOperator;


/**
 * {@link ProcessXMLFilter} to handle operator & process annotations data.
 *
 * @author Michael Knopf, Marco Boeck, Nils Woehler
 * @since 7.2.0
 */
public class AnnotationProcessXMLFilter implements ProcessXMLFilter {

	/** user data key for operator annotations */
	public static final String KEY_OPERATOR_ANNOTATION = "com.rapidminer.io.process.operator_annotation";

	/** user data key for process annotations */
	public static final String KEY_PROCESS_ANNOTATION = "com.rapidminer.io.process.process_annotation";

	private static final String XML_ATTRIBUTE_HEIGHT = "height";
	private static final String XML_ATTRIBUTE_WIDTH = "width";
	private static final String XML_ATTRIBUTE_X_POSITION = "x";
	private static final String XML_ATTRIBUTE_Y_POSITION = "y";

	private static final String XML_TAG_ANNOTATION = "description";
	private static final String XML_ATTRIBUTE_COLOR = "color";
	private static final String XML_ATTRIBUTE_ALIGNMENT = "align";
	private static final String XML_ATTRIBUTE_RESIZED = "resized";
	private static final String XML_ATTRIBUTE_COLORED = "colored";

	@Override
	public void operatorExported(final Operator op, final Element opElement) {
		// add workflow annotations
		Rectangle2D bounds;
		WorkflowAnnotations annotations = lookupOperatorAnnotations(op);
		if (annotations != null) {
			for (WorkflowAnnotation annotation : annotations.getAnnotationsDrawOrder()) {
				Element annotationElement = opElement.getOwnerDocument().createElement(XML_TAG_ANNOTATION);

				Text commentNode = opElement.getOwnerDocument().createTextNode(annotation.getComment());
				bounds = annotation.getLocation().getBounds();
				annotationElement.setAttribute(XML_ATTRIBUTE_WIDTH, "" + (int) bounds.getWidth());
				annotationElement.setAttribute(XML_ATTRIBUTE_COLOR, annotation.getStyle().getAnnotationColor().getKey());
				annotationElement.setAttribute(XML_ATTRIBUTE_ALIGNMENT,
						annotation.getStyle().getAnnotationAlignment().getKey());
				annotationElement.setAttribute(XML_ATTRIBUTE_COLORED, String.valueOf(annotation.wasColored()));
				annotationElement.appendChild(commentNode);

				opElement.appendChild(annotationElement);
			}
		}
	}

	@Override
	public void executionUnitExported(final ExecutionUnit process, final Element element) {
		// add workflow annotations
		WorkflowAnnotations annotations = lookupProcessAnnotations(process);
		if (annotations != null) {
			for (WorkflowAnnotation annotation : annotations.getAnnotationsDrawOrder()) {
				Element annotationElement = element.getOwnerDocument().createElement(XML_TAG_ANNOTATION);

				Text commentNode = element.getOwnerDocument().createTextNode(annotation.getComment());
				Rectangle2D bounds = annotation.getLocation().getBounds();
				annotationElement.setAttribute(XML_ATTRIBUTE_X_POSITION, "" + (int) bounds.getX());
				annotationElement.setAttribute(XML_ATTRIBUTE_Y_POSITION, "" + (int) bounds.getY());
				annotationElement.setAttribute(XML_ATTRIBUTE_WIDTH, "" + (int) bounds.getWidth());
				annotationElement.setAttribute(XML_ATTRIBUTE_HEIGHT, "" + (int) bounds.getHeight());
				annotationElement.setAttribute(XML_ATTRIBUTE_COLOR, annotation.getStyle().getAnnotationColor().getKey());
				annotationElement.setAttribute(XML_ATTRIBUTE_ALIGNMENT,
						annotation.getStyle().getAnnotationAlignment().getKey());
				annotationElement.setAttribute(XML_ATTRIBUTE_RESIZED, String.valueOf(annotation.wasResized()));
				annotationElement.setAttribute(XML_ATTRIBUTE_COLORED, String.valueOf(annotation.wasColored()));
				annotationElement.appendChild(commentNode);

				element.appendChild(annotationElement);
			}
		}
	}

	@Override
	public void operatorImported(final Operator op, final Element opElement) {
		String x = opElement.getAttribute(XML_ATTRIBUTE_X_POSITION);
		String y = opElement.getAttribute(XML_ATTRIBUTE_Y_POSITION);
		String w = opElement.getAttribute(XML_ATTRIBUTE_WIDTH);

		// workflow annotations
		NodeList children = opElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element annotationElem = (Element) child;
				if (XML_TAG_ANNOTATION.equals(annotationElem.getTagName())) {
					Node textNode = annotationElem.getChildNodes().item(0);

					String comment = textNode != null ? textNode.getNodeValue() : "";
					String wStr = annotationElem.getAttribute(XML_ATTRIBUTE_WIDTH);
					String colorStr = annotationElem.getAttribute(XML_ATTRIBUTE_COLOR);
					String alignStr = annotationElem.getAttribute(XML_ATTRIBUTE_ALIGNMENT);
					String coloredStr = annotationElem.getAttribute(XML_ATTRIBUTE_COLORED);

					AnnotationStyle style = new AnnotationStyle(AnnotationColor.fromKey(colorStr),
							AnnotationAlignment.fromKey(alignStr));
					if (op instanceof ProcessRootOperator) {
						// handle prior 6.4.0 root comments and convert to process annotation
						ExecutionUnit process = ((ProcessRootOperator) op).getSubprocess(0);

						// we have no idea (and cannot guess reliably) at size of process
						// so position legacy comment in top left corner
						double xCoord = 25;
						double yCoord = 25;
						int newWidth = 400;
						int newHeight = AnnotationDrawUtils
								.getContentHeight(AnnotationDrawUtils.createStyledCommentString(comment, style), newWidth, AnnotationDrawUtils.ANNOTATION_FONT);
						boolean overflowing = false;
						if (newHeight > 500) {
							newWidth = 1000;
							newHeight = AnnotationDrawUtils.getContentHeight(
									AnnotationDrawUtils.createStyledCommentString(comment, style), newWidth, AnnotationDrawUtils.ANNOTATION_FONT);
							if (newHeight > ProcessAnnotation.MAX_HEIGHT) {
								newHeight = ProcessAnnotation.MAX_HEIGHT;
								overflowing = true;
							}
						}
						ProcessAnnotation annotation = new ProcessAnnotation(comment, style, process, false, false,
								new Rectangle2D.Double(xCoord, yCoord, newWidth, newHeight));
						annotation.setOverflowing(overflowing);
						addProcessAnnotation(annotation);
					} else {
						try {
							double width = Double.parseDouble(wStr);
							if (width > OperatorAnnotation.DEFAULT_WIDTH) {
								width = OperatorAnnotation.DEFAULT_WIDTH;
							}
							int height = AnnotationDrawUtils.getContentHeight(
									AnnotationDrawUtils.createStyledCommentString(comment, style), (int) width,AnnotationDrawUtils.ANNOTATION_FONT);
							boolean overflowing = false;
							if (height > OperatorAnnotation.MAX_HEIGHT) {
								height = OperatorAnnotation.MAX_HEIGHT;
								overflowing = true;
							}

							int opCenter = (int) (Double.parseDouble(x) + Double.parseDouble(w) / 2);
							int xCo = (int) (opCenter - width / 2);
							int yCo = (int) Double.parseDouble(y) + OperatorAnnotation.Y_OFFSET;
							OperatorAnnotation annotation = new OperatorAnnotation(comment, style, op, false,
									Boolean.parseBoolean(coloredStr), xCo, yCo, width, height);
							annotation.setOverflowing(overflowing);
							addOperatorAnnotation(annotation);
						} catch (NullPointerException | NumberFormatException e) {
							// operator annotations prior to 6.4.0
							int width = OperatorAnnotation.DEFAULT_WIDTH;
							int height = AnnotationDrawUtils
									.getContentHeight(AnnotationDrawUtils.createStyledCommentString(comment, style), width, AnnotationDrawUtils.ANNOTATION_FONT);
							boolean overflowing = false;
							if (height > OperatorAnnotation.MAX_HEIGHT) {
								height = OperatorAnnotation.MAX_HEIGHT;
								overflowing = true;
							}

							int opCenter = (int) (Double.parseDouble(x) + Double.parseDouble(w) / 2);
							int xCo = opCenter - width / 2;
							int yCo = (int) Double.parseDouble(y) + OperatorAnnotation.Y_OFFSET;
							OperatorAnnotation annotation = new OperatorAnnotation(comment,
									new AnnotationStyle(AnnotationColor.TRANSPARENT, AnnotationAlignment.CENTER), op, false,
									false, xCo, yCo, width, height);
							annotation.setOverflowing(overflowing);
							addOperatorAnnotation(annotation);
						}
					}
				}
			}
		}
	}

	@Override
	public void executionUnitImported(final ExecutionUnit process, final Element element) {

		// workflow annotations
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element annotationElem = (Element) child;
				if (XML_TAG_ANNOTATION.equals(annotationElem.getTagName())) {
					Node textNode = annotationElem.getChildNodes().item(0);

					String comment = textNode != null ? textNode.getNodeValue() : "";
					comment = comment == null ? "" : comment;
					String xStr = annotationElem.getAttribute(XML_ATTRIBUTE_X_POSITION);
					String yStr = annotationElem.getAttribute(XML_ATTRIBUTE_Y_POSITION);
					String wStr = annotationElem.getAttribute(XML_ATTRIBUTE_WIDTH);
					String hStr = annotationElem.getAttribute(XML_ATTRIBUTE_HEIGHT);
					String colorStr = annotationElem.getAttribute(XML_ATTRIBUTE_COLOR);
					String alignStr = annotationElem.getAttribute(XML_ATTRIBUTE_ALIGNMENT);
					String resizedStr = annotationElem.getAttribute(XML_ATTRIBUTE_RESIZED);
					String coloredStr = annotationElem.getAttribute(XML_ATTRIBUTE_COLORED);

					AnnotationStyle style = new AnnotationStyle(AnnotationColor.fromKey(colorStr),
							AnnotationAlignment.fromKey(alignStr));
					try {
						double xLoc = Double.parseDouble(xStr);
						double yLoc = Double.parseDouble(yStr);
						double wLoc = Double.parseDouble(wStr);
						double hLoc = Double.parseDouble(hStr);
						boolean overflowing = false;
						int requiredHeight = AnnotationDrawUtils
								.getContentHeight(AnnotationDrawUtils.createStyledCommentString(comment, style), (int) wLoc, AnnotationDrawUtils.ANNOTATION_FONT);
						if (requiredHeight > hLoc) {
							overflowing = true;
						}
						ProcessAnnotation annotation = new ProcessAnnotation(comment, style, process,
								Boolean.parseBoolean(resizedStr), Boolean.parseBoolean(coloredStr),
								new Rectangle2D.Double(xLoc, yLoc, wLoc, hLoc));
						annotation.setOverflowing(overflowing);
						addProcessAnnotation(annotation);
					} catch (NullPointerException | NumberFormatException e) {
						// ignore silently
					}
				}
			}
		}
	}

	/**
	 * Returns the operator annotations for the given operator.
	 *
	 * @param operator
	 *            the operator in question
	 * @return the annotations or {@code null} if there are none
	 */
	public static WorkflowAnnotations lookupOperatorAnnotations(Operator operator) {
		return (WorkflowAnnotations) operator.getUserData(KEY_OPERATOR_ANNOTATION);
	}

	/**
	 * Adds a {@link OperatorAnnotation} to the {@link Operator}.
	 *
	 * @param annotation
	 *            the new annotation
	 */
	public static void addOperatorAnnotation(OperatorAnnotation annotation) {
		if (annotation == null) {
			throw new IllegalArgumentException("annotation must not be null!");
		}

		WorkflowAnnotations annotations = lookupOperatorAnnotations(annotation.getAttachedTo());
		if (annotations == null) {
			annotations = new WorkflowAnnotations();
		}
		annotations.addAnnotation(annotation);
		annotation.getAttachedTo().setUserData(KEY_OPERATOR_ANNOTATION, annotations);
	}

	/**
	 * Removes the given {@link OperatorAnnotation}.
	 *
	 * @param annotation
	 *            the annotation to remove
	 */
	public static void removeOperatorAnnotation(OperatorAnnotation annotation) {
		if (annotation == null) {
			throw new IllegalArgumentException("annotation must not be null!");
		}

		WorkflowAnnotations annotations = lookupOperatorAnnotations(annotation.getAttachedTo());
		if (annotations == null) {
			return;
		}
		annotations.removeAnnotation(annotation);
		annotation.getAttachedTo().setUserData(KEY_OPERATOR_ANNOTATION, annotations);
	}

	/**
	 * Returns the process annotations for the given execution unit.
	 *
	 * @param process
	 *            the execution unit in question
	 * @return the annotations or {@code null} if there are none
	 */
	public static WorkflowAnnotations lookupProcessAnnotations(ExecutionUnit process) {
		return (WorkflowAnnotations) process.getUserData(KEY_PROCESS_ANNOTATION);
	}

	/**
	 * Adds a {@link ProcessAnnotation}.
	 *
	 * @param annotation
	 *            the new annotation
	 */
	public static void addProcessAnnotation(ProcessAnnotation annotation) {
		if (annotation == null) {
			throw new IllegalArgumentException("annotation must not be null!");
		}

		WorkflowAnnotations annotations = lookupProcessAnnotations(annotation.getProcess());
		if (annotations == null) {
			annotations = new WorkflowAnnotations();
		}
		annotations.addAnnotation(annotation);
		annotation.getProcess().setUserData(KEY_PROCESS_ANNOTATION, annotations);
	}

	/**
	 * Removes the given {@link ProcessAnnotation}.
	 *
	 * @param annotation
	 *            the annotation to remove
	 */
	public static void removeProcessAnnotation(ProcessAnnotation annotation) {
		if (annotation == null) {
			throw new IllegalArgumentException("annotation must not be null!");
		}

		WorkflowAnnotations annotations = lookupProcessAnnotations(annotation.getProcess());
		if (annotations == null) {
			return;
		}
		annotations.removeAnnotation(annotation);
		annotation.getProcess().setUserData(KEY_PROCESS_ANNOTATION, annotations);
	}
}
