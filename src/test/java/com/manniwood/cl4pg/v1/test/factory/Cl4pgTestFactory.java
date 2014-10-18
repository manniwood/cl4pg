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
package com.manniwood.cl4pg.v1.test.factory;

import org.testng.annotations.Factory;

import com.manniwood.cl4pg.v1.test.PgSessionSelectTest;
import com.manniwood.cl4pg.v1.test.PgSessionStoredProcTest;
import com.manniwood.cl4pg.v1.test.PgSessionUpdateTest;
import com.manniwood.cl4pg.v1.test.ds.hikaricp.HikariCopyTest;
import com.manniwood.cl4pg.v1.test.ds.hikaricp.HikariDeleteTest;
import com.manniwood.cl4pg.v1.test.ds.hikaricp.HikariSessionTest;
import com.manniwood.cl4pg.v1.test.ds.poolingpg.PoolingPgCopyTest;
import com.manniwood.cl4pg.v1.test.ds.poolingpg.PoolingPgDeleteTest;
import com.manniwood.cl4pg.v1.test.ds.poolingpg.PoolingPgSessionTest;
import com.manniwood.cl4pg.v1.test.ds.simplepg.SimplePgCopyTest;
import com.manniwood.cl4pg.v1.test.ds.simplepg.SimplePgDeleteTest;
import com.manniwood.cl4pg.v1.test.ds.simplepg.SimplePgSessionTest;

public class Cl4pgTestFactory {
    @Factory
    public Object[] allTests() {
        return new Object[] {
                new SimplePgSessionTest(),
                new PoolingPgSessionTest(),
                new HikariSessionTest(),

                new SimplePgCopyTest(),
                new PoolingPgCopyTest(),
                new HikariCopyTest(),

                new SimplePgDeleteTest(),
                new PoolingPgDeleteTest(),
                new HikariDeleteTest(),

                new PgSessionUpdateTest(),
                new PgSessionSelectTest(),
                new PgSessionStoredProcTest(),

        };
    }
}
