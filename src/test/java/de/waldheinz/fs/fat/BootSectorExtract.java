/*
 * Copyright (C) 2009,2010 Matthias Treydte <mt@waldheinz.de>
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
