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
package com.rapidminer.core.license;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.rapidminer.license.StudioLicenseConstants;
import com.rapidminer.license.product.Product;
import com.rapidminer.tools.Tools;


/**
 * Registry for custom product links
 *
 * @author Jonas Wilms-Pfau
 * @since 9.1.0
 */
public enum ProductLinkRegistry {
	/**
	 * Links used for purchase
	 */
	PURCHASE;

	private final Map<String, String> productLinkMap = new HashMap<>(0);

	/**
	 * Registers a link for a product. Any existing mapping for the same productId is overwritten.
	 *
	 * <p>Internal, do not use.
	 *
	 * @param productId
	 * 		the {@link Product#getProductId() product id}
	 * @param link
	 * 		the link for the product
	 * @throws NullPointerException
	 * 		if one of the parameters is {@code null}
	 * @throws UnsupportedOperationException
	 * 		if used by 3rd parties, or if {@link StudioLicenseConstants#PRODUCT_ID} is used as a product id
	 * @throws IllegalArgumentException
	 * 		if the link is not a valid url
	 */
	public void register(String productId, String link) {
		Tools.requireInternalPermission();
		Objects.requireNonNull(productId, "productId must not be null");
		Objects.requireNonNull(link, "link must not be null");
		try {
			new URL(link);
		} catch (MalformedURLException mue) {
			throw new IllegalArgumentException(mue.getMessage(), mue);
		}
		if (StudioLicenseConstants.PRODUCT_ID.equals(productId)) {
			throw new UnsupportedOperationException("Modifications of " + StudioLicenseConstants.PRODUCT_ID + " links are not allowed.");
		}
		productLinkMap.put(productId, link);
	}

	/**
	 * Returns the link to which the specified licenseProductKey is mapped, or defaultUrl if this registry contains no
	 * mapping for the key.
	 *
	 * @param productId
	 * 		the {@link Product#getProductId() product id}
	 * @param defaultLink
	 * 		the default link
	 * @return the link for the product, or the {@code defaultLink}
	 **/
	public String get(String productId, String defaultLink) {
		if (productId == null || StudioLicenseConstants.PRODUCT_ID.equals(productId)) {
			return defaultLink;
		}
		return productLinkMap.getOrDefault(productId, defaultLink);
	}

}
