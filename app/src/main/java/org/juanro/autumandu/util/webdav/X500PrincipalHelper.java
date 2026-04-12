/*******************************************************************************
 * Copyright (c) 2008,  Jay Rosenthal
 * <p/>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors:
 * Jay Rosenthal - initial API and implementation
 * Jan Kühle - changes to better fit in Autu Mandu app
 *******************************************************************************/

package org.juanro.autumandu.util.webdav;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

/**
 * X500PrincipalHelper
 * <p/>
 * Helper class to extract pieces (attributes) of an X500Principal object for display
 * in the UI.
 * <p/>
 * This helper uses the X500Principal.RFC2253 format of X500Principal.getname() to parse an X500Principal name
 * into it's component parts.
 * <p/>
 * In principals which contain multiple occurrences of the same attribute,the default for all the methods
 * is to return the most significant (first) attribute found.
 */
public class X500PrincipalHelper {
    public static final String ATTR_CN = "CN";
    public static final String ATTR_O = "O";
    public static final String ATTR_C = "C";
    public static final String ATTR_L = "L";

    private static final String ATTR_TERMINATOR = "=";

    private final List<List<String>> rdnNameArray = new ArrayList<>();

    public X500PrincipalHelper(X500Principal principal) {
        parseDN(principal.getName(X500Principal.RFC2253));
    }

    /**
     * Gets the most significant common name (CN) attribute from the given
     * X500Principal object.
     *
     * @return the Most significant common name attribute.
     */
    public String getCN() {
        return findPart(ATTR_CN);
    }

    /**
     * Gets the most significant Organization (O) attribute from the given
     * X500Principal object.
     *
     * @return the Most significant O attribute.
     */
    public String getO() {
        return findPart(ATTR_O);
    }

    /**
     * Gets the Country (C) attribute from the given
     * X500Principal object.
     *
     * @return the C attribute.
     */
    public String getC() {
        return findPart(ATTR_C);
    }

    /**
     * Gets the Locale (L) attribute from the given
     * X500Principal object.
     *
     * @return the L attribute.
     */
    public String getL() {
        return findPart(ATTR_L);
    }

    private void parseDN(@NonNull String dn) throws IllegalArgumentException {
        int startIndex = 0;
        char c = '\0';
        List<String> nameValues = new ArrayList<>();

        rdnNameArray.clear();

        while (startIndex < dn.length()) {
            int endIndex;
            for (endIndex = startIndex; endIndex < dn.length(); endIndex++) {
                c = dn.charAt(endIndex);
                if (c == ',' || c == '+')
                    break;
                if (c == '\\') {
                    endIndex++;
                }
            }

            if (endIndex > dn.length())
                throw new IllegalArgumentException("unterminated escape " + dn);

            if (nameValues != null) {
                nameValues.add(dn.substring(startIndex, endIndex));

                if (c != '+') {
                    rdnNameArray.add(nameValues);
                    if (endIndex != dn.length())
                        nameValues = new ArrayList<>();
                    else
                        nameValues = null;
                }
            }

            startIndex = endIndex + 1;
        }

        if (nameValues != null) {
            throw new IllegalArgumentException("improperly terminated DN " + dn);
        }
    }

    private String findPart(String attributeID) {
        String searchPart = attributeID + ATTR_TERMINATOR;

        for (List<String> nameList : rdnNameArray) {
            if (!nameList.isEmpty()) {
                String namePart = nameList.get(0);

                if (namePart.startsWith(searchPart)) {
                    return namePart.substring(searchPart.length());
                }
            }
        }

        return null;
    }
}
