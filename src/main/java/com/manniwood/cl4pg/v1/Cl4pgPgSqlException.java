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
package com.manniwood.cl4pg.v1;

import org.postgresql.util.ServerErrorMessage;

public class Cl4pgPgSqlException extends Cl4pgSqlException {

    private static final long serialVersionUID = 1L;

    private String sqlState;
    private String serverMessage;
    private String severity;
    private String detail;
    private String hint;
    private int position;
    private String where;
    private String schema;
    private String table;
    private String column;
    private String dataType;
    private String constraint;
    private String file;
    private int line;
    private String routine;
    private String internalQuery;
    private int internalPosition;

    public Cl4pgPgSqlException() {
        super();
    }

    public Cl4pgPgSqlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public Cl4pgPgSqlException(String message, Throwable cause) {
        super(message, cause);
    }

    public Cl4pgPgSqlException(String message) {
        super(message);
    }

    public Cl4pgPgSqlException(Throwable cause) {
        super(cause);
    }

    public Cl4pgPgSqlException(ServerErrorMessage sem) {
        super();
        initFromServerErrorMessage(sem);
    }

    public Cl4pgPgSqlException(ServerErrorMessage sem, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        initFromServerErrorMessage(sem);
    }

    public Cl4pgPgSqlException(ServerErrorMessage sem, String message, Throwable cause) {
        super(message, cause);
        initFromServerErrorMessage(sem);
    }

    public Cl4pgPgSqlException(ServerErrorMessage sem, String message) {
        super(message);
        initFromServerErrorMessage(sem);
    }

    public Cl4pgPgSqlException(ServerErrorMessage sem, Throwable cause) {
        super(cause);
        initFromServerErrorMessage(sem);
    }

    public String getSqlState() {
        return sqlState;
    }

    public void setSqlState(String sqlState) {
        this.sqlState = sqlState;
    }

    private void initFromServerErrorMessage(ServerErrorMessage sem) {
        if (sem == null) {
            return;
        }
        sqlState = sem.getSQLState();
        serverMessage = sem.getMessage();
        severity = sem.getSeverity();
        detail = sem.getDetail();
        hint = sem.getHint();
        position = sem.getPosition();
        where = sem.getWhere();
        schema = sem.getSchema();
        table = sem.getTable();
        column = sem.getColumn();
        dataType = sem.getDatatype();
        constraint = sem.getConstraint();
        file = sem.getFile();
        line = sem.getLine();
        routine = sem.getRoutine();
        internalQuery = sem.getInternalQuery();
        internalPosition = sem.getInternalPosition();
    }

    public String getServerMessage() {
        return serverMessage;
    }

    public void setServerMessage(String serverMessage) {
        this.serverMessage = serverMessage;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getConstraint() {
        return constraint;
    }

    public void setConstraint(String constraint) {
        this.constraint = constraint;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getRoutine() {
        return routine;
    }

    public void setRoutine(String routine) {
        this.routine = routine;
    }

    public String getInternalQuery() {
        return internalQuery;
    }

    public void setInternalQuery(String internalQuery) {
        this.internalQuery = internalQuery;
    }

    public int getInternalPosition() {
        return internalPosition;
    }

    public void setInternalPosition(int internalPosition) {
        this.internalPosition = internalPosition;
    }

    @Override
    public String toString() {
        return "" + Cl4pgPgSqlException.class.getSimpleName() + " [sqlState=" + sqlState + ", serverMessage=" + serverMessage + ", severity=" + severity
                + ", detail=" + detail + ", hint="
                + hint + ", position=" + position + ", where=" + where + ", schema=" + schema + ", table=" + table + ", column=" + column + ", dataType="
                + dataType + ", constraint=" + constraint + ", file=" + file + ", line=" + line + ", routine=" + routine + ", internalQuery=" + internalQuery
                + ", internalPosition=" + internalPosition + "]";
    }

}
