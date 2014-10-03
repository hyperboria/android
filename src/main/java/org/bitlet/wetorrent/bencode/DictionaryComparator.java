/*
 *              bitlet - Simple bittorrent library
 *
 * Copyright (C) 2008 Alessandro Bahgat Shehata, Daniele Castagna
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Alessandro Bahgat Shehata - ale dot bahgat at gmail dot com
 * Daniele Castagna - daniele dot castagna at gmail dot com
 *
 */

/*
 * This is a very simple byte string comparator.
 */
package org.bitlet.wetorrent.bencode;

import java.nio.ByteBuffer;
import java.util.Comparator;

public class DictionaryComparator implements Comparator<ByteBuffer> {

    public DictionaryComparator() {
    }

    public int bitCompare(byte b1, byte b2) {
        int int1 = b1 & 0xFF;
        int int2 = b2 & 0xFF;
        return int1 - int2;
    }

    public int compare(ByteBuffer o1, ByteBuffer o2) {
        byte[] byteString1 = o1.array();
        byte[] byteString2 = o2.array();
        int minLength = byteString1.length > byteString2.length ? byteString2.length : byteString1.length;
        for (int i = 0; i < minLength; i++) {
            int bitCompare = bitCompare(byteString1[i], byteString2[i]);
            if (bitCompare != 0) {
                return bitCompare;
            }
        }

        if (byteString1.length > byteString2.length) {
            return 1;
        } else if (byteString1.length < byteString2.length) {
            return -1;
        }
        return 0;
    }
}
