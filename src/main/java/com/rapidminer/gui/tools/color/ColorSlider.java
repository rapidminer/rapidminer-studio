/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools.color;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import com.rapidminer.tools.container.Pair;


/**
 * A control component for choosing colors via a slider-like component.
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public abstract class ColorSlider extends JComponent {

	static final int X_OFFSET = 10;
	static final int BOTTOM_OFFSET = 19;

	protected ArrayList<ColorPoint> colorPoints;
	protected ColorPoint hoveredPoint;
	protected ColorPoint draggedPoint;

	protected int minAmountOfColorPoints;
	protected int maxAmountOfColorPoints;

    private EventListenerList eventListeners = new EventListenerList();


	/**
	 * Create a new color slider instance.
	 *
	 * @param colorPoints
	 * 		the preset color points to start with, can be {@code null}
	 * @param minAmountOfColorPoints
	 * 		the minimum number of color points that the user must keep
	 * @param maxAmountOfColorPoints
	 * 		the maximum number of color points hat the user can add
	 */
	public ColorSlider(List<ColorPoint> colorPoints, int minAmountOfColorPoints, int maxAmountOfColorPoints) {
		if (minAmountOfColorPoints < 0) {
			throw new IllegalArgumentException("minAmountOfColorPoints must not be less than 0!");
		}
		if (maxAmountOfColorPoints < 1) {
			throw new IllegalArgumentException("minAmountOfColorPoints must be greater than 0!");
		}

		this.minAmountOfColorPoints = minAmountOfColorPoints;
		this.maxAmountOfColorPoints = maxAmountOfColorPoints;

		if (colorPoints == null) {
			this.colorPoints = new ArrayList<>();
		} else {
			this.colorPoints = new ArrayList<>(colorPoints);
			rearrangePoints();
		}

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				// was there a drag going on?
				if (draggedPoint != null) {
					setDraggedPoint(null);
					rearrangePoints();
					// if drag ends outside of component, we need to remove hovered flag
					if (!getShapeForColorPoint(hoveredPoint, true).contains(e.getPoint())) {
						setHoveredPoint(null);
					}
					repaint();
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (hoveredPoint == null) {
						if (tryAddingPoint(e.getX() - X_OFFSET)) {
							rearrangePoints();
							fireStateChanged();
							repaint();
						}
					}
				} else if (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
					if (hoveredPoint != null) {
						if (tryRemovingHoveredPoint()) {
							rearrangePoints();
							fireStateChanged();
							repaint();
						}
					}
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (hoveredPoint != null) {
						ColorPoint point = hoveredPoint;
						Color selectedColor = ColorChooserUtilities.INSTANCE.chooseColor(point.getColor());
						if (selectedColor != null) {
							point.setColor(selectedColor);
							fireStateChanged();
						}

						repaint();
					}
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// only reset hovered if we don't drag right now. Without this, something would stay hovered even if mouse exited component
				if (draggedPoint == null) {
					setHoveredPoint(null);
				}
			}
		});
		addMouseMotionListener(new MouseAdapter() {

			@Override
			public void mouseMoved(MouseEvent e) {
				// check if any point is under the mouse
				boolean found = false;
				for (ColorPoint colorPoint : ColorSlider.this.colorPoints) {
					if (getShapeForColorPoint(colorPoint, true).contains(e.getPoint())) {
						// we always take the right-most point, so iterate over all
						setHoveredPoint(colorPoint);
						found = true;
					}
				}

				// we are not over any point, reset it
				if (!found) {
					setHoveredPoint(null);
				}

				repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (hoveredPoint != null) {
					setDraggedPoint(hoveredPoint);
				}
				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				if (draggedPoint != null) {
					double minPoint = getMinPoint();
					double maxPoint = getMaxPoint();
					double oldPoint = draggedPoint.getPoint();
					double newPoint = getRelativeXForAbsoluteX(e.getX());
					if (!Objects.equals(oldPoint, newPoint)) {
						newPoint = Math.min(newPoint, maxPoint);
						newPoint = Math.max(newPoint, minPoint);
						newPoint = Math.round(newPoint * 100.0) / 100.0;
						draggedPoint.setPoint(newPoint);

						// sort points just in case they switched positions
						ColorSlider.this.colorPoints.sort((p1, p2) -> {
							int result = Double.compare(p1.getPoint(), p2.getPoint());
							if (result == 0) {
								// in case of exact overlap, favor the currently dragged point
								if (p1 == draggedPoint) {
									return 1;
								} else if (p2 == draggedPoint) {
									return -1;
								} else {
									return 0;
								}
							}
							return result;
						});

						rearrangePoints();

						repaint();
					}
				}
			}

		});
		setPreferredSize(new Dimension(20, 45));
		updateUI();
	}

	/**
	 * Sets the color points. Same effect as if they were set via the constructor. Will overwrite any existing
	 * color points.
	 *
	 * @param colorPoints
	 *         the list, must not be {@code null}
	 */
	public void setColorPoints(List<ColorPoint> colorPoints) {
		if (colorPoints == null) {
			throw new IllegalArgumentException("colorPoints must not be null!");
		}
		if (colorPoints.stream().anyMatch(p -> p.getPoint() > getMaxPoint())) {
			throw new IllegalArgumentException("color points must not exceed max point value!");
		}

		this.colorPoints = new ArrayList<>(colorPoints);
	}

	/**
	 * Gets the color points as they are defined at this moment.
	 *
	 * @return the color points, never {@code null}
	 */
	public List<ColorPoint> getColorPoints() {
		return colorPoints;
	}

	@Override
	public void updateUI() {
		ColorSliderUI ui = (ColorSliderUI) UIManager.getUI(this);
		setUI(ui);
	}

	public abstract String getUIClassID();

	/**
	 * Adds a ChangeListener to the slider.
	 *
	 * @param l the ChangeListener to add
	 */
	public void addChangeListener(ChangeListener l) {
		eventListeners.add(ChangeListener.class, l);
	}

	/**
	 * Removes a ChangeListener from the slider.
	 *
	 * @param l the ChangeListener to remove
	 * @see #addChangeListener

	 */
	public void removeChangeListener(ChangeListener l) {
		eventListeners.remove(ChangeListener.class, l);
	}

	/**
	 * Gets the width of the color bar itself.
	 *
	 * @return the width in pixel
	 */
	int getBarWidth() {
		return getWidth() - 2 * X_OFFSET;
	}

	/**
	 * @return the hovered point or {@code null}
	 */
	ColorPoint getHoveredPoint() {
		return hoveredPoint;
	}

	/**
	 * Checks whether you can still add a color point or not.
	 *
	 * @return {@code true} if there still is capacity left; {@code false} otherwise
	 */
	boolean canAddPoint() {
		return colorPoints.size() < maxAmountOfColorPoints;
	}

	/**
	 * Converts the given absolute x (for this component) to the corresponding x value on the color bar. This is used
	 * for dragging and painting points on the color slider.
	 *
	 * @param absoluteX
	 * 		the absolute x value
	 * @return the x value on the color bar
	 */
	abstract double getRelativeXForAbsoluteX(int absoluteX);

	/**
	 * Creates the color point indicator polygon. Note that this will not be translated later, so this should use proper
	 * x coordinates.
	 *
	 * @param colorPoint
	 * 		the color point
	 * @param hoverShape
	 * 		if {@code true} the shape will be more generous for easier hover targeting
	 * @return the polygon, never {@code null}
	 */
	abstract Polygon getShapeForColorPoint(ColorPoint colorPoint, boolean hoverShape);

	/**
	 * Send a {@code ChangeEvent}, whose source is this ColorSlider, to all {@code ChangeListener}s that
	 * have registered interest in {@code ChangeEvent}s. This method is called each time a color point value
	 * changes.
	 */
	protected void fireStateChanged() {
		Object[] listeners = eventListeners.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ChangeListener.class) {
				((ChangeListener) listeners[i + 1]).stateChanged(new ChangeEvent(this));
			}
		}
	}

	/**
	 * Tries to remove the currently hovered point. If we have <= minAmountOfColorPoints, does nothing.
	 *
	 * @return {@code true} if a point was removed; {@code false} otherwise
	 */
	protected boolean tryRemovingHoveredPoint() {
		if (hoveredPoint == null) {
			return false;
		}

		if (colorPoints.size() > minAmountOfColorPoints) {
			colorPoints.remove(hoveredPoint);
			setHoveredPoint(null);
			return true;
		}

		return false;
	}

	/**
	 * Tries to add a point at the given x coordinate. If we already have >= maxAmountOfColorPoints, does nothing.
	 *
	 * @param x
	 * 		the x coordinate where the point should be added
	 * @return {@code true} if a point was added; {@code false} otherwise
	 */
	protected abstract boolean tryAddingPoint(int x);

	/**
	 * Called after a point was added, removed, or a drag ended. This can be used to change positions of points, or do
	 * nothing.
	 */
	protected abstract void rearrangePoints();

	/**
	 * Gets the points around the given x location.
	 *
	 * @param x
	 * 		the x location for which to find the surrounding points
	 * @return the points surrounding the given x location, may contain one or even two {@code null} values
	 */
	protected abstract Pair<ColorPoint, ColorPoint> getColorPointsAroundPixel(int x);

	/**
	 * Gets the minimum point a color point can have. Used for dragging.
	 *
	 * @return the min point value
	 */
	protected abstract double getMinPoint();

	/**
	 * Gets the maximum point a color point can have. Used for dragging. Also defines the upper limit a color point can
	 * have.
	 *
	 * @return the max point value
	 */
	protected abstract double getMaxPoint();


	/**
	 * Sets the hovered point and updates the mouse cursor.
	 *
	 * @param point
	 * 		the hovered point or {@code null}
	 */
	protected void setHoveredPoint(ColorPoint point) {
		if (point == null) {
			hoveredPoint = null;
			setCursor(Cursor.getDefaultCursor());
		} else {
			hoveredPoint = point;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
	}

	/**
	 * Sets the dragged point and updates the mouse cursor.
	 *
	 * @param point
	 * 		the dragged point or {@code null}
	 */
	protected void setDraggedPoint(ColorPoint point) {
		if (point == null) {
			draggedPoint = null;
			// drag ended, fire state changed event now
			fireStateChanged();
		} else {
			draggedPoint = point;
		}
	}

}
