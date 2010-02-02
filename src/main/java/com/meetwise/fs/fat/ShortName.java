
package com.meetwise.fs.fat;

import com.meetwise.fs.util.LittleEndian;
import java.util.Arrays;

/**
 * Represents a "short" (8.3) file name as used by DOS.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class ShortName {
    
    /**
     * The name of the "current directory" (".") entry of a FAT directory.
     */
    public final static ShortName DOT = new ShortName(".", ""); //NOI18N

    /**
     * The name of the "parent directory" ("..") entry of a FAT directory.
     */
    public final static ShortName DOT_DOT = new ShortName("..", ""); //NOI18N
    
    private final char[] name;
    
    private ShortName(String nameExt) {
        if (nameExt.length() > 12) throw
                new IllegalArgumentException("name too long");
        
        final int i = nameExt.indexOf('.');
        final String nameString, extString;
        
        if (i < 0) {
            nameString = nameExt.toUpperCase();
            extString = "";
        } else {
            nameString = nameExt.substring(0, i).toUpperCase();
            extString = nameExt.substring(i + 1).toUpperCase();
        }

        this.name = toCharArray(nameString, extString);
    }
    
    ShortName(String name, String ext) {
        this.name = toCharArray(name, ext);
    }

    private char[] toCharArray(String name, String ext) {
        checkValidName(name);
        checkValidExt(ext);

        final char[] result = new char[11];
        Arrays.fill(result, ' ');
        System.arraycopy(name.toCharArray(), 0, result, 0, name.length());
        System.arraycopy(ext.toCharArray(), 0, result, 8, ext.length());
        
        return result;
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
            dest[i] = (byte) name[i];

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
     */
    public static ShortName get(String name) throws IllegalArgumentException {
        if (name.equals(".")) return DOT;
        else if (name.equals("..")) return DOT_DOT;
        else return new ShortName(name);
    }

    public static ShortName parse(AbstractDirectoryEntry entry) {
        final char[] nameArr = new char[8];
        
        for (int i = 0; i < nameArr.length; i++) {
            nameArr[i] = (char) LittleEndian.getUInt8(entry.getData(), i);
        }

        if (LittleEndian.getUInt8(entry.getData(), 0) == 0x05) {
            nameArr[0] = (char) 0xe5;
        }
        
        final char[] extArr = new char[3];
        for (int i = 0; i < extArr.length; i++) {
            extArr[i] = (char) LittleEndian.getUInt8(entry.getData(), 0x08 + i);
        }

        return new ShortName(
                new String(nameArr).trim(),
                new String(extArr).trim());
    }

    public void write(AbstractDirectoryEntry entry) {
        final byte[] dest = entry.getData();
        
        for (int i = 0; i < 11; i++) {
            dest[i] = (byte) name[i];
        }

        entry.markDirty();
    }

    public String asSimpleString() {
        return new String(this.name);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [" + asSimpleString() + "]"; //NOI18N
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
                    " must have at least " + maxLength +
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
        return Arrays.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.name);
    }
}
