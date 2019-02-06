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
package com.rapidminer.template;

/**
 * Here we keep the {@link Template Templates} that are required in different places.
 */
public final class PreparedTemplates {

	/**
	 * This class should not be instantiated
	 */
	private PreparedTemplates() {
		throw new UnsupportedOperationException();
	}

	/**
	 * special template which is not loaded from a resource but simply creates a new, empty process
	 */
	public static final Template BLANK_PROCESS_TEMPLATE = new Template.SpecialTemplate("new.empty");

	/**
	 * special template which fires up the Auto Modeler which is included as a bundled extension
	 */
	public static final Template TURBO_PREP_TEMPLATE = new Template.SpecialTemplate("turbo_prep");

	/**
	 * special template which fires up the Auto Modeler which is included as a bundled extension
	 */
	public static final Template AUTO_MODEL_TEMPLATE = new Template.SpecialTemplate("automodel");

	/**
	 * special template to access the online documentation
	 */
	public static final Template GETTING_STARTED_DOCUMENTATION = new Template.SpecialTemplate("documentation");

	/**
	 * special template to acces the community
	 */
	public static final Template GETTING_STARTED_COMMUNITY = new Template.SpecialTemplate("community");

	/**
	 * special template which loads the online training videos
	 */
	public static final Template GETTING_STARTED_TRAINING_VIDEOS = new Template.SpecialTemplate("trainingvideo");
}
