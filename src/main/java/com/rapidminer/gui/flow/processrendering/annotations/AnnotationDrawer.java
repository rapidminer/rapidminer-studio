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
package com.rapidminer.gui.flow.processrendering.annotations;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.JEditorPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationDragHelper;
import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationResizeHelper;
import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationResizeHelper.ResizeDirection;
import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationsModel;
import com.rapidminer.gui.flow.processrendering.annotations.model.OperatorAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.ProcessAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotations;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationColor;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.container.Pair;


/**
 * This class does the actual Java2D drawing for all {@link WorkflowAnnotation}s of the currently
 * displayed process.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class AnnotationDrawer {

	/** the color used to highlight valid drag targets */
	private static final Color DRAG_LINK_COLOR = ProcessDrawer.OPERATOR_BORDER_COLOR_SELECTED;

	/** the color used to indicate invalid drag targets */
	private static final Color GRAY_OUT = new Color(255, 255, 255, 100);

	/** the stroke which is used to highlight drag targets */
	private static final Stroke DRAG_BORDER_STROKE = new BasicStroke(2f);

	/** the editor pane which displays all annotations */
	private final JEditorPane pane;

	/** the model instance */
	private final AnnotationsModel model;

	/** the process renderer model instance */
	private final ProcessRendererModel rendererModel;

	/** this map caches images for workflow annotations for faster drawing */
	private final Map<UUID, WeakReference<Image>> displayCache;

	/**
	 * this map stores an id of the cached image for a workflow annotation to identify old images
	 */
	private final Map<UUID, Integer> cachedID;

	/**
	 * Creates a new drawer for the specified model and decorator.
	 *
	 * @param model
	 *            the model containing all relevant drawing data
	 * @param rendererModel
	 *            the process renderer model
	 */
	public AnnotationDrawer(final AnnotationsModel model, final ProcessRendererModel rendererModel) {
		this.model = model;
		this.rendererModel = rendererModel;

		this.displayCache = new HashMap<>();
		this.cachedID = new HashMap<>();

		pane = new JEditorPane("text/html", "");
		pane.setBorder(null);
		pane.setOpaque(false);
		pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
	}

	/**
	 * Draws the given annotation.
	 *
	 * @param anno
	 *            the annotation to draw
	 * @param g2
	 *            the graphics context to draw upon
	 * @param printing
	 *            if {@code true} we are printing instead of drawing to the screen
	 */
	public void drawAnnotation(final WorkflowAnnotation anno, final Graphics2D g2, final boolean printing) {
		Rectangle2D loc = anno.getLocation();
		AnnotationColor col = anno.getStyle().getAnnotationColor();

		// skip if not in current clip
		if (g2.getClip() != null && !g2.getClip().intersects(anno.getLocation())) {
			return;
		}

		// draw drag indicators if needed
		if (model.getDragged() != null && model.getDragged().getDraggedAnnotation().equals(anno)) {
			drawAnnoDragIndicators(g2, printing);
			shadowOperatorsWhileDragging(g2, anno, printing);
		}

		// background coloring because editor image is transparent
		if (anno.equals(model.getHovered()) && !isProcessInteractionHappening(rendererModel)) {
			g2.setColor(col.getColorHighlight());
		} else {
			g2.setColor(col.getColor());
		}
		if (anno.equals(model.getSelected())) {
			// during resize and drag make background more transparent to improve usability
			if (model.getDragged() != null && model.getDragged().isDragInProgress()
					|| model.getResized() != null && model.getResized().isResizeInProgress()) {
				g2.setColor(col.getColorTransparent());
			} else {
				g2.setColor(col.getColorHighlight());
			}
		}

		// draw background
		g2.fillRect((int) loc.getX(), (int) loc.getY(), (int) loc.getWidth(), (int) loc.getHeight());

		// draw text by drawing image of JEditorPane
		// check if we can paint annotation from cache
		int cacheId = createCacheId(anno);
		WeakReference<Image> cachedImgRef = displayCache.get(anno.getId());
		Image cachedImage = cachedImgRef != null ? cachedImgRef.get() : null;
		if (cachedID.get(anno.getId()) == null || cachedID.get(anno.getId()) != cacheId || cachedImage == null) {
			// not in cache/not up to date, refresh cache
			cachedImage = cacheAnnotationImage(anno, cacheId);
		}
		// if printing, use slow but high-quality rendering (supporting SVG)
		if (printing) {
			printAnnotationFromEditor(anno, g2);
		} else {
			// not printing, use fast image cache
			g2.drawImage(cachedImage, (int) loc.getX(), (int) loc.getY(), (int) loc.getWidth(), (int) loc.getHeight(), null);
		}
		cachedImage = null;

		// border of the annotation
		if (anno.equals(model.getSelected())) {
			// actual border
			Stroke prevStroke = g2.getStroke();
			if (model.getDragged() != null && model.getDragged().getHoveredOperator() != null) {
				g2.setColor(DRAG_LINK_COLOR);
				g2.setStroke(DRAG_BORDER_STROKE);
			} else {
				g2.setColor(ProcessDrawer.OPERATOR_BORDER_COLOR_SELECTED);
			}
			g2.drawRect((int) loc.getX(), (int) loc.getY(), (int) loc.getWidth(), (int) loc.getHeight() - 1);
			g2.setStroke(prevStroke);

			// resize indicators either if process anno or if drag in progress but not over target
			if (anno instanceof ProcessAnnotation || model.getDragged() != null
					&& model.getDragged().getHoveredOperator() == null && model.getDragged().isUnsnapped()) {
				drawProcessAnnoResizeIndicators(g2, loc, printing);
			}
		} else if (anno.equals(model.getHovered())) {
			if (!isProcessInteractionHappening(rendererModel)) {
				// for transparent color, draw hover border
				if (anno.getStyle().getAnnotationColor() == AnnotationColor.TRANSPARENT) {
					g2.setColor(Color.LIGHT_GRAY);
					g2.drawRect((int) loc.getX(), (int) loc.getY(), (int) loc.getWidth() - 1, (int) loc.getHeight() - 1);
				}
			}
		}

		// overflow indicator if needed
		if (anno.isOverflowing() && model.getResized() == null) {
			drawOverflowIndicator(g2, loc, printing);
		}

		// shadow this annotation if another one is dragged and this one is attached to an operator
		if (model.getDragged() != null && anno instanceof OperatorAnnotation
				&& !model.getDragged().getDraggedAnnotation().equals(anno) && model.getDragged().isDragInProgress()
				&& model.getDragged().isUnsnapped()) {
			overshadowRect(loc, g2);
		}

		// mouse cursor update
		if (rendererModel.getHoveringOperator() == null && rendererModel.getHoveringPort() == null) {
			if (RapidMiner.getExecutionMode() == RapidMiner.ExecutionMode.UI) {
				if (model.getHovered() != null && model.getHoveredResizeDirection() == null && model.getHoveredHyperLink() != null) {
					RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				} else {
					RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().setCursor(Cursor.getDefaultCursor());
				}
			}
		}
	}

	/**
	 * Resets the drawer and its caches.
	 */
	public void reset() {
		displayCache.clear();
		cachedID.clear();
	}

	/**
	 * Draws the resize indicators for {@link ProcessAnnotation}s.
	 *
	 * @param g
	 *            the graphics context to draw upon
	 * @param loc
	 *            the location of the annotation
	 * @param printing
	 *            if we are currently printing
	 */
	private void drawProcessAnnoResizeIndicators(final Graphics2D g, final Rectangle2D loc, final boolean printing) {
		if (printing) {
			// never draw them for printing
			return;
		}
		if (model.getDragged() != null && model.getDragged().getHoveredOperator() != null) {
			// don't draw them while hovering over an operator to indicate snap
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();

		Line2D line;
		int offset = 3;
		int startValue = 5;
		int distance = 4;
		int indicatorOffsetXMax = 15;
		int indicatorOffsetXMin = 5;
		int indicatorOffsetYMax = 15;
		int indicatorOffsetYMin = 5;
		AnnotationResizeHelper resized = model.getResized();
		ResizeDirection resizeDirection = model.getHoveredResizeDirection();
		// top right
		if (resizeDirection == ResizeDirection.TOP_RIGHT
				|| resized != null && resized.getDirection() == ResizeDirection.TOP_RIGHT) {
			g2.setColor(Color.BLACK);
			line = new Line2D.Double(loc.getMaxX() - offset, loc.getY() + startValue + distance * 2,
					loc.getMaxX() - (startValue + distance * 2), loc.getY() + offset);
			g2.draw(line);
			line = new Line2D.Double(loc.getMaxX() - offset, loc.getY() + startValue + distance,
					loc.getMaxX() - (startValue + distance), loc.getY() + offset);
			g2.draw(line);
			line = new Line2D.Double(loc.getMaxX() - offset, loc.getY() + startValue, loc.getMaxX() - startValue,
					loc.getY() + offset);
			g2.draw(line);
		} else if (resized == null) {
			g2.setColor(Color.GRAY);
			line = new Line2D.Double(loc.getMaxX() - indicatorOffsetXMax, loc.getY() + indicatorOffsetYMin - 1,
					loc.getMaxX() - indicatorOffsetXMin, loc.getY() + indicatorOffsetYMin - 1);
			g2.draw(line);
			line = new Line2D.Double(loc.getMaxX() - indicatorOffsetXMin, loc.getY() + indicatorOffsetYMin - 1,
					loc.getMaxX() - indicatorOffsetXMin, loc.getY() + indicatorOffsetYMax - 1);
			g2.draw(line);
		}
		// bottom right
		if (resizeDirection == ResizeDirection.BOTTOM_RIGHT
				|| resized != null && resized.getDirection() == ResizeDirection.BOTTOM_RIGHT) {
			g2.setColor(Color.BLACK);
			line = new Line2D.Double(loc.getMaxX() - offset, loc.getMaxY() - (startValue + distance * 2),
					loc.getMaxX() - (startValue + distance * 2), loc.getMaxY() - offset);
			g2.draw(line);
			line = new Line2D.Double(loc.getMaxX() - offset, loc.getMaxY() - (startValue + distance),
					loc.getMaxX() - (startValue + distance), loc.getMaxY() - offset);
			g2.draw(line);
			line = new Line2D.Double(loc.getMaxX() - offset, loc.getMaxY() - startValue, loc.getMaxX() - startValue,
					loc.getMaxY() - offset);
			g2.draw(line);
		} else if (resized == null) {
			g2.setColor(Color.GRAY);
			line = new Line2D.Double(loc.getMaxX() - indicatorOffsetXMax, loc.getMaxY() - indicatorOffsetYMin,
					loc.getMaxX() - indicatorOffsetXMin, loc.getMaxY() - indicatorOffsetYMin);
			g2.draw(line);
			line = new Line2D.Double(loc.getMaxX() - indicatorOffsetXMin, loc.getMaxY() - indicatorOffsetYMin,
					loc.getMaxX() - indicatorOffsetXMin, loc.getMaxY() - indicatorOffsetYMax);
			g2.draw(line);
		}
		// bottom left
		if (resizeDirection == ResizeDirection.BOTTOM_LEFT
				|| resized != null && resized.getDirection() == ResizeDirection.BOTTOM_LEFT) {
			g2.setColor(Color.BLACK);
			line = new Line2D.Double(loc.getX() + offset, loc.getMaxY() - (startValue + distance * 2),
					loc.getX() + startValue + distance * 2, loc.getMaxY() - offset);
			g2.draw(line);
			line = new Line2D.Double(loc.getX() + offset, loc.getMaxY() - (startValue + distance),
					loc.getX() + startValue + distance, loc.getMaxY() - offset);
			g2.draw(line);
			line = new Line2D.Double(loc.getX() + offset, loc.getMaxY() - startValue, loc.getX() + startValue,
					loc.getMaxY() - offset);
			g2.draw(line);
		} else if (resized == null) {
			g2.setColor(Color.GRAY);
			line = new Line2D.Double(loc.getX() + indicatorOffsetXMax - 1, loc.getMaxY() - indicatorOffsetYMin,
					loc.getX() + indicatorOffsetXMin - 1, loc.getMaxY() - indicatorOffsetYMin);
			g2.draw(line);
			line = new Line2D.Double(loc.getX() + indicatorOffsetXMin - 1, loc.getMaxY() - indicatorOffsetYMin,
					loc.getX() + indicatorOffsetXMin - 1, loc.getMaxY() - indicatorOffsetYMax);
			g2.draw(line);
		}
		// top left
		if (resizeDirection == ResizeDirection.TOP_LEFT
				|| resized != null && resized.getDirection() == ResizeDirection.TOP_LEFT) {
			g2.setColor(Color.BLACK);
			line = new Line2D.Double(loc.getX() + offset, loc.getY() + startValue + distance * 2,
					loc.getX() + startValue + distance * 2, loc.getY() + offset);
			g2.draw(line);
			line = new Line2D.Double(loc.getX() + offset, loc.getY() + startValue + distance,
					loc.getX() + startValue + distance, loc.getY() + offset);
			g2.draw(line);
			line = new Line2D.Double(loc.getX() + offset, loc.getY() + startValue, loc.getX() + startValue,
					loc.getY() + offset);
			g2.draw(line);
		} else if (resized == null) {
			g2.setColor(Color.GRAY);
			line = new Line2D.Double(loc.getX() + indicatorOffsetXMax - 1, loc.getY() + indicatorOffsetYMin - 1,
					loc.getX() + indicatorOffsetXMin - 1, loc.getY() + indicatorOffsetYMin - 1);
			g2.draw(line);
			line = new Line2D.Double(loc.getX() + indicatorOffsetXMin - 1, loc.getY() + indicatorOffsetYMin - 1,
					loc.getX() + indicatorOffsetXMin - 1, loc.getY() + indicatorOffsetYMax - 1);
			g2.draw(line);
		}

		g2.dispose();
	}

	/**
	 * Draws the drag indicators for annotations.
	 *
	 * @param g
	 *            the graphics context to draw upon
	 * @param printing
	 *            if we are currently printing
	 */
	private void drawAnnoDragIndicators(final Graphics2D g, final boolean printing) {
		if (printing) {
			// never draw them for printing
			return;
		}

		AnnotationDragHelper dragged = model.getDragged();
		if (dragged.getHoveredOperator() == null) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();

		int padding = 15;
		Rectangle2D opRect = rendererModel.getOperatorRect(dragged.getHoveredOperator());
		opRect = new Rectangle2D.Double(opRect.getX(), opRect.getY(), opRect.getWidth(), opRect.getHeight());
		Rectangle2D shadowRect = new Rectangle2D.Double(opRect.getX() - padding - 1, opRect.getY() - padding - 1,
				opRect.getWidth() + 2 * padding + 1, opRect.getHeight() + 2 * padding + 1);
		g2.setColor(DRAG_LINK_COLOR);

		g2.setStroke(DRAG_BORDER_STROKE);
		g2.draw(shadowRect);

		g2.dispose();
	}

	/**
	 * Shadow non valid drop targets while dragging annotations around.
	 *
	 * @param g
	 *            the graphics context to draw upon
	 * @param anno
	 *            the current annotation to draw
	 * @param printing
	 *            if we are currently printing
	 */
	private void shadowOperatorsWhileDragging(final Graphics2D g, final WorkflowAnnotation anno, final boolean printing) {
		if (printing) {
			// never draw them for printing
			return;
		}

		AnnotationDragHelper dragged = model.getDragged();
		// only shadow if we are actually dragging and the operator annotation is unsnapped
		if (!dragged.isUnsnapped() || !dragged.isDragInProgress()) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();

		// shadow operators which are not a valid drop target
		for (Operator op : anno.getProcess().getOperators()) {
			if (anno instanceof OperatorAnnotation) {
				if (op.equals(((OperatorAnnotation) anno).getAttachedTo())) {
					continue;
				}
			}
			WorkflowAnnotations annotations = rendererModel.getOperatorAnnotations(op);
			if (annotations != null && !annotations.isEmpty()) {
				overshadowRect(rendererModel.getOperatorRect(op), g2);
			}
		}

		g2.dispose();
	}

	/**
	 * Shadows the given rectangle. Gives a disabled look to the given area.
	 *
	 * @param rect
	 *            the area to draw the shadow over
	 * @param g
	 *            the context to draw upon
	 */
	private void overshadowRect(final Rectangle2D rect, final Graphics2D g) {
		Graphics2D g2 = (Graphics2D) g.create();

		g2.setColor(GRAY_OUT);
		g2.fill(rect);

		g2.dispose();
	}

	/**
	 * Draws indicator in case the annotation text overflows on the y axis.
	 *
	 * @param g
	 *            the graphics context to draw upon
	 * @param loc
	 *            the location of the annotation
	 * @param printing
	 *            if we are currently printing
	 */
	private void drawOverflowIndicator(final Graphics2D g, final Rectangle2D loc, final boolean printing) {
		if (printing) {
			// never draw them for printing
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();

		int size = 20;
		int xOffset = 10;
		int yOffset = 10;
		int stepSize = size / 4;
		int dotSize = 3;
		int x = (int) loc.getMaxX() - size - xOffset;
		int y = (int) loc.getMaxY() - size - yOffset;
		GradientPaint gp = new GradientPaint(x, y, Color.WHITE, x, y + size * 1.5f, Color.LIGHT_GRAY);
		g2.setPaint(gp);
		g2.fillRect(x, y, size, size);

		g2.setColor(Color.BLACK);
		g2.drawRect(x, y, size, size);

		g2.fillOval(x + stepSize, y + stepSize * 2, dotSize, dotSize);
		g2.fillOval(x + stepSize * 2, y + stepSize * 2, dotSize, dotSize);
		g2.fillOval(x + stepSize * 3, y + stepSize * 2, dotSize, dotSize);

		g2.dispose();
	}

	/**
	 * Creates an image of the given annotation and caches it with the specified cache id.
	 *
	 * @param anno
	 *            the annotation to cache
	 * @param cacheId
	 *            the cache id for the given annotation
	 * @return the cached image
	 */
	private Image cacheAnnotationImage(final WorkflowAnnotation anno, final int cacheId) {
		Rectangle2D loc = anno.getLocation();
		// paint each annotation with the same JEditorPane
		Dimension size = new Dimension((int) Math.round(loc.getWidth() * rendererModel.getZoomFactor()), (int) Math.round(loc.getHeight() * rendererModel.getZoomFactor()));
		pane.setSize(size);
		float originalSize = AnnotationDrawUtils.ANNOTATION_FONT.getSize();
		// without this, scaling is off even more when zooming out..
		if (rendererModel.getZoomFactor() < 1.0d) {
			originalSize -= 1f;
		}
		float fontSize = (float) (originalSize * rendererModel.getZoomFactor());
		Font annotationFont = AnnotationDrawUtils.ANNOTATION_FONT.deriveFont(fontSize);
		pane.setFont(annotationFont);
		pane.setText(AnnotationDrawUtils.createStyledCommentString(anno));
		pane.setCaretPosition(0);

		// while caching, update the hyperlink bounds visible in this annotation
		HTMLDocument htmlDocument = (HTMLDocument) pane.getDocument();
		HTMLDocument.Iterator linkIterator = htmlDocument.getIterator(HTML.Tag.A);
		List<Pair<String, Rectangle>> hyperlinkBounds = new LinkedList<>();
		while (linkIterator.isValid()) {
			AttributeSet attributes = linkIterator.getAttributes();
			String url = (String) attributes.getAttribute(HTML.Attribute.HREF);
			int startOffset = linkIterator.getStartOffset();
			int endOffset = linkIterator.getEndOffset();
			try {
				// rectangle for leftmost character
				Rectangle rectangleLeft = pane.getUI().modelToView(pane, startOffset, Position.Bias.Forward);
				// rectangle for rightmost character
				Rectangle rectangleRight = pane.getUI().modelToView(pane, endOffset, Position.Bias.Backward);
				// merge both rectangles to get full bounds of hyperlink
				// also remove the zoom factor to not get distorted bounds when zoomed in/out
				int x = (int) (rectangleLeft.getX() / rendererModel.getZoomFactor());
				int y = (int) (rectangleLeft.getY() / rendererModel.getZoomFactor());
				int w = (int) ((rectangleRight.getX() - rectangleLeft.getX() + rectangleRight.getWidth()) / rendererModel.getZoomFactor());
				int h = (int) ((rectangleRight.getY() - rectangleLeft.getY() + rectangleRight.getHeight()) / rendererModel.getZoomFactor());
				hyperlinkBounds.add(new Pair<>(url, new Rectangle(x, y, w, h)));
			} catch (BadLocationException e) {
				// silently ignored because at worst you cannot click a hyperlink, nothing to spam the log with
			}
			linkIterator.next();
		}
		model.setHyperlinkBoundsForAnnotation(anno.getId(), hyperlinkBounds);

		// draw annotation area to image and then to graphics
		// otherwise heavyweight JEdiorPane draws over everything and outside of panel
		BufferedImage img = new BufferedImage((int) (loc.getWidth() * rendererModel.getZoomFactor()), (int) (loc.getHeight() * rendererModel.getZoomFactor()), BufferedImage.TYPE_INT_ARGB);
		Graphics2D gImg = img.createGraphics();
		gImg.setRenderingHints(ProcessDrawer.HI_QUALITY_HINTS);
		// without this, the text is pixelated on half opaque backgrounds
		gImg.setComposite(AlphaComposite.SrcOver);
		// paint JEditorPane to image
		pane.paint(gImg);
		displayCache.put(anno.getId(), new WeakReference<>(img));
		cachedID.put(anno.getId(), cacheId);

		return img;
	}

	/**
	 * Bypass the cache and the speedy image drawing and directly paint the JEditorPane to the
	 * context. Required for printing in SVG format which would turn out pixelated if it were drawn
	 * as an image.
	 *
	 * @param anno
	 *            the annotation to draw
	 * @param g2
	 *            the graphics context to draw upon
	 */
	private void printAnnotationFromEditor(final WorkflowAnnotation anno, final Graphics2D g2) {
		Graphics2D gPr = (Graphics2D) g2.create();

		Rectangle2D loc = anno.getLocation();
		Dimension size = new Dimension((int) loc.getWidth(), (int) loc.getHeight());
		gPr.translate(loc.getX(), loc.getY());
		gPr.setClip(0, 0, (int) size.getWidth(), (int) size.getHeight());
		// paint each annotation with the same JEditorPane
		pane.setSize(size);
		pane.setFont(AnnotationDrawUtils.ANNOTATION_FONT);
		pane.setText(AnnotationDrawUtils.createStyledCommentString(anno));
		pane.setCaretPosition(0);
		// draw annotation area to image and then to graphics
		// otherwise heavyweight JEdiorPane draws over everything and outside of panel
		// paint JEditorPane to context
		pane.paint(gPr);

		gPr.dispose();
	}

	/**
	 * Creates a unique id for a {@link WorkflowAnnotation}, but does <strong>not</strong> take x/y
	 * coordinates into account. Reason is that a cached image can still be used if the x/y
	 * coordinates have changed.
	 *
	 * @param anno
	 *            the annotation for which to calculate the cache id
	 * @return a unique id identifying an annotation
	 */
	private Integer createCacheId(final WorkflowAnnotation anno) {
		final int prime = 31;
		int result = 1;
		result = prime * result + (anno.getComment() == null ? 0 : anno.getComment().hashCode());
		result = prime * result + (anno.getLocation() == null ? 0 : new Double(anno.getLocation().getWidth()).hashCode());
		result = prime * result + (anno.getLocation() == null ? 0 : new Double(anno.getLocation().getHeight()).hashCode());
		result = prime * result + (anno.getStyle() == null ? 0 : anno.getStyle().hashCode());
		result = prime * result + (anno.wasResized() ? 1231 : 1237);
		return result;
	}

	/**
	 * Checks whether some process interaction (hovering over operators/ports/dragging/connecting
	 * ports) is going on.
	 *
	 * @param rendererModel
	 *            the process renderer model instance
	 * @return {@code true} if some process interaction is happening; {@code false} otherwise
	 */
	public static boolean isProcessInteractionHappening(final ProcessRendererModel rendererModel) {
		if (rendererModel == null) {
			throw new IllegalArgumentException("rendererModel must not be null!");
		}
		return !(rendererModel.getHoveringOperator() == null && rendererModel.getHoveringPort() == null
				&& rendererModel.getHoveringConnectionSource() == null && !rendererModel.isDragStarted()
				&& rendererModel.getConnectingPortSource() == null);
	}
}
