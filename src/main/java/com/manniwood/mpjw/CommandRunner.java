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

import java.sql.Connection;

import com.manniwood.mpjw.commands.OldCommand;
import com.manniwood.mpjw.converters.ConverterStore;
import com.manniwood.pg4j.v1.commands.Command;

public class CommandRunner {
    public static void execute(OldCommand pg) {
        try {
            pg.execute();
        } catch (Exception e) {
            // Above, catch Exception instead of SQLException so that you can
            // also
            // roll back on other problems.
            try {
                pg.getConnection().rollback();
            } catch (Exception e1) {
                // put e inside e1, so the user has all of the exceptions
                e1.initCause(e);
                throw new MPJWException("Could not roll back connection after catching exception trying to execute:\n"
                        + pg.getSQL(),
                                        e1);
            }
            throw new MPJWException("ROLLED BACK. Exception while trying to run this sql statement:\n"
                    + pg.getSQL(),
                                    e);
        } finally {
            try {
                pg.cleanUp();
            } catch (Exception e) {
                throw new MPJWException("Could not clean up after running the following SQL command; resources may have been left open! SQL command is:\n"
                        + pg.getSQL(),
                                        e);
            }
        }
    }

    public static void execute(Command command,
                               Connection conn,
                               ConverterStore converterStore) {
        try {
            command.execute(conn, converterStore);
        } catch (Exception e) {
            // Above, catch Exception instead of SQLException so that you can
            // also
            // roll back on other problems.
            try {
                conn.rollback();
            } catch (Exception e1) {
                // put e inside e1, so the user has all of the exceptions
                e1.initCause(e);
                throw new MPJWException("Could not roll back connection after catching exception trying to execute:\n"
                        + command.getSQL(),
                                        e1);
            }
            throw new MPJWException("ROLLED BACK. Exception while trying to run this sql statement:\n"
                    + command.getSQL(),
                                    e);
        } finally {
            try {
                command.cleanUp();
            } catch (Exception e) {
                throw new MPJWException("Could not clean up after running the following SQL command; resources may have been left open! SQL command is:\n"
                        + command.getSQL(),
                                        e);
            }
        }
    }
}
