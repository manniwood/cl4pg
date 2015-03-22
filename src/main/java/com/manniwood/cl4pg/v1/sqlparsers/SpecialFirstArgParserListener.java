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

import java.util.ArrayList;
import java.util.List;

/**
 * Listens for each <code>#{blah}</code> in a Cl4pg SQL template, storing the
 * first-encountered #{blah} specially, and adding the remaining
 * <code>#{blah}</code>s to a List. For instance, the template
 *
 * <pre>
 * <code>
 * { #{java.lang.Integer} = call add_and_return( #{getFirst}, #{getSecond} ) }</code>
 * </pre>
 *
 * will reserve "java.lang.Integer" as the special first arg, which can be
 * fetched using getFirstArg(), and create the List of Strings "getFirst",
 * "getSecond", which can be fetched using getArgs().
 *
 * 
 * @author mwood
 *
 */
public class SpecialFirstArgParserListener implements ParserListener {

    private int argNumber = 0;

    private String firstArg;

    private final List<String> args = new ArrayList<>();

    @Override
    public String arg(String arg) {
        if (argNumber == 0) {
            firstArg = arg;
        } else {
            args.add(arg);
        }
        argNumber++;
        return "?";
    }

    public List<String> getArgs() {
        return args;
    }

    public String getFirstArg() {
        return firstArg;
    }
}
