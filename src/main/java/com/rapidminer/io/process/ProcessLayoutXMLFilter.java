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

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.FlagUserData;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.UserData;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;


/**
 * {@link ProcessXMLFilter} to handle position data for operators and ports.
 *
 * @author Michael Knopf, Marco Boeck, Nils Woehler, Jan Czogalla
 * @since 7.2.0
 */
public class ProcessLayoutXMLFilter implements ProcessXMLFilter {

	private static final String KEY_OPERATOR_RECTANGLE = "com.rapidminer.io.process.operator_rectangle";
	private static final String KEY_PORT_SPACING = "com.rapidminer.io.process.port_spacing";
	public static final String KEY_OPERATOR_CHAIN_POSITION = "com.rapidminer.io.process.operator_chain_position";
	public static final String KEY_OPERATOR_CHAIN_ZOOM = "com.rapidminer.io.process.operator_chain_zoom";
	private static final String KEY_SCROLL_POSITION = "com.rapidminer.io.process.scroll_position";
	private static final String KEY_SCROLL_INDEX = "com.rapidminer.io.process.scroll_index";
	private static final String KEY_RESTORE = "com.rapidminer.io.process.restore";

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
	 * User data wrapper for {@link Point} objects, e.g. to store view positions of
	 * {@link OperatorChain}.
	 *
	 * @author Jan Czogalla
	 * @since 7.5
	 */
	private static class PointWrapper implements UserData<Object> {

		/** Wrapped object. */
		private final Point point;

		public PointWrapper(Point point) {
			this.point = point;
		}

		public Point get() {
			return this.point;
		}

		@Override
		public UserData<Object> copyUserData(Object newParent) {
			return new PointWrapper(point != null ? new Point(point) : null);
		}
	}

	/**
	 * User data wrapper for {@link Double} objects.
	 *
	 * @author Jan Czogalla
	 * @since 7.5
	 */
	private static class DoubleWrapper implements UserData<Object> {

		private final double value;

		public DoubleWrapper(Double value) {
			this.value = value;
		}

		public Double get() {
			return this.value;
		}

		@Override
		public UserData<Object> copyUserData(Object newParent) {
			return this;
		}

		@Override
		public String toString() {
			return String.valueOf(get());
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
		if (w == null || w.isEmpty()) {
			w = Double.toString(ProcessDrawer.OPERATOR_WIDTH);
		}
		String h = opElement.getAttribute(XML_ATTRIBUTE_HEIGHT);
		if (h == null || h.isEmpty()) {
			h = Double.toString(ProcessDrawUtils.calcHeighForOperator(op));
		}
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
		setPortSpacings(process.getInnerSources(), children, XML_ATTRIBUTE_SOURCE);
		setPortSpacings(process.getInnerSinks(), children, XML_ATTRIBUTE_SINK);
	}

	/**
	 * Extracts port spacings for the given {@link Ports} instance.
	 *
	 * @param ports
	 * 		the ports to be spaced
	 * @param children
	 * 		the list of children to check
	 * @since 8.2
	 */
	private void setPortSpacings(Ports<? extends Port> ports, NodeList children, String portPrefix) {
		for (Port port : ports.getAllPorts()) {
			for (int i = 0; i < children.getLength(); i++) {
				Node item = children.item(i);
				if (!(item instanceof Element) || !XML_TAG_PORT_SPACING.equals(((Element) item).getTagName())) {
					continue;
				}
				Element psElement = (Element) item;
				String portName = psElement.getAttribute(XML_ATTRIBUTE_PORT);
				if ((portPrefix + port.getName()).equals(portName)) {
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

	/**
	 * Looks up the spacing of the specified {@link Port}.
	 *
	 * @param port
	 * 		The port.
	 * @return Additional spacing.
	 */
	public static int lookupPortSpacing(Port port) {
		Operator operator = port.getPorts().getOwner().getOperator();
		PortSpacingWrapper wrapper = (PortSpacingWrapper) operator.getUserData(KEY_PORT_SPACING);
		if (wrapper != null) {
			Map<Port, Integer> spacings = wrapper.get();
			// get spacing or no spacing
			return spacings.getOrDefault(port, 0);
		} else {
			// no spacing data available
			return 0;
		}
	}

	/**
	 * Sets the spacing of the specified {@link Port}.
	 *
	 * @param port
	 * 		The port.
	 * @param spacing
	 * 		The additional spacing.
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
	 * 		The port.
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
	 * 		The operator.
	 * @return The rectangle or null.
	 */
	public static Rectangle2D lookupOperatorRectangle(Operator operator) {
		Rectangle2DWrapper wrapper = (Rectangle2DWrapper) operator.getUserData(KEY_OPERATOR_RECTANGLE);
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
	 * 		The operator.
	 * @param rect
	 * 		The rectangle.
	 */
	public static void setOperatorRectangle(Operator operator, Rectangle2D rect) {
		operator.setUserData(KEY_OPERATOR_RECTANGLE, new Rectangle2DWrapper(rect));
	}

	/**
	 * Resets the position rectangle of the specified {@link Operator}.
	 *
	 * @param operator
	 * 		The operator.
	 */
	public static void resetOperatorRectangle(Operator operator) {
		operator.setUserData(KEY_OPERATOR_RECTANGLE, null);
	}

	/**
	 * Looks up the view position of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain
	 * @return The position or null
	 * @since 7.5
	 */
	public static Point lookupOperatorChainPosition(OperatorChain operatorChain) {
		PointWrapper wrapper = (PointWrapper) operatorChain.getUserData(KEY_OPERATOR_CHAIN_POSITION);
		if (wrapper == null) {
			return null;
		} else {
			return wrapper.get();
		}
	}

	/**
	 * Sets the view position of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain
	 * @param position
	 * 		The center position
	 * @since 7.5
	 */
	public static void setOperatorChainPosition(OperatorChain operatorChain, Point position) {
		operatorChain.setUserData(KEY_OPERATOR_CHAIN_POSITION, position == null ? null : new PointWrapper(position));
	}

	/**
	 * Resets the view position of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain
	 * @since 7.5
	 */
	public static void resetOperatorChainPosition(OperatorChain operatorChain) {
		operatorChain.setUserData(KEY_OPERATOR_CHAIN_POSITION, null);
	}

	/**
	 * Looks up the zoom of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain
	 * @return The zoom or null
	 * @since 7.5
	 */
	public static Double lookupOperatorChainZoom(OperatorChain operatorChain) {
		DoubleWrapper wrapper = (DoubleWrapper) operatorChain.getUserData(KEY_OPERATOR_CHAIN_ZOOM);
		if (wrapper == null) {
			return null;
		} else {
			return wrapper.get();
		}
	}

	/**
	 * Sets the zoom of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain
	 * @param zoom
	 * 		The zoom
	 * @since 7.5
	 */
	public static void setOperatorChainZoom(OperatorChain operatorChain, Double zoom) {
		operatorChain.setUserData(KEY_OPERATOR_CHAIN_ZOOM, zoom == null ? null : new DoubleWrapper(zoom));
	}

	/**
	 * Resets the zoom of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain
	 * @since 7.5
	 */
	public static void resetOperatorChainZoom(OperatorChain operatorChain) {
		operatorChain.setUserData(KEY_OPERATOR_CHAIN_ZOOM, null);
	}

	/**
	 * Looks up the scroll position of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain
	 * @return The scroll position or null
	 * @since 7.5
	 */
	public static Point lookupScrollPosition(OperatorChain operatorChain) {
		PointWrapper wrapper = (PointWrapper) operatorChain.getUserData(KEY_SCROLL_POSITION);
		if (wrapper == null) {
			return null;
		} else {
			return wrapper.get();
		}
	}

	/**
	 * Sets the scroll position of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain
	 * @param scrollPos
	 * 		The scroll position
	 * @since 7.5
	 */
	public static void setScrollPosition(OperatorChain operatorChain, Point scrollPos) {
		operatorChain.setUserData(KEY_SCROLL_POSITION, new PointWrapper(scrollPos));
	}

	/**
	 * Resets the scroll position of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain
	 * @since 7.5
	 */
	public static void resetScrollPosition(OperatorChain operatorChain) {
		operatorChain.setUserData(KEY_SCROLL_POSITION, null);
	}

	/**
	 * Looks up the scroll process index of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain
	 * @return The index or null
	 * @since 7.5
	 */
	public static Double lookupScrollIndex(OperatorChain operatorChain) {
		DoubleWrapper wrapper = (DoubleWrapper) operatorChain.getUserData(KEY_SCROLL_INDEX);
		if (wrapper == null) {
			return null;
		} else {
			return wrapper.get();
		}
	}

	/**
	 * Sets the scroll process index of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain
	 * @param index
	 * 		The process index
	 * @since 7.5
	 */
	public static void setScrollIndex(OperatorChain operatorChain, Double index) {
		operatorChain.setUserData(KEY_SCROLL_INDEX, new DoubleWrapper(index));
	}

	/**
	 * Resets the scroll process index of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain.
	 * @since 7.5
	 */
	public static void resetScrollIndex(OperatorChain operatorChain) {
		operatorChain.setUserData(KEY_SCROLL_INDEX, null);
	}

	/**
	 * Looks up the restore flag of the specified {@link Operator}.
	 *
	 * @param operator
	 * 		the operator
	 * @return the flag or null
	 * @since 7.5
	 */
	public static boolean lookupRestore(Operator operator) {
		UserData<?> restore = operator.getUserData(KEY_RESTORE);
		return restore != null;
	}

	/**
	 * Sets the restore flag of the specified {@link Operator}.
	 *
	 * @param operator
	 * 		the operator
	 * @since 7.5
	 */
	public static void setRestore(Operator operator) {
		operator.setUserData(KEY_RESTORE, new FlagUserData());
	}

	/**
	 * Resets the restore flag of the specified {@link OperatorChain}.
	 *
	 * @param operator
	 * 		The operator
	 * @since 7.5
	 */
	public static void resetRestore(Operator operator) {
		operator.setUserData(KEY_RESTORE, null);
	}

}
