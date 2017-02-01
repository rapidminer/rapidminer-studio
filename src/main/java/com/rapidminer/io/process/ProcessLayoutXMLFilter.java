/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
import org.w3c.dom.NodeList;

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserData;
import com.rapidminer.operator.ports.Port;


/**
 * {@link ProcessXMLFilter} to handle position data for operators and ports.
 *
 * @author Michael Knopf, Marco Boeck, Nils Woehler
 * @since 7.2.0
 */
public class ProcessLayoutXMLFilter implements ProcessXMLFilter {

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

}
