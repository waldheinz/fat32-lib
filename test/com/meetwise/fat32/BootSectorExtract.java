
package com.meetwise.fat32;

import java.io.FileInputStream;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class BootSectorExtract {
    private static final int BYTES_PER_LINE = 5;

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        final String fName = "/home/trem/Desktop/image-40000.img";
        FileInputStream fis = new FileInputStream(fName);
        final byte[] bytes = new byte[512];
        int read = 0;
        
        do {
            read += fis.read(bytes, read, bytes.length - read);
        } while (read < bytes.length);
        
        System.out.println(convert(bytes));
    }
    
    public static String convert(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (int i=0; i < bytes.length; i++) {
            sb.append("(byte) 0x");
            final String hexString = Integer.toHexString(bytes[i] & 255);
            if (hexString.length() < 2) sb.append("0");
            sb.append(hexString);
            if (i < bytes.length-1)
                sb.append(",");
            
            if ((i+1) % BYTES_PER_LINE == 0) {
                sb.append("\n");
            } else {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    private BootSectorExtract() { /* no instances needed */ }
}
