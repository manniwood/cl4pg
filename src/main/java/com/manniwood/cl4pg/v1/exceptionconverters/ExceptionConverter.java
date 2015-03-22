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
package com.manniwood.cl4pg.v1.exceptionconverters;

import com.manniwood.cl4pg.v1.exceptions.Cl4pgException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgPgSqlException;

/**
 * An ExceptionConverter can be used to convert a plain incoming
 * Cl4pgPgSqlException to a sub-class of Cl4pgPgSqlException that
 * you have written. This allows you to write code that catches a
 * more narrow sub-class of Cl4pgPgSqlException (such as a unique
 * key violation while trying to insert an already-taken username),
 * yet still allow other forms of Cl4pgPgSqlException (database is
 * unreachable) to pass by.
 * @author mwood
 *
 */
public interface ExceptionConverter {
    Cl4pgException convert(Cl4pgPgSqlException e);
}
