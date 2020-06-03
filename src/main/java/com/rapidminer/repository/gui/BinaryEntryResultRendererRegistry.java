/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository.gui;


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rapidminer.gui.renderer.Renderer;
import com.rapidminer.gui.renderer.binary.BinaryTextRenderer;
import com.rapidminer.operator.nio.file.BinaryEntryFileObject;
import com.rapidminer.repository.AbstractFileSuffixRegistry;
import com.rapidminer.tools.I18N;


/**
 * Registry for {@link com.rapidminer.gui.renderer.Renderer Renderers} for {@link com.rapidminer.repository.BinaryEntry
 * BinaryEntries}. When implementing, it is strongly advised to extend from {@link
 * com.rapidminer.gui.renderer.AbstractRenderer} instead of implementing the interface directly!
 * <p>
 * Note that this registry has lists of renderers per file suffix (see {@link com.rapidminer.repository.BinaryEntry#getSuffix()}!
 * Therefore please use {@link #registerRendererProvider(String, RendererProvider)} and {@link
 * #unregisterRendererProvider(String, RendererProvider)} instead of registerCallback(String, Object) and
 * unregisterCallback(String, Object)!
 * </p>
 * <p> Suffix is defined as the content after the last . in a file name. See {@link
 * com.rapidminer.repository.RepositoryTools#getSuffixFromFilename(String)}
 * </p>
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class BinaryEntryResultRendererRegistry extends AbstractFileSuffixRegistry<List<RendererProvider>> {

	private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
	private static BinaryEntryResultRendererRegistry instance;


	/**
	 * Get the registry instance.
	 *
	 * @return the instance, never {@code null}
	 */
	public static synchronized BinaryEntryResultRendererRegistry getInstance() {
		if (instance == null) {
			instance = new BinaryEntryResultRendererRegistry();
		}

		return instance;
	}

	/**
	 * Registers the given renderer provider. If it is already registered, does nothing.
	 *
	 * @param suffix           the suffix, should not start with a leading '.', must not be {@code null}
	 * @param rendererProvider the {@link RendererProvider}, must not be {@code null}
	 */
	public void registerRendererProvider(String suffix, RendererProvider rendererProvider) {
		if (rendererProvider == null) {
			throw new IllegalArgumentException("rendererProvider must not be null!");
		}

		suffix = prepareSuffix(suffix);
		List<RendererProvider> list = getCallback(suffix);
		if (list == null) {
			registerCallback(suffix, new CopyOnWriteArrayList<>());
		}
		list = getCallback(suffix);
		if (!list.contains(rendererProvider)) {
			list.add(rendererProvider);
		}

	}

	/**
	 * Tries to unregister the given renderer provider for the given suffix. If the renderer passed here is not the same
	 * as the one stored in the registry for the provided suffix, it will <strong>not</strong> be unregistered! Calling
	 * this multiple times has no effect. If the renderer is not registered has also no effect.
	 *
	 * @param suffix           the suffix, should not start with a leading '.', must not be {@code null}
	 * @param rendererProvider the renderer provider, must not be {@code null}
	 */
	public void unregisterRendererProvider(String suffix, RendererProvider rendererProvider) {
		if (rendererProvider == null) {
			throw new IllegalArgumentException("rendererProvider must not be null!");
		}

		suffix = prepareSuffix(suffix);
		List<RendererProvider> list = getCallback(suffix);
		if (list != null) {
			list.remove(rendererProvider);
		}
	}

	/**
	 * Initialize the renderer registry. Calling multiple times has no effect.
	 */
	public static void initialize() {
		if (INITIALIZED.compareAndSet(false, true)) {
			RendererProvider textRenderer = new RendererProvider() {
				@Override
				public Renderer getRenderer(BinaryEntryFileObject binaryEntryFileObject) {
					return new BinaryTextRenderer();
				}

				@Override
				public String getIconName(BinaryEntryFileObject binaryEntryFileObject) {
					return I18N.getGUILabel("gui.cards.result_view.content.icon");
				}
			};
			getInstance().registerRendererProvider("txt", textRenderer);
		}
	}
}
