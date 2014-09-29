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
package com.manniwood.cl4pg.test.factory;

import org.testng.annotations.Factory;

import com.manniwood.cl4pg.test.PgSessionCopyTest;
import com.manniwood.cl4pg.test.PgSessionDeleteTest;
import com.manniwood.cl4pg.test.PgSessionSelectTest;
import com.manniwood.cl4pg.test.PgSessionStoredProcTest;
import com.manniwood.cl4pg.test.PgSessionTest;
import com.manniwood.cl4pg.test.PgSessionUpdateTest;

public class Cl4pgTestFactory {
    @Factory
    public Object[] allTests() {
        return new Object[] {
                new PgSessionCopyTest(),
                new PgSessionDeleteTest(),
                new PgSessionSelectTest(),
                new PgSessionStoredProcTest(),
                new PgSessionTest(),
                new PgSessionUpdateTest()
        };
    }

}
