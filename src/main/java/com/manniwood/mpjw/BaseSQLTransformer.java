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
package com.manniwood.mpjw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseSQLTransformer {

    private final static Logger log = LoggerFactory.getLogger(BaseSQLTransformer.class);

    protected final String sql;
    protected String transformedSQL;

    public BaseSQLTransformer(String sql) {
        this.sql = sql;
    }

    public void transform() {
        log.debug("incoming sql:\n{}", sql);
        char[] chrs = sql.toCharArray();
        int chrsLen = chrs.length;
        StringBuilder sqlSB = new StringBuilder();
        for (int i = 0; i < chrsLen; i++) {
            if (chrs[i] == '#') {
                i++;
                if (i >= chrsLen) {
                    break;
                }
                if (chrs[i] == '{') {
                    i = extractArg(chrs, chrsLen, i);
                }
                sqlSB.append("?");
            } else {
                sqlSB.append(chrs[i]);
            }
        }
        transformedSQL = sqlSB.toString();
        log.debug("outgoing sql:\n{}", transformedSQL);
    }

    abstract public int extractArg(
            char[] chrs,
            int chrsLen,
            int i);

}
