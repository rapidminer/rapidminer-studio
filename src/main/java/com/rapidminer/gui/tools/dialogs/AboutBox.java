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
package com.rapidminer.gui.tools.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.license.LicenseTools;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.gui.tools.VersionNumber.VersionNumberException;
import com.rapidminer.gui.tools.components.LinkRemoteButton;
import com.rapidminer.license.License;
import com.rapidminer.license.StudioLicenseConstants;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.PlatformUtilities;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.plugin.Plugin;


/**
 * This dialog displays some informations about the product. The product logo should have a size of
 * approximately 270 times 70 pixels.
 *
 * @author Ingo Mierswa
 */
public class AboutBox extends JDialog {

	private static final long serialVersionUID = -3889559376722324215L;

	private static final int MAX_SHOWN_LINK_LENGTH = 50;

	private static final String PROPERTY_FILE = "about_infos.properties";

	private static final String DEFAULT_VENDOR = "RapidMiner";
	private static final String DEFAULT_VENDOR_OLD = "Rapid-I";

	public static Image backgroundImage = null;

	public static Image backgroundImageWithoutLogo = null;

	static {
		URL url = Tools.getResource("about_background.png");
		if (url != null) {
			try {
				backgroundImage = ImageIO.read(url);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.tools.dialogs.AboutBox.loading_background_error");
			}
		}
		url = Tools.getResource("about_background_wo_logo.png");
		if (url != null) {
			try {
				backgroundImageWithoutLogo = ImageIO.read(url);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.tools.dialogs.AboutBox.loading_background_error");
			}
		}
	}

	private final ContentPanel contentPanel;

	private static class ContentPanel extends JPanel {

		private static final String[] DISPLAYED_KEYS = new String[] { "copyright", "licensor", "license" };

		private static final Font FONT_SANS_SERIF_11 = FontTools.getFont(Font.SANS_SERIF, Font.PLAIN, 11);
		private static final Font FONT_SANS_SERIF_BOLD_11 = FontTools.getFont(Font.SANS_SERIF, Font.BOLD, 11);
		private static final Font FONT_SANS_SERIF_BOLD_26 = FontTools.getFont(Font.SANS_SERIF, Font.BOLD, 26);
		private static final Font FONT_OPEN_SANS_15 = FontTools.getFont("Open Sans", Font.PLAIN, 15);

		private static final List<Font> FONTS_PRODUCT_NAME = new ArrayList<>(15);

		static {
			for (int size = 60; size >= 8; size -= 4) {
				FONTS_PRODUCT_NAME.add(FontTools.getFont("Open Sans Light", Font.PLAIN, size));
			}
		}

		private static final long serialVersionUID = -1763842074674706654L;

		private static final int LOGO_INSET_Y = 342;
		private static final int LOGO_INSET_X = 10;

		private static final int ADDITIONAL_LINE_HEIGHT = 15;

		private static final Paint MAIN_PAINT = new Color(96, 96, 96);

		private static final int MARGIN = 20;

		private final Properties properties;

		private transient Image productLogo;

		public ContentPanel(Properties properties, Image productLogo) {
			this.properties = properties;
			this.productLogo = productLogo;
			int width = 550;
			int height = 400;
			if (backgroundImage != null) {
				width = backgroundImage.getWidth(this);
				height = backgroundImage.getHeight(this);
			}

			// Add additional space, if we display more than two keys
			int foundKeys = 0;
			for (String key : DISPLAYED_KEYS) {
				if (properties.containsKey(key)) {
					foundKeys++;
					if (foundKeys > 2) {
						height += ADDITIONAL_LINE_HEIGHT;
					}
				}
			}
			setPreferredSize(new Dimension(width, height));
			setMinimumSize(new Dimension(width, height));
			setMaximumSize(new Dimension(width, height));
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			drawMain(g2d);
			g2d.dispose();
		}

		public void drawMain(Graphics2D g) {
			g.setPaint(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());

			// draw the background image without RapidMiner branding if the vendor is not RM
			if (properties.get("licensor") != null && !String.valueOf(properties.get("licensor")).contains(DEFAULT_VENDOR)
					&& !String.valueOf(properties.get("licensor")).contains(DEFAULT_VENDOR_OLD)
					&& backgroundImageWithoutLogo != null) {
				g.drawImage(backgroundImageWithoutLogo, 0, 0, this);
			} else if (backgroundImage != null) {
				g.drawImage(backgroundImage, 0, 0, this);
			}

			g.setFont(FONT_SANS_SERIF_BOLD_26);
			if (productLogo != null) {
				g.drawImage(productLogo, LOGO_INSET_X, LOGO_INSET_Y, this);
			}

			String productName = removeLeadingRapidMinerString(properties.getProperty("name"));
			g.setFont(FONTS_PRODUCT_NAME.get(FONTS_PRODUCT_NAME.size() - 1));
			g.setPaint(Color.WHITE);
			for (Font f : FONTS_PRODUCT_NAME) {
				if (getFontMetrics(f).stringWidth(productName) <= getSize().width - 2 * MARGIN) {
					g.setFont(f);
					break;
				}
			}
			FontMetrics fm = getFontMetrics(g.getFont());
			int x_product = (getSize().width - fm.stringWidth(productName)) / 2;
			int y_product = ((backgroundImage != null ? backgroundImage.getHeight(null) : getSize().height) - 70
					- fm.getHeight()) / 2 + fm.getAscent();
			g.drawString(productName, x_product, y_product);

			StringBuilder builder = new StringBuilder();
			builder.append(I18N.getGUILabel("version"));
			builder.append(" ");
			VersionNumber versionNumber = null;
			if (properties.getProperty("version") != null) {
				try {
					versionNumber = new VersionNumber(properties.getProperty("version"));
				} catch (VersionNumberException e) {
					// nothing to do
				}
			}
			builder.append(versionNumber != null ? versionNumber.getShortVersion() : I18N.getGUILabel("unknown_version"));
			String version = builder.toString();
			int x_version = x_product + fm.stringWidth(productName);
			Rectangle2D bounds = getStringBounds(g, productName, x_product, y_product);
			int y_version = (int) (bounds.getY() + bounds.getHeight());
			g.setFont(FONT_OPEN_SANS_15);
			fm = getFontMetrics(g.getFont());

			x_version -= fm.stringWidth(version);
			y_version += fm.getHeight();
			g.drawString(version, x_version, y_version);
			g.setPaint(MAIN_PAINT);

			int y = 355;
			g.setFont(FONT_SANS_SERIF_BOLD_11);
			builder = new StringBuilder();
			builder.append(properties.getProperty("name"));
			builder.append(" ");
			builder.append(properties.getProperty("version"));
			String revision = properties.getProperty("revision");
			if (revision != null) {
				builder.append(" (rev: ");
				builder.append(revision.substring(0, 6));
				String platform = properties.getProperty("platform");
				if (platform != null) {
					builder.append(", platform: ");
					builder.append(platform);
				}
				builder.append(")");
			}

			drawString(g, builder.toString(), y);
			y += 15;

			g.setFont(FONT_SANS_SERIF_11);
			builder = new StringBuilder();
			if (properties.getProperty("edition") != null) {
				builder.append(properties.getProperty("edition"));
			}
			if (properties.getProperty("registered_to") != null) {
				builder.append(" ");
				builder.append(I18N.getGUILabel("registered_to", properties.getProperty("registered_to")));
			}
			String edition = builder.toString();
			if (!edition.trim().isEmpty()) {
				drawString(g, builder.toString(), y);
				y += 15;
			}

			for (String key : DISPLAYED_KEYS) {
				y = drawStringAndAdvance(g, properties.getProperty(key), y);
			}
		}

		private Rectangle getStringBounds(Graphics2D g2, String str, float x, float y) {
			FontRenderContext frc = g2.getFontRenderContext();
			GlyphVector gv = g2.getFont().createGlyphVector(frc, str);
			return gv.getPixelBounds(null, x, y);
		}

		private int drawStringAndAdvance(Graphics2D g, String string, int y) {
			if (string == null) {
				return y;
			} else {
				List<String> lines = new LinkedList<>();
				String[] words = string.split("\\s+");
				String current = "";
				for (String word : words) {
					if (current.length() + word.length() < 80) {
						current += word + " ";
					} else {
						lines.add(current);
						current = word + " ";
					}
				}
				if (!current.isEmpty()) {
					lines.add(current);
				}
				for (String line : lines) {
					drawString(g, line, y);
					y += 15;
				}
				return y;
			}
		}

		private void drawString(Graphics2D g, String text, int y) {
			drawString(g, text, y, MARGIN + (productLogo != null ? productLogo.getWidth(null) : 0));
		}

		private void drawString(Graphics2D g, String text, int y, int x) {
			if (text == null) {
				return;
			}
			float xPos = x;
			float yPos = y;
			g.drawString(text, xPos, yPos);
		}
	}

	public AboutBox(Frame owner, String productName, String productVersion, String licensor, String url, String text,
			boolean renderTextNextToLogo, Image productLogo) {
		this(owner, createProperties(productName, productVersion, licensor, url, text, renderTextNextToLogo), productLogo);
	}

	public AboutBox(Frame owner, String productVersion, License license, Image productLogo) {
		this(owner, createProperties(productVersion, license), productLogo);
	}

	public AboutBox(Frame owner, String productVersion, License license) {
		this(owner, createProperties(productVersion, license), null);
	}

	public AboutBox(Frame owner, Properties properties, Image productLogo) {
		super(owner, "About", true);
		setResizable(false);

		setLayout(new BorderLayout());

		String name = properties.getProperty("name");
		if (name != null) {
			setTitle("About " + name);
		}
		contentPanel = new ContentPanel(properties, productLogo);
		add(contentPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final String url = properties.getProperty("url");
		if (url != null) {
			String shownURL = url;
			if (shownURL.length() > MAX_SHOWN_LINK_LENGTH) {
				shownURL = shownURL.substring(0, MAX_SHOWN_LINK_LENGTH) + "...";
			}
			buttonPanel.add(new LinkRemoteButton(new ResourceAction("link_action", url, shownURL) {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					RMUrlHandler.openInBrowser(url);
				}
			}));
		}

		ResourceAction closeAction = new ResourceAction("close") {

			private static final long serialVersionUID = 1407089394491740308L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				dispose();
			}

		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", closeAction);

		pack();
		if (owner != null) {
			setLocationRelativeTo(owner);
		} else {
			setLocationRelativeTo(ApplicationFrame.getApplicationFrame());
		}
	}

	public static Properties createProperties(InputStream inputStream, String productVersion) {
		Properties properties = new Properties();
		if (inputStream != null) {
			try {
				properties.load(inputStream);
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE,
						"com.rapidminer.gui.tools.dialogs.AboutBox.reading_splash_screen_error", e.getMessage());
			}
		}
		properties.setProperty("version", productVersion);
		Plugin.initAboutTexts(properties);
		return properties;
	}

	private static Properties createProperties(String productVersion, License license) {
		Properties properties = new Properties();
		try {
			URL propUrl = Tools.getResource(PROPERTY_FILE);
			if (propUrl != null) {
				InputStream in = propUrl.openStream();
				properties.load(in);
				in.close();
			}
		} catch (Exception e) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.tools.dialogs.AboutBox.reading_splash_screen_error",
					e.getMessage());
		}
		properties.setProperty("name", LicenseTools.translateProductName(license));
		properties.setProperty("version", productVersion);
		if (StudioLicenseConstants.PRODUCT_ID.equals(license.getProductId())
				&& PlatformUtilities.getReleaseRevision() != null) {
			properties.setProperty("revision", PlatformUtilities.getReleaseRevision());
			properties.setProperty("platform", PlatformUtilities.getReleasePlatform().toString());
		}
		properties.setProperty("edition",
				I18N.getGUILabel("license_edition", LicenseTools.translateProductEdition(license)));
		if (license.getLicenseUser().getName() != null) {
			properties.setProperty("registered_to", license.getLicenseUser().getName());
		}
		Plugin.initAboutTexts(properties);
		return properties;
	}

	private static Properties createProperties(String productName, String productVersion, String licensor, String url,
			String text, boolean renderTextNextToLogo) {
		Properties properties = new Properties();
		properties.setProperty("name", productName);
		properties.setProperty("version", productVersion);
		properties.setProperty("licensor", licensor);
		properties.setProperty("license", "Website: " + url);
		properties.setProperty("more", text);
		properties.setProperty("textNextToLogo", "" + renderTextNextToLogo);
		properties.setProperty("url", url);
		return properties;
	}

	/**
	 * This method removes a leading "RapidMiner " String from e.g. product names.
	 */
	private static String removeLeadingRapidMinerString(String productString) {
		return productString.startsWith("RapidMiner ") ? productString.substring(11) : productString;
	}

}
