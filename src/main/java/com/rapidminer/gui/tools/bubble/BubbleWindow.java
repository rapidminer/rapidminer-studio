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
package com.rapidminer.gui.tools.bubble;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.PerspectiveChangeListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.DockingDesktop;
import com.vlsolutions.swing.docking.event.DockableSelectionEvent;
import com.vlsolutions.swing.docking.event.DockableSelectionListener;
import com.vlsolutions.swing.docking.event.DockableStateChangeEvent;
import com.vlsolutions.swing.docking.event.DockableStateChangeListener;
import com.vlsolutions.swing.docking.event.DockingActionEvent;
import com.vlsolutions.swing.docking.event.DockingActionListener;


/**
 * This class creates a speech bubble-shaped JDialog, which can be attached to Buttons, Dockables or
 * Operators, either by using its ID. The bubble triggers two events which are obserable by the
 * {@link BubbleListener}; either if the close button was clicked, or if the corresponding button
 * was used.
 * <p>
 * To create instances, subclasses are encouraged to extend {@link BubbleWindowBuilder} to handle
 * bubble creation without exposing massive constructors or long constructor chains.
 * </p>
 *
 * @author Philipp Kersting and Thilo Kamradt
 */
public abstract class BubbleWindow extends JDialog {

	/**
	 * Abstract builder for {@link BubbleWindow} implementations. After calling all relevant
	 * setters, call {@link #build()} to create the actual bubble instance.
	 *
	 * @author Marco Boeck
	 * @since 6.5.0
	 *
	 */
	protected static abstract class BubbleWindowBuilder<T extends BubbleWindow, U extends BubbleWindowBuilder<T, U>> {

		/** the owner window of the bubble, used for z-ordering by Swing. Can be {@code null} */
		protected Window owner;

		/**
		 * tthe i18n key for the bubble. Format: {@code gui.bubble.[i18nkey].title} and
		 * {@code gui.bubble.[i18nkey].body}
		 */
		protected String i18nKey;

		/** optional i18n arguments */
		protected Object[] arguments;

		/** the dockable to target of the bubble resides in. Can be {@code null} */
		protected String dockKey;

		/** the style of the bubble, never {@code null} */
		protected BubbleStyle style;

		/** the preferred relative position next to the target, never {@code null} */
		protected AlignedSide alignment;

		/** the font for the title, can be {@code null} */
		protected Font titleFont;

		/** the font for the body and all additional components, can be {@code null} */
		protected Font bodyFont;

		/** whether the bubble window can be dragged around by the user */
		protected boolean moveable;

		/** whether the bubble should have a close button in the top right corner */
		protected boolean showCloseButton;

		/**
		 * additional components which are added to the bottom of the bubble in a horizontal row.
		 * Can be {@code null}
		 */
		protected JComponent[] componentsToAdd;

		/**
		 * Creates a new builder for {@link BubbleWindow} implementations. Extend and overwrite
		 * {@link #build()} for specific implementations.
		 *
		 * @param owner
		 *            the parent window for the bubble
		 * @param i18nKey
		 *            the i18n key for the bubble. Format: {@code gui.bubble.[i18nkey].title} and
		 *            {@code gui.bubble.[i18nkey].body}.
		 * @param arguments
		 *            optional i18n arguments
		 */
		public BubbleWindowBuilder(final Window owner, final String i18nKey, final Object... arguments) {
			if (i18nKey == null) {
				throw new IllegalArgumentException("i18nKey must not be null!");
			}

			this.owner = owner;
			this.i18nKey = i18nKey;
			this.arguments = arguments;

			// default values
			this.style = BubbleStyle.COMIC;
			this.alignment = AlignedSide.BOTTOM;
			this.moveable = true;
			this.showCloseButton = true;
		}

		/**
		 * Set the style of the bubble. See {@link BubbleStyle} for more information. Default is
		 * {@link BubbleStyle#COMIC}.
		 *
		 * @param style
		 *            the style the bubble should have
		 * @return the builder instance
		 */
		public U setStyle(final BubbleStyle style) {
			if (style == null) {
				throw new IllegalArgumentException("style must not be null!");
			}
			this.style = style;
			return getThis();
		}

		/**
		 * Sets the preferred side where the bubble should be positioned relative to the target. If
		 * the defined position is not possible, it will be changed automatically. Defaults to
		 * {@link AlignedSide#BOTTOM}.
		 *
		 * @param alignment
		 *            the side relative to the target where the bubble should appear
		 * @return the builder instance
		 */
		public U setAlignment(final AlignedSide alignment) {
			if (alignment == null) {
				throw new IllegalArgumentException("alignment must not be null!");
			}
			this.alignment = alignment;
			return getThis();
		}

		/**
		 * Sets whether the bubble can be moved by dragging the title. Defaults to {@code true}.
		 *
		 * @param moveable
		 *            if {@code true} the user can drag the bubble around via the title
		 * @return the builder instance
		 */
		public U setMoveable(final boolean moveable) {
			this.moveable = moveable;
			return getThis();
		}

		/**
		 * Sets the key of the {@link Dockable} the target component is in. This is useful if the
		 * dockable is moved/removed by the user so the bubble can react automatically.
		 *
		 * @param dockKey
		 *            the key of the dockable
		 * @return the builder instance
		 */
		public U setDockableOfTargetComponent(final String dockKey) {
			if (dockKey == null) {
				throw new IllegalArgumentException("dockKey must not be null!");
			}
			this.dockKey = dockKey;
			return getThis();
		}

		/**
		 * Sets the font for the title text of the bubble. If not set, defaults to a font matching
		 * the specified {@link BubbleStyle}.
		 *
		 * @param font
		 *            the font which should be used for the title
		 * @return the builder instance
		 */
		public U setTitleFont(final Font font) {
			if (font == null) {
				throw new IllegalArgumentException("font must not be null!");
			}
			this.titleFont = font;
			return getThis();
		}

		/**
		 * Sets the font for the body text of the bubble. If not set, defaults to a font matching
		 * the specified {@link BubbleStyle}.
		 *
		 * @param font
		 *            the font which should be used for the body
		 * @return the builder instance
		 */
		public U setBodyFont(final Font font) {
			if (font == null) {
				throw new IllegalArgumentException("font must not be null!");
			}
			this.bodyFont = font;
			return getThis();
		}

		/**
		 * Sets additional components which are added to the bottom of the bubble in a horizontal
		 * row. By default, nothing is visible below the body text and the bubble can only be closed
		 * via the 'x' button in the top right corner.
		 *
		 * @param componentsToAdd
		 *            the components to add. Usually {@link JButton}s or similar
		 * @return the builder instance
		 */
		public U setAdditionalComponents(final JComponent[] componentsToAdd) {
			if (componentsToAdd == null) {
				throw new IllegalArgumentException("componentsToAdd must not be null!");
			}
			this.componentsToAdd = componentsToAdd;
			return getThis();
		}

		/**
		 * Hides the close button in the top right corner. Note that if it is hidden and no
		 * additional components have been added, there is no way to close the bubble.
		 *
		 * @return the builder instance
		 */
		public U hideCloseButton() {
			this.showCloseButton = false;
			return getThis();
		}

		/**
		 * Creates the {@link BubbleWindow} implementation instance according to the specified
		 * settings.
		 *
		 * @return the bubble instance, never {@code null}
		 */
		public abstract T build();

		/**
		 * Returns the implementation instance of the builder. Needed for chaining of setters in
		 * abstract class.
		 *
		 * @return the actual builder implementation, never {@code null}
		 */
		public abstract U getThis();
	}

	/**
	 * The possible styles of a {@link BubbleWindow}.
	 *
	 * @since 6.5.0
	 *
	 */
	public static enum BubbleStyle {
		/** a bubble with a green style indicating everything is fine */
		OK(FONT_TITLE, FONT_BODY, ICON_OK, new Color(173, 237, 200)),

		/** a bubble with a blue style indicating something if interest */
		INFORMATION(FONT_TITLE, FONT_BODY, ICON_INFORMATION, new Color(177, 215, 241)),

		/** a bubble with a yellow style indicating a warning */
		WARNING(FONT_TITLE, FONT_BODY, ICON_WARNING, new Color(250, 217, 164)),

		/** a bubble with a red style indicating a severe problem */
		ERROR(FONT_TITLE, FONT_BODY, ICON_ERROR, new Color(245, 187, 181)),

		/** a bubble with a comic style which is used by the comic tutorials */
		COMIC(FONT_TITLE_COMIC, FONT_BODY_COMIC, null, COLOR_BACKGROUND_COMIC);

		private final Font titleFont;
		private final Font bodyFont;
		private final ImageIcon icon;
		private final Color color;

		private BubbleStyle(final Font titleFont, final Font bodyFont, final ImageIcon icon, final Color color) {
			this.titleFont = titleFont;
			this.bodyFont = bodyFont;
			this.icon = icon;
			this.color = color;
		}

		/**
		 * @return the title font, never {@code null}
		 */
		public Font getTitleFont() {
			return titleFont;
		}

		/**
		 * @return the title font, never {@code null}
		 */
		public Font getBodyFont() {
			return bodyFont;
		}

		/**
		 * @return the icon for this style or {@code null} if it has none
		 */
		public ImageIcon getIcon() {
			return icon;
		}

		/**
		 * @return the bubble color, never {@code null}
		 */
		public Color getColor() {
			return color;
		}
	}

	/**
	 * Indicates on which side of the target component the Bubble will be positioned.
	 *
	 */
	public static enum AlignedSide {
		/**
		 * to the right of the component
		 */
		RIGHT,

		/**
		 * to the left of the component
		 */
		LEFT,

		/**
		 * above the component
		 */
		TOP,

		/**
		 * below the component
		 */
		BOTTOM,

		/**
		 * in the middle of the component
		 */
		MIDDLE
	}

	/**
	 * The listener which can be registered to be notified of buble events. See
	 * {@link BubbleWindow#addBubbleListener(BubbleListener)}.
	 *
	 */
	public static interface BubbleListener {

		/**
		 * Called when the bubble has been closed.
		 *
		 * @param bw
		 *            the origin of the event
		 */
		public void bubbleClosed(BubbleWindow bw);

		/**
		 * Called when {@link BubbleWindow#triggerFire()} is called.
		 *
		 * @param bw
		 *            the origin of the event
		 */
		public void actionPerformed(BubbleWindow bw);
	}

	/**
	 * Used to define the position of the pointer of the bubble, aka the corner which points to the
	 * component.
	 */
	protected enum Alignment {
		TOPLEFT,

		TOPRIGHT,

		BOTTOMLEFT,

		BOTTOMRIGHT,

		LEFTTOP,

		LEFTBOTTOM,

		RIGHTTOP,

		RIGHTBOTTOM,

		INNERRIGHT,

		INNERLEFT,

		/**
		 * bubble is placed inside the component, no pointer.
		 */
		MIDDLE;
	}

	/**
	 * Used to determine the type of a possible assistant bubble.
	 */
	protected enum AssistantType {
		/**
		 * the dockable to which the bubble should be attached is not in the selected tab in a
		 * multi-tab environment
		 */
		NOT_SHOWING,

		/**
		 * the dockable to which the bubble should be attached is not on the screen
		 */
		NOT_ON_SCREEN,

		/**
		 * user looks at wrong operator chain (subprocess)
		 */
		NOT_IN_CHAIN,

		/**
		 * no assistent currently active
		 */
		NO_ASSISTANT_ACTIVE,

		/**
		 * when the dockable of the target becomes hidden
		 */
		HIDDEN,

		/**
		 * when the user is in the wrong perspective
		 */
		WRONG_PERSPECTIVE,
	}

	/**
	 * Responsible for allowing to drag the bubble around by the user.
	 *
	 */
	private class MoveListener extends MouseInputAdapter {

		private Component comp;
		private Point startLoc;
		private Point lastLoc;

		private MoveListener(Component comp) {
			this.comp = comp;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			startLoc = e.getLocationOnScreen();
			lastLoc = comp.getLocation();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			int x = startLoc.x;
			int y = startLoc.y;
			int xOffset = e.getXOnScreen() - x;
			int yOffset = e.getYOnScreen() - y;

			Point newLoc = new Point(lastLoc.x + xOffset, lastLoc.y + yOffset);
			comp.setLocation(newLoc);
		}
	}

	private static final long serialVersionUID = -7508372660983304065L;

	private static final Font FONT_TITLE = FontTools.getFont("Open Sans Light", Font.PLAIN, 18);
	private static final Font FONT_BODY = FontTools.getFont("Open Sans", Font.PLAIN, 13);
	private static final Font FONT_TITLE_COMIC = FontTools.getFont("AlterEgoBB", Font.PLAIN, 14)
			.deriveFont(Font.BOLD);
	private static final Font FONT_BODY_COMIC = FontTools.getFont("AlterEgoBB", Font.PLAIN, 13);

	private static final Color COLOR_BACKGROUND_COMIC = new Color(249, 200, 127);
	private static final Color COLOR_TRANSPARENT = new Color(0, 0, 0, 0);

	private static final float BORDER_STROKE_WIDTH = 1.5f;

	private static final ImageIcon ICON_INFORMATION = SwingTools
			.createIcon("flat_icons/white/192/" + I18N.getGUIMessage("gui.bubble.information.icon"));
	private static final ImageIcon ICON_WARNING = SwingTools
			.createIcon("flat_icons/white/192/" + I18N.getGUIMessage("gui.bubble.warning.icon"));
	private static final ImageIcon ICON_ERROR = SwingTools
			.createIcon("flat_icons/white/192/" + I18N.getGUIMessage("gui.bubble.error.icon"));
	private static final ImageIcon ICON_OK = SwingTools
			.createIcon("flat_icons/white/192/" + I18N.getGUIMessage("gui.bubble.ok.icon"));

	private static final String KEY_NOT_ON_SCREEN = "lostDockable";
	private static final String KEY_NOT_SHOWING = "not_showing";
	private static final String KEY_HIDDEN = "hiddenDockable";
	private static final String KEY_PERSPECTIVE = "changePerspective";

	/**
	 * Width of the little pointer triangle attached to the bubble. check this value in case of a
	 * redesign.
	 */
	private static final int BUBBLE_CONNECTOR_HEIGHT = 35;

	/**
	 * Height of the little pointer triangle attached to the bubble. check this value in case of a
	 * redesign.
	 */
	private static final int BUBBLE_CONNECTOR_WIDTH = 46;

	/** Radius of the rounded rectangle. */
	private static final int CORNER_RADIUS = 20;

	/** Fixed width of the bubble. */
	private static final int WINDOW_WIDTH = 225;

	/** Fixed width of the bubble for comics. */
	private static final int WINDOW_WIDTH_COMIC = 200;

	/** high quality rendering hints */
	private static final RenderingHints HI_QUALITY_HINTS = new RenderingHints(null);

	static {
		HI_QUALITY_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		HI_QUALITY_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		HI_QUALITY_HINTS.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
	}

	protected static final int HIDDEN_WIDTH = 19;
	protected static final int HIDDEN_HEIGHT = 200;
	protected static final Point HIDDEN_POS = new Point(1, 24);

	/*
	 * constants to return from the static methods isDockableOnScreen(String) and
	 * isButtonOnSreen(String)
	 */
	public static final int OBJECT_SHOWING_ON_SCREEN = 0;
	public static final int OBJECT_NOT_SHOWING = 1;
	public static final int OBJECT_NOT_ON_SCREEN = -1;

	private final String i18nKey;
	private final Object[] arguments;
	private final Font titleFont;
	private final Font bodyFont;
	private final BubbleStyle style;
	private final AlignedSide preferredAlignment;
	private final List<BubbleListener> listeners = new LinkedList<>();
	private final DockingDesktop desktop = RapidMinerGUI.getMainFrame().getDockingDesktop();

	private boolean built = false;
	private Alignment realAlignment = Alignment.TOPLEFT;
	private GridBagConstraints constraints = null;
	private JPanel bubble;
	private JButton close;
	private ActionListener listener;
	private JLabel headline;
	private JLabel mainText;
	private String docKey = null;
	private Dockable dockable;
	private JComponent[] componentsInBubble;
	private boolean addPerspective = true;
	/** indicates whether the listeners have been added or not */
	private boolean listenersAdded = false;
	private boolean isPerPixelTranslucencySupported;
	private boolean moveable;
	private boolean showCloseButton;

	private PerspectiveChangeListener perspectiveListener;
	private WindowAdapter windowListener;
	private ComponentListener compListener;
	private DockingActionListener dockListener;
	private HierarchyListener hierachyListener;
	private ComponentListener componentListenerToWindow;

	/* assistant Attributes */
	private HierarchyListener assistantHierarchy;
	private BubbleWindow assistantBubble;
	private DockableStateChangeListener assistantDockStateChange;
	private DockableSelectionListener assistantDockSelect;
	private DockingActionListener assistantDockingAction;
	private PerspectiveChangeListener assistantPerspective;
	private AssistantType currentAssistant = AssistantType.NO_ASSISTANT_ACTIVE;

	private volatile boolean killed = false;
	private volatile int dockingCounter = 0;

	/**
	 * the origin perspective where this bubble lives. If initial construction takes place in a
	 * different perspective than where the bubble should live, change this
	 */
	protected String myPerspective;

	/**
	 * creates a BubbleWindow-Object. To paint and repaint this BubbleWindow call paint(boolean
	 * refreshListerns)
	 *
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            key of the message which should be shown.
	 * @param docKey
	 *            key of the Dockable the BubbleWindow will attach to.
	 * @param componentsToAdd
	 *            Array of {@link JComponent}s which will be added to the Bubble (null instead of
	 *            the array won't throw an error).
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public BubbleWindow(final Window owner, final AlignedSide preferredAlignment, final String i18nKey, final String docKey,
			final JComponent[] componentsToAdd, final Object... arguments) {
		this(owner, BubbleStyle.COMIC, preferredAlignment, i18nKey, docKey, null, null, false, true, componentsToAdd,
				arguments);
	}

	/**
	 * Creates an instance with the given parameters. Should be called with the arguments gathered
	 * from {@link BubbleWindowBuilder} implementations.
	 *
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown
	 * @param style
	 *            the style the bubble should have
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable
	 * @param i18nKey
	 *            key of the message which should be shown
	 * @param docKey
	 *            key of the Dockable the BubbleWindow will attach to
	 * @param moveable
	 *            if {@code true} the user can drag the bubble around on screen
	 * @param showCloseButton
	 *            if {@code true} the user can close the bubble via an "x" button in the top right
	 *            corner
	 * @param titleFont
	 *            the font for the title, can be {@code null}
	 * @param bodyFont
	 *            the font for the body, can be {@code null}
	 * @param componentsToAdd
	 *            Array of {@link JComponent}s which will be added to the Bubble (null instead of
	 *            the array won't throw an error)
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	protected BubbleWindow(final Window owner, final BubbleStyle style, final AlignedSide preferredAlignment,
			final String i18nKey, final String docKey, final Font titleFont, final Font bodyFont, final boolean moveable,
			final boolean showCloseButton, final JComponent[] componentsToAdd, final Object... arguments) {
		super(owner);

		// if this check fails, the bubbles will look very ugly, but we can't change that I guess
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		isPerPixelTranslucencySupported = gd
				.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
		if (!isPerPixelTranslucencySupported) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.bubble.BubbleWindow.per_pixel_not_supported");
		}

		this.i18nKey = i18nKey;
		this.arguments = arguments;
		this.myPerspective = getCurrentPerspectiveName();
		this.preferredAlignment = preferredAlignment;
		this.moveable = moveable;
		if (docKey != null) {
			this.docKey = docKey;
			dockable = desktop.getContext().getDockableByKey(docKey);
		}

		this.style = style;
		if (titleFont != null) {
			this.titleFont = titleFont;
		} else {
			this.titleFont = style.getTitleFont();
		}
		if (bodyFont != null) {
			this.bodyFont = bodyFont;
		} else {
			this.bodyFont = style.getBodyFont();
		}
		this.showCloseButton = showCloseButton;
		if (componentsToAdd == null) {
			componentsInBubble = new JComponent[] {};
		} else {
			componentsInBubble = componentsToAdd;
		}
	}

	/**
	 * should be used to update the Bubble. Call this instead of repaint and similar. Update the
	 * Alignment, shape and location. Also this method builds the Bubble by the first call.
	 *
	 * @param reregisterListerns
	 *            indicates whether the the Listeners will be refreshed too or not
	 */
	public void paint(final boolean refreshListerns) {
		if (!built) {
			this.buildBubble();
		}
		this.paintAgain(refreshListerns);
	}

	/**
	 * builds the Bubble for the first time
	 */
	private void buildBubble() {
		built = true;
		this.realAlignment = this.calculateAlignment(this.realAlignment);
		setLayout(new BorderLayout());
		setFocusable(false);
		setFocusableWindowState(false);
		setUndecorated(true);
		if (isPerPixelTranslucencySupported) {
			setBackground(COLOR_TRANSPARENT);
		} else {
			setBackground(Color.WHITE);
		}
		initRegularListener();

		GridBagLayout gbl = new GridBagLayout();
		bubble = new JPanel(gbl) {

			private static final long serialVersionUID = 1L;

			@Override
			public void paintComponent(final Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHints(HI_QUALITY_HINTS);

				Shape shape = createShape(realAlignment, BORDER_STROKE_WIDTH);
				if (style == BubbleStyle.COMIC) {
					g2.setColor(COLOR_BACKGROUND_COMIC);
				} else {
					g2.setColor(style.getColor());
				}
				g2.fill(shape);

				// draw icon in bottom right corner if existing
				ImageIcon icon = style.getIcon();
				if (icon != null) {
					int xRight = (int) shape.getBounds().getMaxX();
					int yBottom = (int) shape.getBounds().getMaxY();
					if (realAlignment == Alignment.BOTTOMLEFT || realAlignment == Alignment.BOTTOMRIGHT) {
						yBottom -= CORNER_RADIUS;
					}
					if (realAlignment == Alignment.RIGHTBOTTOM || realAlignment == Alignment.RIGHTTOP
							|| realAlignment == Alignment.INNERRIGHT) {
						xRight -= CORNER_RADIUS;
					}
					int iconW = icon.getIconWidth();
					int iconH = icon.getIconHeight();
					int x = (int) (xRight - iconW * 0.80f);
					int y = (int) (yBottom - iconH * 0.80f);

					// prevent icon drawing over border via clip
					Graphics2D gImg = (Graphics2D) g2.create();
					Shape previousClip = gImg.getClip();
					Shape iconClipShape = createShape(realAlignment, BORDER_STROKE_WIDTH);
					gImg.setClip(iconClipShape);
					gImg.drawImage(icon.getImage(), x, y, this);

					gImg.setClip(previousClip);
					gImg.dispose();
				}

				g2.setColor(Color.BLACK);
				g2.setStroke(new BasicStroke(BORDER_STROKE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
				g2.draw(shape);

				g2.dispose();
			}

		};
		bubble.setSize(getSize());
		bubble.setOpaque(false);
		bubble.setDoubleBuffered(false);
		add(bubble, BorderLayout.CENTER);

		// headline label
		headline = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.bubble." + i18nKey + ".title"));
		headline.setFont(titleFont);
		int width = style != BubbleStyle.COMIC ? WINDOW_WIDTH : WINDOW_WIDTH_COMIC;
		headline.setMinimumSize(new Dimension(width, 25));
		headline.setPreferredSize(new Dimension(width, 25));
		if (moveable) {
			MoveListener listener = new MoveListener(this);
			headline.addMouseListener(listener);
			headline.addMouseMotionListener(listener);
			headline.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}

		// mainText label
		mainText = new JLabel("<html><div style=\"line-height: 150%;width:" + width + "px \">"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.bubble." + i18nKey + ".body", arguments) + "</div></html>");
		mainText.setFont(this.bodyFont);
		mainText.setMinimumSize(new Dimension(150, 20));
		mainText.setMaximumSize(new Dimension(width, 800));

		// create and add close Button for the Bubble if needed
		close = new JButton("x");
		close.setFont(titleFont.deriveFont(Font.BOLD, titleFont.getSize()));
		close.setBorderPainted(true);
		close.setContentAreaFilled(false);
		final Color hoveredColor = new Color(110, 110, 110);
		final Color notHoveredColor = close.getForeground();
		// change Icons and set close operation
		close.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				BubbleWindow.this.dispose();
				fireEventCloseClicked();
			}
		});
		close.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseExited(final MouseEvent e) {
				close.setForeground(notHoveredColor);
			}

			@Override
			public void mouseEntered(final MouseEvent e) {
				close.setForeground(hoveredColor);
			}
		});
		close.setMargin(new Insets(0, 5, 0, 5));

		// modify components
		for (int i = 0; i < this.componentsInBubble.length; i++) {
			this.componentsInBubble[i].setFont(bodyFont);
			this.componentsInBubble[i].setOpaque(false);
			this.componentsInBubble[i].setFont(bodyFont);
		}

		layoutBubble();
		pack();

		if (this.calculateAlignment(this.realAlignment) == this.realAlignment) {
			positionRelative();
		}
	}

	/**
	 * Adds the components to the bubble.
	 */
	private void layoutBubble() {
		bubble.removeAll();

		constraints = new GridBagConstraints();
		Insets insetsLabel = new Insets(10, 10, 10, 10);
		Insets insetsMainText = new Insets(0, 10, 10, 10);
		int numberAdditionalComponents = this.componentsInBubble.length;
		switch (realAlignment) {
			case TOPLEFT:
				insetsLabel = new Insets(CORNER_RADIUS + 15, 10, 10, 10);
				break;
			case TOPRIGHT:
				insetsLabel = new Insets(CORNER_RADIUS + 15, 10, 10, 10);
				break;
			case INNERLEFT:
			case LEFTTOP:
				insetsLabel = new Insets(10, CORNER_RADIUS + 15, 10, 10);
				insetsMainText = new Insets(0, CORNER_RADIUS + 15, 10, 10);
				break;
			case LEFTBOTTOM:
				insetsLabel = new Insets(10, CORNER_RADIUS + 15, 10, 10);
				insetsMainText = new Insets(0, CORNER_RADIUS + 15, 10, 10);
				break;
			case BOTTOMRIGHT:
				insetsLabel = new Insets(10, 10, 10, 10);
				insetsMainText = new Insets(0, 10, CORNER_RADIUS + 15, 10);
				break;
			case BOTTOMLEFT:
				insetsLabel = new Insets(10, 10, 10, 10);
				insetsMainText = new Insets(0, 10, CORNER_RADIUS + 15, 10);
				break;
			case INNERRIGHT:
			case RIGHTTOP:
				insetsLabel = new Insets(10, 10, 10, CORNER_RADIUS + 15);
				insetsMainText = new Insets(0, 10, 10, CORNER_RADIUS + 15);
				break;
			case RIGHTBOTTOM:
				insetsLabel = new Insets(10, 10, 10, CORNER_RADIUS + 15);
				insetsMainText = new Insets(0, 10, 10, CORNER_RADIUS + 15);
				break;
			// $CASES-OMITTED$
			default:
		}

		// add the headline
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = insetsLabel;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridwidth = numberAdditionalComponents > 0 ? numberAdditionalComponents : 1;
		constraints.weightx = 1;
		constraints.weighty = 0;
		bubble.add(headline, constraints);

		// create and add close Button for the Bubble
		constraints.gridx += numberAdditionalComponents > 0 ? numberAdditionalComponents : 1;
		constraints.weightx = 0;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = insetsLabel;
		constraints.gridwidth = 1;
		if (showCloseButton) {
			bubble.add(close, constraints);
		} else {
			bubble.add(new JLabel(), constraints);
		}

		// add the main Text
		constraints.gridx = 0;
		constraints.gridy += 1;
		constraints.insets = insetsMainText;
		constraints.gridwidth = numberAdditionalComponents > 0 ? numberAdditionalComponents + 1 : 2;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.anchor = GridBagConstraints.WEST;
		bubble.add(mainText, constraints);

		// adding the given Buttons to the Bubble
		int insetsLeft = 10;
		int insetsBottom = 10;
		if (realAlignment == Alignment.LEFTBOTTOM || realAlignment == Alignment.LEFTTOP
				|| realAlignment == Alignment.INNERLEFT) {
			insetsLeft += CORNER_RADIUS;
		}
		if (realAlignment == Alignment.BOTTOMLEFT || realAlignment == Alignment.BOTTOMRIGHT) {
			insetsBottom += CORNER_RADIUS;
		}

		constraints.gridx = 0;
		constraints.gridy += 1;
		constraints.insets = new Insets(10, insetsLeft, insetsBottom, 0);
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridwidth = 1;
		constraints.weightx = 0;
		constraints.weighty = 0;
		for (int i = 0; i < this.componentsInBubble.length; i++) {
			// add button to bubble
			bubble.add(this.componentsInBubble[i], constraints);
			constraints.gridx += 1;
		}

		bubble.revalidate();
		bubble.repaint();
	}

	/**
	 * updates the Alignment and Position and repaints the Bubble
	 *
	 * @param reregisterListeners
	 *            if true the listeners will be removed and added again after the repaint
	 */
	protected void paintAgain(final boolean reregisterListeners) {
		if (!isVisible()) {
			return;
		}
		Alignment newAlignment = this.calculateAlignment(realAlignment);
		if (realAlignment.equals(newAlignment)) {
			this.pointAtComponent();
			return;
		} else {
			realAlignment = newAlignment;
		}
		if (reregisterListeners) {
			this.unregisterRegularListener();
		}

		layoutBubble();
		pack();

		positionRelative();

		// repaint the entire dialog because otherwise the previous "pointer" will not be gone if
		// the alignment has changed
		// that would result in 2 pointers being displayed
		this.repaint();
	}

	/**
	 *
	 * Adds a {@link BubbleListener}.
	 *
	 * @param l
	 *            The listener
	 */
	public void addBubbleListener(final BubbleListener l) {
		listeners.add(l);
	}

	/**
	 * removes the given {@link BubbleListener}.
	 *
	 * @param l
	 *            {@link BubbleListener} to remove.
	 */
	public void removeBubbleListener(final BubbleListener l) {
		listeners.remove(l);
	}

	/**
	 * Creates a speech bubble-shaped Shape.
	 *
	 * @param alignment
	 *            The alignment of the pointer.
	 *
	 * @return A speech-bubble <b>Shape</b>.
	 */
	public Shape createShape(final Alignment alignment, final float stroke) {
		float w = getSize().width - 2 * CORNER_RADIUS - stroke;
		float h = getSize().height - 2 * CORNER_RADIUS - stroke;
		float o = CORNER_RADIUS;

		GeneralPath gp = new GeneralPath();
		switch (alignment) {
			case TOPLEFT:
				gp.moveTo(0, 0);
				gp.lineTo(0, h + o);
				gp.quadTo(0, h + 2 * o, o, h + 2 * o);
				gp.lineTo(w + o, h + 2 * o);
				gp.quadTo(w + 2 * o, h + 2 * o, w + 2 * o, h + o);
				gp.lineTo(w + 2 * o, 2 * o);
				gp.quadTo(w + 2 * o, o, w + o, o);
				gp.lineTo(o, o);
				gp.closePath();
				break;
			case TOPRIGHT:
				gp.moveTo(0, 2 * o);
				gp.lineTo(0, h + o);
				gp.quadTo(0, h + 2 * o, o, h + 2 * o);
				gp.lineTo(w + o, h + 2 * o);
				gp.quadTo(w + 2 * o, h + 2 * o, w + 2 * o, h + o);
				gp.lineTo(w + 2 * o, 0);
				gp.lineTo(w + o, o);
				gp.lineTo(o, o);
				gp.quadTo(0, o, 0, 2 * o);
				break;
			case BOTTOMLEFT:
				gp.moveTo(0, o);
				gp.lineTo(0, h + 2 * o);
				gp.lineTo(o, h + o);
				gp.lineTo(w + o, h + o);
				gp.quadTo(w + 2 * o, h + o, w + 2 * o, h);
				gp.lineTo(w + 2 * o, o);
				gp.quadTo(w + 2 * o, 0, w + o, 0);
				gp.lineTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				break;
			case BOTTOMRIGHT:
				gp.moveTo(0, o);
				gp.lineTo(0, h);
				gp.quadTo(0, h + o, o, h + o);
				gp.lineTo(w + o, h + o);
				gp.lineTo(w + 2 * o, h + 2 * o);
				gp.lineTo(w + 2 * o, o);
				gp.quadTo(w + 2 * o, 0, w + o, 0);
				gp.lineTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				break;
			case LEFTBOTTOM:
				gp.moveTo(0, h + 2 * o);
				gp.lineTo(w + o, h + 2 * o);
				gp.quadTo(w + 2 * o, h + 2 * o, w + 2 * o, h + o);
				gp.lineTo(w + 2 * o, o);
				gp.quadTo(w + 2 * o, 0, w + o, 0);
				gp.lineTo(2 * o, 0);
				gp.quadTo(o, 0, o, o);
				gp.lineTo(o, h + o);
				gp.closePath();
				break;
			case INNERLEFT:
			case LEFTTOP:
				gp.moveTo(0, 0);
				gp.lineTo(o, o);
				gp.lineTo(o, h + o);
				gp.quadTo(o, h + 2 * o, 2 * o, h + 2 * o);
				gp.lineTo(w + o, h + 2 * o);
				gp.quadTo(w + 2 * o, h + 2 * o, w + 2 * o, h + o);
				gp.lineTo(w + 2 * o, o);
				gp.quadTo(w + 2 * o, 0, w + o, 0);
				gp.lineTo(0, 0);
				break;
			case RIGHTBOTTOM:
				gp.moveTo(0, h + o);
				gp.quadTo(0, h + 2 * o, o, h + 2 * o);
				gp.lineTo(w + 2 * o, h + 2 * o);
				gp.lineTo(w + o, h + o);
				gp.lineTo(w + o, o);
				gp.quadTo(w + o, 0, w, 0);
				gp.lineTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				gp.lineTo(0, h + o);
				break;
			case INNERRIGHT:
			case RIGHTTOP:
				gp.moveTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				gp.lineTo(0, h + o);
				gp.quadTo(0, h + 2 * o, o, h + 2 * o);
				gp.lineTo(w, h + 2 * o);
				gp.quadTo(w + o, h + 2 * o, w + o, h + o);
				gp.lineTo(w + o, o);
				gp.lineTo(w + 2 * o, 0);
				gp.lineTo(o, 0);
				break;
			case MIDDLE:
				gp.moveTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				gp.lineTo(0, h + o);
				gp.quadTo(0, h + 2 * o, o, h + 2 * o);
				gp.lineTo(w + o, h + 2 * o);
				gp.quadTo(w + 2 * o, h + 2 * o, w + 2 * o, h + o);
				gp.lineTo(w + 2 * o, o);
				gp.quadTo(w + 2 * o, 0, w + o, 0);
				gp.lineTo(o, 0);
				break;
			default:
		}
		AffineTransform tx = new AffineTransform();
		return gp.createTransformedShape(tx);
	}

	/**
	 * places the {@link BubbleWindow} relative to the Component which was given and adds the
	 * listeners.
	 */
	private void positionRelative() {
		pointAtComponent();
		registerRegularListener();
	}

	/**
	 * places the Bubble-speech so that it points to the Component
	 */
	protected void pointAtComponent() {
		double targetx = 0;
		double targety = 0;
		Point target = new Point(0, 0);
		if (realAlignment == Alignment.MIDDLE) {
			targetx = getOwner().getWidth() * 0.5 - getWidth() * 0.5;
			targety = getOwner().getHeight() * 0.5 - getHeight() * 0.5;
		} else {
			Point location = this.getObjectLocation();
			if (location == null) {
				return;
			}

			int xposObject = (int) location.getX();
			int yposObject = (int) location.getY();
			int height = this.getObjectHeight();
			int width = this.getObjectWidth();
			switch (realAlignment) {
				case TOPLEFT:
					targetx = xposObject + 0.5 * width;
					targety = yposObject + height;
					break;
				case TOPRIGHT:
					targetx = xposObject + 0.5 * width - getWidth();
					targety = yposObject + height;
					break;
				case LEFTBOTTOM:
					targetx = xposObject + width;
					targety = yposObject + 0.5 * height - getHeight();
					break;
				case LEFTTOP:
					targetx = xposObject + width;
					targety = yposObject + 0.5 * height;
					break;
				case RIGHTBOTTOM:
					targetx = xposObject - getWidth();
					targety = yposObject + 0.5 * height - getHeight();
					break;
				case RIGHTTOP:
					targetx = xposObject - getWidth();
					targety = yposObject + 0.5 * height;
					break;
				case BOTTOMLEFT:
					targetx = xposObject + 0.5 * width;
					targety = yposObject - getHeight();
					break;
				case BOTTOMRIGHT:
					targetx = xposObject + 0.5 * width - getWidth();
					targety = yposObject - getHeight();
					break;
				case INNERLEFT:
					targetx = xposObject + width - 0.5 * getWidth();
					double xShift = targetx + getWidth() - (getOwner().getX() + getOwner().getWidth());
					if (xShift > 0) {
						targetx -= xShift;
					}
					targety = yposObject + height - 0.5 * getHeight();
					double yShift = targety + getHeight() - (getOwner().getY() + getOwner().getHeight());
					if (yShift > 0) {
						targety -= yShift;
					}
					break;
				case INNERRIGHT:
					targetx = xposObject - 0.5 * getWidth();
					xShift = getOwner().getX() - targetx;
					if (xShift > 0) {
						targetx += xShift;
					}
					targety = yposObject + height - 0.5 * getHeight();
					yShift = targety + getHeight() + 25 - (getOwner().getY() + getOwner().getHeight());
					if (yShift > 0) {
						targety -= yShift;
					}
					break;
				// $CASES-OMITTED$
				default:
			}
		}

		target = new Point((int) Math.round(targetx), (int) Math.round(targety));
		setLocation(target);
	}

	/**
	 * method to get to know whether the dockable with the given key is on Screen
	 *
	 * @param dockableKey
	 *            i18nKey of the wanted Dockable
	 * @return returns 1 if the Dockable is on the Screen but not showing, -1 if the Dockable is not
	 *         on the Screen and 0 if the Dockable is on Screen and showing.
	 */
	public static int isDockableOnScreen(final String dockableKey) {
		Dockable dock = RapidMinerGUI.getMainFrame().getDockingDesktop().getContext().getDockableByKey(dockableKey);
		DockableState state = RapidMinerGUI.getMainFrame().getDockingDesktop().getDockableState(dock);
		if (!state.isClosed()) {
			if (dock.getComponent().isShowing()) {
				return OBJECT_SHOWING_ON_SCREEN;
			}
			return OBJECT_NOT_SHOWING;
		}
		return OBJECT_NOT_ON_SCREEN;

	}

	/**
	 * method to get to know whether the AbstractButton with the given key is on Screen
	 *
	 * @param dockableKey
	 *            i18nKey of the wanted AbstractButton
	 * @return returns 0 if the AbstractButton is on the Screen, 1 if the AbstractButton is on
	 *         Screen but the user can not see it with the current settings of the perspective and
	 *         -1 if the AbstractButton is not on the Screen.
	 */
	public static int isButtonOnScreen(final String buttonKey) {
		// find the Button and return -1 if we can not find it
		Component onScreen;
		try {
			onScreen = BubbleWindow.findButton(buttonKey, RapidMinerGUI.getMainFrame());
		} catch (NullPointerException e) {
			return OBJECT_NOT_ON_SCREEN;
		}
		if (onScreen == null) {
			return OBJECT_NOT_ON_SCREEN;
		}
		// detect whether the Button is viewable
		int xposition = onScreen.getLocationOnScreen().x;
		int yposition = onScreen.getLocationOnScreen().y;
		int otherXposition = xposition + onScreen.getWidth();
		int otherYposition = yposition + onScreen.getHeight();
		Window frame = RapidMinerGUI.getMainFrame();
		if (otherXposition <= frame.getWidth() && otherYposition <= frame.getHeight() && xposition > 0 && yposition > 0) {
			return OBJECT_SHOWING_ON_SCREEN;
		} else {
			return OBJECT_NOT_SHOWING;
		}
	}

	/**
	 * @param name
	 *            i18nKey of the Button
	 * @param searchRoot
	 *            {@link Component} in which will be searched for the Button
	 * @return returns the {@link AbstractButton} or null if the Button was not found.
	 */
	public static AbstractButton findButton(final String name, final Component searchRoot) {
		if (searchRoot instanceof AbstractButton) {

			AbstractButton b = (AbstractButton) searchRoot;
			if (b.getAction() instanceof ResourceAction) {
				String id = (String) b.getAction().getValue("rm_id");
				if (name.equals(id)) {
					return b;
				}
			}
		}
		if (searchRoot instanceof Container) {
			Component[] all = ((Container) searchRoot).getComponents();
			for (Component child : all) {
				AbstractButton result = findButton(name, child);
				if (result != null) {
					return result;

				}
			}
		}
		return null;
	}

	/** initiate all regular Listeners */
	private void initRegularListener() {
		perspectiveListener = new PerspectiveChangeListener() {

			@Override
			public void perspectiveChangedTo(final Perspective perspective) {
				if (!BubbleWindow.this.myPerspective.equals(perspective.getName())) {
					BubbleWindow.this.changeToAssistant(AssistantType.WRONG_PERSPECTIVE);
				}
			}
		};
		compListener = new ComponentListener() {

			@Override
			public void componentShown(final ComponentEvent e) {
				BubbleWindow.this.pointAtComponent();
				BubbleWindow.this.setVisible(true);
			}

			@Override
			public void componentResized(final ComponentEvent e) {
				if (BubbleWindow.this.realAlignment.equals(BubbleWindow.this.calculateAlignment(realAlignment))) {
					BubbleWindow.this.pointAtComponent();
				} else {
					BubbleWindow.this.paintAgain(false);
				}
				BubbleWindow.this.setVisible(true);
			}

			@Override
			public void componentMoved(final ComponentEvent e) {
				if (BubbleWindow.this.realAlignment.equals(BubbleWindow.this.calculateAlignment(realAlignment))) {
					BubbleWindow.this.pointAtComponent();
				} else {
					BubbleWindow.this.paintAgain(true);
				}
				BubbleWindow.this.setVisible(true);
			}

			@Override
			public void componentHidden(final ComponentEvent e) {
				BubbleWindow.this.setVisible(false);
			}
		};
		hierachyListener = new HierarchyListener() {

			@Override
			public void hierarchyChanged(final HierarchyEvent e) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						DockableState state = desktop.getDockableState(dockable);
						if (state != null && !state.isHidden() && !dockable.getComponent().isShowing()
								&& dockable.getComponent().getParent() != null
								&& dockable.getComponent().getParent().getParent() != null) {
							BubbleWindow.this.changeToAssistant(AssistantType.NOT_SHOWING);
						}
					}
				});
			}
		};
		dockListener = new DockingActionListener() {

			@Override
			public void dockingActionPerformed(final DockingActionEvent event) {
				// actionType 2 indicates that a Dockable was splitted (ACTION_SPLIT_DOCKABLE)
				// actionType 3 indicates that the Dockable has created his own position
				// (ACTION_SPLIT_COMPONENT)
				// actionType 5 indicates that the Dockable was docked to another position
				// (ACTION_CREATE_TAB)
				// actionType 6 indicates that the Dockable was separated (ACTION_STATE_CHANGE)
				if (event.getActionType() == DockingActionEvent.ACTION_CREATE_TAB
						|| event.getActionType() == DockingActionEvent.ACTION_SPLIT_COMPONENT) {
					if (++dockingCounter % 2 == 0) {
						// repaint
						BubbleWindow.this.paintAgain(false);
						BubbleWindow.this.setVisible(true);
					}
				}

				if (event.getActionType() == DockingActionEvent.ACTION_STATE_CHANGE
						|| event.getActionType() == DockingActionEvent.ACTION_SPLIT_DOCKABLE) {
					if (desktop.getDockableState(dockable).isHidden()) {
						// tab is minimized
						BubbleWindow.this.changeToAssistant(AssistantType.HIDDEN);
					} else {
						// repaint
						BubbleWindow.this.paintAgain(false);
						BubbleWindow.this.setVisible(true);
					}
				}
				if (event.getActionType() == DockingActionEvent.ACTION_CLOSE) {
					if (desktop.getDockableState(dockable) == null || desktop.getDockableState(dockable).isClosed()) {
						BubbleWindow.this.changeToAssistant(AssistantType.NOT_ON_SCREEN);
					}
				}
			}

			@Override
			public boolean acceptDockingAction(final DockingActionEvent arg0) {
				// no need to deny anything
				return true;
			}
		};
		windowListener = new WindowAdapter() {

			@Override
			public void windowStateChanged(WindowEvent e) {
				// hide after iconification adn restore after deiconification
				// this bitwise operation tests if the new window status is iconified
				if ((e.getNewState() & Frame.ICONIFIED) != 0) {
					BubbleWindow.this.setVisible(false);
				} else {
					BubbleWindow.this.pointAtComponent();
					BubbleWindow.this.setVisible(true);
				}
			}

		};
		componentListenerToWindow = new ComponentListener() {

			@Override
			public void componentShown(final ComponentEvent e) {
				BubbleWindow.this.paint(false);
				BubbleWindow.this.setVisible(true);
			}

			@Override
			public void componentResized(final ComponentEvent e) {
				BubbleWindow.this.paint(false);
				BubbleWindow.this.setVisible(true);
			}

			@Override
			public void componentMoved(final ComponentEvent e) {
				BubbleWindow.this.paint(false);
				BubbleWindow.this.setVisible(true);
			}

			@Override
			public void componentHidden(final ComponentEvent e) {
				BubbleWindow.this.setVisible(false);
			}
		};
	}

	/**
	 * registers all possible Listeners(regular and the special Listeners of the subclasses)
	 */
	protected void registerRegularListener() {
		if (!listenersAdded) {
			listenersAdded = true;

			this.registerSpecificListener();
			RapidMinerGUI.getMainFrame().addWindowStateListener(windowListener);
			RapidMinerGUI.getMainFrame().addComponentListener(componentListenerToWindow);
			if (preferredAlignment == AlignedSide.MIDDLE) {
				if (addPerspective) {
					RapidMinerGUI.getMainFrame().getPerspectiveController().getModel()
							.addPerspectiveChangeListener(perspectiveListener);
				}
				RapidMinerGUI.getMainFrame().addComponentListener(compListener);
			} else {
				if (addPerspective) {
					RapidMinerGUI.getMainFrame().getPerspectiveController().getModel()
							.addPerspectiveChangeListener(perspectiveListener);
				}
				if (docKey == null) {
					// no component was attached but possible there are some side effects
					RapidMinerGUI.getMainFrame().addComponentListener(compListener);
				} else {
					BubbleWindow.this.dockable.getComponent().addComponentListener(compListener);
					dockable.getComponent().addHierarchyListener(hierachyListener);
					desktop.addDockingActionListener(dockListener);
				}
			}
		}
	}

	/**
	 * unregisters the regular Listeners and the special Listeners of the subclasses
	 */
	protected void unregisterRegularListener() {
		if (listenersAdded) {
			this.unregisterSpecificListeners();
			RapidMinerGUI.getMainFrame().removeWindowStateListener(windowListener);
			RapidMinerGUI.getMainFrame().removeComponentListener(componentListenerToWindow);
			if (preferredAlignment == AlignedSide.MIDDLE) {
				if (addPerspective) {
					RapidMinerGUI.getMainFrame().getPerspectiveController().getModel()
							.removePerspectiveChangeListener(perspectiveListener);
				}
				RapidMinerGUI.getMainFrame().removeComponentListener(compListener);
			} else {
				if (addPerspective) {
					RapidMinerGUI.getMainFrame().getPerspectiveController().getModel()
							.removePerspectiveChangeListener(perspectiveListener);
				}
				if (docKey == null) {
					RapidMinerGUI.getMainFrame().removeComponentListener(compListener);
				} else {
					BubbleWindow.this.dockable.getComponent().removeComponentListener(compListener);
					dockable.getComponent().removeHierarchyListener(hierachyListener);
					desktop.removeDockingActionListener(dockListener);
				}
			}
			listenersAdded = false;
		}
	}

	/**
	 * removes the Actionlistener from the close-Button of the Bubble
	 */
	private void unregister() {
		if (close != null) {
			close.removeActionListener(listener);
		}
	}

	/**
	 * notifies the {@link BubbleListener}s and disposes the Bubble-speech.
	 */
	public void triggerFire() {
		fireEventActionPerformed();
	}

	/**
	 * closes the Bubble and calls bubbleClosed() of the {@link BubbleListener}
	 */
	protected void fireEventCloseClicked() {
		LinkedList<BubbleListener> listenerList = new LinkedList<>(listeners);
		this.unregister();
		for (BubbleListener l : listenerList) {
			l.bubbleClosed(this);
		}
		unregisterRegularListener();
		closeAssistants();
		this.dispose();
	}

	/**
	 * Kills the bubble in the EDT.
	 *
	 * @param notifyListeners
	 *            if <code>false</code>, does <strong>not</strong> notify listeners about this
	 */
	public void killBubble(final boolean notifyListeners) {
		if (SwingUtilities.isEventDispatchThread()) {
			kill(notifyListeners);
		} else {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					kill(notifyListeners);
				}
			});
		}
	}

	@Override
	public void setVisible(boolean b) {
		// don't show killed bubbles or bubbles in wrong perspective
		if (b && (killed || !isRightPerspective())) {
			return;
		}
		super.setVisible(b);
	}

	/**
	 * Returns <code>true</code> if the bubble has been killed via {@link #killBubble(boolean)}.
	 *
	 * @return
	 */
	public boolean isKilled() {
		return killed;
	}

	/**
	 * closes the Bubble and calls actionPerformed() of the {@link BubbleListener}
	 */
	protected void fireEventActionPerformed() {
		LinkedList<BubbleListener> listenerList = new LinkedList<>(listeners);
		for (BubbleListener l : listenerList) {
			l.actionPerformed(this);
		}
		unregisterRegularListener();
		unregister();
		closeAssistants();
		this.dispose();
	}

	/**
	 * calculates the Alignment in the way, that the Bubble do not leave the Window
	 *
	 * @param currentAlignment
	 *            the current Alignment which will be tried to keep if the preferred Alignment is
	 *            not possible
	 * @return returns the calculated {@link Alignment}
	 */
	protected Alignment calculateAlignment(final Alignment currentAlignment) {
		if (AlignedSide.MIDDLE == this.preferredAlignment) {
			return Alignment.MIDDLE;
		}
		if (dockable != null
				&& (desktop.getDockableState(dockable) == null || desktop.getDockableState(dockable).isHidden())) {
			return Alignment.LEFTTOP;
		}
		// get Mainframe location
		Point frameLocation = getOwner().getLocationOnScreen();
		double xlocFrame = frameLocation.getX();
		double ylocFrame = frameLocation.getY();

		// get Mainframe size
		int frameWidth = getOwner().getWidth();
		int frameHeight = getOwner().getHeight();

		// location and size of Component the want to attach to
		Point objectLocation = this.getObjectLocation();
		if (objectLocation == null) {
			return realAlignment;
		}
		double xlocComponent = objectLocation.getX();
		double ylocComponent = objectLocation.getY();
		int componentWidth = this.getObjectWidth();
		int componentHeight = this.getObjectHeight();

		// load height and width or the approximate Value of worst case
		double bubbleWidth = this.getWidth();
		double bubbleHeight = this.getHeight();
		if (bubbleWidth == 0 || bubbleHeight == 0) {
			bubbleWidth = 326;
			bubbleHeight = 200;
		}

		if (currentAlignment == Alignment.TOPLEFT || currentAlignment == Alignment.TOPRIGHT
				|| currentAlignment == Alignment.BOTTOMLEFT || currentAlignment == Alignment.BOTTOMRIGHT) {
			bubbleWidth += BUBBLE_CONNECTOR_WIDTH;
		} else {
			bubbleHeight += BUBBLE_CONNECTOR_HEIGHT;
		}
		// 0 = space above the component
		// 1 = space right of the component
		// 2 = space below the component
		// 3 = space left of the Component
		double space[] = new double[4];
		space[0] = (ylocComponent - ylocFrame) / bubbleHeight;
		space[1] = (frameWidth + xlocFrame - (xlocComponent + componentWidth)) / bubbleWidth;
		space[2] = (frameHeight + ylocFrame - (ylocComponent + componentHeight)) / bubbleHeight;
		space[3] = (xlocComponent - xlocFrame) / bubbleWidth;
		// check if the preferred Alignment is valid and take it if it is valid
		switch (this.preferredAlignment) {
			case BOTTOM:
				if (space[2] > 1) {
					return this.fineTuneAlignment(Alignment.TOPLEFT, frameWidth, frameHeight, frameLocation, objectLocation,
							componentWidth, componentHeight);
				}
				break;
			case RIGHT:
				if (space[1] > 1) {
					return this.fineTuneAlignment(Alignment.LEFTBOTTOM, frameWidth, frameHeight, frameLocation,
							objectLocation, componentWidth, componentHeight);
				}
				break;
			case LEFT:
				if (space[3] > 1) {
					return this.fineTuneAlignment(Alignment.RIGHTBOTTOM, frameWidth, frameHeight, frameLocation,
							objectLocation, componentWidth, componentHeight);
				}
				break;
			case TOP:
				if (space[0] > 1) {
					return this.fineTuneAlignment(Alignment.BOTTOMLEFT, frameWidth, frameHeight, frameLocation,
							objectLocation, componentWidth, componentHeight);
				}
				break;
			// $CASES-OMITTED$
			default:
		}
		// preferred Alignment was not valid. try to show bubble at the same position as before
		if (currentAlignment != null) {
			switch (currentAlignment) {
				case BOTTOMRIGHT:
				case BOTTOMLEFT:
					if (space[0] > 1) {
						return this.fineTuneAlignment(Alignment.BOTTOMLEFT, frameWidth, frameHeight, frameLocation,
								objectLocation, componentWidth, componentHeight);
					}
					break;
				case LEFTTOP:
				case LEFTBOTTOM:
					if (space[1] > 1) {
						return this.fineTuneAlignment(Alignment.LEFTBOTTOM, frameWidth, frameHeight, frameLocation,
								objectLocation, componentWidth, componentHeight);
					}
					break;
				case TOPRIGHT:
				case TOPLEFT:
					if (space[2] > 1) {
						return this.fineTuneAlignment(Alignment.TOPLEFT, frameWidth, frameHeight, frameLocation,
								objectLocation, componentWidth, componentHeight);
					}
					break;
				case RIGHTTOP:
				case RIGHTBOTTOM:
					if (space[3] > 1) {
						return this.fineTuneAlignment(Alignment.RIGHTBOTTOM, frameWidth, frameHeight, frameLocation,
								objectLocation, componentWidth, componentHeight);
					}
					break;
				case INNERRIGHT:
				case INNERLEFT:
					if (space[0] > 1) {
						return this.fineTuneAlignment(Alignment.BOTTOMLEFT, frameWidth, frameHeight, frameLocation,
								objectLocation, componentWidth, componentHeight);
					} else if (space[1] > 1) {
						return this.fineTuneAlignment(Alignment.LEFTBOTTOM, frameWidth, frameHeight, frameLocation,
								objectLocation, componentWidth, componentHeight);
					} else if (space[2] > 1) {
						return this.fineTuneAlignment(Alignment.TOPLEFT, frameWidth, frameHeight, frameLocation,
								objectLocation, componentWidth, componentHeight);
					} else if (space[3] > 1) {
						return this.fineTuneAlignment(Alignment.RIGHTBOTTOM, frameWidth, frameHeight, frameLocation,
								objectLocation, componentWidth, componentHeight);
					} else {
						return realAlignment;
					}
					// $CASES-OMITTED$
				default:
					throw new IllegalStateException(
							"this part of code should be unreachable for this state of BubbleWindow");
			}
		}
		if (space[1] > 1) {
			return this.fineTuneAlignment(Alignment.LEFTTOP, frameWidth, frameHeight, frameLocation, objectLocation,
					componentWidth, componentHeight);
		}

		// can not keep the old alignment. take the best fitting place
		int pointer = 0;
		for (int i = 1; i < space.length; i++) {
			if (space[i] > space[pointer]) {
				pointer = i;
			}
		}
		if (space[pointer] > 1) {
			switch (pointer) {
				case 0:
					return this.fineTuneAlignment(Alignment.BOTTOMLEFT, frameWidth, frameHeight, frameLocation,
							objectLocation, componentWidth, componentHeight);
				case 1:
					return this.fineTuneAlignment(Alignment.LEFTTOP, frameWidth, frameHeight, frameLocation, objectLocation,
							componentWidth, componentHeight);
				case 2:
					return this.fineTuneAlignment(Alignment.TOPLEFT, frameWidth, frameHeight, frameLocation, objectLocation,
							componentWidth, componentHeight);
				case 3:
					return this.fineTuneAlignment(Alignment.RIGHTTOP, frameWidth, frameHeight, frameLocation, objectLocation,
							componentWidth, componentHeight);
				default:
					throw new RuntimeException("Could not find Alignment because index was out of bound");
			}
		} else {
			// can not place Bubble outside of the component so we take the right side of the inner
			// of the Component.
			return this.fineTuneAlignment(Alignment.INNERLEFT, frameWidth, frameHeight, frameLocation, objectLocation,
					componentWidth, componentHeight);
		}

	}

	/**
	 * after the calculateAlignment() decided the optimal side, this method decides which is the
	 * optimal direction for the Bubble
	 *
	 * @param firstCompute
	 *            first computed Alignment
	 * @param xframe
	 *            width of the owner
	 * @param yframe
	 *            height of the owner
	 * @param frameLocation
	 *            location of the origin of the owner
	 * @param componentLocation
	 *            location of the origin of the Component to attach to
	 * @param compWidth
	 *            width of the component to attach to
	 * @param compHeight
	 *            height of the component to attach to
	 * @return the optimal Alignment in this situation
	 */
	private Alignment fineTuneAlignment(final Alignment firstCompute, final int xframe, final int yframe,
			final Point frameLocation, final Point componentLocation, final int compWidth, final int compHeight) {
		switch (firstCompute) {
			case TOPLEFT:
			case TOPRIGHT:
				if (componentLocation.x - frameLocation.x + compWidth / 2 > xframe / 2) {
					return Alignment.TOPRIGHT;
				} else {
					return Alignment.TOPLEFT;
				}
			case LEFTBOTTOM:
			case LEFTTOP:
				if (componentLocation.y - frameLocation.y + compHeight / 2 > yframe / 2) {
					return Alignment.LEFTBOTTOM;
				} else {
					return Alignment.LEFTTOP;
				}
			case RIGHTBOTTOM:
			case RIGHTTOP:
				if (componentLocation.y - frameLocation.y + compHeight / 2 > yframe / 2) {
					return Alignment.RIGHTBOTTOM;
				} else {
					return Alignment.RIGHTTOP;
				}
			case BOTTOMLEFT:
			case BOTTOMRIGHT:
				if (componentLocation.x - frameLocation.x + compWidth / 2 > xframe / 2) {
					return Alignment.BOTTOMRIGHT;
				} else {
					return Alignment.BOTTOMLEFT;
				}
				// $CASES-OMITTED$
			default:
				if (realAlignment == Alignment.INNERLEFT || realAlignment == Alignment.INNERRIGHT) {
					return realAlignment;
				}

				if (componentLocation.x - frameLocation.x > xframe + frameLocation.x - (compWidth + componentLocation.x)) {
					return Alignment.INNERRIGHT;
				} else {
					return Alignment.INNERLEFT;
				}
		}

	}

	/**
	 * indicates whether the Perspective Listener should be added or not
	 *
	 * @param addListener
	 *            true if the PerspectiveListener should be added and false if not
	 */
	protected void setAddPerspectiveListener(final boolean addListener) {
		this.addPerspective = addListener;
		if (perspectiveListener != null) {
			RapidMinerGUI.getMainFrame().getPerspectiveController().getModel()
					.removePerspectiveChangeListener(perspectiveListener);
		}
	}

	/**
	 * returns the location of the Object the Bubble should attach to
	 *
	 * @return the Point that indicates the left upper corner of the Object the Bubble should point
	 *         to
	 */
	protected abstract Point getObjectLocation();

	/**
	 * method to get the width of the Object the Bubble should attach to
	 *
	 * @return the width of the Object
	 */
	protected abstract int getObjectWidth();

	/**
	 * method to get the height of the Object the Bubble should attach to
	 *
	 * @return the height of the Object
	 */
	protected abstract int getObjectHeight();

	/**
	 * unregister the components specific listeners defined in the subclasses
	 */
	protected abstract void unregisterSpecificListeners();

	/** register the components specific listeners defined in the subclasses */
	protected abstract void registerSpecificListener();

	/**
	 * creates an Assistant-Bubble which pauses the current Step-Bubble. Every Bubble can only have
	 * one Assistant but an Assistant can have an Assistant too.
	 *
	 * @param type
	 *            of the Assistant you want to create
	 */
	protected void changeToAssistant(final AssistantType type) {
		if (assistantBubble == null && currentAssistant != AssistantType.NO_ASSISTANT_ACTIVE
				|| assistantBubble != null && currentAssistant == AssistantType.NO_ASSISTANT_ACTIVE) {
			currentAssistant = AssistantType.NO_ASSISTANT_ACTIVE;
			assistantBubble = null;
		}
		if (currentAssistant != AssistantType.NO_ASSISTANT_ACTIVE || assistantBubble != null) {
			return;
		}
		// leftover listener fires with a delay, prevent popup
		if (killed) {
			return;
		}
		this.setVisible(false);
		this.unregisterRegularListener();
		switch (type) {
			case NOT_SHOWING:
				assistantBubble = new DockableBubble(getOwner(), AlignedSide.RIGHT, KEY_NOT_SHOWING, docKey,
						new Object[] { dockable.getDockKey().getName() });
				if (dockable != null) {
					assistantHierarchy = new HierarchyListener() {

						@Override
						public void hierarchyChanged(final HierarchyEvent e) {
							if (BubbleWindow.this.dockable.getComponent().isShowing()) {
								BubbleWindow.this.changeToMainBubble();
							}
						}
					};
					dockable.getComponent().addHierarchyListener(assistantHierarchy);
				}
				break;
			case NOT_ON_SCREEN:
				assistantBubble = new DockableBubble(getOwner(), AlignedSide.MIDDLE, KEY_NOT_ON_SCREEN, docKey,
						new Object[] { dockable.getDockKey().getName() });
				assistantDockStateChange = new DockableStateChangeListener() {

					@Override
					public void dockableStateChanged(final DockableStateChangeEvent changed) {
						if (changed.getNewState().getDockable().getDockKey().getKey().equals(docKey)
								&& !changed.getNewState().isClosed()) {
							BubbleWindow.this.changeToMainBubble();
						}
					}
				};
				desktop.getContext().addDockableStateChangeListener(assistantDockStateChange);

				assistantDockSelect = new DockableSelectionListener() {

					@Override
					public void selectionChanged(final DockableSelectionEvent arg0) {
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								if (dockable.getComponent().isShowing()) {
									BubbleWindow.this.changeToMainBubble();
								}
							}
						});
					}
				};
				desktop.addDockableSelectionListener(assistantDockSelect);
				break;
			case HIDDEN:
				assistantBubble = new DockableBubble(getOwner(), AlignedSide.RIGHT, KEY_HIDDEN, docKey,
						dockable.getDockKey().getName());
				assistantDockingAction = new DockingActionListener() {

					@Override
					public void dockingActionPerformed(final DockingActionEvent arg0) {
						if (!desktop.getDockableState(dockable).isHidden()) {
							BubbleWindow.this.changeToMainBubble();
						}
					}

					@Override
					public boolean acceptDockingAction(final DockingActionEvent arg0) {
						// nothing to deny
						return true;
					}
				};
				desktop.addDockingActionListener(assistantDockingAction);
				assistantHierarchy = new HierarchyListener() {

					@Override
					public void hierarchyChanged(final HierarchyEvent e) {
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								DockableState state = desktop.getDockableState(dockable);
								if (state != null && state.isHidden()) {
									assistantBubble.paint(false);
								} else if (state != null) {
									// this case means that the user restored the perspective
									BubbleWindow.this.changeToMainBubble();
								}
							}
						});
					}
				};
				dockable.getComponent().addHierarchyListener(assistantHierarchy);
				break;
			case WRONG_PERSPECTIVE:
				String buttonKey = "";
				if (myPerspective.equals("design")) {
					buttonKey = "workspace_design";
				} else if (myPerspective.equals("result")) {
					buttonKey = "workspace_result";
				}
				assistantBubble = new ButtonBubble(getOwner(), null, AlignedSide.BOTTOM, KEY_PERSPECTIVE, buttonKey, false,
						false, new Object[] { myPerspective });
				assistantPerspective = new PerspectiveChangeListener() {

					@Override
					public void perspectiveChangedTo(final Perspective perspective) {
						if (BubbleWindow.this.myPerspective.equals(perspective.getName())) {
							BubbleWindow.this.changeToMainBubble();
						}
					}
				};
				RapidMinerGUI.getMainFrame().getPerspectiveController().getModel()
						.addPerspectiveChangeListener(assistantPerspective);
				break;
			case NOT_IN_CHAIN:
			case NO_ASSISTANT_ACTIVE:
				// will not be called here
				break;
			default:
				throw new IllegalArgumentException(
						"the AssistantType " + type.toString() + " is not supported by this class");
		}
		currentAssistant = type;
		assistantBubble.addBubbleListener(new BubbleListener() {

			@Override
			public void bubbleClosed(final BubbleWindow bw) {
				BubbleWindow.this.fireEventCloseClicked();
			}

			@Override
			public void actionPerformed(final BubbleWindow bw) {
				// do not care
			}

		});
		assistantBubble.setVisible(true);
	}

	/**
	 * closes the current Assistant and restarts the current Step
	 */
	protected void changeToMainBubble() {
		if (currentAssistant == AssistantType.NO_ASSISTANT_ACTIVE) {
			return;
		}
		switch (currentAssistant) {
			case NOT_SHOWING:
				closeShowingAssistant();
				break;
			case NOT_ON_SCREEN:
				closeNotOnScreenAssistant();
				break;
			case HIDDEN:
				closeHiddenAssistant();
				break;
			case WRONG_PERSPECTIVE:
				closePerspectiveAssistant();
				break;
			case NOT_IN_CHAIN:
			case NO_ASSISTANT_ACTIVE:
			default:
		}
		this.registerRegularListener();
		this.paint(false);
		this.setVisible(true);
	}

	/**
	 * closes the WRONG_PERSPECTIVE-Assistant
	 */
	private void closePerspectiveAssistant() {
		if (assistantBubble != null && currentAssistant == AssistantType.WRONG_PERSPECTIVE) {
			assistantBubble.triggerFire();
			assistantBubble = null;
			RapidMinerGUI.getMainFrame().getPerspectiveController().getModel()
					.removePerspectiveChangeListener(assistantPerspective);
			currentAssistant = AssistantType.NO_ASSISTANT_ACTIVE;
		}
	}

	/**
	 * closes the NOTSHOWING-Assistant
	 */
	private void closeShowingAssistant() {
		if (assistantBubble != null && currentAssistant == AssistantType.NOT_SHOWING) {
			assistantBubble.triggerFire();
			assistantBubble = null;
			dockable.getComponent().removeHierarchyListener(assistantHierarchy);
			currentAssistant = AssistantType.NO_ASSISTANT_ACTIVE;
		}
	}

	/**
	 * closes the NOTONSCREEN-Assistant
	 */
	private void closeNotOnScreenAssistant() {
		if (assistantBubble != null && currentAssistant == AssistantType.NOT_ON_SCREEN) {
			assistantBubble.triggerFire();
			assistantBubble = null;
			desktop.getContext().removeDockableStateChangeListener(assistantDockStateChange);
			desktop.removeDockableSelectionListener(assistantDockSelect);
			currentAssistant = AssistantType.NO_ASSISTANT_ACTIVE;
		}
	}

	/**
	 * closes the HIDDEN-Assistant
	 */
	private void closeHiddenAssistant() {
		if (assistantBubble != null && currentAssistant == AssistantType.HIDDEN) {
			assistantBubble.triggerFire();
			assistantBubble = null;
			desktop.removeDockingActionListener(assistantDockingAction);
			dockable.getComponent().removeHierarchyListener(assistantHierarchy);
			currentAssistant = AssistantType.NO_ASSISTANT_ACTIVE;
		}
	}

	/**
	 * closes every Assistant does not matter which is active
	 */
	protected void closeAssistants() {
		closeShowingAssistant();
		closeNotOnScreenAssistant();
		closeHiddenAssistant();
		closePerspectiveAssistant();
	}

	protected BubbleWindow getAssistantBubble() {
		return assistantBubble;
	}

	protected void setAssistantBubble(final DockableBubble newAssistant) {
		assistantBubble = newAssistant;
	}

	/** returns which Assistant is currently active */
	protected AssistantType getCurrentAssistantType() {
		return currentAssistant;
	}

	/** sets the currentAssistant to the given value */
	protected void setCurrentAssistantType(final AssistantType newType) {
		if (newType == null) {
			throw new IllegalArgumentException("parameter can not be null, please choose NO_ASSISTANT instead");
		}
		currentAssistant = newType;
	}

	/**
	 * The real alignment of the bubble, indicating where the pointer is, as opposed to the
	 * positioning on within the window.
	 */
	protected Alignment getRealAlignment() {
		return realAlignment;
	}

	/** the direct or indirect Dockable which is attached by the Bubble */
	protected Dockable getDockable() {
		return dockable;
	}

	/** the key of the used Dockable for this Bubble */
	protected String getDockableKey() {
		return docKey;
	}

	/** the DockingDesktop of the UI */
	protected DockingDesktop getDockingDesktop() {
		return desktop;
	}

	void addComponentListenerTo(JComponent comp) {
		comp.addComponentListener(compListener);
	}

	public void setHeadline(String headline) {
		this.headline.setText(headline);
	}

	public void setMainText(String mainText) {
		int width = style != BubbleStyle.COMIC ? WINDOW_WIDTH : WINDOW_WIDTH_COMIC;
		this.mainText.setText("<html><div style=\"line-height: 150%;width:" + width + "px \">" + mainText + "</div></html>");
	}

	@Override
	public void paint(Graphics g) {
		// this is necessary due to a bug on Windows which involves setting the clip in the bubble
		// panel paintComponent() method in combination with the transparent JDialog and hovering
		// over a JButton
		// without this, the inner JPanel only repaints on a clip equal to the hovered JButton
		g.setClip(null);
		super.paint(g);
	}

	/**
	 * Kills the bubble.
	 *
	 * @param notifyListeners
	 *            if <code>false</code>, does <strong>not</strong> notify listeners about this
	 */
	private void kill(boolean notifyListeners) {
		if (!killed) {
			killed = true;
			this.unregister();
			if (notifyListeners) {
				LinkedList<BubbleListener> listenerList = new LinkedList<>(listeners);
				for (BubbleListener l : listenerList) {
					l.bubbleClosed(this);
				}
			}
			unregisterRegularListener();
			closeAssistants();
			this.dispose();
		}
	}

	/**
	 * @return the current {@link BubbleStyle}
	 */
	final BubbleStyle getStyle() {
		return style;
	}

	/**
	 * Checks if the current perspective matches {@link #myPerspective}.
	 *
	 * @return {@code true} if {@link #myPerspective} is equal to
	 *         {@link #getCurrentPerspectiveName()}, otherwise {@code false}
	 */
	protected boolean isRightPerspective() {
		return myPerspective.equals(getCurrentPerspectiveName());
	}

	/**
	 * Getter for the current perspective name.
	 *
	 * @return The name of the current perspective.
	 */
	protected String getCurrentPerspectiveName() {
		return RapidMinerGUI.getMainFrame().getPerspectiveController().getModel().getSelectedPerspective().getName();
	}

}
