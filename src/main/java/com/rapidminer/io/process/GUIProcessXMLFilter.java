/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.util.HashMap;
import java.util.Map;

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
import com.rapidminer.gui.flow.processrendering.background.ProcessBackgroundImage;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.UserData;
import com.rapidminer.operator.ports.Port;


/**
 * {@link ProcessXMLFilter} to handle position data for operators and ports as well as operator
 * annotations. The filter provides a set of static utility methods to work with the loaded user
 * data.
 *
 *
 * @author Michael Knopf, Marco Boeck
 */
public class GUIProcessXMLFilter implements ProcessXMLFilter {

	/** user data key for operator annotations */
	public static final String KEY_OPERATOR_ANNOTATION = "com.rapidminer.io.process.operator_annotation";
	/** user data key for process annotations */
	public static final String KEY_PROCESS_ANNOTATION = "com.rapidminer.io.process.process_annotation";

	/** user data key for process background image */
	public static final String KEY_PROCESS_BACKGROUND_IMAGE = "com.rapidminer.io.process.process_background_image";

	private static final String KEY_PORT_RECTANGLE = "com.rapidminer.io.process.operator_rectangle";
	private static final String KEY_PORT_SPACING = "com.rapidminer.io.process.port_spacing";

	private static final String XML_TAG_PORT_SPACING = "portSpacing";
	private static final String XML_ATTRIBUTE_HEIGHT = "height";
	private static final String XML_ATTRIBUTE_SINK = "sink_";
	private static final String XML_ATTRIBUTE_SOURCE = "source_";
	private static final String XML_ATTRIBUTE_SPACING = "spacing";
	private static final String XML_ATTRIBUTE_PORT = "port";
	private static final String XML_ATTRIBUTE_WIDTH = "width";
	private static final String XML_ATTRIBUTE_X_POSITION = "x";
	private static final String XML_ATTRIBUTE_Y_POSITION = "y";
	private static final String XML_ATTRIBUTE_LOCATION = "location";

	private static final String XML_TAG_ANNOTATION = "description";
	private static final String XML_TAG_BACKGROUND = "background";
	private static final String XML_ATTRIBUTE_COLOR = "color";
	private static final String XML_ATTRIBUTE_ALIGNMENT = "align";
	private static final String XML_ATTRIBUTE_RESIZED = "resized";
	private static final String XML_ATTRIBUTE_COLORED = "colored";

	/**
	 * User data wrapper for {@link Rectangle2D}.
	 *
	 * @author Michael Knopf
	 */
	private static class Rectangle2DWrapper implements UserData<Object> {

		/** Wrapped object. */
		private final Rectangle2D rect;

		public Rectangle2DWrapper(Rectangle2D rect) {
			this.rect = rect;
		}

		public Rectangle2D get() {
			return this.rect;
		}

		@Override
		public UserData<Object> copyUserData(Object newParent) {
			Rectangle2D newRect = new Rectangle2D.Double();
			newRect.setRect(this.rect);
			return new Rectangle2DWrapper(newRect);
		}

	}

	/**
	 * User data wrapper for {@link Map}s from {@link Port} to {@link Integer} as used to store port
	 * spacings.
	 *
	 * @author Michael Knopf
	 */
	private static class PortSpacingWrapper implements UserData<Object> {

		/** Wrapped map. */
		private final Map<Port, Integer> spaces;

		public PortSpacingWrapper(Map<Port, Integer> spacings) {
			this.spaces = spacings;
		}

		public Map<Port, Integer> get() {
			return this.spaces;
		}

		@Override
		public UserData<Object> copyUserData(Object newParent) {
			// mapping cannot be reused in copied instance
			return null;
		}

	}

	/**
	 * Adds the position and size of the operator to the XML element.
	 */
	@Override
	public void operatorExported(final Operator op, final Element opElement) {
		// add operator location and size
		Rectangle2D bounds = lookupOperatorRectangle(op);
		if (bounds != null) {
			opElement.setAttribute(XML_ATTRIBUTE_X_POSITION, "" + (int) bounds.getX());
			opElement.setAttribute(XML_ATTRIBUTE_Y_POSITION, "" + (int) bounds.getY());
			opElement.setAttribute(XML_ATTRIBUTE_WIDTH, "" + (int) bounds.getWidth());
			opElement.setAttribute(XML_ATTRIBUTE_HEIGHT, "" + (int) bounds.getHeight());
		}

		// add workflow annotations
		WorkflowAnnotations annotations = lookupOperatorAnnotations(op);
		if (annotations != null) {
			for (WorkflowAnnotation annotation : annotations.getAnnotationsDrawOrder()) {
				Element annotationElement = opElement.getOwnerDocument().createElement(XML_TAG_ANNOTATION);

				Text commentNode = opElement.getOwnerDocument().createTextNode(annotation.getComment());
				bounds = annotation.getLocation().getBounds();
				annotationElement.setAttribute(XML_ATTRIBUTE_WIDTH, "" + (int) bounds.getWidth());
				annotationElement.setAttribute(XML_ATTRIBUTE_COLOR, annotation.getStyle().getAnnotationColor().getKey());
				annotationElement.setAttribute(XML_ATTRIBUTE_ALIGNMENT, annotation.getStyle().getAnnotationAlignment()
						.getKey());
				annotationElement.setAttribute(XML_ATTRIBUTE_COLORED, String.valueOf(annotation.wasColored()));
				annotationElement.appendChild(commentNode);

				opElement.appendChild(annotationElement);
			}
		}
	}

	/**
	 * Adds port spacings to the XML element.
	 */
	@Override
	public void executionUnitExported(final ExecutionUnit process, final Element element) {
		for (Port port : process.getInnerSources().getAllPorts()) {
			Element spacingElement = element.getOwnerDocument().createElement(XML_TAG_PORT_SPACING);
			spacingElement.setAttribute(XML_ATTRIBUTE_PORT, XML_ATTRIBUTE_SOURCE + port.getName());
			spacingElement.setAttribute(XML_ATTRIBUTE_SPACING, "" + lookupPortSpacing(port));
			element.appendChild(spacingElement);
		}
		for (Port port : process.getInnerSinks().getAllPorts()) {
			Element spacingElement = element.getOwnerDocument().createElement(XML_TAG_PORT_SPACING);
			spacingElement.setAttribute(XML_ATTRIBUTE_PORT, XML_ATTRIBUTE_SINK + port.getName());
			spacingElement.setAttribute(XML_ATTRIBUTE_SPACING, "" + lookupPortSpacing(port));
			element.appendChild(spacingElement);
		}

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
				annotationElement.setAttribute(XML_ATTRIBUTE_ALIGNMENT, annotation.getStyle().getAnnotationAlignment()
						.getKey());
				annotationElement.setAttribute(XML_ATTRIBUTE_RESIZED, String.valueOf(annotation.wasResized()));
				annotationElement.setAttribute(XML_ATTRIBUTE_COLORED, String.valueOf(annotation.wasColored()));
				annotationElement.appendChild(commentNode);

				element.appendChild(annotationElement);
			}
		}

		// add background image
		ProcessBackgroundImage image = lookupBackgroundImage(process);
		if (image != null) {
			Element backgroundElement = element.getOwnerDocument().createElement(XML_TAG_BACKGROUND);

			backgroundElement.setAttribute(XML_ATTRIBUTE_X_POSITION, "" + image.getX());
			backgroundElement.setAttribute(XML_ATTRIBUTE_Y_POSITION, "" + image.getY());
			backgroundElement.setAttribute(XML_ATTRIBUTE_WIDTH, "" + image.getOriginalWidth());
			backgroundElement.setAttribute(XML_ATTRIBUTE_HEIGHT, "" + image.getOriginalHeight());
			backgroundElement.setAttribute(XML_ATTRIBUTE_LOCATION, image.getLocation());

			element.appendChild(backgroundElement);
		}
	}

	/**
	 * Extracts operator position and size from the XML element.
	 */
	@Override
	public void operatorImported(final Operator op, final Element opElement) {
		String x = opElement.getAttribute(XML_ATTRIBUTE_X_POSITION);
		String y = opElement.getAttribute(XML_ATTRIBUTE_Y_POSITION);
		String w = opElement.getAttribute(XML_ATTRIBUTE_WIDTH);
		String h = opElement.getAttribute(XML_ATTRIBUTE_HEIGHT);
		if (x != null && x.length() > 0) {
			try {
				Rectangle2D rect = new Rectangle2D.Double(Double.parseDouble(x), Double.parseDouble(y),
						Double.parseDouble(w), Double.parseDouble(h));
				setOperatorRectangle(op, rect);
			} catch (Exception e) {
				// ignore silently
			}
		}

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
						int newHeight = AnnotationDrawUtils.getContentHeight(
								AnnotationDrawUtils.createStyledCommentString(comment, style), newWidth);
						boolean overflowing = false;
						if (newHeight > 500) {
							newWidth = 1000;
							newHeight = AnnotationDrawUtils.getContentHeight(
									AnnotationDrawUtils.createStyledCommentString(comment, style), newWidth);
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
									AnnotationDrawUtils.createStyledCommentString(comment, style), (int) width);
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
							int height = AnnotationDrawUtils.getContentHeight(
									AnnotationDrawUtils.createStyledCommentString(comment, style), width);
							boolean overflowing = false;
							if (height > OperatorAnnotation.MAX_HEIGHT) {
								height = OperatorAnnotation.MAX_HEIGHT;
								overflowing = true;
							}

							int opCenter = (int) (Double.parseDouble(x) + Double.parseDouble(w) / 2);
							int xCo = opCenter - width / 2;
							int yCo = (int) Double.parseDouble(y) + OperatorAnnotation.Y_OFFSET;
							OperatorAnnotation annotation = new OperatorAnnotation(comment, new AnnotationStyle(
									AnnotationColor.TRANSPARENT, AnnotationAlignment.CENTER), op, false, false, xCo, yCo,
									width, height);
							annotation.setOverflowing(overflowing);
							addOperatorAnnotation(annotation);
						}
					}
				}
			}
		}
	}

	/**
	 * Extracts port spacings from the XML element.
	 */
	@Override
	public void executionUnitImported(final ExecutionUnit process, final Element element) {
		NodeList children = element.getChildNodes();
		for (Port port : process.getInnerSources().getAllPorts()) {
			for (int i = 0; i < children.getLength(); i++) {
				if (children.item(i) instanceof Element
						&& XML_TAG_PORT_SPACING.equals(((Element) children.item(i)).getTagName())) {
					Element psElement = (Element) children.item(i);
					if ((XML_ATTRIBUTE_SOURCE + port.getName()).equals(psElement.getAttribute(XML_ATTRIBUTE_PORT))) {
						try {
							setPortSpacing(port, Integer.parseInt(psElement.getAttribute(XML_ATTRIBUTE_SPACING)));
						} catch (NumberFormatException e) {
							// do nothing
						}
						break;
					}
				}
			}
		}
		for (Port port : process.getInnerSinks().getAllPorts()) {
			for (int i = 0; i < children.getLength(); i++) {
				if (children.item(i) instanceof Element
						&& XML_TAG_PORT_SPACING.equals(((Element) children.item(i)).getTagName())) {
					Element psElement = (Element) children.item(i);
					if ((XML_ATTRIBUTE_SINK + port.getName()).equals(psElement.getAttribute(XML_ATTRIBUTE_PORT))) {
						try {
							setPortSpacing(port, Integer.parseInt(psElement.getAttribute(XML_ATTRIBUTE_SPACING)));
						} catch (NumberFormatException e) {
							// do nothing
						}
						break;
					}
				}
			}
		}

		// workflow annotations
		children = element.getChildNodes();
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
						int requiredHeight = AnnotationDrawUtils.getContentHeight(
								AnnotationDrawUtils.createStyledCommentString(comment, style), (int) wLoc);
						if (requiredHeight > hLoc) {
							overflowing = true;
						}
						ProcessAnnotation annotation = new ProcessAnnotation(comment, style, process,
								Boolean.parseBoolean(resizedStr), Boolean.parseBoolean(coloredStr), new Rectangle2D.Double(
										xLoc, yLoc, wLoc, hLoc));
						annotation.setOverflowing(overflowing);
						addProcessAnnotation(annotation);
					} catch (NullPointerException | NumberFormatException e) {
						// ignore silently
					}
				}
			}
		}

		// background image
		children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element backgroundElement = (Element) child;
				if (XML_TAG_BACKGROUND.equals(backgroundElement.getTagName())) {

					String xStr = backgroundElement.getAttribute(XML_ATTRIBUTE_X_POSITION);
					String yStr = backgroundElement.getAttribute(XML_ATTRIBUTE_Y_POSITION);
					String wStr = backgroundElement.getAttribute(XML_ATTRIBUTE_WIDTH);
					String hStr = backgroundElement.getAttribute(XML_ATTRIBUTE_HEIGHT);
					String imgLocStr = backgroundElement.getAttribute(XML_ATTRIBUTE_LOCATION);

					try {
						int xLoc = Integer.parseInt(xStr);
						int yLoc = Integer.parseInt(yStr);
						int wLoc = Integer.parseInt(wStr);
						int hLoc = Integer.parseInt(hStr);
						ProcessBackgroundImage bgImg = new ProcessBackgroundImage(xLoc, yLoc, wLoc, hLoc, imgLocStr, process);
						setBackgroundImage(bgImg);
					} catch (NullPointerException | IllegalArgumentException e) {
						// ignore silently
					}
				}
			}
		}
	}

	/**
	 * Looks up the spacing of the specified {@link Port}.
	 *
	 * @param port
	 *            The port.
	 * @return Additional spacing.
	 */
	public static int lookupPortSpacing(Port port) {
		Operator operator = port.getPorts().getOwner().getOperator();
		PortSpacingWrapper wrapper = (PortSpacingWrapper) operator.getUserData(KEY_PORT_SPACING);
		if (wrapper != null) {
			Map<Port, Integer> spacings = wrapper.get();
			if (spacings.containsKey(port)) {
				return spacings.get(port);
			} else {
				// no spacing stored for this particular port
				return 0;
			}
		} else {
			// no spacing data available
			return 0;
		}
	}

	/**
	 * Sets the spacing of the specified {@link Port}.
	 *
	 * @param port
	 *            The port.
	 * @param spacing
	 *            The additional spacing.
	 */
	public static void setPortSpacing(Port port, Integer spacing) {
		Operator operator = port.getPorts().getOwner().getOperator();
		PortSpacingWrapper wrapper = (PortSpacingWrapper) operator.getUserData(KEY_PORT_SPACING);
		Map<Port, Integer> spacings;
		if (wrapper == null) {
			spacings = new HashMap<>();
			operator.setUserData(KEY_PORT_SPACING, new PortSpacingWrapper(spacings));
		} else {
			spacings = wrapper.get();
		}
		spacings.put(port, spacing);
	}

	/**
	 * Resets the spacing of the specified {@link Port}.
	 *
	 * @param port
	 *            The port.
	 */
	public static void resetPortSpacing(Port port) {
		Operator operator = port.getPorts().getOwner().getOperator();
		PortSpacingWrapper wrapper = (PortSpacingWrapper) operator.getUserData(KEY_PORT_SPACING);
		if (wrapper != null) {
			Map<Port, Integer> spacings = wrapper.get();
			spacings.remove(port);
		}
	}

	/**
	 * Looks up the position rectangle of the specified {@link Operator}.
	 *
	 * @param operator
	 *            The operator.
	 * @return The rectangle or null.
	 */
	public static Rectangle2D lookupOperatorRectangle(Operator operator) {
		Rectangle2DWrapper wrapper = (Rectangle2DWrapper) operator.getUserData(KEY_PORT_RECTANGLE);
		if (wrapper == null) {
			return null;
		} else {
			return wrapper.get();
		}
	}

	/**
	 * Sets the position rectangle of the specified {@link Operator}.
	 *
	 * @param operator
	 *            The operator.
	 * @param rect
	 *            The rectangle.
	 */
	public static void setOperatorRectangle(Operator operator, Rectangle2D rect) {
		operator.setUserData(KEY_PORT_RECTANGLE, new Rectangle2DWrapper(rect));
	}

	/**
	 * Resets the position rectangle of the specified {@link Operator}.
	 *
	 * @param operator
	 *            The operator.
	 */
	public static void resetOperatorRectangle(Operator operator) {
		operator.setUserData(KEY_PORT_RECTANGLE, null);
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

	/**
	 * Returns the background image for the given execution unit.
	 *
	 * @param process
	 *            the execution unit in question
	 * @return the background image or {@code null} if there is none
	 */
	public static ProcessBackgroundImage lookupBackgroundImage(ExecutionUnit process) {
		return (ProcessBackgroundImage) process.getUserData(KEY_PROCESS_BACKGROUND_IMAGE);
	}

	/**
	 * Adds a {@link ProcessBackgroundImage}.
	 *
	 * @param image
	 *            the new background image
	 */
	public static void setBackgroundImage(ProcessBackgroundImage image) {
		if (image == null) {
			throw new IllegalArgumentException("image must not be null!");
		}

		image.getProcess().setUserData(KEY_PROCESS_BACKGROUND_IMAGE, image);
	}

	/**
	 * Removes the given {@link ProcessBackgroundImage}.
	 *
	 * @param process
	 *            the execution unit for which to remove the background image
	 */
	public static void removeBackgroundImage(ExecutionUnit process) {
		process.setUserData(KEY_PROCESS_BACKGROUND_IMAGE, null);
	}
}
