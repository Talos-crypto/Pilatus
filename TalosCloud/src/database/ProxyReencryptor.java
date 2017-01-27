package database;

import crypto.CRTPreRelic;
import crypto.PRERelic;
import crypto.PRERelic.*;
import util.User;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * Implements the proxy-re-encryption worker pool, which operates on the databases
 * and re-encrpyts data when a new share relation is created.
 * Assumes a 'reEncrypt' stored procedure exists and is implemented in the database.
 */
public class ProxyReencryptor implements Closeable {

    private ExecutorService pool;
    private static ProxyReencryptor temp = null;

    private ProxyReencryptor() {
        pool = Executors.newFixedThreadPool(2);
    }

    public static ProxyReencryptor getReencryptor() {
        if(temp==null) {
            temp = new ProxyReencryptor();
        }
        return temp;
    }

    /**
     * Post a re-encryption task from A to B on the database
     * @param uidfrom the userid from user A
     * @param combined the combined user (A->B)
     * @param token the re-encryption token from A->B
     */
    public void postReEncTask(int uidfrom, int combined, PREToken token) {
        this.pool.execute(new REEncTask(uidfrom, combined, token));
    }

    /**
     * Post insert commands tasks from A to B's on the database.
     * I.e A shares datae with B's and has exectued an new insert command,
     * and the data for B's has also to be updated.
     * @param others the B's
     * @param cmd the description of the command
     * @param args the command arguments.
     */
    public void postReEncTaskAdd(List<User> others, CommandDescription cmd, String[] args) {
        this.pool.execute(new REEncTaskAdd(others, cmd, args));
    }

    private class REEncTask implements Runnable {
        private int uidfrom;
        private int combined;
        private PREToken token;

        public REEncTask(int uidfrom, int combined, PREToken token) {
            this.uidfrom = uidfrom;
            this.combined = combined;
            this.token = token;
        }

        private static final String SQL_REENC_JOB = "{CALL reEncrypt(?,?,?)}";
        private static final String SQL_UPDATE_INFO = "UPDATE Share SET Share.replicated=1 WHERE Share.fromid=? AND Share.combined=?;";

        @Override
        public void run() {
            Connection conn = null;
            CallableStatement re = null;
            PreparedStatement update = null;
            try {
                conn = MySqlConnectionPool.getInstance().getConnection();
                re = conn.prepareCall(SQL_REENC_JOB);
                re.setInt(1, uidfrom);
                re.setInt(2, combined);
                re.setBytes(3, token.getToken());
                re.executeUpdate();

                update = conn.prepareStatement(SQL_UPDATE_INFO);
                update.setInt(1, uidfrom);
                update.setInt(2, combined);
                update.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
                try { if(re!=null) re.close();} catch (SQLException e) { e.printStackTrace();}
                try { if(update!=null) update.close();} catch (SQLException e) { e.printStackTrace();}
            }
        }
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        builder.append("0x");
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private class REEncTaskAdd implements Runnable {
        private List<User> others;
        private CommandDescription cmd;
        private String[] args;

        public REEncTaskAdd(List<User> others, CommandDescription cmd, String[] args) {
            this.others = others;
            this.cmd = cmd;
            this.args = args;
        }

        @Override
        public void run() {
            List<Integer> indexes = new ArrayList<>();
            int idx = -1;
            for(String annot : cmd.getTypeInformation()) {
                if(annot.equals(CommandDescription.TYPE_PRE_UID))
                    indexes.add(idx);
                idx++;
            }

            for (User other : others) {
                String[] newArgs = new String[args.length];
                for(int iter=0;iter<newArgs.length; iter++) {
                    if (indexes.contains(iter)) {
                        byte[] data = DBAccessAPI.HexToBytes(args[iter].replace("0x",""));
                        CRTPreRelic.CRTPreCipher cipher = CRTPreRelic.reEncrypt(CRTPreRelic.CRTPreCipher.decodeCipher(data), new PREToken(other.getPk()));
                        newArgs[iter] = bytesToHex(cipher.encodeCipher());
                    } else {
                        newArgs[iter] = args[iter];
                    }
                }
                DBAccessAPI.excecuteCommand(other, cmd, newArgs);
            }
        }
    }

    @Override
    public void close() throws IOException {
        pool.shutdown();
    }
}
