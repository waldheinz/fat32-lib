
package com.meetwise.fat32;

import java.io.PrintStream;
import java.nio.ByteBuffer;

/**
 * Contains some utility methods which are mainly useful for debugging
 * purposes.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class Utils {



    private Utils() { /* utility class */ }

    /**
     * Converts a integer value to an hex string which will have the specified
     * width.
     *
     * @param value the value to be converted to a hex string
     * @param width the desired width of the output string
     * @return the string representation of the value
     * @throws IllegalArgumentException if the {@code value} is too big for the
     *      specified {@code width}
     * @see Integer#toHexString(int)
     */
    public static String hexOffset(int value, int width)
            throws IllegalArgumentException {

        final String digits = Integer.toHexString(value);

        if (digits.length() > width) throw new
                IllegalArgumentException("value too big for width"); //NOI18N

        StringBuilder sb = new StringBuilder();
        for (int i=digits.length(); i < width; i++)
            sb.append("0");
        sb.append(digits);
        sb.append(" : ");

        return sb.toString();
    }

    /**
     * Returns the {@code char} corresponding to the specified {@code byte}.
     *
     * @param b the {@code byte} to convert to a {@code char}
     * @return the {@code char} for the specified {@code byte}, or '.' if
     *      the byte corresponds to no (printable) character
     */
    public static char getChar(byte b) {

        final char c = (char) b;
        final Character.UnicodeBlock bl = Character.UnicodeBlock.of( c );

        if (!Character.isISOControl(c) &&
                bl != null &&
                bl != Character.UnicodeBlock.SPECIALS) {

            return c;
        } else {
            return '.';
        }
    }

    /**
     * Prints a hex dump of the specified {@code ByteBuffer} to the specified
     * {@code PrintStream}. Only the "remaining" bytes in the buffer are
     * printed. The byte buffer is not modified by this method (i.e. no
     * bytes from the buffer are "consumed").
     *
     * @param ps the print stream to write the hex dump to
     * @param bb the byte buffer to be hex-dumped
     */
    public static void hexDump(PrintStream ps, ByteBuffer bb) {
        int inLine = 0;

        for (int pos=0; pos < bb.remaining(); pos++) {
            final int b = bb.get(bb.position() + pos) & 0xff;

            if (inLine == 0) ps.print(hexOffset(pos, 4));

            ps.print(Character.forDigit(b / 16, 16));
            ps.print(Character.forDigit(b % 16, 16));
            ps.print(" ");

            inLine++;

            if (inLine == 8) ps.print("- ");

            if (inLine >= 16) {
                ps.print(": ");
                for (int i=0; i < 16; i++) {
                    final byte get = bb.get(bb.position() + (pos - 15 + i));

                    ps.print(getChar(get));
                }

                ps.println();
                inLine = 0;
            }
        }

        if (inLine > 0) {

            for (int i=0; i < (16 - inLine); i++)
                ps.print("   ");
            if (inLine < 8) ps.print("  ");
            ps.print(": ");

            for (int i=0; i < inLine; i++) {
                final byte get = bb.get(
                        bb.position() + bb.remaining() - inLine + i);

                ps.print(getChar(get));
            }

            ps.println();
        }
    }
}
