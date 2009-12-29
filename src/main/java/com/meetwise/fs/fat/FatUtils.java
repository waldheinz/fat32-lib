/*
 * $Id: FatUtils.java 4975 2009-02-02 08:30:52Z lsantha $
 *
 * Copyright (C) 2003-2009 JNode.org
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
 
package com.meetwise.fs.fat;

import java.io.IOException;
import com.meetwise.fs.util.LittleEndian;

/**
 * <description>
 * 
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 * @author Fabien DUMINY
 */
class FatUtils {

    /**
     * Gets the offset (in bytes) of the fat with the given index
     * 
     * @param bs
     * @param fatNr (0..)
     * @return long
     * @throws IOException 
     */
    public static long getFatOffset(BootSector bs, int fatNr) throws IOException {
        long sectSize = bs.getBytesPerSector();
        long sectsPerFat = bs.getSectorsPerFat();
        long resSects = bs.getNrReservedSectors();

        long offset = resSects * sectSize;
        long fatSize = sectsPerFat * sectSize;

        offset += fatNr * fatSize;

        return offset;
    }

    /**
     * Gets the offset (in bytes) of the root directory with the given index
     * 
     * @param bs
     * @return long
     * @throws IOException 
     */
    public static long getRootDirOffset(BootSector bs) throws IOException {
        long sectSize = bs.getBytesPerSector();
        long sectsPerFat = bs.getSectorsPerFat();
        int fats = bs.getNrFats();

        long offset = getFatOffset(bs, 0);

        offset += fats * sectsPerFat * sectSize;

        return offset;
    }

    /**
     * Gets the offset of the data (file) area
     * 
     * @param bs
     * @return long
     * @throws IOException 
     */
    public static long getFilesOffset(BootSector bs) throws IOException {
        long offset = getRootDirOffset(bs);
        
        offset += bs.getRootDirEntryCount() * 32;

        return offset;
    }

    /**
     * Return the name (without extension) of a full file name
     * 
     * @param nameExt
     * @return
     */
    public static String splitName(String nameExt) {
        int i = nameExt.indexOf('.');
        if (i < 0) {
            return nameExt;
        } else {
            return nameExt.substring(0, i);
        }
    }

    /**
     * Return the extension (without name) of a full file name
     * 
     * @param nameExt
     * @return
     */
    public static String splitExt(String nameExt) {
        int i = nameExt.indexOf('.');
        if (i < 0) {
            return "";
        } else {
            return nameExt.substring(i + 1);
        }
    }

    /**
     * Normalize full file name in DOS 8.3 format from the name and the ext
     * 
     * @param name
     * @param ext
     * @return
     */
    public static String normalizeName(String name, String ext) {
        if (ext.length() > 0) {
            return (name + "." + ext).toUpperCase();
        } else {
            return name.toUpperCase();
        }
    }

    /**
     * Normalize full file name in DOS 8.3 format from the given full name
     * 
     * @param nameExt
     * @return
     */
    public static String normalizeName(String nameExt) {
        if (nameExt.equals("."))
            return nameExt;

        if (nameExt.equals(".."))
            return nameExt;

        return normalizeName(splitName(nameExt), splitExt(nameExt));
    }

    public static void checkValidName(String name) {
        checkString(name, "name", 1, 8);
    }

    public static void checkValidExt(String ext) {
        checkString(ext, "extension", 0, 3);
    }

    private static void checkString(String str, String strType, int minLength, int maxLength) {
        if (str == null)
            throw new IllegalArgumentException(strType + " is null");
        if (str.length() < minLength)
            throw new IllegalArgumentException(strType + " must have at least " + maxLength +
                    " characters: " + str);
        if (str.length() > maxLength)
            throw new IllegalArgumentException(strType + " has more than " + maxLength +
                    " characters: " + str);
    }

    public static final int SUBNAME_SIZE = 13;

    /**
     * Write the part of a long file name to the given byte array
     * 
     * @param src
     * @param srcOffset
     * @param ordinal
     * @param checkSum
     * @param isLast
     * @param dest
     * @param destOffset
     */
    public static void writeSubString(char[] src, int srcOffset, int ordinal, byte checkSum,
            boolean isLast, byte[] dest, int destOffset) {
        if (isLast) {
            LittleEndian.setInt8(dest, destOffset, ordinal + (1 << 6)); // set
                                                                        // the
                                                                        // 6th
            // security ending
            // bit
        } else {
            LittleEndian.setInt8(dest, destOffset, ordinal);
        }

        LittleEndian.setInt16(dest, destOffset + 1, src[srcOffset + 0]);
        LittleEndian.setInt16(dest, destOffset + 3, src[srcOffset + 1]);
        LittleEndian.setInt16(dest, destOffset + 5, src[srcOffset + 2]);
        LittleEndian.setInt16(dest, destOffset + 7, src[srcOffset + 3]);
        LittleEndian.setInt16(dest, destOffset + 9, src[srcOffset + 4]);
        LittleEndian.setInt8(dest, destOffset + 11, 0x0f); // this is the
                                                            // hidden attribute
                                                            // tag for
        // lfn
        LittleEndian.setInt8(dest, destOffset + 12, 0); // reserved
        LittleEndian.setInt8(dest, destOffset + 13, checkSum); // checksum
        LittleEndian.setInt16(dest, destOffset + 14, src[srcOffset + 5]);
        LittleEndian.setInt16(dest, destOffset + 16, src[srcOffset + 6]);
        LittleEndian.setInt16(dest, destOffset + 18, src[srcOffset + 7]);
        LittleEndian.setInt16(dest, destOffset + 20, src[srcOffset + 8]);
        LittleEndian.setInt16(dest, destOffset + 22, src[srcOffset + 9]);
        LittleEndian.setInt16(dest, destOffset + 24, src[srcOffset + 10]);
        LittleEndian.setInt16(dest, destOffset + 26, 0); // sector... unused
        LittleEndian.setInt16(dest, destOffset + 28, src[srcOffset + 11]);
        LittleEndian.setInt16(dest, destOffset + 30, src[srcOffset + 12]);

    }

    /**
     * convert an array of bytes to an array of chars (unicode)
     */
    static char[] getUnicodeChars(byte[] rawData, int offset) {
        char[] unicodechar = new char[SUBNAME_SIZE];
        unicodechar[0] = (char) LittleEndian.getUInt16(rawData, offset + 1);
        unicodechar[1] = (char) LittleEndian.getUInt16(rawData, offset + 3);
        unicodechar[2] = (char) LittleEndian.getUInt16(rawData, offset + 5);
        unicodechar[3] = (char) LittleEndian.getUInt16(rawData, offset + 7);
        unicodechar[4] = (char) LittleEndian.getUInt16(rawData, offset + 9);
        unicodechar[5] = (char) LittleEndian.getUInt16(rawData, offset + 14);
        unicodechar[6] = (char) LittleEndian.getUInt16(rawData, offset + 16);
        unicodechar[7] = (char) LittleEndian.getUInt16(rawData, offset + 18);
        unicodechar[8] = (char) LittleEndian.getUInt16(rawData, offset + 20);
        unicodechar[9] = (char) LittleEndian.getUInt16(rawData, offset + 22);
        unicodechar[10] = (char) LittleEndian.getUInt16(rawData, offset + 24);
        unicodechar[11] = (char) LittleEndian.getUInt16(rawData, offset + 28);
        unicodechar[12] = (char) LittleEndian.getUInt16(rawData, offset + 30);
        return unicodechar;
    }

    private FatUtils() { /* no instances */ }

}
