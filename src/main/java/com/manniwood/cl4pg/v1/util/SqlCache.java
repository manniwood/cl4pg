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

import com.manniwood.cl4pg.v1.datasourceadapters.PgSimpleDataSourceAdapter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgFileNotFoundException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Caches SQL files in RAM so that subsequent requests for
 * a SQL file from the classpath will check RAM first. Thread-safe.
 * @author mwood
 *
 */
public class SqlCache {
    private final static Logger log = LoggerFactory.getLogger(SqlCache.class);

    public static final String SQL_CACHE_FILE = "cl4pg/SqlCache.txt";

    private final Map<String, String> cache;

    public SqlCache() {
        String cacheFileContents = null;
        try {
            cacheFileContents = ResourceUtil.slurpFileFromClasspath(SQL_CACHE_FILE);
        } catch (Cl4pgFileNotFoundException e) {
            log.info("File {} not found on classpath; SQL Cache will not be initialized.", SQL_CACHE_FILE);
        }
        if (Str.isNullOrEmpty(cacheFileContents)) {
            cache = null;
            return;
        }

        // Multiple threads will be fetching from this map, so let's make
        // it unmodifiable just to make this clear.
        cache = cacheStringToUnmodifiableMap(cacheFileContents);
    }

    private Map<String, String> cacheStringToUnmodifiableMap(String cacheFileContents) {
        String[] lines = cacheFileContents.split(System.lineSeparator());
        Map<String, String> rwCache = new HashMap<>();
        int lineNumber = 0;  // Keep track of what line we are on so we have nice error reporting.
        for (String line: lines) {
            lineNumber++;

            // Trim leading and trailing whitespace.
            String cleanLine = line.trim();

            // See if this line has a comment.
            int firstCommentCharIdx = cleanLine.indexOf("#");
            // If this line does have a comment, remove the comment.
            if (firstCommentCharIdx != -1) {
                cleanLine = cleanLine.substring(0, cleanLine.indexOf("#"));
                // Re-trim the line of leading and trailing whitespace,
                // because there was likely whitespace between the content
                // and the comment.
                cleanLine = cleanLine.trim();
            }

            // After all this, we may have ended up with an empty line;
            // only pay attention to lines with actual content.
            if ( ! Str.isNullOrEmpty(cleanLine)) {
                String fileContents = null;
                try {
                    fileContents = ResourceUtil.slurpFileFromClasspath(cleanLine);
                } catch (Cl4pgFileNotFoundException | Cl4pgIOException e) {
                    throw new Cl4pgIOException("Problem at line " + lineNumber + " of " + SQL_CACHE_FILE + ": Problem reading \"" + cleanLine + "\" from classpath", e);
                }
                log.info("Loading \"{}\" from line {} of {}", cleanLine, lineNumber, SQL_CACHE_FILE);
                rwCache.put(cleanLine, fileContents);
            }
        }
        return Collections.unmodifiableMap(rwCache);
    }

    public String get(String path) {
        return cache.get(path);
    }
}
