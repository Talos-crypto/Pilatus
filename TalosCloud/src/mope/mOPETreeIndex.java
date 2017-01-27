package mope;

import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Copyright (c) 2016, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 *
 * Author:
 *       Lukas Burkhalter <lubu@student.ethz.ch>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */


/**
 * Created by lukas on 30.12.15.
 */
public class mOPETreeIndex {

    private int ID;

    private String tableName;

    private String columnOPE;

    private String columnDET;

    private String preparedStatement = null;

    private String preparedStatementUpdate = null;

    private String preparedStatementCheck = null;

    public mOPETreeIndex(int ID, String tableName, String columnOPE, String columnDET) {
        this.ID = ID;
        this.tableName = tableName;
        this.columnOPE = columnOPE;
        this.columnDET = columnDET;
    }

    private String generatePreparedStmt() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(columnDET)
                .append(", ").append(columnOPE)
                .append(" FROM ").append(tableName)
                .append(" WHERE UID = ?;");
        return sb.toString();
    }

    private String generatePreparedStmtUpdate() {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(tableName)
                .append(" SET ").append(columnOPE)
                .append(" = ? WHERE ").append(columnDET)
                .append(" = ? AND UID = ?;");
        return sb.toString();
    }

    private String generatePreparedStmtCheck() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(columnOPE)
                .append(" FROM ").append(tableName)
                .append(" WHERE ").append(columnOPE).append(" = ? AND UID = ?;");
        return sb.toString();
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnOPE() {
        return columnOPE;
    }

    public String getColumnDET() {
        return columnDET;
    }

    public String getPreparedStatement() {
        if(preparedStatement == null)
            preparedStatement = generatePreparedStmt();
        return preparedStatement;
    }

    public String getPreparedStatementUpdate() {
        if(preparedStatementUpdate == null)
            preparedStatementUpdate = generatePreparedStmtUpdate();
        return preparedStatementUpdate;
    }

    public String getPreparedStatementCheck() {
        if(preparedStatementCheck == null)
            preparedStatementCheck = generatePreparedStmtCheck();
        return preparedStatementCheck;
    }

    public int getID() {
        return ID;
    }

}
