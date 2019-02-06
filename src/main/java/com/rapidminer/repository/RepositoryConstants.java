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
package com.rapidminer.repository;

/**
 * Contains error codes for exceptions etc.
 * 
 * @author Simon Fischer
 * 
 */
public interface RepositoryConstants {

	public static final int OK = 0;

	public static final int GENERAL_ERROR = -1;

	public static final int NO_SUCH_ENTRY = -2;

	public static final int NO_SUCH_REVISION = -3;

	public static final int DUPLICATE_ENTRY = -4;

	public static final int WRONG_TYPE = -5;

	public static final int ILLEGAL_CHARACTER = -6;

	public static final int ILLEGAL_TYPE = -7;

	public static final int ILLEGAL_DATA_FORMAT = -8;

	public static final int MALFORMED_CRON_EXPRESSION = -9;

	public static final int NO_SUCH_TRIGGER = -10;

	public static final int MALFORMED_PROCESS = -11;

	public static final int ACCESS_DENIED = -12;

	public static final int NO_SUCH_GROUP = -13;

	public static final int DUPLICATE_USERNAME = -14;

	public static final int DUPLICATE_GROUPNAME = -15;

	public static final int NO_SUCH_USER_IN_GROUP = -16;

	public static final int FORBIDDEN = -17;

	public static final int NO_SUCH_PROCESS = -18;

	public static final int MISSING_PARAMETER = -19;

	public static final int VERSION_OUT_OF_DATE = -20;

	public static final int NO_SUCH_USER = -21;

	public static final int PROCESS_FAILED = -22;

	public static final int NO_SUCH_QUEUE = -23;

}
