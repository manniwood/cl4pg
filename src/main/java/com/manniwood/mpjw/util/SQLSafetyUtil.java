/*
The MIT License (MIT)

Copyright (c) 2014 Manni Wood

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package com.manniwood.mpjw.util;

import com.manniwood.pg4j.v1.Pg4jException;

public class SQLSafetyUtil {
    /**
     * MUST prevent SQL injection attack. Easiest way is to scrub anything that
     * is not a-z/A-Z, meaning all whitespace and punctuation will get removed,
     * preventing little Bobby Droptables.
     * 
     * @param channel2
     * @return
     */
    public static String throwIfUnsafe(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isLetter(c)) {
                throw new Pg4jException("The character " + c + " in string " + str + " is not safe for use in SQL");
            }
        }
        return str;
    }
}
