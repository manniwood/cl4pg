/*
The MIT License (MIT)

Copyright (c) 2015 Manni Wood

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
package com.manniwood.cl4pg.v1.sqlparsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses Cl4pg SQL templates and returns question-mark-using SQL strings for
 * use in PreparedStatements.
 * <p>
 * For instance, the Cl4pg SQL template
 *
 * <pre>
 * <code>
 * select foo from bars where foo_bar = #{java.lang.String}
 * </code>
 * </pre>
 *
 * will be converted to
 *
 * <pre>
 * <code>
 * select foo from bars where foo_bar = ?
 * </code>
 * </pre>
 *
 * which is the format that JDBC actually uses.
 * <p>
 * This SqlParser is also handed a ParserListener, which listens for every
 * occurrence of <code>#{blah}</code> in the C4pg SQL template.
 *
 * @author mwood
 *
 */
public class SqlParser {

    private final static Logger log = LoggerFactory
            .getLogger(SqlParser.class);

    private final ParserListener parserListener;

    public SqlParser(ParserListener parserListener) {
        this.parserListener = parserListener;
    }

    /**
     * Transforms a Cl4pg SQL template into a JDBC string, replacing
     * <code>#{blah}</code> with <code>?</code>, and calling the
     * ParserListener's arg() method each time a <code>#{blah}</code> is
     * encountered.
     * 
     * @param sql
     * @return
     */
    public String transform(String sql) {
        log.debug("incoming sql:\n{}", sql);
        char[] chrs = sql.toCharArray();
        int chrsLen = chrs.length;
        StringBuilder sqlSb = new StringBuilder();
        for (int i = 0; i < chrsLen; i++) {
            if (chrs[i] == '#') {
                i++;
                if (i >= chrsLen) {
                    break;
                }
                if (chrs[i] == '{') {
                    i = extractArg(sqlSb, chrs, chrsLen, i);
                }
            } else {
                sqlSb.append(chrs[i]);
            }
        }
        String transformedSql = sqlSb.toString();
        log.debug("outgoing sql:\n{}", transformedSql);
        return transformedSql;
    }

    private int extractArg(StringBuilder sqlSb,
                           char[] chrs,
                           int chrsLen,
                           int i) {
        StringBuilder arg = new StringBuilder();
        while (i < chrsLen && chrs[i] != '}') {
            i++;
            if (chrs[i] != '}') {
                arg.append(chrs[i]);
            }
        }
        if (chrs[i] == '}') {
            log.debug("adding arg: {}", arg.toString());
            String replacer = parserListener.arg(arg.toString());
            sqlSb.append(replacer);
        }
        return i;
    }
}
