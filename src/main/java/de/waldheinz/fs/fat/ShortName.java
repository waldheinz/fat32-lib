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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

/**
 * Represents a "short" (8.3) file name as used by DOS.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class ShortName {

    public final static Charset ASCII = Charset.forName("ASCII");
    
    /**
     * These are taken from the FAT specification.
     */
    private final static byte[] ILLEGAL_CHARS = {
        0x22, 0x2A, 0x2B, 0x2C, 0x2E, 0x2F, 0x3A, 0x3B,
        0x3C, 0x3D, 0x3E, 0x3F, 0x5B, 0x5C, 0x5D, 0x7C
    };
    
    private final static byte ASCII_SPACE = 0x20;
    
    /**
     * The name of the "current directory" (".") entry of a FAT directory.
     */
    public final static ShortName DOT = new ShortName(".", ""); //NOI18N

    /**
     * The name of the "parent directory" ("..") entry of a FAT directory.
     */
    public final static ShortName DOT_DOT = new ShortName("..", ""); //NOI18N

    private final byte[] nameBytes;

    protected boolean lowercaseBasename = false;
    protected boolean lowercaseExtension = false;
    
    private ShortName(String nameExt) {
        if (nameExt.length() > 12) throw
                new IllegalArgumentException("name too long");
        
        if (nameExt.contains(" ")) throw
                new IllegalArgumentException("space found");

        final int i = nameExt.indexOf('.');
        final String nameString, extString;
        
        if (i < 0) {
            resolveBasenameCase(nameExt);
            nameString = nameExt.toUpperCase(Locale.ROOT);
            extString = "";
        } else {
            resolveBasenameCase(nameExt);
            resolveExtensionCase(nameExt);
            nameString = nameExt.substring(0, i).toUpperCase(Locale.ROOT);
            extString = nameExt.substring(i + 1).toUpperCase(Locale.ROOT);
        }
        
        this.nameBytes = toCharArray(nameString, extString);
        checkValidChars(nameBytes);
    }
    
    ShortName(String name, String ext) {
        this.nameBytes = toCharArray(name, ext);
    }

    public void resolveBasenameCase(String nameExt) {
        final int i = nameExt.indexOf('.');
        final String basename;

        // Look for the dot and set the basename
        if (i < 0) {
            basename = nameExt;
        } else {
            basename = nameExt.substring(0, i);
        }

        if (basename.length() == 0) return;

        if (basename.toLowerCase(Locale.ROOT) == basename) {
            // The basename is all lowercase
            lowercaseBasename = true;
        } else if (basename.toUpperCase(Locale.ROOT) == basename) {
            // The basename is all uppercase
            lowercaseBasename = false;
        } else {
            // The basename is mixed case, and so name cannot be a short name
            new IllegalArgumentException("mixed case basename");
        }
    }

    public void resolveExtensionCase(String nameExt) {
        final int i = nameExt.indexOf('.');
        final String extension;

        // Look for the dot and set the extension
        if (i < 0) {
            extension = "";
            return;
        } else {
            extension = nameExt.substring(i + 1);
        }
      
        if (extension.toLowerCase(Locale.ROOT) == extension) {
            // The extension is all lowercase
            lowercaseExtension = true;
        } else if (extension.toUpperCase(Locale.ROOT) == extension) {
            // The extension is all uppercase
            lowercaseExtension = false;
        } else {
            // The extension is mixed case, and so name cannot be a short name
            new IllegalArgumentException("mixed case extension");
        }
    }

    private static byte[] toCharArray(String name, String ext) {
        checkValidName(name);
        checkValidExt(ext);
        
        final byte[] result = new byte[11];
        Arrays.fill(result, ASCII_SPACE);
        System.arraycopy(name.getBytes(ASCII), 0, result, 0, name.length());
        System.arraycopy(ext.getBytes(ASCII), 0, result, 8, ext.length());
        
        return result;
    }

    public boolean isLowercaseBasename() {
        return lowercaseBasename;
    }

    public boolean isLowercaseExtension() {
        return lowercaseExtension;
    }

    /**
     * Calculates the checksum that is used to test a long file name for
     * it's validity.
     *
     * @return the {@code ShortName}'s checksum
     */
    public byte checkSum() {
        final byte[] dest = new byte[11];
        for (int i = 0; i < 11; i++)
            dest[i] = (byte) nameBytes[i];

        int sum = dest[0];
        for (int i = 1; i < 11; i++) {
            sum = dest[i] + (((sum & 1) << 7) + ((sum & 0xfe) >> 1));
        }
        
        return (byte) (sum & 0xff);
    }

    /**
     * Parses the specified string into a {@code ShortName}.
     *
     * @param name the name+extension of the {@code ShortName} to get
     * @return the {@code ShortName} representing the specified name
     * @throws IllegalArgumentException if the specified name can not be parsed
     *      into a {@code ShortName}
     * @see #canConvert(java.lang.String) 
     */
    public static ShortName get(String name) throws IllegalArgumentException {
        if (name.equals(".")) return DOT;
        else if (name.equals("..")) return DOT_DOT;
        else return new ShortName(name);
    }
    
    /**
     * Tests if the specified string can be converted to a {@code ShortName}. A {@code ShortName} is
     * an 8.3 name, either all upper case, the base name is all upper or all lowercase, 
     * or the extension is all upper or all lowercase.
     *
     * @param nameExt the string to test
     * @return if the string can be converted
     * @see #get(java.lang.String) 
     */
    public static boolean canConvert(String nameExt) {
        /* TODO: do this without exceptions */
        try {
            ShortName.get(nameExt);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
    
    public static ShortName parse(byte[] data, boolean isBasenameLowercase, boolean isExtensionLowercase) {
        final char[] nameArr = new char[8];
        
        for (int i = 0; i < nameArr.length; i++) {
            nameArr[i] = (char) LittleEndian.getUInt8(data, i);
        }

        if (LittleEndian.getUInt8(data, 0) == 0x05) {
            nameArr[0] = (char) 0xe5;
        }
        
        final char[] extArr = new char[3];
        for (int i = 0; i < extArr.length; i++) {
            extArr[i] = (char) LittleEndian.getUInt8(data, 0x08 + i);
        }

        String name = new String(nameArr).trim();
        String ext = new String(extArr).trim();

        ShortName shortName = new ShortName(name, ext);
        if (nameArr[0] != 0xe5) {
            shortName.lowercaseBasename = isBasenameLowercase;
            shortName.lowercaseExtension = isExtensionLowercase;
        }
        return shortName;
    }

    public byte[] getNameBytes() {
      // TODO: This should be a copy...
      return this.nameBytes;
    }
    
    public String asSimpleString() {
        String name = new String(this.nameBytes, 0, 8, ASCII).trim();
        if (isLowercaseBasename()) {
          name = name.toLowerCase(Locale.ROOT);
        }
        String ext = new String(this.nameBytes, 8, 3, ASCII).trim();
        if (isLowercaseExtension()) {
          ext = ext.toLowerCase(Locale.ROOT);
        }
        return ext.isEmpty() ? name : name + "." + ext;
    }
    
    @Override
    public String toString() {
        return "ShortName [" + asSimpleString() + "]"; //NOI18N
    }
    
    private static void checkValidName(String name) {
        checkString(name, "name", 1, 8);
    }

    private static void checkValidExt(String ext) {
        checkString(ext, "extension", 0, 3);
    }

    private static void checkString(String str, String strType,
            int minLength, int maxLength) {

        if (str == null)
            throw new IllegalArgumentException(strType +
                    " is null");
        if (str.length() < minLength)
            throw new IllegalArgumentException(strType +
                    " must have at least " + minLength +
                    " characters: " + str);
        if (str.length() > maxLength)
            throw new IllegalArgumentException(strType +
                    " has more than " + maxLength +
                    " characters: " + str);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ShortName)) {
            return false;
        }

        final ShortName other = (ShortName) obj;
        return Arrays.equals(nameBytes, other.nameBytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.nameBytes);
    }

    /**
     * Checks if the specified char array consists only of "valid" byte values
     * according to the FAT specification.
     *
     * @param chars the char array to test
     * @throws IllegalArgumentException if invalid chars are contained
     */
    public static void checkValidChars(byte[] chars)
            throws IllegalArgumentException {
            
        if (chars[0] == 0x20) throw new IllegalArgumentException(
                "0x20 can not be the first character");

        for (int i=0; i < chars.length; i++) {
            if ((chars[i] & 0xff) != chars[i]) throw new
                    IllegalArgumentException("multi-byte character at " + i);

            final byte toTest = (byte) (chars[i] & 0xff);
            
            if (toTest < 0x20 && toTest != 0x05) throw new
                    IllegalArgumentException("character < 0x20 at" + i);

            for (int j=0; j < ILLEGAL_CHARS.length; j++) {
                if (toTest == ILLEGAL_CHARS[j]) throw new
                        IllegalArgumentException("illegal character " +
                        ILLEGAL_CHARS[j] + " at " + i);
            }
        }
    }
}
