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
package com.manniwood.cl4pg.v1.test.etc;

import java.util.Objects;
import java.util.UUID;

public class ImmutableUser {

    private final UUID id;
    private final String name;
    private final String password;
    private final int employeeId;

    public ImmutableUser(UUID id, String name, String password, Integer employeeId) {
        super();
        this.id = id;
        this.name = name;
        this.password = password;
        this.employeeId = employeeId.intValue();
    }

    public ImmutableUser(UUID id, String name, String password, int employeeId) {
        super();
        this.id = id;
        this.name = name;
        this.password = password;
        this.employeeId = employeeId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, password, employeeId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ImmutableUser other = (ImmutableUser) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(password, other.password)
                && employeeId == other.employeeId;
    }
}
