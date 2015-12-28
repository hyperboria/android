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

/**
 * This is just a little class that lets you read and write bencoded files.
 * It uses List, Map, Long, and ByteBuffer in memory to represents data
 */
package org.bitlet.wetorrent.bencode;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Bencode {

    private Object rootElement = null;

    /**
     * This creates and parse a bencoded StringReader
     */
    public Bencode(StringReader is) throws IOException {
        if (!is.markSupported()) {
            throw new IOException("is.markSupported should be true");
        }
        rootElement = parse(is);
    }

    /**
     * This creates a new instance of Bencode class
     */
    public Bencode() {
    }

    /**
     * This method prints the bencoded file on the OutputStream os
     */
    public void print(OutputStream os) throws IOException {
        print(rootElement, os);
    }

    private void print(Object object, OutputStream os) throws IOException {
        if (object instanceof Long) {
            os.write('i');
            os.write((object).toString().getBytes());
            os.write('e');
        }
        if (object instanceof ByteBuffer) {
            byte[] byteString = ((ByteBuffer) object).array();
            os.write(Integer.toString(byteString.length).getBytes());
            os.write(':');
            for (byte aByteString : byteString) {
                os.write(aByteString);
            }
        } else if (object instanceof List) {
            List list = (List) object;
            os.write('l');
            for (Object elem : list) {
                print(elem, os);
            }
            os.write('e');
        } else if (object instanceof Map) {
            Map map = (Map) object;
            os.write('d');

            SortedMap<ByteBuffer, Object> sortedMap = new TreeMap<>(new DictionaryComparator());
            // sortedMap.putAll(map);

            for (Object elem : map.entrySet()) {
                Map.Entry entry = (Map.Entry) elem;
                sortedMap.put((ByteBuffer) entry.getKey(), entry.getValue());
            }

            for (Object elem : sortedMap.entrySet()) {
                Map.Entry entry = (Map.Entry) elem;
                print(entry.getKey(), os);
                print(entry.getValue(), os);
            }
            os.write('e');
        }
    }

    private Object parse(StringReader is) throws IOException {
        is.mark(0);
        int readChar = is.read();
        switch (readChar) {
            case 'i':
                return parseInteger(is);
            case 'l':
                return parseList(is);
            case 'd':
                return parseDictionary(is);
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                is.reset();
                return parseByteString(is);
            default:
                throw new IOException("Problem parsing bencoded file");
        }
    }

    public Object getRootElement() {
        return rootElement;
    }

    public void setRootElement(Object rootElement) {
        this.rootElement = rootElement;
    }

    private Long parseInteger(StringReader is) throws IOException {

        int readChar = is.read();

        StringBuilder buff = new StringBuilder();
        do {
            if (readChar < 0) {
                throw new IOException("Unexpected EOF found");
            }
            buff.append((char) readChar);
            readChar = is.read();
        } while (readChar != 'e');

        // System.out.println("Loaded int: " + buff);
        return Long.parseLong(buff.toString());
    }

    private List<Object> parseList(StringReader is) throws IOException {

        List<Object> list = new LinkedList<>();
        is.mark(0);
        int readChar = is.read();
        while (readChar != 'e') {
            if (readChar < 0) {
                throw new IOException("Unexpected EOF found");
            }
            is.reset();
            list.add(parse(is));
            is.mark(0);
            readChar = is.read();
        }

        return list;
    }

    private SortedMap parseDictionary(StringReader is) throws IOException {
        SortedMap<ByteBuffer, Object> map = new TreeMap<>(new DictionaryComparator());
        is.mark(0);
        int readChar = is.read();
        while (readChar != 'e') {
            if (readChar < 0) {
                throw new IOException("Unexpected EOF found");
            }
            is.reset();
            map.put(parseByteString(is), parse(is));
            is.mark(0);
            readChar = is.read();
        }

        return map;
    }

    private ByteBuffer parseByteString(StringReader is) throws IOException {

        int readChar = is.read();

        StringBuilder buff = new StringBuilder();
        do {
            if (readChar < 0) {
                throw new IOException("Unexpected EOF found");
            }
            buff.append((char) readChar);
            readChar = is.read();
        } while (readChar != ':');
        Integer length = Integer.parseInt(buff.toString());

        byte[] byteString = new byte[length];
        for (int i = 0; i < byteString.length; i++) {
            byteString[i] = (byte) is.read();
            // System.out.println("Loaded string: " + new String(byteString));
        }
        return ByteBuffer.wrap(byteString);
    }
}
