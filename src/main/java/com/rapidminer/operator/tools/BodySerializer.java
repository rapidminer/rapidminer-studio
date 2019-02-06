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
package com.rapidminer.operator.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * De/serializes the body of a stream, i.e. the part without the header (MAGIC_NUMBER plus type id).
 * 
 * @author Simon Fischer
 * */
public interface BodySerializer {

	public Object deserialize(InputStream in) throws IOException;

	public void serialize(Object object, OutputStream out) throws IOException;
}
