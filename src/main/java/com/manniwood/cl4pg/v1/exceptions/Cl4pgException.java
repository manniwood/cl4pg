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
package com.manniwood.cl4pg.v1.exceptions;

/**
 * All Cl4pg Exceptions are either this exception or children of this Exception.
 * Because Cl4pgException extends RuntimeException, all Cl4pg Exceptions are
 * unchecked exceptions. This is good for Exceptions that actually are difficult
 * to handle such as a database running out of space, or a SQL query being
 * malformed; but it is not so good for exceptions that would be programatically
 * easy to handle, such as a primary key exception (for instance, trying to
 * enter a username that has already been chosen). However, for easily-handled
 * exceptions, consider implementing an ExceptionConverter.
 *
 * @author mwood
 *
 */
public class Cl4pgException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public Cl4pgException() {
        super();
    }

    public Cl4pgException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public Cl4pgException(String message, Throwable cause) {
        super(message, cause);
    }

    public Cl4pgException(String message) {
        super(message);
    }

    public Cl4pgException(Throwable cause) {
        super(cause);
    }

}
