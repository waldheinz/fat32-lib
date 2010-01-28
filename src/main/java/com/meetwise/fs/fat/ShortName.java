
package com.meetwise.fs.fat;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class ShortName {

    private final String name;
    private final String ext;
    
    public final static ShortName DOT = new ShortName(".", "");
    public final static ShortName DOT_DOT = new ShortName("..", "");
    
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

    public static ShortName get(String name) {
        if (name.equals(".")) return DOT;
        else if (name.equals("..")) return DOT_DOT;
        else return new ShortName(name);
    }

    @Override
    public String toString() {
        return name + "." + ext;
    }
    
    public static void checkValidName(String name) {
        checkString(name, "name", 1, 8);
    }

    public static void checkValidExt(String ext) {
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
