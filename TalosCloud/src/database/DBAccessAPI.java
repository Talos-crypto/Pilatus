package database;

import util.MessageUtil;
import util.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;

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
 * Implements the MySql database API for executing a talos command in the database
 * which internally is a stored procedure.
 */
public class DBAccessAPI {

    /**
     * Executed the stored procedure, given the command description, the arguments of the procedure
     * and the user. which wants to execute it
     * @param user the executing user
     * @param cmd the description of the command in the database
     * @param args the arguments
     * @return a talos message string representation of the answer
     * @throws APIException
     */
    public static String excecuteCommand(User user, CommandDescription cmd, String[] args) throws APIException {
        Connection conn = null;
        CallableStatement cStmt = null;
        String[] argTypes = cmd.getTypeInformation();
        ResultSet res = null;

        if(cmd.getNumArgs()-1!=args.length) {
            throw new APIException("Wrong number of arguments");
        }

        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            cStmt = getCmdPrepareStmt(conn, user, cmd, args);
            if(cmd.isQuery()) {
                res = cStmt.executeQuery();
            } else {
                cStmt.executeUpdate();
            }
            return handleResultSet(res);
        } catch (SQLException e) {
            throw new APIException(e.getMessage());
        } catch (NumberFormatException e) {
            throw new APIException("Wrong type for argument");
        } catch (IOException e) {
            throw new APIException("Error while formatting JSON response");
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(cStmt!=null) cStmt.close();} catch (SQLException e) { e.printStackTrace();}
            try { if(res!=null) res.close();} catch (SQLException e) { e.printStackTrace();}
        }

    }

    /**
     * Creates the database command for exxecuting the stored procedure
     */
    static CallableStatement getCmdPrepareStmt(Connection conn, User user, CommandDescription cmd, String[] args) throws SQLException {
        String[] argTypes = cmd.getTypeInformation();
        CallableStatement cStmt = conn.prepareCall("{call "+cmd.getCmdDescription()+"}");
        int index = 0;
        for(int i=0; i<argTypes.length; i++) {
            if(argTypes[i].equals(CommandDescription.TYPE_ANNOT_INT)) {
                cStmt.setBigDecimal(i+1, new BigDecimal(args[index++]));
            } else if(argTypes[i].equals(CommandDescription.TYPE_ANNOT_STRING)) {
                cStmt.setString(i + 1, args[index++]);
            } else if(argTypes[i].equals(CommandDescription.TYPE_ANNOT_UID)) {
                cStmt.setInt(i + 1, user.getLocalid());
            } else if(argTypes[i].equals(CommandDescription.TYPE_HEXSTR_UID) ||
                    argTypes[i].equals(CommandDescription.TYPE_PRE_UID)) {
                cStmt.setBytes(i+1, HexToBytes(args[index++].replace("0x","")));
            } else {
                throw new APIException("Wrong type for argument");
            }
        }
        return cStmt;
    }

    public static byte[] HexToBytes(String in) {
        int len = in.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(in.charAt(i), 16) << 4)
                    + Character.digit(in.charAt(i + 1), 16));
        }
        return data;
    }

    private static String handleResultSet(ResultSet rs) throws IOException {
        if(rs == null) {
            return MessageUtil.getRepsonseMessageFromUpdtaeDBResult();
        } else {
            return MessageUtil.getRepsonseMessageFromDBResult(rs);
        }
    }

}
