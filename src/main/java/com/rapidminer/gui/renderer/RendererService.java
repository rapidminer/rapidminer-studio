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
package com.rapidminer.gui.renderer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.tools.DominatingClassFinder;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.WebServiceTools;


/**
 * The renderer service is the basic provider for all registered renderers. All {@link IOObject}s
 * which want to provide a Renderer for visualization and reporting must place an entry in the
 * <code>ioobjects.xml</xml> file in order to allow
 * for renderer retrieval.
 * 
 * @author Ingo Mierswa, Nils Woehler
 */
public class RendererService {

	/** Used in {@link RendererService#class2IconMap}. */
	private static class IconData {

		private final String iconName;
		private final Icon icon;

		public IconData(String iconName, Icon icon) {
			this.iconName = iconName;
			this.icon = icon;
		}

		public Icon getIcon() {
			return icon;
		}

		public String getIconName() {
			return iconName;
		}
	}

	private static final IconData ICON_DEFAULT = new IconData("data.png", SwingTools.createIcon("16/data.png"));

	private static Set<String> objectNames = new TreeSet<>();

	/**
	 * Maps names of IOObjects to lists of renderers that can render this object. These instances
	 * are shared!
	 */
	private static Map<String, List<Renderer>> objectRenderers = new HashMap<>();

	/** Maps names of IOObjects to lists of renderer classes that can render this object. */
	private static Map<String, Map<String, Class<? extends Renderer>>> rendererNameToRendererClasses = new HashMap<>();

	private static Map<String, Class<? extends IOObject>> objectClassesByReportableName = new HashMap<>();

	private static Map<String, Class<? extends IOObject>> objectClassesByClassName = new HashMap<>();

	/** Set of names of reportable objects. */
	private static Set<String> reportableMap = new HashSet<>();

	private static Map<Class<?>, String> class2NameMap = new HashMap<>();

	private static Map<Class<? extends IOObject>, IconData> class2IconMap = new HashMap<>();

	private static boolean isInitialized = false;

	public static void init() {
		URL url = Tools.getResource("ioobjects.xml");
		init(url);
		init("ioobjects.xml", url, RendererService.class.getClassLoader());
	}

	public static void init(URL ioObjectsURL) {
		init(ioObjectsURL.getFile(), ioObjectsURL, RendererService.class.getClassLoader());
	}

	public static void init(String name, InputStream in) {
		init(name, in, RendererService.class.getClassLoader());
	}

	public static void init(String name, URL ioObjectsURL, ClassLoader classLoader) {
		InputStream in = null;
		try {
			if (ioObjectsURL != null) {
				in = WebServiceTools.openStreamFromURL(ioObjectsURL);
				init(name, in, classLoader);
			}
		} catch (IOException e) {
			LogService
					.getRoot()
					.log(Level.WARNING,
							I18N.getMessage(
									LogService.getRoot().getResourceBundle(),
									"com.rapidminer.gui.renderer.RendererService.initializing_io_object_description_from_plugin_error",
									name, e), e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
	}

	public static void init(String rendererFileName, InputStream in, ClassLoader classLoader) {
		LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.renderer.RendererService.loading_renderers",
				rendererFileName);
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
			Element ioObjectsElement = document.getDocumentElement();
			if (ioObjectsElement.getTagName().equals("ioobjects")) {
				NodeList ioObjectNodes = ioObjectsElement.getElementsByTagName("ioobject");
				for (int i = 0; i < ioObjectNodes.getLength(); i++) {
					Node ioObjectNode = ioObjectNodes.item(i);
					if (ioObjectNode instanceof Element) {
						Element ioObjectElement = (Element) ioObjectNode;

						String name = ioObjectElement.getAttribute("name");
						String className = ioObjectElement.getAttribute("class");
						String reportableString = "true";
						if (ioObjectElement.hasAttribute("reportable")) {
							reportableString = ioObjectElement.getAttribute("reportable");
						}
						boolean reportable = Tools.booleanValue(reportableString, true);

						String icon = null;
						if (ioObjectElement.hasAttribute("icon")) {
							icon = ioObjectElement.getAttribute("icon");
						}

						NodeList rendererNodes = ioObjectElement.getElementsByTagName("renderer");
						List<String> renderers = new LinkedList<>();
						for (int k = 0; k < rendererNodes.getLength(); k++) {
							Node rendererNode = rendererNodes.item(k);
							if (rendererNode instanceof Element) {
								Element rendererElement = (Element) rendererNode;
								String rendererName = rendererElement.getTextContent();
								renderers.add(rendererName);
							}
						}

						registerRenderers(name, className, reportable, icon, renderers, classLoader);
					}
				}
				isInitialized = true;
			} else {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.renderer.RendererService.initializing_io_object_description_tag_error");
			}
		} catch (IOException e) {
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.renderer.RendererService.initializing_io_object_description_parsing_error",
							e), e);
		} catch (javax.xml.parsers.ParserConfigurationException e) {
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.renderer.RendererService.initializing_io_object_description_error", e), e);
		} catch (SAXException e) {
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.renderer.RendererService.initializing_io_object_description_parsing_error",
							e), e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}

	}

	/**
	 * This method remains for compatibility reasons. But one should use
	 * {@link #registerRenderers(String, String, boolean, String, List, ClassLoader)} in order to
	 * assign an icon to the ioobject class.
	 */
	@Deprecated
	public static void registerRenderers(String reportableNames, String className, boolean reportable,
			List<String> rendererClassNames, ClassLoader classLoader) {
		registerRenderers(reportableNames, className, reportable, null, rendererClassNames, classLoader);
	}

	@SuppressWarnings("unchecked")
	public static void registerRenderers(String reportableName, String className, boolean reportable, String iconName,
			List<String> rendererClassNames, ClassLoader classLoader) {
		objectNames.add(reportableName);

		try {

			Class<? extends IOObject> clazz = (Class<? extends IOObject>) Class.forName(className, true, classLoader);

			List<Renderer> renderers = new LinkedList<>();
			Map<String, Class<? extends Renderer>> rendererClassMap = new HashMap<>();
			for (String rendererClassName : rendererClassNames) {
				Class<? extends Renderer> rendererClass;
				try {
					rendererClass = (Class<? extends Renderer>) Class.forName(rendererClassName, true, classLoader);
				} catch (Exception e) { // should be unnecessary in most cases, because plugin
										// loader contains core
					// classes
					rendererClass = (Class<? extends Renderer>) Class.forName(rendererClassName);
				}
				Renderer renderer = rendererClass.newInstance();
				renderers.add(renderer);
				rendererClassMap.put(renderer.getName(), rendererClass);
			}

			rendererNameToRendererClasses.put(reportableName, rendererClassMap);
			objectRenderers.put(reportableName, renderers);
			objectClassesByReportableName.put(reportableName, clazz);
			objectClassesByClassName.put(className, clazz);
			class2NameMap.put(clazz, reportableName);
			if (reportable) {
				reportableMap.add(reportableName);
			}

			// try to create icon
			if (iconName != null && !iconName.isEmpty()) {
				ImageIcon icon = SwingTools.createIcon("16/" + iconName);
				if (icon != null) {
					class2IconMap.put(clazz, new IconData(iconName, icon));
				}
			}

		} catch (Throwable e) {
			// LogService.getRoot().log(Level.WARNING, "Cannot register renderer: " + e, e);
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.renderer.RendererService.registering_renderer_error", e), e);

		}
	}

	public static Set<String> getAllRenderableObjectNames() {
		return objectNames;
	}

	public static Set<String> getAllReportableObjectNames() {
		Set<String> result = new TreeSet<>();
		for (String name : objectNames) {
			if (reportableMap.contains(name)) {
				result.add(name);
			}
		}
		return result;
	}

	/**
	 * Returns the Reportable name for objects of the given class.
	 *
	 * @return the name of the IOObject as specified in the ioobjectsXXX.xml file. Returns null if
	 *         the object is not registered
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String getName(Class<?> clazz) {
		String result = class2NameMap.get(clazz);
		if (result == null) {

			Class<?> parentClass = new DominatingClassFinder().findNextDominatingClass(clazz, class2NameMap.keySet());

			if (parentClass == null) {
				return null;
			} else {
				return class2NameMap.get(parentClass);
			}
		}
		return result;
	}

	/**
	 * This returns the highest super class of the report type with the given reportable name.
	 */
	public static Class<? extends IOObject> getClass(String reportableName) {
		return objectClassesByReportableName.get(reportableName);
	}

	/**
	 * This returns the highest super class of the report type with the given class name.
	 */
	public static Class<? extends IOObject> getClassByClassName(String className) {
		return objectClassesByClassName.get(className);
	}

	/**
	 * Returns a list of renderers defined for this IOObject name (as returned by
	 * {@link #getName(Class)} for the respective object). It is recommended to use
	 * {@link #getRenderers(IOObject)} instead.
	 * */
	public static List<Renderer> getRenderers(String reportableName) {
		List<Renderer> renderers = objectRenderers.get(reportableName);
		if (renderers != null) {
			return renderers;
		}
		return new LinkedList<>();
	}

	/** Returns a list of shared (i.e. not thread-safe!) renderers defined for this IOObject. */
	public static List<Renderer> getRenderers(IOObject ioo) {
		String reportableName = RendererService.getName(ioo.getClass());
		return getRenderers(reportableName);
	}

	public static Renderer getRenderer(String reportableName, String rendererName) {
		List<Renderer> renderers = getRenderers(reportableName);
		for (Renderer renderer : renderers) {
			if (renderer.getName().equals(rendererName)) {
				return renderer;
			}
		}
		return null;
	}

	/**
	 * This returns the icon registered for the given class or a default icon, if nothing has been
	 * registered.
	 */
	public static Icon getIcon(Class<? extends IOObject> objectClass) {
		return getIconData(objectClass).getIcon();
	}

	/**
	 * This returns the icon name registered for the given class or a default icon, if nothing has
	 * been registered.
	 */
	public static String getIconName(Class<? extends IOObject> objectClass) {
		return getIconData(objectClass).getIconName();
	}

	private static IconData getIconData(Class<? extends IOObject> objectClass) {
		if (objectClass == null) {
			return ICON_DEFAULT;
		}
		IconData icon = class2IconMap.get(objectClass);
		if (icon == null) {
			for (Entry<Class<? extends IOObject>, IconData> renderableClassEntry : class2IconMap.entrySet()) {
				if (renderableClassEntry.getKey().isAssignableFrom(objectClass)) {
					class2IconMap.put(objectClass, renderableClassEntry.getValue());
					return renderableClassEntry.getValue();
				}
			}
			return ICON_DEFAULT;
		}
		return icon;
	}

	/**
	 * Creates a new renderer for the given object.
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static Renderer createRenderer(IOObject ioobject, String rendererName) {
		String reportableName = getName(ioobject.getClass());
		Map<String, Class<? extends Renderer>> rendererClassMap = rendererNameToRendererClasses.get(reportableName);
		if (rendererClassMap == null) {
			throw new IllegalArgumentException("Illegal reportable name: " + rendererName);
		}
		Class<? extends Renderer> rendererClass = rendererClassMap.get(rendererName);
		if (rendererClass == null) {
			throw new IllegalArgumentException("Illegal renderer name: " + rendererName);
		}
		try {
			return rendererClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create renderer: " + e, e);
		}
	}

	/**
	 * This returns whether the {@link RendererService} has already been initialized at least once.
	 * This holds true even if only the core objects have been registered.
	 */
	public static boolean isInitialized() {
		return isInitialized;
	}
}
