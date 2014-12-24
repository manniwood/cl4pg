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
package com.manniwood.cl4pg.v1.util;

import com.manniwood.cl4pg.v1.exceptions.Cl4pgIllegalTransactionIsolationLevelException;

import java.sql.Connection;

/**
 * @author mwood
 */
public class TransactionIsolationLevelConverter {

    private TransactionIsolationLevelConverter() {
        // utility class; cannot be instantiated
    }


    public static int convert(String name) {
        if (name.equals("1")
                || name.equalsIgnoreCase("TRANSACTION_READ_UNCOMMITTED")
                || name.equalsIgnoreCase("READ_UNCOMMITTED")
                || name.equalsIgnoreCase("TRANSACTION READ UNCOMMITTED")
                // this is what the PostgreSQL docs name this transaction level
                || name.equalsIgnoreCase("READ UNCOMMITTED")) {
            return Connection.TRANSACTION_READ_UNCOMMITTED;
        }
        if (name.equals("2")
                || name.equalsIgnoreCase("TRANSACTION_READ_COMMITTED")
                || name.equalsIgnoreCase("READ_COMMITTED")
                || name.equalsIgnoreCase("TRANSACTION READ COMMITTED")
                // this is what the PostgreSQL docs name this transaction level
                || name.equalsIgnoreCase("READ COMMITTED")) {
            return Connection.TRANSACTION_READ_COMMITTED;
        }
        if (name.equals("4")
                ||name.equalsIgnoreCase("TRANSACTION_REPEATABLE_READ")
                || name.equalsIgnoreCase("REPEATABLE_READ")
                || name.equalsIgnoreCase("TRANSACTION REPEATABLE READ")
                // this is what the PostgreSQL docs name this transaction level
                || name.equalsIgnoreCase("REPEATABLE READ")) {
            return Connection.TRANSACTION_REPEATABLE_READ;
        }
        if (name.equals("8")
                || name.equalsIgnoreCase("TRANSACTION_SERIALIZABLE")
                // this is what the PostgreSQL docs name this transaction level
                || name.equalsIgnoreCase("SERIALIZABLE")
                || name.equalsIgnoreCase("TRANSACTION SERIALIZABLE")) {
            return Connection.TRANSACTION_SERIALIZABLE;
        }
        throw new Cl4pgIllegalTransactionIsolationLevelException("\"" + name + "\" is not a legal transaction isolation level");
    }
}
