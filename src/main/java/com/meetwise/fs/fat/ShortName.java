
package com.meetwise.fs.fat;

import com.meetwise.fs.util.LittleEndian;

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
    
    private final String name;
    private final String ext;
    
    private ShortName(String nameExt) {
        if (nameExt.length() > 12) throw
                new IllegalArgumentException("name too long");
        
        final int i = nameExt.indexOf('.');

        if (i < 0) {
            this.name = nameExt.toUpperCase();
            this.ext = "";
        } else {
            this.name = nameExt.substring(0, i).toUpperCase();
            this.ext = nameExt.substring(i + 1).toUpperCase();
        }

        checkValidName(name);
        checkValidExt(ext);
    }
    
    ShortName(String name, String ext) {
        checkValidName(name);
        checkValidExt(ext);
        
        this.name = name;
        this.ext = ext;
    }

    /**
     * Calculates the checksum that is used to test a long file name for
     * it's validity.
     *
     * @return the {@code ShortName}'s checksum
     */
    public byte checkSum() {
        final char[] fullName = new char[] {
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
            ' ', ' ', ' '};

        final char[] nameChars = getName().toCharArray();
        final char[] extChars = getExt().toCharArray();
        
        System.arraycopy(nameChars, 0, fullName, 0, nameChars.length);
        System.arraycopy(extChars, 0, fullName, 8, extChars.length);

        final byte[] dest = new byte[11];
        for (int i = 0; i < 11; i++)
            dest[i] = (byte) fullName[i];

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
     * Tests if the specified string can be converted to a {@code ShortName}.
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
        
        for (int i = 0; i < 8; i++) {
            char ch;
            if (i < name.length()) {
//                if (!isLabel()) {
//                    ch = Character.toUpperCase(name.charAt(i));
//                } else {
                    ch = name.charAt(i);
//                }
                if (ch == 0xe5) {
                    ch = (char) 0x05;
                }
            } else {
                ch = ' ';
            }

            dest[i] = (byte) ch;
        }

        for (int i = 0; i < 3; i++) {
            final char ch;
            if (i < ext.length()) {
//                if (!isLabel()) {
//                    ch = Character.toUpperCase(ext.charAt(i));
//                } else {
                    ch = ext.charAt(i);
//                }
            } else {
                ch = ' ';
            }

            dest[0x08 + i] = (byte) ch;
        }

        entry.markDirty();
    }

    public String asSimpleString() {
        return name + "." + ext; //NOI18N
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
        if ((this.name == null) ? (other.name != null) :
            !this.name.equals(other.name)) {
            return false;
        }

        if ((this.ext == null) ? (other.ext != null) :
            !this.ext.equals(other.ext)) {
            return false;
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 41 * hash + (this.ext != null ? this.ext.hashCode() : 0);
        return hash;
    }

    String getName() {
        return this.name;
    }

    String getExt() {
        return this.ext;
    }
}
