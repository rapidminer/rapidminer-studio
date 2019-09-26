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
package com.rapidminer.gui.tools;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.JTextComponent;

import org.jdesktop.swingx.plaf.AbstractUIChangeHandler;
import org.jdesktop.swingx.prompt.PromptSupport;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.fc.Bookmark;
import com.rapidminer.gui.look.fc.BookmarkIO;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.gui.tools.components.ToolTipWindow.TooltipLocation;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.ErrorDialog;
import com.rapidminer.gui.tools.dialogs.ExtendedErrorDialog;
import com.rapidminer.gui.tools.dialogs.InputDialog;
import com.rapidminer.gui.tools.dialogs.InputValidator;
import com.rapidminer.gui.tools.dialogs.LongMessageDialog;
import com.rapidminer.gui.tools.dialogs.MessageDialog;
import com.rapidminer.gui.tools.dialogs.RepositoryEntryInputDialog;
import com.rapidminer.gui.tools.dialogs.ResultViewDialog;
import com.rapidminer.gui.tools.dialogs.SelectionInputDialog;
import com.rapidminer.gui.tools.syntax.SyntaxStyle;
import com.rapidminer.gui.tools.syntax.SyntaxUtilities;
import com.rapidminer.gui.tools.syntax.TextAreaDefaults;
import com.rapidminer.gui.tools.syntax.Token;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ParentResolvingMap;
import com.rapidminer.tools.StringColorMap;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.tools.update.internal.UpdateManagerRegistry;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * This helper class provides some static methods and properties which might be useful for several
 * GUI classes. These methods include
 * <ul>
 * <li>the creation of gradient paints</li>
 * <li>displaying (simple) error messages</li>
 * <li>creation of file chosers</li>
 * <li>creation of text panels</li>
 * <li>escaping HTML messages</li>
 * </ul>
 *
 * @author Ingo Mierswa
 */
public class SwingTools {

	/**
	 * Scaling of the GUI of the application. Currently, only non-scaling and Retina displays are supported. Likely to be expanded in the future.
	 * @since 9.0.0
	 */
	public enum Scaling {

		/** For regular screens without HighDPI */
		DEFAULT(1d),

		/** A HighDPI OS X screen */
		RETINA(2d);


		private double scalingFactor;

		Scaling(double scalingFactor) {
			this.scalingFactor = scalingFactor;
		}

		/**
		 * Returns the scaling factor. E.g. for a 4K Retina display, the factor is 2.
		 *
		 * @return the scaling factor, {@code 1} for non HighDPI displays
		 */
		public double getScalingFactor() {
			return scalingFactor;
		}
	}


	/**
	 * A simple interface which has a run method that returns a result.
	 *
	 * @since 6.5.0
	 *
	 */
	public interface ResultRunnable<T> {

		T run();
	}

	/**
	 * A container class for results generated by an implementation of {@link ResultRunnable}.
	 */
	private static class ResultContainer<T> {

		public T value;
	}

	/** see {@link #setPrompt(String, JTextComponent)} */
	private static final AtomicBoolean needToMakeSwingXWeakHashMapSynchronized = new AtomicBoolean(false);
	private static final Color PROMPT_TEXT_COLOR = new Color(160, 160, 160);

	/** whether we are on a Mac or not */
	private static final boolean IS_MAC = SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX;

	/** Defines the maximal length of characters in a line of the tool tip text. */
	private static final int TOOL_TIP_LINE_LENGTH = 100;

	/** Defines the extra height for each row in a table. */
	public static final int TABLE_ROW_EXTRA_HEIGHT = 4;

	/**
	 * Defines the extra height for rows in a table with components. If an {@link ExtendedJTable} is
	 * used, this amount can be added additionally to the amount of {@link #TABLE_ROW_EXTRA_HEIGHT}
	 * which is already added in the constructor.
	 */
	public static final int TABLE_WITH_COMPONENTS_ROW_EXTRA_HEIGHT = 10;

	/** Some color constants for Java Look and Feel. */
	public static final Color DARKEST_YELLOW = new Color(250, 219, 172);

	/** Some color constants for Java Look and Feel. */
	public static final Color DARK_YELLOW = new Color(250, 226, 190);

	/** Some color constants for Java Look and Feel. */
	public static final Color LIGHT_YELLOW = new Color(250, 233, 207);

	/** Some color constants for Java Look and Feel. */
	public static final Color LIGHTEST_YELLOW = new Color(250, 240, 225);

	/** Some color constants for Java Look and Feel. */
	public static final Color TRANSPARENT_YELLOW = new Color(255, 245, 230, 190);

	/** Some color constants for Java Look and Feel. */
	public static final Color VERY_DARK_BLUE = new Color(172, 172, 212);

	/** Some color constants for Java Look and Feel. */
	public static final Color DARK_GREEN = new Color(0, 130, 50);

	/** Some color constants for Java Look and Feel. */
	public static final Color DARKEST_BLUE = new Color(182, 202, 242);

	/** Some color constants for Java Look and Feel. */
	public static final Color DARK_BLUE = new Color(199, 213, 242);

	/** Some color constants for Java Look and Feel. */
	public static final Color LIGHT_BLUE = new Color(216, 224, 242);

	/** Some color constants for Java Look and Feel. */
	public static final Color LIGHTEST_BLUE = new Color(233, 236, 242);

	/** Some color constants for Java Look and Feel. */
	public static final Color FAINT_YELLOW = new Color(250, 240, 225, 150);

	/** Some color constants for Java Look and Feel. */
	public static final Color FAINTER_YELLOW = new Color(255, 245, 230, 50);

	/** Some color constants for Java Look and Feel. */
	public static final Color FAINT_BLUE = new Color(225, 230, 235, 150);

	/** Some color constants for Java Look and Feel. */
	public static final Color FAINTER_BLUE = new Color(230, 235, 240, 50);

	/**
	 * The RapidMiner orange color.
	 *
	 * @deprecated since RapidMiner 6.0, replaced by RAPIDMINER_ORANGE
	 */
	@Deprecated
	public static final Color RAPID_I_ORANGE = new Color(242, 146, 0);

	/**
	 * The RapidMiner brown color.
	 *
	 * @deprecated since RapidMiner 6.0, replaced by RAPIDMINER_GRAY
	 */
	@Deprecated
	public static final Color RAPID_I_BROWN = new Color(97, 66, 11);

	/**
	 * The RapidMiner beige color.
	 *
	 * @deprecated since RapidMiner 6.0, replaced by RAPIDMINER_LIGHT_GRAY
	 */
	@Deprecated
	public static final Color RAPID_I_BEIGE = new Color(202, 188, 165);
	/** The RapidMiner Orange. */
	public static final Color RAPIDMINER_ORANGE = new Color(241, 96, 34);

	/** The RapidMiner Light Orange. */
	public static final Color RAPIDMINER_LIGHT_ORANGE = new Color(235, 122, 3);

	/** The RapidMiner Yellow. */
	public static final Color RAPIDMINER_YELLOW = new Color(244, 232, 11);

	/** The RapidMiner Orange. */
	public static final Color RAPIDMINER_GRAY = new Color(50, 53, 62);

	/** The RapidMiner Light Orange. */
	public static final Color RAPIDMINER_LIGHT_GRAY = new Color(121, 124, 130);

	/** Some color constants for Java Look and Feel. */
	public static final Color LIGHTEST_RED = new Color(250, 210, 210);

	/** A brown font color. */
	public static final Color BROWN_FONT_COLOR = new Color(63, 53, 24);

	/** A brown font color. */
	public static final Color LIGHT_BROWN_FONT_COLOR = new Color(113, 103, 74);

	/** This set stores all lookup paths for icons */
	private static Set<String> iconPaths = new LinkedHashSet<>(Collections.singleton("icons/"));

	/** Contains the small frame icons in all possible sizes. */
	private static List<Image> allFrameIcons = new LinkedList<>();

	private static FrameIconProvider frameIconProvider;

	private static final String DEFAULT_FRAME_ICON_BASE_NAME = "rapidminer_frame_icon_";

	private static ParentResolvingMap<String, Color> GROUP_TO_COLOR_MAP = new StringColorMap();

	/** step width used when cropping a string */
	private static final int RESIZE_STEP_WIDTH = 3;

	/** separator used between two parts of a cropped string */
	public static final String SEPARATOR = "[...]";

	/** delay before showing the tool tip for help icons */
	private static final int TOOL_TIP_DELAY = 80;

	/**
	 * the path to the icon that is used when displaying help texts, the icon itself cannot be a
	 * constant since icons are cached and the cache is only initialized later
	 */
	private static final String HELP_ICON_PATH = "13/" + I18N.getGUIMessage("gui.label.operator_pararameters.help_icon");

	/**
	 * the property that can be used on windows to disable clear type for a component, see
	 * {@link #disableClearType(JComponent)}
	 */
	private static final Object AA_TEXT_PROPERTY = getAaTextProperty();

	/** The replacement icon for missing icons */
	private static final String UNKNOWN_ICON_NAME = "symbol_questionmark.png";
	private static final URL UNKNOWN_ICON_URL = Tools.getResource("icons/16/" + UNKNOWN_ICON_NAME);
	private static final ImageIcon UNKNOWN_ICON = new ImageIcon(UNKNOWN_ICON_URL);

	/** The prefix for mono color icons */
	private static final String MONO_COLOR_ICON_PREFIX = "/mono";

	/** The prefix for retina icons */
	private static final String RETINA_ICON_PREFIX = "/@2x";

	private static Scaling scaling = Scaling.DEFAULT;

	static {
		setupFrameIcons(DEFAULT_FRAME_ICON_BASE_NAME);
		try {
			GROUP_TO_COLOR_MAP.parseProperties("com/rapidminer/resources/groups.properties", "group.", ".color",
					OperatorDescription.class.getClassLoader(), null);
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.tools.SwingTools.loading_operator_group_colors_error");
		}

		// Detect apple retina display. Will return 2.0 with retina, 1.0 without and null if not
		// running an apple system.
		if (isRetina()) {
			scaling = Scaling.RETINA;
		}
	}

	public static void setFrameIconProvider(final FrameIconProvider _frameIconProvider) {
		frameIconProvider = _frameIconProvider;
		reloadFrameIcons();
	}

	public static void setupFrameIcons(final String frameIconBaseName) {
		setFrameIconProvider(new DefaultFrameIconProvider(frameIconBaseName));
		reloadFrameIcons();
	}

	private static void reloadFrameIcons() {
		if (frameIconProvider == null) {
			allFrameIcons = new LinkedList<>();
		} else {
			allFrameIcons = frameIconProvider.getFrameIcons();
		}
	}

	/**
	 * Tries to determine whether we are on an Apple Retina display.
	 *
	 * @return {@code true} iff we are on OS X and a scaling factor is set
	 */
	private static boolean isRetina() {
		if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
			try {
				// The lines below might throw a HeadlessException. Do not move outside of try
				// block!
				GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
				final GraphicsDevice device = env.getDefaultScreenDevice();
				Field field = device.getClass().getDeclaredField("scale");

				if (field != null) {
					field.setAccessible(true);
					Object scale = field.get(device);
					// On OS X the scale field contains the scaling factor of the device (e.g., 2
					// for '@2x' Retina devices).
					if (scale instanceof Integer && ((Integer) scale).intValue() >= 2) {
						return true;
					}
				}
			} catch (HeadlessException headless) {
				// we do not care about scaling factors on headless systems
			} catch (Exception e) {
				LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.tools.SwingTools.retina_detection_error", e);
			}
		}
		return false;
	}

	/**
	 * Returns the list of all available program icon sizes.
	 */
	public static List<Image> getFrameIconList() {
		return allFrameIcons;
	}

	/** Returns the list of all possible window icons. */
	public static void setFrameIcon(final Window window) {
		Method iconImageMethod = null;
		try {
			iconImageMethod = window.getClass().getMethod("setIconImages", new Class[] { List.class });
		} catch (Throwable e) {
			// ignore this and use single small icon below
		}

		if (iconImageMethod != null) {
			try {
				iconImageMethod.invoke(window, new Object[] { allFrameIcons });
			} catch (Throwable e) {
				// ignore this and use single small icon
				if (allFrameIcons.size() > 0) {
					window.setIconImage(allFrameIcons.get(0));
				}
			}
		} else {
			if (allFrameIcons.size() > 0) {
				window.setIconImage(allFrameIcons.get(0));
			}
		}
	}

	/** Returns the list of all possible frame icons. */
	public static void setDialogIcon(final JDialog dialog) {
		Method iconImageMethod = null;
		try {
			iconImageMethod = dialog.getClass().getMethod("setIconImages", new Class[] { List.class });
		} catch (Throwable e) {
			// ignore this and use no icons or parent icon
		}

		if (iconImageMethod != null) {
			try {
				iconImageMethod.invoke(dialog, new Object[] { allFrameIcons });
			} catch (Throwable e) {
				// ignore this and use no or parent icon
			}
		}
	}

	/** Creates a red gradient paint. */
	public static GradientPaint makeRedPaint(final double width, final double height) {
		return new GradientPaint(0f, 0f, new Color(200, 50, 50), (float) width / 2, (float) height / 2,
				new Color(255, 100, 100), true);
	}

	/** Creates a blue gradient paint. */
	public static GradientPaint makeBluePaint(final double width, final double height) {
		return new GradientPaint(0f, 0f, LIGHT_BLUE, (float) width / 2, (float) height / 2, LIGHTEST_BLUE, true);
	}

	/** Creates a yellow gradient paint. */
	public static GradientPaint makeYellowPaint(final double width, final double height) {
		return new GradientPaint(0f, 0f, LIGHT_YELLOW, (float) width / 2, (float) height / 2, LIGHTEST_YELLOW, true);
	}

	private static final Map<String, ImageIcon> ICON_CACHE = new HashMap<>(1000);

	private static final Object ICON_LOCK = new Object();

	private static final String BRACKETS = " [...] ";

	/**
	 * Tries to load the icon for the given resource. Returns {@code null} (and writes a warning) if
	 * the resource file cannot be loaded. This method automatically adds all icon paths specified
	 * since startup time. The default /icons is always searched. Additional paths might be
	 * specified by {@link SwingTools#addIconStoragePath(String)}.
	 *
	 * The given names must contain '/' instead of backslashes!
	 */
	public static ImageIcon createIcon(final String iconName) {
		return createIcon(iconName, false);
	}

	/**
	 * Tries to load the icon for the given resource. Returns {@code null} (and writes a warning) if
	 * the resource file cannot be loaded. This method automatically adds all icon paths specified
	 * since startup time. The default /icons is always searched. Additional paths might be
	 * specified by {@link SwingTools#addIconStoragePath(String)}.
	 *
	 * The given names must contain '/' instead of backslashes!
	 *
	 * @param iconName
	 *            the name of the icon including the size path component
	 * @param preferMonochrome
	 *            if {@code true} will chose a monochrome version over a colored version if it
	 *            exists (looking in the {@code [size]/mono} folder). Will fall back to colored if
	 *            no monochrome version exists.
	 */
	public static ImageIcon createIcon(final String iconName, final boolean preferMonochrome) {
		ImageIcon icon = null;
		for (String path : iconPaths) {
			if (preferMonochrome) {
				String newIconName = null;
				if (iconName.contains("16/")) {
					newIconName = iconName.replaceFirst("16/", "16/mono/");
				} else if (iconName.contains("24/")) {
					newIconName = iconName.replaceFirst("24/", "24/mono/");
				} else if (iconName.contains("32/")) {
					newIconName = iconName.replaceFirst("32/", "32/mono/");
				} else if (iconName.contains("48/")) {
					newIconName = iconName.replaceFirst("48/", "48/mono/");
				}
				icon = createImage(path + newIconName);
			} else {
				icon = createImage(path + iconName);
			}
			return icon;
		}
		return icon;
	}

	/**
	 * This method returns the path of the icon given.
	 *
	 * @param iconName
	 * @return
	 */
	public static String getIconPath(final String iconName) {
		for (String path : iconPaths) {
			ImageIcon icon = createImage(path + iconName);
			if (icon != null) {
				URL resource = Tools.getResource(path + iconName);
				if (resource != null) {
					return resource.toString();
				}
			}
		}
		return UNKNOWN_ICON_URL.toString();
	}

	/**
	 * This method adds a path to the set of paths which are searched for icons if the
	 * {@link SwingTools#createIcon(String)} is called.
	 */
	public static void addIconStoragePath(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		iconPaths.add(path);
	}

	/**
	 * Tries to load the image for the given resource. Returns {@code null} (and writes a warning)
	 * if the resource file cannot be loaded.
	 */
	public static ImageIcon createImage(final String imageName) {
		return createImage(imageName, true);
	}

	/**
	 * Same as {@link #createImage(String)} but can return {@code null} if no placeholder should be
	 * used. Uses a retina image, if running on a retina display and an icon is available.
	 *
	 * @param imageName
	 * @param usePlaceholder
	 *            if {@code true} will try to replace missing icons via placeholder. Otherwise,
	 *            returns {@code null}
	 * @return
	 */
	private static ImageIcon createImage(final String imageName, final boolean usePlaceholder) {
		synchronized (ICON_LOCK) {
			if (ICON_CACHE.containsKey(imageName)) {
				return ICON_CACHE.get(imageName);
			}

			// Whenever the image name points to a folder, return null
			// Before 7.2.3 this would return an uninitialized icon but that causes Swing NPEs
			int indexOfLastSlash = imageName.lastIndexOf("/");
			if (indexOfLastSlash != -1 && indexOfLastSlash == imageName.length() - 1) {
				// we cannot return an uninitialized icon here. This would cause an NPE inside Swing
				// internals (disabled icon painting code)
				// therefore, always return null regardless of whether the user wants a placeholder
				return null;
			}

			// Try to load high-resolution icon (if appropriate)
			if (getGUIScaling() == Scaling.RETINA) {
				// an icon path of the format ".../size(/mono)/icon.png" is expected
				if (indexOfLastSlash != -1) {
					String prefix = imageName.substring(0, indexOfLastSlash);
					if (prefix.endsWith(MONO_COLOR_ICON_PREFIX)) {
						prefix = prefix.substring(0, prefix.length() - MONO_COLOR_ICON_PREFIX.length());
					}

					int indexOfSlashBeforeResolution = prefix.lastIndexOf("/");
					if (indexOfSlashBeforeResolution >= 0) {
						String potentialSizeString = prefix.substring(indexOfSlashBeforeResolution + 1);
						try {
							int iconSize = Integer.parseInt(potentialSizeString);

							// load icon from high-dpi subfolder
							StringBuilder scaledIconName = new StringBuilder(32);
							scaledIconName.append(imageName.substring(0, indexOfLastSlash));
							scaledIconName.append(RETINA_ICON_PREFIX);
							scaledIconName.append(imageName.substring(indexOfLastSlash, imageName.length()));
							URL scaledIconUrl = Tools.getResource(scaledIconName.toString());
							if (scaledIconUrl != null) {
								ImageIcon icon = new ScaledImageIcon(scaledIconUrl, iconSize, iconSize);
								ICON_CACHE.put(imageName, icon);
								return icon;
							}

						} catch (NumberFormatException | NullPointerException e) {
							// Do nothing and fall back to normal icon. The NPE might occur if
							// the security manager intercepts the lookup of the icon (e.g., if
							// the icon is missing on OS X).
						}
					}
				}
			}

			// try to load standard-resolution icon
			URL iconUrl = Tools.getResource(imageName);
			if (iconUrl != null) {
				try {
					ImageIcon icon = new ImageIcon(iconUrl);
					ICON_CACHE.put(imageName, icon);
					return icon;
				} catch (NullPointerException e) {
					// Do nothing and fall back to placeholder icon. The NPE might occur if
					// the security manager intercepts the lookup of the icon (e.g., if
					// the icon is missing on OS X).
				}
			}

			// fall back to placeholder or null
			if (usePlaceholder) {
				LogService.getRoot().log(Level.FINE, "com.rapidminer.gui.tools.SwingTools.loading_image_error", imageName);
				if (indexOfLastSlash != -1) {
					String errorIconName = imageName.substring(0, indexOfLastSlash + 1) + UNKNOWN_ICON_NAME;
					iconUrl = Tools.getResource(errorIconName);
					if (iconUrl != null) {
						ImageIcon icon = new ImageIcon(iconUrl);
						return icon;
					}
				}
				// return default 16x16 unknown icon
				return UNKNOWN_ICON;
			} else {
				return null;
			}
		}

	}

	/**
	 * This method transforms the given tool tip text into HTML. Lines are splitted at linebreaks
	 * and additional line breaks are added after ca. {@link #TOOL_TIP_LINE_LENGTH} characters.
	 */
	public static String transformToolTipText(final String description) {
		return transformToolTipText(description, true, TOOL_TIP_LINE_LENGTH);
	}

	/**
	 * This method transforms the given tool tip text into HTML. Lines are splitted at linebreaks
	 * and additional line breaks are added after ca. {@link #TOOL_TIP_LINE_LENGTH} characters.
	 *
	 * @param escapeSlashes
	 *            Inidicates if forward slashes ("/") are escaped by the html code "&#47;"
	 */
	public static String transformToolTipText(final String description, final boolean escapeSlashes) {
		return transformToolTipText(description, true, TOOL_TIP_LINE_LENGTH, escapeSlashes);
	}

	/**
	 * This method transforms the given tool tip text into HTML. Lines are splitted at linebreaks
	 * and additional line breaks are added after ca. {@link #TOOL_TIP_LINE_LENGTH} characters.
	 *
	 * @param escapeSlashes
	 *            Inidicates if forward slashes ("/") are escaped by the html code "&#47;"
	 * @param escapeHTML
	 *            Indicates if previously added html tags are escaped
	 */
	public static String transformToolTipText(final String description, final boolean escapeSlashes,
			final boolean escapeHTML) {
		return transformToolTipText(description, true, TOOL_TIP_LINE_LENGTH, escapeSlashes, escapeHTML);
	}

	public static String transformToolTipText(final String description, final boolean addHTMLTags, final int lineLength) {
		return transformToolTipText(description, addHTMLTags, lineLength, false);
	}

	/**
	 * This method transforms the given tool tip text into HTML. Lines are splitted at linebreaks
	 * and additional line breaks are added after ca. lineLength characters.
	 *
	 * @param escapeSlashes
	 *            Inidicates if forward slashes ("/") are escaped by the html code "&#47;"
	 */
	public static String transformToolTipText(final String description, final boolean addHTMLTags, final int lineLength,
			final boolean escapeSlashes) {
		return transformToolTipText(description, addHTMLTags, lineLength, escapeSlashes, true);
	}

	/**
	 * This method transforms the given tool tip text into HTML. Lines are splitted at linebreaks
	 * and additional line breaks are added after ca. lineLength characters.
	 *
	 * @param escapeSlashes
	 *            Inidicates if forward slashes ("/") are escaped by the html code "&#47;"
	 * @param escapeHTML
	 *            Indicates if previously added html tags are escaped TODO: Use
	 *            <div style="width:XXXpx">
	 */
	public static String transformToolTipText(final String description, final boolean addHTMLTags, final int lineLength,
			final boolean escapeSlashes, final boolean escapeHTML) {
		String completeText = description.trim();
		if (escapeHTML) {
			completeText = Tools.escapeHTML(completeText);
		}
		if (escapeSlashes) {
			completeText = completeText.replaceAll("/", "&#47;");
		}
		StringBuffer result = new StringBuffer();
		if (addHTMLTags) {
			result.append("<html>");
		}
		// line.separator does not work here (transform and use \n)
		completeText = Tools.transformAllLineSeparators(completeText);
		String[] lines = completeText.split("\n");
		for (String text : lines) {
			boolean first = true;
			while (text.length() > lineLength) {
				int spaceIndex = text.indexOf(" ", lineLength);
				if (!first) {
					result.append("<br>");
				}
				first = false;
				if (spaceIndex >= 0) {
					result.append(text.substring(0, spaceIndex));
					text = text.substring(spaceIndex + 1);
				} else {
					result.append(text);
					text = "";
				}
			}
			if (!first && text.length() > 0) {
				result.append("<br>");
			}
			result.append(text);
			result.append("<br>");
		}
		if (addHTMLTags) {
			result.append("</html>");
		}
		return result.toString();
	}

	/** Adds line breaks after {@link #TOOL_TIP_LINE_LENGTH} letters. */
	public static String addLinebreaks(String message) {
		if (message == null) {
			return null;
		}
		String completeText = message.trim();
		StringBuffer result = new StringBuffer();
		// line.separator does not work here (transform and use \n)
		completeText = Tools.transformAllLineSeparators(completeText);
		String[] lines = completeText.split("\n");
		for (String text : lines) {
			boolean first = true;
			while (text.length() > TOOL_TIP_LINE_LENGTH) {
				int spaceIndex = text.indexOf(" ", TOOL_TIP_LINE_LENGTH);
				if (!first) {
					result.append(Tools.getLineSeparator());
				}
				first = false;
				if (spaceIndex >= 0) {
					result.append(text.substring(0, spaceIndex));
					text = text.substring(spaceIndex + 1);
				} else {
					result.append(text);
					text = "";
				}
			}
			if (!first && text.length() > 0) {
				result.append(Tools.getLineSeparator());
			}
			result.append(text);
			result.append(Tools.getLineSeparator());
		}
		return result.toString();
	}

	/**
	 * Capitalizes every first letter in a string. Note that if a string contains multiple words separated by a whitespace, each word will be capitalized.
	 * If the string contains more than a single whitespace between words, the spaces are reduced to a single whitespace.
	 *
	 * @param string
	 * 		the string to capitalize
	 * @return the capitalized string with a single whitespace between words, never {@code null}
	 * @since 8.1
	 */
	public static String capitalizeString(final String string) {
		if (string == null) {
			throw new IllegalArgumentException("string must not be nulL!");
		}
		if (string.trim().isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (String split : string.split("\\s+")) {
			sb.append(split.substring(0, 1).toUpperCase(Locale.ENGLISH));
			sb.append(split.substring(1));
			sb.append(' ');
		}

		return sb.toString().trim();
	}

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.results.-key-.icon
	 */
	public static void showResultsDialog(final String i18nKey, final JComponent results, final Object... i18nArgs) {
		showResultsDialog(ApplicationFrame.getApplicationFrame(), i18nKey, results, i18nArgs);
	}

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.results.-key-.icon
	 *
	 * @since 7.5.0
	 */
	public static void showResultsDialog(final Window owner, final String i18nKey, final JComponent results,
			final Object... i18nArgs) {
		invokeLater(new Runnable() {

			@Override
			public void run() {
				ResultViewDialog dialog = new ResultViewDialog(owner, i18nKey, results, i18nArgs);
				dialog.setVisible(true);
			}
		});

	}

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.message.-key-.icon
	 */
	public static void showMessageDialog(final String key, final Object... keyArguments) {
		showMessageDialog(key, null, keyArguments);
	}

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.message.-key-.icon
	 *
	 * @since 7.5.0
	 */
	public static void showMessageDialog(final Window owner, final String key, final Object... keyArguments) {
		showMessageDialog(owner, key, null, keyArguments);
	}

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.message.-key-.icon
	 */
	public static void showMessageDialog(final String key, final JComponent component, final Object... keyArguments) {
		showMessageDialog(ApplicationFrame.getApplicationFrame(), key, component, keyArguments);
	}

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.message.-key-.icon
	 *
	 * @since 7.5.0
	 */
	public static void showMessageDialog(final Window owner, final String key, final JComponent component,
			final Object... keyArguments) {
		invokeLater(new Runnable() {

			@Override
			public void run() {
				MessageDialog dialog = new MessageDialog(owner, key, component, keyArguments);
				dialog.setVisible(true);
			}
		});
	}

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.confirm.-key-.icon
	 *
	 * See {@link ConfirmDialog} for details on the mode options.
	 */
	public static int showConfirmDialog(final String key, final int mode, final Object... keyArguments) {
		return showConfirmDialog(ApplicationFrame.getApplicationFrame(), key, mode, keyArguments);
	}

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.confirm.-key-.icon
	 *
	 * See {@link ConfirmDialog} for details on the mode options.
	 *
	 * @since 7.5.0
	 */
	public static int showConfirmDialog(final Window owner, final String key, final int mode, final Object... keyArguments) {
		return invokeAndWaitWithResult(new ResultRunnable<Integer>() {

			@Override
			public Integer run() {
				ConfirmDialog dialog = new ConfirmDialog(owner, key, mode, false, keyArguments);
				dialog.setVisible(true);
				return dialog.getReturnOption();
			}
		});
	}

	/**
	 * This method will present a dialog to enter a text. This text will be returned if the user
	 * confirmed the edit. Otherwise {@code null} is returned. The key will be used for the
	 * properties gui.dialog.input.-key-.title, gui.dialog.input.-key-.message and
	 * gui.dialog.input.-key-.icon
	 */
	public static String showInputDialog(final String key, final String text, final Object... keyArguments) {
		return showInputDialog(ApplicationFrame.getApplicationFrame(), key, text, keyArguments);
	}

	/**
	 * This method will present a dialog to enter a text. This text will be returned if the user
	 * confirmed the edit. Otherwise {@code null} is returned. The key will be used for the
	 * properties gui.dialog.input.-key-.title, gui.dialog.input.-key-.message and
	 * gui.dialog.input.-key-.icon
	 *
	 * @since 7.5.0
	 */
	public static String showInputDialog(final Window owner, final String key, final String text,
			final Object... keyArguments) {
		return showInputDialog(owner, key, text, null, keyArguments);
	}

	/**
	 * This method will present a dialog to enter a text. This text will be returned if the user
	 * confirmed the edit and the validator does not report an error. Otherwise {@code null} is
	 * returned. The key will be used for the properties gui.dialog.input.-key-.title,
	 * gui.dialog.input.-key-.message and gui.dialog.input.-key-.icon
	 *
	 * @since 7.0.0
	 */
	public static String showInputDialog(final Window owner, final String key, final String text,
			final InputValidator<String> inputValidator, final Object... keyArguments) {
		return invokeAndWaitWithResult(new ResultRunnable<String>() {

			@Override
			public String run() {
				InputDialog dialog = new InputDialog(owner, key, text, inputValidator, keyArguments);
				dialog.setVisible(true);
				if (dialog.wasConfirmed()) {
					return dialog.getInputText();
				} else {
					return null;
				}
			}
		});
	}

	/**
	 * This method will present a repository entry dialog to enter a text. This text will be
	 * returned if the user confirmed the edit. Otherwise {@code null} is returned. Prevents invalid
	 * repository names. The key will be used for the properties gui.dialog.input.-key-.title,
	 * gui.dialog.input.-key-.message and gui.dialog.input.-key-.icon
	 */
	public static String showRepositoryEntryInputDialog(final String key, final String text, final Object... keyArguments) {
		return showRepositoryEntryInputDialog(ApplicationFrame.getApplicationFrame(), key, text, keyArguments);
	}

	/**
	 * This method will present a repository entry dialog to enter a text. This text will be
	 * returned if the user confirmed the edit. Otherwise {@code null} is returned. Prevents invalid
	 * repository names. The key will be used for the properties gui.dialog.input.-key-.title,
	 * gui.dialog.input.-key-.message and gui.dialog.input.-key-.icon
	 *
	 * @since 7.5.0
	 */
	public static String showRepositoryEntryInputDialog(final Window owner, final String key, final String text,
			final Object... keyArguments) {
		return invokeAndWaitWithResult(new ResultRunnable<String>() {

			@Override
			public String run() {
				RepositoryEntryInputDialog dialog = new RepositoryEntryInputDialog(owner, key, text, keyArguments);
				dialog.setVisible(true);
				if (dialog.wasConfirmed()) {
					return dialog.getInputText();
				} else {
					return null;
				}
			}
		});
	}

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.input.-key-.icon
	 */
	public static <T> T showInputDialog(final String key, final T[] selectionValues, final T initialSelectionValue,
			final Object... keyArguments) {
		return showInputDialog(ApplicationFrame.getApplicationFrame(), key, selectionValues, initialSelectionValue,
				keyArguments);
	}

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.input.-key-.icon
	 *
	 * @since 7.5.0
	 */
	public static <T> T showInputDialog(final Window owner, final String key, final T[] selectionValues,
			final T initialSelectionValue, final Object... keyArguments) {
		return showInputDialog(owner, key, false, Arrays.asList(selectionValues), initialSelectionValue, null, keyArguments);
	}

	/**
	 * This will open a simple input dialog, where a comboBox presents the given values. The
	 * Combobox might be editable depending on parameter setting.
	 *
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.input.-key-.icon
	 */
	public static <T> T showInputDialog(final String key, final boolean editable, final T[] selectionValues,
			final T initialSelectionValue, final Object... keyArguments) {
		return showInputDialog(ApplicationFrame.getApplicationFrame(), key, editable, selectionValues, initialSelectionValue,
				null, keyArguments);
	}

	/**
	 * This will open a simple input dialog, where a comboBox presents the given values. The
	 * Combobox might be editable depending on parameter setting.
	 *
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.input.-key-.icon
	 *
	 * @since 7.5.0
	 */
	public static <T> T showInputDialog(final Window owner, final String key, final boolean editable,
			final T[] selectionValues, final T initialSelectionValue, final Object... keyArguments) {
		return showInputDialog(owner, key, editable, Arrays.asList(selectionValues), initialSelectionValue, null,
				keyArguments);
	}

	/**
	 * This will open a simple input dialog, where a comboBox presents the given values. The
	 * Combobox might be editable depending on parameter setting. The selection will be returned if
	 * the user confirmed the dialog and the validator does not report an error. Otherwise
	 * {@code null} is returned.
	 *
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.input.-key-.icon
	 *
	 * @since 7.0.0
	 */
	public static <T> T showInputDialog(final Window owner, final String key, final boolean editable,
			final Collection<T> selectionValues, final T initialSelectionValue, final InputValidator<T> inputValidator,
			final Object... keyArguments) {
		return invokeAndWaitWithResult(new ResultRunnable<T>() {

			@Override
			public T run() {
				SelectionInputDialog<T> dialog = new SelectionInputDialog<>(owner, key, editable, selectionValues,
						initialSelectionValue, inputValidator, keyArguments);
				dialog.setVisible(true);
				if (dialog.wasConfirmed()) {
					return dialog.getInputSelection();
				} else {
					return null;
				}
			}
		});
	}

	/**
	 * Shows a very simple error message without any Java exception hints.
	 *
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 */
	public static void showVerySimpleErrorMessage(final String key, final Object... arguments) {
		showVerySimpleErrorMessage(ApplicationFrame.getApplicationFrame(), key, arguments);
	}

	/**
	 * Shows a very simple error message without any Java exception hints.
	 *
	 * @param owner
	 *            the owner of the opened dialog
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 * @since 7.5.0
	 */
	public static void showVerySimpleErrorMessage(final Window owner, final String key, final Object... arguments) {
		invokeLater(new Runnable() {

			@Override
			public void run() {
				ErrorDialog dialog = new ErrorDialog(owner, key, arguments);
				dialog.setModal(true);
				dialog.setVisible(true);
			}

		});
	}

	public static void showVerySimpleErrorMessageAndWait(final String key, final Object... arguments) {
		showVerySimpleErrorMessageAndWait(ApplicationFrame.getApplicationFrame(), key, arguments);
	}

	/**
	 * @since 7.5.0
	 */
	public static void showVerySimpleErrorMessageAndWait(final Window owner, final String key, final Object... arguments) {
		invokeAndWait(new Runnable() {

			@Override
			public void run() {
				ErrorDialog dialog = new ErrorDialog(owner, key, arguments);
				dialog.setModal(true);
				dialog.setVisible(true);
			}
		});
	}

	/**
	 * This is the normal method which could be used by GUI classes for errors caused by some
	 * exception (e.g. IO issues). Of course these error message methods should never be invoked by
	 * operators or similar.
	 *
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param e
	 *            the exception associated to this message
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 */
	public static void showSimpleErrorMessage(final String key, final Throwable e, final Object... arguments) {
		showSimpleErrorMessage(key, e, true, arguments);
	}

	/**
	 * This is the normal method which could be used by GUI classes for errors caused by some
	 * exception (e.g. IO issues). Of course these error message methods should never be invoked by
	 * operators or similar.
	 *
	 * @param owner
	 *            the owner of the opened dialog
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param e
	 *            the exception associated to this message
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 * @since 7.5.0
	 */
	public static void showSimpleErrorMessage(final Window owner, final String key, final Throwable e,
			final Object... arguments) {
		showSimpleErrorMessage(owner, key, e, true, arguments);
	}

	/**
	 * This is the normal method which could be used by GUI classes for errors caused by some
	 * exception (e.g. IO issues). Of course these error message methods should never be invoked by
	 * operators or similar.
	 *
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param e
	 *            the exception associated to this message
	 * @param displayExceptionMessage
	 *            indicates if the exception message will be displayed in the dialog or just in the
	 *            detailed panel
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 */
	public static void showSimpleErrorMessage(final String key, final Throwable e, final boolean displayExceptionMessage,
			final Object... arguments) {
		showSimpleErrorMessage(ApplicationFrame.getApplicationFrame(), key, e, displayExceptionMessage, arguments);
	}

	/**
	 * This is the normal method which could be used by GUI classes for errors caused by some
	 * exception (e.g. IO issues). Of course these error message methods should never be invoked by
	 * operators or similar.
	 *
	 * @param owner
	 *            the owner of the opened dialog
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param e
	 *            the exception associated to this message
	 * @param displayExceptionMessage
	 *            indicates if the exception message will be displayed in the dialog or just in the
	 *            detailed panel
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 * @since 7.5.0
	 */
	public static void showSimpleErrorMessage(final Window owner, final String key, final Throwable e,
			final boolean displayExceptionMessage, final Object... arguments) {
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_ERROR, key,
				e != null ? e.getClass().getName() : null);
		// if debug mode is enabled, send exception to logger
		if ("true".equals(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE))) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.gui.tools.SwingTools.show_simple_get_message", e.getMessage()), e);
		}
		invokeLater(new Runnable() {

			@Override
			public void run() {
				ExtendedErrorDialog dialog = new ExtendedErrorDialog(owner, key, e, displayExceptionMessage, arguments);
				dialog.setVisible(true);
			}
		});
	}

	/**
	 * This is the normal method which could be used by GUI classes for errors caused by some
	 * exception (e.g. IO issues). Of course these error message methods should never be invoked by
	 * operators or similar. The key is constructed as gui.dialog.error.-key- and uses .title and
	 * .icon properties
	 *
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param errorMessage
	 *            the error message associated to this message
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 */
	public static void showSimpleErrorMessage(final String key, final String errorMessage, final Object... arguments) {
		showSimpleErrorMessage(ApplicationFrame.getApplicationFrame(), key, errorMessage, arguments);
	}

	/**
	 * This is the normal method which could be used by GUI classes for errors caused by some
	 * exception (e.g. IO issues). Of course these error message methods should never be invoked by
	 * operators or similar. The key is constructed as gui.dialog.error.-key- and uses .title and
	 * .icon properties
	 *
	 * @param owner
	 *            the owner of the opened dialog
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param errorMessage
	 *            the error message associated to this message
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 */
	public static void showSimpleErrorMessage(final Window owner, final String key, final String errorMessage,
			final Object... arguments) {
		invokeLater(new Runnable() {

			@Override
			public void run() {
				ExtendedErrorDialog dialog = new ExtendedErrorDialog(owner, key, errorMessage, arguments);
				dialog.setVisible(true);
			}
		});

		// if debug mode is enabled, print throwable into logger
		if (ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE).equals("true")) {
			LogService.getRoot().log(Level.WARNING, errorMessage);
		}
	}

	/**
	 * Shows the final error message dialog. This dialog also allows to send a bug report if the
	 * error was not (definitely) a user error.
	 *
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param e
	 *            the exception associated to this message
	 * @param displayExceptionMessage
	 *            indicates if the exception message will be displayed in the dialog or just in the
	 *            detailed panel
	 * @param objects
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 */
	public static void showFinalErrorMessage(final String key, final Throwable e, final boolean displayExceptionMessage,
			final Object... objects) {
		showFinalErrorMessage(ApplicationFrame.getApplicationFrame(), key, e, displayExceptionMessage, objects);
	}

	/**
	 * Shows the final error message dialog. This dialog also allows to send a bug report if the
	 * error was not (definitely) a user error.
	 *
	 * @param owner
	 *            the owner of the opened dialog
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param e
	 *            the exception associated to this message
	 * @param displayExceptionMessage
	 *            indicates if the exception message will be displayed in the dialog or just in the
	 *            detailed panel
	 * @param objects
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 * @since 7.5.0
	 */
	public static void showFinalErrorMessage(final Window owner, final String key, final Throwable e,
			final boolean displayExceptionMessage, final Object... objects) {
		// if debug mode is enabled, print throwable into logger
		if (ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE).equals("true")) {
			// Some exceptions don't have messages, logging null throws a NPE
			LogService.getRoot().log(Level.SEVERE, String.valueOf(e.getMessage()), e);
		}
		invokeLater(new Runnable() {

			@Override
			public void run() {
				ExtendedErrorDialog dialog = new ExtendedErrorDialog(owner, key, e, displayExceptionMessage, objects);
				dialog.setVisible(true);
			}
		});
	}

	/**
	 * Shows the final error message dialog. This dialog also allows to send a bug report if the
	 * error was not (definitely) a user error.
	 *
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param e
	 *            the exception associated to this message
	 * @param objects
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 */
	public static void showFinalErrorMessage(final String key, final Throwable e, final Object... objects) {
		showFinalErrorMessage(key, e, false, objects);
	}

	/**
	 * Opens a file chooser with a reasonable start directory. If the extension is null, no file
	 * filters will be used.
	 */
	public static File chooseFile(final Component parent, final File file, final boolean open, final String extension,
			final String extensionDescription) {
		return chooseFile(parent, null, file, open, extension, extensionDescription);
	}

	public static File chooseFile(final Component parent, final String i18nKey, final File file, final boolean open,
			final String extension, final String extensionDescription) {
		return chooseFile(parent, i18nKey, file, open, false, extension, extensionDescription);
	}

	/**
	 * Opens a file chooser with a reasonable start directory. If the extension is null, no file
	 * filters will be used. This method allows choosing directories.
	 */
	public static File chooseFile(final Component parent, final File file, final boolean open, final boolean onlyDirs,
			final String extension, final String extensionDescription) {
		return chooseFile(parent, null, file, open, onlyDirs, extension, extensionDescription);
	}

	public static File chooseFile(final Component parent, final String i18nKey, final File file, final boolean open,
			final boolean onlyDirs, final String extension, final String extensionDescription) {
		return chooseFile(parent, i18nKey, file, open, onlyDirs, extension == null ? null : new String[] { extension },
				extensionDescription == null ? null : new String[] { extensionDescription });
	}

	public static File chooseFile(final Component parent, final String i18nKey, final File file, final boolean open,
			final boolean onlyDirs, final String extension, final String extensionDescription,
			final boolean acceptAllFiles) {
		return chooseFile(parent, i18nKey, file, open, onlyDirs, extension == null ? null : new String[] { extension },
				extensionDescription == null ? null : new String[] { extensionDescription }, acceptAllFiles);
	}

	/** Returns the user selected file. */
	public static File chooseFile(final Component parent, final File file, final boolean open, final boolean onlyDirs,
			final String[] extensions, final String[] extensionDescriptions) {
		return chooseFile(parent, null, file, open, onlyDirs, extensions, extensionDescriptions);
	}

	/**
	 * @param addDefaultAllFileExtensionFilter
	 *            if set to <code>true</code> and more than one file extension is provided a new
	 *            filter that contains all file extension will be added as default filter. This
	 *            makes sense for file reading operations that can read files with different file
	 *            extensions. For file writing operations it is however not recommended as the new
	 *            file filter will not add the correct file ending when entering the path of a file
	 *            that does not exist.
	 * @return the user selected file.
	 */
	public static File chooseFile(final Component parent, final File file, final boolean open, final boolean onlyDirs,
			final String[] extensions, final String[] extensionDescriptions,
			final boolean addDefaultAllFileExtensionFilter) {
		return chooseFile(parent, null, file, open, onlyDirs, addDefaultAllFileExtensionFilter, extensions,
				extensionDescriptions);
	}

	public static File chooseFile(final Component parent, final String i18nKey, final File file, final boolean open,
			final boolean onlyDirs, final String[] extensions, final String[] extensionDescriptions) {
		return chooseFile(parent, i18nKey, file, open, onlyDirs, false, extensions, extensionDescriptions);
	}

	public static File chooseFile(final Component parent, final String i18nKey, final File file, final boolean open,
			final boolean onlyDirs, boolean addDefaultAllFileExtensionFilter, final String[] extensions,
			final String[] extensionDescriptions) {
		List<FileFilter> fileFilters = new LinkedList<>();
		if (extensions != null) {
			int i = 0;
			if (addDefaultAllFileExtensionFilter && extensions.length > 1) {
				/*
				 * In case multiple formats are supported add a filter which shows all supported
				 * formats first
				 */
				fileFilters.add(new SimpleFileFilter(getAllExtensionFilterDescription(extensions), extensions, -1));
			}
			for (String extension : extensions) {
				if (extension != null) {
					fileFilters
							.add(new SimpleFileFilter(extensionDescriptions[i] + " (*." + extension + ")", "." + extension));
				}
				++i;
			}
		}
		return chooseFile(parent, i18nKey, file, open, onlyDirs, fileFilters.toArray(new FileFilter[fileFilters.size()]),
				true);
	}

	private static String getAllExtensionFilterDescription(String[] extensions) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String extension : extensions) {
			if (!first) {
				builder.append(", ");
			}
			builder.append("*.");
			builder.append(extension);
			first = false;
		}
		return builder.toString();
	}

	public static File chooseFile(final Component parent, final String i18nKey, final File file, final boolean open,
			final boolean onlyDirs, final String[] extensions, final String[] extensionDescriptions,
			final boolean acceptAllFiles) {
		FileFilter[] filters = null;
		if (extensions != null) {
			filters = new FileFilter[extensions.length];
			for (int i = 0; i < extensions.length; i++) {
				filters[i] = new SimpleFileFilter(extensionDescriptions[i] + " (*." + extensions[i] + ")",
						"." + extensions[i]);
			}
		}
		return chooseFile(parent, i18nKey, file, open, onlyDirs, filters, acceptAllFiles);
	}

	/**
	 * Opens a file chooser with a reasonable start directory. onlyDirs indidcates if only files or
	 * only can be selected.
	 *
	 * @param file
	 *            The initially selected value of the file chooser dialog
	 * @param open
	 *            Open or save dialog?
	 * @param onlyDirs
	 *            Only allow directories to be selected
	 * @param fileFilters
	 *            List of FileFilters to use
	 */
	private static File chooseFile(Component parent, String i18nKey, File file, boolean open, boolean onlyDirs,
			FileFilter[] fileFilters, boolean acceptAllFiles) {
		if (parent == null) {
			parent = RapidMinerGUI.getMainFrame();
		}
		String key = "file_chooser." + (i18nKey != null ? i18nKey : open ? onlyDirs ? "open_directory" : "open" : "save");
		JFileChooser fileChooser = createFileChooser(key, file, onlyDirs, fileFilters);
		fileChooser.setAcceptAllFileFilterUsed(acceptAllFiles);
		int returnValue = open ? fileChooser.showOpenDialog(parent) : fileChooser.showSaveDialog(parent);
		switch (returnValue) {
			case JFileChooser.APPROVE_OPTION:
				// check extension
				File selectedFile = fileChooser.getSelectedFile();

				FileFilter selectedFilter = fileChooser.getFileFilter();
				String extension = null;
				if (selectedFilter instanceof SimpleFileFilter) {
					SimpleFileFilter simpleFF = (SimpleFileFilter) selectedFilter;
					extension = simpleFF.getExtension();
				}
				if (extension != null) {
					if (!selectedFile.getAbsolutePath().toLowerCase().endsWith(extension.toLowerCase())) {
						selectedFile = new File(selectedFile.getAbsolutePath() + extension);
					}
				}

				storeLastDirectory(selectedFile.toPath());
				return selectedFile;
			default:
				return null;
		}
	}

	/**
	 * Stores the directory of the selectedFile under the bookmark "--- Last Directory"
	 *
	 * @param selectedFile
	 *            the file defining the last selected directory
	 * @since 7.0
	 */
	public static void storeLastDirectory(Path selectedFile) {
		if (selectedFile != null) {
			Path parentFile = selectedFile.getParent();
			if (parentFile != null) {
				File bookmarksFile = new File(FileSystemService.getUserRapidMinerDir(), ".bookmarks");
				if (bookmarksFile.exists()) {
					List<Bookmark> bookmarks = BookmarkIO.readBookmarks(bookmarksFile);
					Iterator<Bookmark> b = bookmarks.iterator();
					while (b.hasNext()) {
						Bookmark bookmark = b.next();
						if (bookmark.getName().equals("--- Last Directory")) {
							b.remove();
						}
					}
					bookmarks.add(new Bookmark("--- Last Directory", parentFile.toAbsolutePath().toString()));
					Collections.sort(bookmarks);
					BookmarkIO.writeBookmarks(bookmarks, bookmarksFile);
				}
			}
		}
	}

	/**
	 * Creates file chooser with a reasonable start directory. You may use the following code
	 * snippet in order to retrieve the file:
	 *
	 * <pre>
	 * 	if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
	 * 	    File selectedFile = fileChooser.getSelectedFile();
	 * </pre>
	 *
	 * Usually, the method {@link #chooseFile(Component, String, File, boolean, boolean, FileFilter[], boolean)} or
	 * one of the convenience wrapper methods can be used to do this. This method is only useful if
	 * one is interested, e.g., in the selected file filter.
	 *
	 * @param file
	 *            The initially selected value of the file chooser dialog
	 * @param onlyDirs
	 *            Only allow directories to be selected
	 * @param fileFilters
	 *            List of FileFilters to use
	 */
	public static JFileChooser createFileChooser(final String i18nKey, final File file, final boolean onlyDirs,
			final FileFilter[] fileFilters) {
		File directory = null;

		if (file != null) {
			if (file.isDirectory()) {
				directory = file;
			} else {
				directory = file.getAbsoluteFile().getParentFile();
			}
		} else {
			directory = FileSystemView.getFileSystemView().getDefaultDirectory();
		}

		JFileChooser fileChooser = new ExtendedJFileChooser(i18nKey, directory);
		if (onlyDirs) {
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		if (fileFilters != null) {
			fileChooser.setAcceptAllFileFilterUsed(true);
			for (FileFilter fileFilter : fileFilters) {
				fileChooser.addChoosableFileFilter(fileFilter);
			}
			if (fileFilters.length > 0) {
				fileChooser.setFileFilter(fileFilters[0]);
			}
		}

		if (file != null && !file.isDirectory()) {
			fileChooser.setSelectedFile(file);
		}

		return fileChooser;
	}

	/**
	 * Creates a panel with title and text. The panel has a border layout and the text is placed
	 * into the NORTH section.
	 */
	public static JPanel createTextPanel(final String title, final String text) {
		JPanel panel = new JPanel(new java.awt.BorderLayout());
		JLabel label = new JLabel("<html><h3>" + title + "</h3>" + (text != null ? "<p>" + text + "</p>" : "") + "</html>");
		label.setBorder(BorderFactory.createEmptyBorder(11, 11, 11, 11));
		label.setFont(label.getFont().deriveFont(java.awt.Font.PLAIN));
		panel.add(label, java.awt.BorderLayout.NORTH);
		return panel;
	}

	// ================================================================================

	// /**
	// * Replaces simple html tags and quotes by RapidMiner specific text
	// elements.
	// * These can be used in XML files without confusing an XML parser.
	// */
	// public static String html2RapidMinerText(String html) {
	// if (html == null)
	// return null;
	// String result = html.replaceAll("<", "#ylt#");
	// result = result.replaceAll(">", "#ygt#");
	// result = result.replaceAll("\"", "#yquot#");
	// result = result.replaceAll(Tools.getLineSeparator(), "");
	// return result;
	// }

	/**
	 * Returns enlarged string, with a suffix and a prefix of blanks to reach the given minimal text
	 * length
	 *
	 * Example:
	 *
	 * enlargeString("1234", 8) -> " 1234 "
	 *
	 *
	 * @param string
	 *            The string to enlarge
	 * @param minimalTextLength
	 *            Minimal length of the text
	 */
	public static String enlargeString(String string, int minimalTextLength) {
		if (string == null || string.isEmpty()) {
			throw new IllegalArgumentException("No string given");
		}
		if (minimalTextLength < 0) {
			throw new IllegalArgumentException("Number of blanks has to be positive");
		}

		int n = minimalTextLength - string.length();
		// text is large enough
		if (n <= 0) {
			return string;
		}

		int nrSpaces = (int) Math.ceil(n / 2.0);
		String spaces = String.format("%" + nrSpaces + "s", "");
		return spaces + string + spaces;
	}

	/**
	 * Replaces the RapidMiner specific tag elements by normal HTML tags. The given text is also
	 * embedded in an HTML and body tag with an appropriated style sheet definition.
	 *
	 * Currently, the only replaced tag is &lt;icon&gt;NAME&lt;/icon&gt; which will be replaced by
	 * &lt;img src="path/to/NAME"/&gt;.
	 *
	 */
	public static String text2DisplayHtml(final String text) {
		String result = "<html><head><style type=text/css>body { font-family:sans-serif; font-size:12pt; }</style></head><body>"
				+ text + "</body></html>";
		result = text2SimpleHtml(result);
		// result = result.replaceAll("#yquot#", "&quot;");
		while (result.indexOf("<icon>") != -1) {
			int startIndex = result.indexOf("<icon>");
			int endIndex = result.indexOf("</icon>");
			String start = result.substring(0, startIndex);
			String end = result.substring(endIndex + 7);
			String icon = result.substring(startIndex + 6, endIndex).trim().toLowerCase();
			java.net.URL url = Tools.getResource("icons/" + icon + ".png");
			if (url != null) {
				result = start + "<img src=\"" + url + "\"/>" + end;
			} else {
				result = start + end;
			}
		}
		return result;
	}

	/**
	 * Replaces the RapidMiner specific tag elements by normal HTML tags. This method does not embed
	 * the given text in a root HTML tag.
	 */
	private static String text2SimpleHtml(String htmlText) {
		if (htmlText == null) {
			return null;
		}
		String replaceString = htmlText;
		// replaceString = htmlText.replaceAll("#ygt#", ">");
		// replaceString = replaceString.replaceAll("#ylt#", "<");

		StringBuffer result = new StringBuffer();
		boolean afterClose = true;
		int currentLineLength = 0;
		for (int i = 0; i < replaceString.length(); i++) {
			char c = replaceString.charAt(i);
			// skip white space after close
			if (afterClose) {
				if (c == ' ') {
					continue;
				}
			}

			// opening bracket
			if (c == '<') {
				if (!afterClose) {
					result.append(Tools.getLineSeparator());
					currentLineLength = 0;
				}
			}

			// append char
			afterClose = false;
			result.append(c);
			currentLineLength++;

			// break long lines
			if (currentLineLength > 70 && c == ' ') {
				result.append(Tools.getLineSeparator());
				currentLineLength = 0;
			}

			// closing bracket
			if (c == '>') {
				result.append(Tools.getLineSeparator());
				currentLineLength = 0;
				afterClose = true;
			}
		}
		return result.toString();
	}

	/**
	 * Returns a color equivalent to the value of <code>value</code>. The value has to be normalized
	 * between 0 and 1.
	 */
	public static Color getPointColor(final double value) {
		return new Color(Color.HSBtoRGB((float) (0.68 * (1.0d - value)), 1.0f, 1.0f)); // all
		// colors
	}

	/**
	 * Returns a color equivalent to the value of <code>value</code>. The value will be normalized
	 * between 0 and 1 using the parameters max and min. Which are the minimum and maximum of the
	 * complete dataset.
	 */
	public static Color getPointColor(double value, final double max, final double min) {
		value = (value - min) / (max - min);
		return getPointColor(value);
	}

	/** Returns JEditTextArea defaults with adapted syntax color styles. */
	public static TextAreaDefaults getTextAreaDefaults() {
		TextAreaDefaults defaults = TextAreaDefaults.getDefaults();
		defaults.styles = getSyntaxStyles();
		return defaults;
	}

	/**
	 * Returns adapted syntax color and font styles matching RapidMiner colors.
	 */
	public static SyntaxStyle[] getSyntaxStyles() {
		SyntaxStyle[] styles = SyntaxUtilities.getDefaultSyntaxStyles();
		styles[Token.COMMENT1] = new SyntaxStyle(new Color(0x990033), true, false);
		styles[Token.COMMENT2] = new SyntaxStyle(Color.black, true, false);
		styles[Token.KEYWORD1] = new SyntaxStyle(Color.black, false, true);
		styles[Token.KEYWORD2] = new SyntaxStyle(new Color(255, 51, 204), false, false);
		styles[Token.KEYWORD3] = new SyntaxStyle(new Color(255, 51, 204), false, false);
		styles[Token.LITERAL1] = new SyntaxStyle(new Color(51, 51, 255), false, false);
		styles[Token.LITERAL2] = new SyntaxStyle(new Color(51, 51, 255), false, false);
		styles[Token.LABEL] = new SyntaxStyle(new Color(0x990033), false, true);
		styles[Token.OPERATOR] = new SyntaxStyle(Color.black, false, true);
		styles[Token.INVALID] = new SyntaxStyle(Color.red, false, true);
		return styles;
	}

	public static String toHTMLString(final Ports<? extends Port> ports) {
		StringBuilder b = new StringBuilder();
		boolean first = true;
		for (Port port : ports.getAllPorts()) {
			if (!first) {
				b.append(", ");
			} else {
				first = false;
			}
			b.append(port.getName());
			String desc = port.getDescription();
			if (desc.length() > 0) {
				b.append(": ");
				b.append(port.getDescription());
			}
		}
		return b.toString();
	}

	public static void showLongMessage(final String i18nKey, final String message) {
		showLongMessage(ApplicationFrame.getApplicationFrame(), i18nKey, message);
	}

	/**
	 * @since 7.5.0
	 */
	public static void showLongMessage(final Window owner, final String i18nKey, final String message) {
		invokeLater(new Runnable() {

			@Override
			public void run() {
				LongMessageDialog dialog = new LongMessageDialog(owner, i18nKey, message);
				dialog.setVisible(true);
			}
		});

	}

	public static void setEnabledRecursive(final Component c, final boolean enabled) {
		c.setEnabled(enabled);
		if (c instanceof Container) {
			for (Component child : ((Container) c).getComponents()) {
				setEnabledRecursive(child, enabled);
			}
		}
	}

	public static void setOpaqueRecursive(final Component c, final boolean enabled) {
		if (c instanceof JComponent) {
			((JComponent) c).setOpaque(enabled);
		}
		if (c instanceof Container) {
			for (Component child : ((Container) c).getComponents()) {
				setOpaqueRecursive(child, enabled);
			}
		}
	}

	public static void setProcessEditorsEnabled(final boolean enabled) {
		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		setEnabledRecursive(mainFrame.getProcessPanel().getComponent(), enabled);
		setEnabledRecursive(mainFrame.getPropertyPanel().getComponent(), enabled);
		setEnabledRecursive(mainFrame.getOperatorTree(), enabled);
		setEnabledRecursive(mainFrame.getProcessContextEditor().getComponent(), enabled);
		setEnabledRecursive(mainFrame.getXMLEditor(), enabled);

		mainFrame.getActions().enableActions();
	}

	public static Color getOperatorColor(final Operator operator) {
		OperatorDescription operatorDescription = operator.getOperatorDescription();
		String groupKey = operatorDescription.getGroup();

		/*
		 * Operators that use the extension tree root need to be handled differently.
		 */
		if (operatorDescription.isUsingExtensionTreeRoot()
				&& groupKey.startsWith(OperatorDescription.EXTENSIONS_GROUP_IDENTIFIER)) {

			// remove extension group identifier
			groupKey = groupKey.substring(groupKey.indexOf('.') + 1, groupKey.length());

			Color operatorColor = GROUP_TO_COLOR_MAP.get(groupKey, operatorDescription.getProvider());

			if (operatorColor != StringColorMap.DEFAULT_COLOR) {
				return operatorColor;
			} else {
				// either remove extension name (if more groups are present) or use extension name
				// in case of top-level operator
				groupKey = groupKey.substring(groupKey.indexOf('.') + 1, groupKey.length());
			}
		}
		return GROUP_TO_COLOR_MAP.get(groupKey, operatorDescription.getProvider());
	}

	public static Color getOperatorColor(final String operatorGroup) {
		return GROUP_TO_COLOR_MAP.get(operatorGroup);
	}

	/**
	 * This method adds the colors of the given property file to the global group colors
	 */
	public static void registerAdditionalGroupColors(final String groupProperties, final String pluginName,
			final ClassLoader classLoader, Plugin provider) {
		try {
			GROUP_TO_COLOR_MAP.parseProperties(groupProperties, "group.", ".color", classLoader, provider);
		} catch (IOException e) {
			LogService.getRoot().warning("Cannot load operator group colors for plugin " + pluginName + ".");
		}
	}

	/**
	 * Returns <code>true</code> if the given {@link MouseEvent} only exited to child components of
	 * the specified parent {@link Component}; <code>false</code> otherwise. This fixes the problem
	 * that Swing triggers MouseExited events when moving the mouse for example over a button inside
	 * the panel on which the mouse listener is.
	 *
	 * @param parent
	 * @param e
	 * @return
	 */
	public static boolean isMouseEventExitedToChildComponents(final Component parent, final MouseEvent e) {
		if (parent == null) {
			throw new IllegalArgumentException("parent must not be null!");
		}

		return SwingUtilities.getDeepestComponentAt(parent, e.getX(), e.getY()) != null;
	}

	/**
	 * Darkens the given {@link Color} by the specified factor.
	 *
	 * @param color
	 * @param factor
	 * @return
	 */
	public static Color darkenColor(final Color color, final float factor) {
		int initialAlpha = Math.min(255, (int) (color.getAlpha() * 1.1f));
		// convert to H(ue) S(aturation) B(rightness), which is designed for
		// this kind of operation
		float hsb[] = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		// turn down brightness and return color
		Color returnColor = Color.getHSBColor(hsb[0], hsb[1], factor * hsb[2]);
		return new Color(returnColor.getRed(), returnColor.getGreen(), returnColor.getBlue(), initialAlpha);
	}

	/**
	 * Changes the saturation of the given {@link Color} by multiplying the saturation with the
	 * specified factor.
	 *
	 * @param color
	 *            the color to change
	 * @param factor
	 *            the factor to multiply the current saturation with
	 * @return color with changed saturation
	 */
	public static Color saturateColor(final Color color, final float factor) {
		int initialAlpha = color.getAlpha();
		// convert to H(ue) S(aturation) B(rightness), which is designed for
		// this kind of operation
		float hsb[] = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		// adjust saturation and return color
		Color returnColor = Color.getHSBColor(hsb[0], factor * hsb[1], hsb[2]);
		return new Color(returnColor.getRed(), returnColor.getGreen(), returnColor.getBlue(), initialAlpha);
	}

	/**
	 * Makes the given {@link Color} a bit brighter, though not as much as {@link Color#brighter()}.
	 *
	 * @param color
	 * @return
	 */
	public static Color brightenColor(final Color color) {
		int initialAlpha = (int) (color.getAlpha() * 0.9f);
		// convert to H(ue) S(aturation) B(rightness), which is designed for
		// this kind of operation
		float hsb[] = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		// turn up brightness and return color
		Color returnColor = Color.getHSBColor(hsb[0], hsb[1], 0.5f * (1f + hsb[2]));
		return new Color(returnColor.getRed(), returnColor.getGreen(), returnColor.getBlue(), initialAlpha);
	}

	/**
	 * Darkens the given {@link Color} a bit, though not as much as {@link Color#darker()}.
	 *
	 * @param color
	 * @return
	 */
	public static Color darkenColor(final Color color) {
		return darkenColor(color, 0.9f);
	}

	/**
	 * Returns the hex value of this color to use in html with starting #
	 *
	 * @param color
	 *            the color that should be converted to a hex string representation
	 * @return the hex value of this color to use in html
	 * @since 7.0
	 */
	public static String getColorHexValue(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	/**
	 * Paints the given overlay icon over the base icon. This can be used for example to cross out
	 * icons. Provided the overlay icon has a transparent background, the base icon will still be
	 * visible.
	 *
	 * @param baseIcon
	 * @param overlayIcon
	 * @return
	 */
	public static ImageIcon createOverlayIcon(final ImageIcon baseIcon, final ImageIcon overlayIcon) {
		if (baseIcon == null) {
			throw new IllegalArgumentException("baseIcon must not be null!");
		}
		if (overlayIcon == null) {
			throw new IllegalArgumentException("overlayIcon must not be null!");
		}

		BufferedImage bufferedImg = new BufferedImage(baseIcon.getIconWidth(), baseIcon.getIconHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = (Graphics2D) bufferedImg.getGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		AffineTransform identityAT = new AffineTransform();
		g2.drawImage(baseIcon.getImage(), identityAT, null);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		g2.drawImage(overlayIcon.getImage(), identityAT, null);
		g2.dispose();

		return new ImageIcon(bufferedImg);
	}

	/**
	 * Creates an icon of the specified size and the given shape and colors.
	 *
	 * @param color
	 *            the color which is used to fill the shape
	 * @param borderColor
	 *            the color for the shape border
	 * @param width
	 *            the width of the icon
	 * @param height
	 *            the height of the icon
	 * @param shape
	 *            the shape of the icon
	 * @return the icon, never {@code null}
	 */
	public static ImageIcon createIconFromColor(final Color color, final Color borderColor, final int width,
			final int height, final Shape shape) {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(color);
		g2.fill(shape);

		g2.setColor(borderColor);
		g2.draw(shape);

		// completely transparent? Cross-out icon
		if (color.getAlpha() == 0) {
			g2.drawLine(shape.getBounds().x + 1, shape.getBounds().y + 1, shape.getBounds().width, shape.getBounds().height);
		}

		g2.dispose();

		return new ImageIcon(img);
	}

	/**
	 * If the input name exceeds the maximum length, it is shortened by cutting the input into two
	 * shortened parts and adding [...] in between.
	 *
	 * @param maxLength
	 *            the maximum allowed input length
	 * @return the provided input if it does not exceed the maximum input length, otherwise the
	 *         shortened input is returned.
	 */
	public static String getShortenedDisplayName(String input, int maxLength) {
		if (input.length() > maxLength + BRACKETS.length()) {
			StringBuilder builder = new StringBuilder();
			builder.append(input, 0, maxLength / 2);
			builder.append(BRACKETS);
			builder.append(input.substring(input.length() - maxLength / 2 - 1));
			return builder.toString();
		}
		return input;
	}

	/**
	 * Converts {@link KeyStroke} to a human-readable string.
	 *
	 * @param keyStroke
	 *            the keystroke
	 * @return a human-readable string like 'Ctrl+E'
	 */
	public static String formatKeyStroke(KeyStroke keyStroke) {
		StringBuilder builder = new StringBuilder();
		String modifierString = KeyEvent.getKeyModifiersText(keyStroke.getModifiers());
		String keyString = KeyEvent.getKeyText(keyStroke.getKeyCode());

		if (modifierString != null && !modifierString.trim().isEmpty()) {
			builder.append(modifierString);
			builder.append("+");
		}

		builder.append(keyString);

		return builder.toString();
	}

	/**
	 * Gets stripped text for a JComponent object
	 *
	 * @since 6.4.0
	 * @param jComponent
	 *            The component object used for the calculation of the width
	 * @param text
	 *            The text to show
	 * @param maxWidth
	 *            Maximum width of component
	 * @param suffixLength
	 *            Minimum length of the suffix after the separator
	 */
	public static String getStrippedJComponentText(JComponent jComponent, String text, int maxWidth, int suffixLength) {

		if (jComponent == null || text == null || text.isEmpty()) {
			return "";
		}
		if (maxWidth < 0) {
			throw new IllegalArgumentException("Maximum width of JComponent has to be positive");
		}

		int prefixLength = text.length() - suffixLength;
		while (getStringWidth(jComponent, text) > maxWidth) {
			prefixLength -= RESIZE_STEP_WIDTH;
			text = stripString(text, prefixLength, suffixLength);
			if (prefixLength < suffixLength) {
				break;
			}
		}

		return text;
	}

	/**
	 * Gets precalculated width of string used in jComponent with the default font.
	 *
	 * @since 6.4.0
	 * @param jComponent
	 *            The component, whose font is used
	 * @param string
	 *            The string, whose width is calculated
	 */
	public static int getStringWidth(JComponent jComponent, String string) {
		return getStringWidth(jComponent, string, jComponent.getFont());
	}

	/**
	 * Gets precalculated width of string used in jComponent.
	 *
	 * @since 6.4.0
	 * @param jComponent
	 *            The component, whose font is used
	 * @param string
	 *            The string, whose width is calculated
	 * @param font
	 *            The font of the string
	 */
	public static int getStringWidth(JComponent jComponent, String string, Font font) {
		if (jComponent == null) {
			return 0;
		}
		if (string == null || string.isEmpty()) {
			return 0;
		}

		return jComponent.getFontMetrics(font).stringWidth(string);
	}

	/**
	 * Returns stripped string, if input string is too long.
	 *
	 * Examples:
	 *
	 * stripString("1234", 2, 2) -> 1234
	 *
	 * stripString("1234", 2, 1) -> 12[...]4
	 *
	 * @since 6.4.0
	 * @param string
	 *            The string to split
	 * @param beginLength
	 *            Minimum length of prefix
	 * @param endLength
	 *            Minimum length of suffix
	 */
	public static String stripString(String string, int beginLength, int endLength) {
		if (string == null || string.isEmpty()) {
			return "";
		}
		if (beginLength < 0) {
			beginLength = 0;
		}
		if (endLength < 0) {
			endLength = 0;
		}

		if (string.length() > beginLength + endLength) {
			return string.substring(0, beginLength) + SEPARATOR + string.substring(string.length() - endLength);
		} else {
			return string;
		}
	}

	/**
	 * Returns whether or not the control modifier is down during the event. This takes the OS into
	 * account, so on Mac it will check if the meta modifier is down during the event.
	 *
	 * @param e
	 *            the event
	 * @return {@code true} if the control modifier is down on Windows/Linux or the meta modifier is
	 *         down on Mac; {@code false} otherwise
	 */
	public static boolean isControlOrMetaDown(KeyEvent e) {
		if (!IS_MAC) {
			return e.isControlDown();
		} else {
			return e.isMetaDown();
		}
	}

	/**
	 * Returns whether or not the control modifier is down during the event. This takes the OS into
	 * account, so on Mac it will check if the meta modifier is down during the event.
	 *
	 * @param e
	 *            the event
	 * @return {@code true} if the control modifier is down on Windows/Linux or the meta modifier is
	 *         down on Mac; {@code false} otherwise
	 */
	public static boolean isControlOrMetaDown(MouseEvent e) {
		if (!IS_MAC) {
			return e.isControlDown();
		} else {
			return e.isMetaDown();
		}
	}

	/**
	 * Invokes a runnable on the EDT and waits until the execution has finished. If this method is
	 * called in the EDT the runnable is executed directly. Otherwise
	 * {@link SwingUtilities#invokeAndWait(Runnable)} is used to execute the runnable.
	 *
	 * @param runnable
	 *            the {@link Runnable} to execute
	 */
	public static void invokeAndWait(final Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch (InvocationTargetException | InterruptedException ex) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.SwingTools.edt_event_failed", ex);
			}
		}
	}

	/**
	 * Invokes a runnable on the EDT and waits until the execution has finished. If this method is
	 * called in the EDT the runnable is executed directly. Otherwise
	 * {@link SwingUtilities#invokeAndWait(Runnable)} is used to execute the runnable.
	 *
	 * @param resultRunnable
	 *            the {@link ResultRunnable} to execute
	 * @return the value returned by {@link ResultRunnable}
	 * @since 6.5.0
	 */
	public static <T> T invokeAndWaitWithResult(final ResultRunnable<T> resultRunnable) {
		final ResultContainer<T> resultContainer = new ResultContainer<>();
		if (SwingUtilities.isEventDispatchThread()) {
			resultContainer.value = resultRunnable.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {

					@Override
					public void run() {
						resultContainer.value = resultRunnable.run();
					}
				});
			} catch (InvocationTargetException | InterruptedException ex) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.SwingTools.edt_event_failed", ex);
			}
		}
		return resultContainer.value;
	}

	/**
	 * Invokes a runnable on the EDT via {@link SwingUtilities#invokeLater(Runnable)} or directly if
	 * this method is called from within the EDT.
	 *
	 * @param runnable
	 *            the {@link Runnable} to execute
	 * @since 6.5.0
	 */
	public static void invokeLater(final Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			SwingUtilities.invokeLater(runnable);
		}
	}

	/**
	 * Adds a help icon to the provided JPanel which shows a tooltip when hovering over it.
	 *
	 * @param tooltipContent
	 *            the content of the tooltip
	 * @param labelPanel
	 *            the panel which will be used to add the label. The panel needs to have a
	 *            {@link BorderLayout} as layout manager as the label will be added with the
	 *            constraint {@link BorderLayout#EAST}.
	 * @param owner
	 *            the dialog owner of the labelPanel
	 * @since 7.0.0
	 */
	public static void addTooltipHelpIconToLabel(final String tooltipContent, JPanel labelPanel, final JDialog owner) {
		final JLabel helpLabel = initializeHelpLabel(labelPanel);
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				TipProvider tipProvider = new TipProvider() {

					@Override
					public String getTip(Object id) {
						if (id == null) {
							return null;
						} else {
							return tooltipContent;
						}
					}

					@Override
					public Object getIdUnder(Point point) {
						return helpLabel;
					}

					@Override
					public Component getCustomComponent(Object id) {
						return null;
					}

				};
				setupTooltip(tipProvider, owner, helpLabel);
			}

		});
	}

	/**
	 * Adds a tooltip associated with the tipProvider to the helpLabel.
	 *
	 * @param tipProvider
	 *            provides the tooltip.
	 * @param owner
	 *            the owner of the dialog of which contains the helpLabel
	 * @param helpLabel
	 *            the label for which to display the tooltip
	 * @since 7.0.0
	 */
	public static void setupTooltip(TipProvider tipProvider, final JDialog owner, final JLabel helpLabel) {
		ToolTipWindow toolTipWindow = new ToolTipWindow(owner, tipProvider, helpLabel, TooltipLocation.BELOW);
		toolTipWindow.setOnlyWhenFocussed(false);
		toolTipWindow.setToolTipDelay(TOOL_TIP_DELAY);
	}

	/**
	 * Creates a helpLabel and adds it to the labelPanel. The helpLabel shows a help icon.
	 *
	 * @param labelPanel
	 *            the panel which will be used to add the label. The panel needs to have a
	 *            {@link BorderLayout} as layout manager as the label will be added with the
	 *            constraint {@link BorderLayout#EAST}.
	 * @return the helpLabel for further use
	 * @since 7.0.0
	 */
	public static JLabel initializeHelpLabel(JPanel labelPanel) {
		JPanel helpPanel = new JPanel();
		helpPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 3, 0, 0);

		final JLabel helpLabel = new JLabel();
		helpLabel.setIcon(createIcon(HELP_ICON_PATH));
		helpLabel.setFocusable(false);

		gbc.anchor = GridBagConstraints.CENTER;
		helpPanel.add(helpLabel, gbc);
		gbc.gridy += 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 1.0;
		gbc.insets = new Insets(0, 0, 0, 0);
		helpPanel.add(new JLabel(), gbc);

		labelPanel.add(helpPanel, BorderLayout.EAST);
		return helpLabel;
	}

	/**
	 * Attempts to disable the default clear type rendering on Windows such that rendering hints can
	 * be used. Has no effect on operating systems other than Microsoft Windows. Might fail
	 * silently.
	 *
	 * @param component
	 *            the component for which to disable clear type in order to use rendering hints
	 */
	public static void disableClearType(JComponent component) {
		if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.WINDOWS && AA_TEXT_PROPERTY != null) {
			component.putClientProperty(AA_TEXT_PROPERTY, null);
		}
	}

	/**
	 * Creates a panel which is shown and fades slowly away.
	 *
	 * @param iconKey
	 *            the I18N-string for the icon on the panel
	 * @param messageKey
	 *            the I18N-string for the message on the panel
	 * @return a panel with specified icon and message
	 */
	public static JPanel createNotificationPanel(String iconKey, String messageKey) {
		JPanel notificationPanel = new JPanel() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {

				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				Color bg = getBackground();
				GradientPaint gp = new GradientPaint(0, 0, bg.brighter(), 0, getHeight(), bg);
				g2.setPaint(gp);
				g2.fillRect(0, 0, getWidth(), getHeight());
			}

		};

		notificationPanel.setLayout(new BoxLayout(notificationPanel, BoxLayout.X_AXIS));

		JLabel icon = new JLabel();
		icon.setIcon(SwingTools.createIcon("48/" + I18N.getGUIMessage(iconKey)));
		icon.setBorder(new EmptyBorder(20, 20, 20, 10));

		JLabel label = new JLabel("<html><div style='font-size:12px;'>" + I18N.getGUIMessage(messageKey) + "</div></html>");
		label.setBorder(new EmptyBorder(20, 10, 20, 20));

		notificationPanel.add(icon);
		notificationPanel.add(label);
		return notificationPanel;
	}

	/**
	 * The GUI scaling.
	 *
	 * @return either {@link Scaling#RETINA} for high DPI displays, or {@link Scaling#DEFAULT} for regular displays.
	 * @since 9.0.0
	 */
	public static Scaling getGUIScaling() {
		return scaling;
	}

	/**
	 * Looks up the internal property {@code SwingUtilities2.AA_TEXT_PROPERTY_KEY}, returns
	 * {@code null} if not defined or if running on an operating system other than Microsoft
	 * Windows.
	 * <p>
	 * The implementation uses reflection to look up the property, since it it not part of the
	 * official Swing specification and might not be available.
	 *
	 * @return the property to disable clear type on windows or {@code null}
	 * @see "http://stackoverflow.com/questions/18764585/text-antialiasing-broken-in-java-1-7-windows"
	 */
	private static Object getAaTextProperty() {
		Object aatextProperty = null;
		if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.WINDOWS) {
			try {
				Class<?> clazz = Class.forName("sun.swing.SwingUtilities2");
				Field field = clazz.getField("AA_TEXT_PROPERTY_KEY");
				if (field != null) {
					aatextProperty = field.get(null);
				}
			} catch (Throwable e) {
				// do nothing, cannot use the property
			}
		}
		return aatextProperty;
	}

	/**
	 * Returns the first visible component, aka the component that is currently displayed. Can be used to find the
	 * currently displayed card in a {@link java.awt.CardLayout}.
	 *
	 * @param parent
	 * 		the container which contains all the cards
	 * @return the component or {@code null} if no component of the parent is visible
	 * @since 9.0.0
	 */
	public static Component findDisplayedComponent(Container parent) {
		for (Component comp : parent.getComponents()) {
			if (comp.isVisible()) {
				return comp;
			}
		}

		return null;
	}

	/**
	 * Replaces {@link PromptSupport#setPrompt(String, JTextComponent)}, as that library contains a bug that can cause
	 * the entire Studio UI to freeze permanently.
	 *
	 * @param promptText
	 * 		the prompt text or {@code null} if an existing prompt text should be removed
	 * @param textComponent
	 * 		the text component for which the prompt should be, must not be {@code null}
	 * @since 9.3.0
	 */
	public static void setPrompt(String promptText, JTextComponent textComponent) {
		if (textComponent == null) {
			throw new IllegalArgumentException("textComponent must not be null!");
		}

		PromptSupport.setPrompt(promptText, textComponent);
		PromptSupport.setForeground(PROMPT_TEXT_COLOR, textComponent);
		PromptSupport.setFontStyle(Font.ITALIC, textComponent);
		PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.SHOW_PROMPT, textComponent);

		// we already exchanged the map with a synchronized map in SwingX, nothing to do anymore
		if (!needToMakeSwingXWeakHashMapSynchronized.compareAndSet(false, true)) {
			return;
		}

		// we need to exchange the WeakHashMap with a synchronized one once (as it's the same instance all the time)
		// otherwise we can get a permanent freeze of the UI due to a broken map (https://mailinator.blogspot.com/2009/06/beautiful-race-condition.html)
		AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
			try {
				PropertyChangeListener[] uiListener = textComponent.getPropertyChangeListeners("UI");
				for (PropertyChangeListener listener : uiListener) {
					if (listener instanceof AbstractUIChangeHandler) {
						Field installedWeakHashMap = AbstractUIChangeHandler.class.getDeclaredField("installed");
						installedWeakHashMap.setAccessible(true);
						Map mapObject = (Map) installedWeakHashMap.get(listener);
						if (mapObject instanceof WeakHashMap) {
							installedWeakHashMap.set(listener, Collections.synchronizedMap(mapObject));
						}
					}
				}
			} catch (NoSuchFieldException | IllegalAccessException e) {
				// should not happen
				LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.tools.SwingTools.failed_hashmap_fix", e);
			}

			return null;
		});
	}

	/**
	 * Creates an action that, when triggered, searches the marketplace for the given namespace and offers to download
	 * the extension.
	 *
	 * @param i18nKey
	 * 		the i18n key that the action uses. Will be resolved to {@code gui.action.{i18nkey}.xyz} where xyz out of
	 *        {label, icon, tip, mne}
	 * @param namespace
	 * 		the namespace, e.g. {@code rmx_my_extension}
	 * @return the action, never {@code null}
	 * @since 9.3.0
	 */
	public static ResourceAction createMarketplaceDownloadActionForNamespace(String i18nKey, String namespace) {
		return new ResourceAction(i18nKey, namespace) {

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				new ProgressThread("search_extension_on_mp") {

					@Override
					public void run() {
						try {
							String extensionId = UpdateManagerRegistry.INSTANCE.get().getExtensionIdForOperatorPrefix(namespace);
							if (extensionId != null) {
								UpdateManagerRegistry.INSTANCE.get().showUpdateDialog(false, extensionId);
							} else {
								SwingTools.showVerySimpleErrorMessage("extension_unknown", namespace);
							}
						} catch (URISyntaxException | IOException e1) {
							SwingTools.showSimpleErrorMessage("marketplace_connection_error", e1);
						}
					}
				}.start();
			}

		};
	}
}
