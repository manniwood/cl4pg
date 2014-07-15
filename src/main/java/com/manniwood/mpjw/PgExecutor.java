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

import java.sql.SQLException;

public class PgExecutor {
    public static void execute(PgExecutable pg) {
        try {
            pg.execute();
        } catch (SQLException e) {
            try {
                pg.getConnection().rollback();
            } catch (SQLException e1) {
                // XXX: do we put e inside e1, so the user has all of the exceptions?
                throw new MPJWException("Could not roll back connection after catching exception trying to execute " + pg.getSQL(), e1);
            }
            throw new MPJWException("ROLLED BACK. Exception while trying to run this sql statement: " + pg.getSQL(), e);
        } finally {
            if (pg.getPreparedStatement() != null) {
                try {
                    pg.getPreparedStatement().close();
                } catch (SQLException e) {
                    throw new MPJWException("Could not close PreparedStatement for " + pg.getSQL(), e);
                }
            }
        }
    }
}
