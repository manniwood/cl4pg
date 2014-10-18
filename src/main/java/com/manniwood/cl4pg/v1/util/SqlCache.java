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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SqlCache {

    ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

    public SqlCache() {

    }

    public String slurpFileFromClasspath(String path) {
        String sql = cache.get(path);
        // THREADING: Two or more threads inquiring about the same path
        // will find the path is not in the cache.
        if (sql != null) {
            return sql;
        }
        // THREADING: Two or more threads inquiring about the same path
        // will read the contents of path into their own copy of sql; all
        // threads' copies of sql will be the same. (Note that we do not expect
        // any changes to the file contents during normal operation.)
        sql = ResourceUtil.slurpFileFromClasspath(path);
        // THREADING: Two or more threads will try to enter the file contents
        // into the cache; one will win. It is OK that the others fail,
        // because the contents of the file are the same for each thread.
        cache.putIfAbsent(path, sql);
        // THREADING: Two or more threads inquiring about the same path
        // will have identical copies of sql and will return those identical
        // copies of sql, which is fine.
        return sql;
    }
}
