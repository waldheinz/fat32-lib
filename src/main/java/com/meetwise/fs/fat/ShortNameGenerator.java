
package com.meetwise.fs.fat;

import java.util.Collections;
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
     * Its in the DOS manual!(DOS 5: page 72) Valid: A..Z 0..9 _ ^ $ ~ ! # % & - {} () @ ' `
     *
     * Unvalid: spaces/periods,
     */
    public static boolean validChar(char toTest) {
        if (toTest >= 'A' && toTest <= 'Z') return true;
        if (toTest >= '0' && toTest <= '9') return true;
        if (toTest == '_' || toTest == '^' || toTest == '$' || toTest == '~' ||
                toTest == '!' || toTest == '#' || toTest == '%' || toTest == '&' ||
                toTest == '-' || toTest == '{' || toTest == '}' || toTest == '(' ||
                toTest == ')' || toTest == '@' || toTest == '\'' || toTest == '`')
            return true;

        return false;
    }

    /**
     * Generates a new unique 8.3 file name that is not already contained in
     * the set specified to the constructor.
     *
     * @param longFullName the long file name to generate the short name for
     * @return the generated 8.3 file name
     */
    public String generateShortName(String longFullName) {
        int dotIndex = longFullName.lastIndexOf('.');

        String longName;
        String longExt;

        if (dotIndex == -1) {
            // No dot in the name
            longName = longFullName;
            longExt = ""; // so no extension
        } else {
            // split it at the dot
            longName = longFullName.substring(0, dotIndex);
            longExt = longFullName.substring(dotIndex + 1);
        }

        String shortName = longName;
        String shortExt = longExt;

        // make the extension short
        if (shortExt.length() > 3) {
            shortExt = shortExt.substring(0, 3);
        }

        char[] shortNameChar = shortName.length() > 8 ?
            shortName.substring(0, 7).toUpperCase().toCharArray() :
            shortName.toCharArray();

        /* epurate it from alien characters */
        for (int i = 0; i < shortNameChar.length; i++) {
            char toTest = shortNameChar[i];

            if (!validChar(toTest))
                shortNameChar[i] = '_';
        }

        shortName = new String(shortNameChar);

        if (usedNames.contains(shortName + "." + shortExt)) {
            // name range from "nnnnnn~1" to "~9999999"
            for (int i = 1; i <= 9999999; i++) {
                String tildeStuff = "~" + i;
                int tildeStuffLength = tildeStuff.length();
                System.arraycopy(tildeStuff.toCharArray(), 0, shortNameChar, 7 - tildeStuffLength,
                        tildeStuffLength);
                shortName = new String(shortNameChar);
                if (!usedNames.contains(shortName + "." + shortExt))
                    break;
            }
        }

        String shortFullName = shortName + "." + shortExt;
        return shortFullName.toUpperCase();
    }

}
