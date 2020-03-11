/*
 * Copyright (C) 2009-2013 Matthias Treydte <mt@waldheinz.de>
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.waldheinz.fs.fat;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Generates the 8.3 file names that are associated with the long names.
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
final class ShortNameGenerator {
    
    private final Set<String> usedNames;

    /**
     * Creates a new instance of {@code ShortNameGenerator} that will use
     * the specified set to avoid short-name collisions. It will never generate
     * a short name that is already contained in the specified set, neither
     * will the specified set be modified by this class. This class can be
     * used to generate any number of short file names.
     *
     * @param usedNames the look-up for already used 8.3 names
     */
    public ShortNameGenerator(Set<String> usedNames) {
        this.usedNames = Collections.unmodifiableSet(usedNames);
    }
    
    /*
     * Its in the DOS manual!(DOS 5: page 72)
     *
     * Valid: A..Z 0..9 _ ^ $ ~ ! # % & - {} () @ ' `
     */
    public static boolean validChar(char toTest) {
        if (toTest >= 'A' && toTest <= 'Z') return true;
        if (toTest >= '0' && toTest <= '9') return true;
        
        return (toTest == '_' || toTest == '^' || toTest == '$' ||
                toTest == '~' || toTest == '!' || toTest == '#' ||
                toTest == '%' || toTest == '&' || toTest == '-' ||
                toTest == '{' || toTest == '}' || toTest == '(' ||
                toTest == ')' || toTest == '@' || toTest == '\''||
                toTest == '`');
    }
    
    public static boolean isSkipChar(char c) {
        return (c == '.') || (c == ' ');
    }

    private String tidyString(String dirty) {
        final StringBuilder result = new StringBuilder();

        /* epurate it from alien characters */
        for (int src=0; src < dirty.length(); src++) {
            final char toTest = Character.toUpperCase(dirty.charAt(src));
            if (isSkipChar(toTest)) continue;

            if (validChar(toTest)) {
                result.append(toTest);
            } else {
                result.append('_');
            }
        }

        return result.toString();
    }

    private boolean cleanString(String s) {
        for (int i=0; i < s.length(); i++) {
            if (isSkipChar(s.charAt(i))) return false;
            if (!validChar(s.charAt(i))) return false;
        }

        return true;
    }

    private String stripLeadingPeriods(String str) {
        final StringBuilder sb = new StringBuilder(str.length());

        for (int i=0; i < str.length(); i++) {
            if (str.charAt(i) != '.') { //NOI18N
                sb.append(str.substring(i));
                break;
            }
        }
        
        return sb.toString();
    }

    /**
     * Generates a new unique 8.3 file name that is not already contained in
     * the set specified to the constructor.
     *
     * @param longFullName the long file name to generate the short name for
     * @return the generated 8.3 file name
     * @throws IllegalStateException if no unused short name could be found
     */
    public ShortName generateShortName(String longFullName)
            throws IllegalStateException {
        
        String originalLongFullName = longFullName;

        longFullName =
                stripLeadingPeriods(longFullName).toUpperCase(Locale.ROOT);
        
        final String longName;
        final String longExt;
        final int dotIdx = longFullName.lastIndexOf('.');
        final boolean forceSuffix;
        
        if (dotIdx == -1) {
            /* no dot in the name */
            forceSuffix = !cleanString(longFullName);
            longName = tidyString(longFullName);
            longExt = ""; /* so no extension */
        } else {
            /* split at the dot */
            forceSuffix = !cleanString(longFullName.substring(
                    0, dotIdx));
            longName = tidyString(longFullName.substring(0, dotIdx));
            longExt = tidyString(longFullName.substring(dotIdx + 1));
        }
        
        final String shortExt = (longExt.length() > 3) ?
            longExt.substring(0, 3) : longExt;
            
        if (forceSuffix || (longName.length() > 8) || (longExt.length() > 3) ||
                usedNames.contains(new ShortName(longName, shortExt).
                asSimpleString().toLowerCase(Locale.ROOT))) {

            /* we have to append the "~n" suffix */

            final int maxLongIdx = Math.min(longName.length(), 8);

            for (int i=1; i < 99999; i++) {
                final String serial = "~" + i; //NOI18N
                final int serialLen = serial.length();
                final String shortName = longName.substring(
                        0, Math.min(maxLongIdx, 8-serialLen)) + serial;
                final ShortName result = new ShortName(shortName, shortExt);
                
                if (!usedNames.contains(
                        result.asSimpleString().toLowerCase(Locale.ROOT))) {
                    
                    return result;
                }
            }

            throw new IllegalStateException(
                    "could not generate short name for \""
                    + longFullName + "\"");
        }

        ShortName shortName = new ShortName(longName, shortExt);
        shortName.resolveBasenameCase(originalLongFullName);
        shortName.resolveExtensionCase(originalLongFullName);
        return shortName;
    }
    
}
