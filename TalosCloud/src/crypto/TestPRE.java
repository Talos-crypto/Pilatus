package crypto;

import com.mysql.jdbc.Driver;
import org.junit.Test;
import org.restlet.engine.util.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

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
 * A Class for testing the PRE implementation in combination
 * with a MySQL database. (test_db_pre.sql)
 */
public class TestPRE {

    private static final String DB_HOSTNAME = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "pre_db";
    private static final String DB_UNAME = "test";
    private static final String DB_PW = "test";

    private static Connection getConn() throws SQLException {
        new Driver();
        return DriverManager.getConnection("jdbc:mysql://" + DB_HOSTNAME	+ ":" + DB_PORT + "/" + DB_NAME, DB_UNAME, DB_PW);
    }

    @Test
    public void testPRE() {
        PRERelic.PREKey key = PRERelic.generatePREKeys();
        PRERelic.PRECipher cipher = PRERelic.encrypt(1, key);
        int i = 1;
    }

    @Test
    public void testCRTPRE() {
        CRTPreRelic.CRTParams params = CRTPreRelic.generateCRTParams(new PRNGImpl(), 17, 2);
        CRTPreRelic.CRTPreKey key = CRTPreRelic.generateKeys(params);
        CRTPreRelic.CRTPreCipher cipher = CRTPreRelic.encrypt(BigInteger.valueOf(1), key);
        byte[] encoded = cipher.encodeCipher();
        CRTPreRelic.CRTPreCipher decoded = CRTPreRelic.CRTPreCipher.decodeCipher(encoded);
    }


    private static int resetDB() {
        Connection conn = null;
        PreparedStatement insert = null;
        int sum=0;
        try {
            conn = getConn();
            insert = conn.prepareStatement(DB_RESET);
            insert.execute();
        } catch (SQLException e) {
            fail(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(insert!=null) insert.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return sum;
    }



    private static int insertDB(PRERelic.PREKey aliceKey, int uid) {
        Connection conn = null;
        PreparedStatement insert = null;
        int sum=0;
        try {
            conn = getConn();

            for(int i=0;i<1000;i++) {
                insert = conn.prepareStatement(DB_INSERT);
                PRERelic.PRECipher cipher = PRERelic.encrypt(i, aliceKey);
                insert.setInt(1, uid);
                insert.setBytes(2, cipher.encode());
                insert.execute();
                insert.close();
                sum+=i;
            }

        } catch (SQLException e) {
            fail(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(insert!=null) insert.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return sum;
    }

    private static int insertDBCRT(CRTPreRelic.CRTPreKey aliceKey, int uid) {
        Connection conn = null;
        PreparedStatement insert = null;
        int sum=0;
        try {
            conn = getConn();

            for(int i=0;i<1000;i++) {
                insert = conn.prepareStatement(DB_INSERT);
                CRTPreRelic.CRTPreCipher cipher = CRTPreRelic.encrypt(BigInteger.valueOf(i), aliceKey);
                insert.setInt(1, uid);
                insert.setBytes(2, cipher.encodeCipher());
                insert.execute();
                insert.close();
                sum+=i;
            }

        } catch (SQLException e) {
            fail(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(insert!=null) insert.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return sum;
    }

    private static List<CRTPreRelic.CRTPreCipher> reencDBCRT(PRERelic.PREToken token) {
        Connection conn = null;
        PreparedStatement re = null;
        ArrayList<CRTPreRelic.CRTPreCipher> ciphers = new ArrayList<>();
        int sum=0;
        try {
            conn = getConn();
            re = conn.prepareStatement(DB_RE);
            re.setBytes(1, token.getToken());
            ResultSet res = re.executeQuery();
            while(res.next()) {
                byte[] temp = res.getBytes(1);
                ciphers.add(CRTPreRelic.CRTPreCipher.decodeCipher(temp));
            }

        } catch (SQLException e) {
            fail(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(re!=null) re.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return ciphers;
    }

    private static void reencDBCRTJob(int uidBefore, int uidAfter, PRERelic.PREToken token) {
        Connection conn = null;
        CallableStatement re = null;
        int sum=0;
        try {
            conn = getConn();
            re = conn.prepareCall(DB_REENC);
            re.setInt(1, uidBefore);
            re.setInt(2, uidAfter);
            re.setBytes(3, token.getToken());
            re.executeUpdate();
        } catch (SQLException e) {
            fail(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(re!=null) re.close();} catch (SQLException e) { e.printStackTrace();}
        }
    }

    private static void insertDBREENCS(List<CRTPreRelic.CRTPreCipher> reencs, int uid) {
        Connection conn = null;
        PreparedStatement insert = null;
        int sum=0;
        try {
            conn = getConn();

            for(CRTPreRelic.CRTPreCipher cipher : reencs) {
                insert = conn.prepareStatement(DB_INSERT);
                insert.setBytes(2, cipher.encodeCipher());
                insert.setInt(1, uid);
                insert.execute();
                insert.close();
            }

        } catch (SQLException e) {
            fail(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(insert!=null) insert.close();} catch (SQLException e) { e.printStackTrace();}
        }
    }

    private static byte[] doSUMDB() {
        Connection conn = null;
        PreparedStatement sum = null;
        try {
            conn = getConn();
            sum = conn.prepareStatement(DB_SUM);
            ResultSet res = sum.executeQuery();
            if(res.next()) {
                byte[] temp = res.getBytes("PRE_REL_SUM(testsum.add)");
                return temp;
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(sum!=null) sum.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return null;
    }
    private static final String DB_INSERT = "INSERT INTO testsum VALUES(0,?,?)";
    private static final String DB_SUM = "SELECT PRE_REL_SUM(testsum.add) FROM testsum;";
    private static final String DB_RE = "SELECT PRE_REL_REENC(testsum.add, ?) FROM testsum;";
    private static final String DB_RESET = "DELETE FROM testsum";
    private static final String DB_REENC= "{CALL reEncrypt(?,?,?)}";

    @Test
    public void fillDB() {
        PRERelic.PREKey key = PRERelic.generatePREKeys();
        PRERelic.PRECipher cipher = PRERelic.encrypt(1, key);
        resetDB();
        int res = insertDB(key,1);
        PRERelic.PRECipher sum =  new PRERelic.PRECipher(doSUMDB());
        long exp_res = PRERelic.decrypt(sum, key, true);
        resetDB();
        assertEquals(res, exp_res);
    }

    @Test
    public void fillDBCRT() {
        CRTPreRelic.CRTParams params = CRTPreRelic.generateCRTParams(new PRNGImpl(), 17, 2);
        CRTPreRelic.CRTPreKey key = CRTPreRelic.generateKeys(params);
        resetDB();
        int res = insertDBCRT(key,1);
        CRTPreRelic.CRTPreCipher sum = CRTPreRelic.CRTPreCipher.decodeCipher(doSUMDB());
        BigInteger exp_res = CRTPreRelic.decrypt(sum, key, true);
        resetDB();
        assertEquals(res, exp_res.intValue());
    }

    @Test
    public void reencDBCRT() {
        CRTPreRelic.CRTParams params = CRTPreRelic.generateCRTParams(new PRNGImpl(), 17, 2);
        CRTPreRelic.CRTPreKey akey = CRTPreRelic.generateKeys(params);
        CRTPreRelic.CRTPreKey bkey = CRTPreRelic.generateKeys(params);
        PRERelic.PREToken token = PRERelic.createToken(akey, bkey);
        resetDB();
        int res = insertDBCRT(akey,1);
        List<CRTPreRelic.CRTPreCipher> reencs = reencDBCRT(token);
        int i=0;
        for(CRTPreRelic.CRTPreCipher cipher:reencs) {
            int temp = CRTPreRelic.decrypt(cipher, bkey, true).intValue();
            assertEquals(i, temp);
            i++;
        }
        resetDB();
        insertDBREENCS(reencs,2);
        CRTPreRelic.CRTPreCipher sum = CRTPreRelic.CRTPreCipher.decodeCipher(doSUMDB());
        BigInteger exp_res = CRTPreRelic.decrypt(sum, bkey, true);
        assertEquals(res, exp_res.intValue());
        resetDB();
    }

    @Test
    public void reencDBCRTJob() {
        CRTPreRelic.CRTParams params = CRTPreRelic.generateCRTParams(new PRNGImpl(), 17, 2);
        CRTPreRelic.CRTPreKey akey = CRTPreRelic.generateKeys(params);
        CRTPreRelic.CRTPreKey bkey = CRTPreRelic.generateKeys(params);
        PRERelic.PREToken token = PRERelic.createToken(akey, bkey);
        resetDB();
        int res = insertDBCRT(akey, 1);
        reencDBCRTJob(1, 2, token);
    }

    @Test
    public void genPreParamams() throws IOException, ClassNotFoundException {
        CRTPreRelic.CRTParams params32 = CRTPreRelic.generateCRTParams(new PRNGImpl(), 17, 2);
        CRTPreRelic.CRTParams params64 = CRTPreRelic.generateCRTParams(new PRNGImpl(), 23, 3);
        PRERelic.PREKey keyA = PRERelic.generatePREKeys();
        PRERelic.PREKey keyB = PRERelic.generatePREKeys();

        System.out.println(params32.getStringRep());

        System.out.println(params64.getStringRep());


        System.out.println(Base64.encode(keyA.getEncoded(), false));


        System.out.println(Base64.encode(keyB.getEncoded(), false));

    }
}
