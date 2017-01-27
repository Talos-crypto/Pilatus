package testmope;

import com.mysql.jdbc.Driver;
import database.CommandDescription;
import database.DBAccessAuthenticate;
import database.MySqlConnectionPool;
import database.mOPEDBInterface;
import mope.mOPEInteractionState;
import mope.mOPEInteractionTree;
import mope.mOPEJob;
import mope.messages.mOPEClientStepMessage;
import mope.messages.mOPEResponseMessage;
import mope.messages.mOPEResultMessage;
import mopetree.Tuple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import util.MessageUtil;
import util.SystemUtil;
import util.User;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
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
 * Created by lukas on 01.01.16.
 */
public class TestmOPEState {

    private static final String DB_HOSTNAME = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "OPETest";
    private static final String DB_UNAME = "dmdb";
    private static final String DB_PW = "1234";

    private static final int NUM_USERS = 10;

    @BeforeClass
    public static void setUpTestDB() {
        MySqlConnectionPool.DEBUG = true;

        MySqlConnectionPool.debugSource.put(SystemUtil.APPLICATION_SERVER_DBPOOL_RES, new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return  getConn();
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return null;
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return null;
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {

            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {

            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return 0;
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return null;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }
        });
    }

    private User[] users = null;
    private static final Random rnd = new Random();
    private static final String FUNC_1 = "storemOPE";
    private static final String FUNC_2 = "rangemOPE";
    private static final String FUNC_3 = "deleteOPE";

    private static class UniqueRND {
        HashSet<Integer> before = new HashSet<>();
        public int getInt() {
            int res = rnd.nextInt();
            while (before.contains(res)) {
                res = rnd.nextInt();
            }
            before.add(res);
            return res;
        }
    }

    private void generateUsers() {
        users = new User[NUM_USERS];
        for(int i = 0; i < users.length; i++) {
            users[i] = new User(i, String.valueOf(i), "mail@test"+i+".ch", null);
        }
    }

    public User getRndUser() {
        return users[rnd.nextInt(users.length)];
    }

    @Before
    public void setUp() {
        generateUsers();
    }

    @Test
    public void checkInsert() {
        resetDB();
        performInsertsWithUser(users[0], 1000);
        try {
            Thread.sleep(1*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testDBOrderForUser(users[0]);

        String before = printTrees();
        mOPEDBInterface.getInstance().resetCaches();
        String after = printTrees();

        assertEquals(before, after);
    }

    @Test
    public void checkQuery() {
        checkInsert();
        int maxRounds = 1000;
        for(int i=1; i<maxRounds; i++) {
            int small = rnd.nextInt(), big = rnd.nextInt();
            if(small>big) {
                int temp = small;
                small = big;
                big = temp;
            }
            System.out.println("Small: "+small+ " Big: "+big);
            mOPEResultMessage msg = performQuery(users[0], small, big);
            String compare = testQueryValid(users[0], small, big);
            assertEquals(compare, msg.getResult());
        }
    }

    private Tuple<Integer,Integer> getRndtupleFromList(ArrayList<Tuple<Integer,Integer>> list) {
        int index = rnd.nextInt(list.size());
        Tuple<Integer,Integer> res = list.get(index);
        list.remove(index);
        return res;
    }

    @Test
    public void checkDelete() {
        resetDB();
        UniqueRND rand = new UniqueRND();
        int numAdds = 1000;
        int numDel = 100;
        ArrayList<Tuple<Integer,Integer>> vals = new ArrayList<>();
        for(int i=0; i<numAdds; i++) {
            int first = rand.getInt();
            int second = rand.getInt();
            vals.add(new Tuple<>(first, second));
            performInsert(users[0], first, second);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(int i=0; i<numDel; i++) {
            Tuple<Integer, Integer> cur = getRndtupleFromList(vals);
            performDelete(users[0], cur.a, cur.b);
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testDBOrderForUser(users[0]);
        assertTrue(rowCount(users[0]) == numAdds-numDel);

        String before = printTrees();
        mOPEDBInterface.getInstance().resetCaches();
        String after = printTrees();
        assertEquals(before, after);
    }

    private void performInsertsWithUser(User u, int max) {
        for(int i = 0; i<max; i++) {
            performInsert(u, rnd.nextInt(), rnd.nextInt());
        }
    }

    @Test
    public void checkMultiUserInsert() {
        resetDB();
        ArrayList<Thread> threads = new ArrayList<>();
        final CyclicBarrier barrier = new CyclicBarrier(NUM_USERS + 1);
        final CyclicBarrier barrier2 = new CyclicBarrier(NUM_USERS + 1);
        for(int cur=0; cur < NUM_USERS; cur ++) {
            threads.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    User u = getRndUser();
                    performInsertsWithUser(u, 100);
                    try {
                        barrier.await();
                        testDBOrderForUser(u);
                        barrier2.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }));
        }
        for(Thread x : threads)
            x.start();
        try {
            barrier.await();
            barrier2.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        String before = printTrees();
        mOPEDBInterface.getInstance().resetCaches();
        String after = printTrees();

        assertEquals(before, after);
    }

    private String printTrees() {
        StringBuilder sb = new StringBuilder();
        for(User u : users) {
            sb.append("ID:"+u.getLocalid()+"::"+u.getMail() +":\n");
            mOPEInteractionTree tree1 = mOPEDBInterface.getInstance().loadTree(u, 1);
            sb.append(tree1.getTreeRepresentation()+"\n");
        }
        return sb.toString();
    }


    private void performInsert(User u, int val1, int val2) {
        //System.out.println("-----------------------------------------------------------");
        List<mOPEJob> jobs = new ArrayList<>();
        CommandDescription des = DBAccessAuthenticate.getCommandDescription(FUNC_1);
        String[] args = new String[] {String.valueOf(val1), "?", String.valueOf(val2), "?"};
        jobs.add(new mOPEJob(mOPEJob.INSERT, String.valueOf(val1),1,1));
        jobs.add(new mOPEJob(mOPEJob.INSERT, String.valueOf(val2),3,2));
        mOPEInteractionState state = new mOPEInteractionState(u,args,des,jobs);
        perfromClientInteractions(state);
        //System.out.println("-----------------------------------------------------------");
    }

    private void performDelete(User u, int value1, int value2) {
        List<mOPEJob> jobs = new ArrayList<>();
        CommandDescription des = DBAccessAuthenticate.getCommandDescription(FUNC_3);
        String[] args = new String[] {String.valueOf(value1), "?", String.valueOf(value2), "?"};
        jobs.add(new mOPEJob(mOPEJob.DELETE, String.valueOf(value1),1,1));
        jobs.add(new mOPEJob(mOPEJob.DELETE, String.valueOf(value2),3,2));
        mOPEInteractionState state = new mOPEInteractionState(u,args,des,jobs);
        perfromClientInteractions(state);
    }

    private mOPEResultMessage performQuery(User u, int val1, int val2) {
        List<mOPEJob> jobs = new ArrayList<>();
        CommandDescription des = DBAccessAuthenticate.getCommandDescription(FUNC_2);
        String[] args = new String[] {"?", "?"};
        jobs.add(new mOPEJob(mOPEJob.QUERY, String.valueOf(val1),0,1));
        jobs.add(new mOPEJob(mOPEJob.QUERY, String.valueOf(val2),1,1));
        mOPEInteractionState state = new mOPEInteractionState(u,args,des,jobs);
        return perfromClientInteractions(state);
    }

    private mOPEResultMessage perfromClientInteractions(mOPEInteractionState state) {
        mOPEResponseMessage msg = state.getFirstStep();
        while (!(msg.isResultMessage())) {
            if(msg.isClientStep()) {
                mOPEClientStepMessage step = (mOPEClientStepMessage) msg;
                Tuple<Integer, Boolean> temp = performCompare(step.getValue(), step.getValuesToCompare());
                msg = state.handleClientStep(temp.a,temp.b);
            } else {
                fail("Non Step Message");
            }
        }
        return (mOPEResultMessage) msg;
    }

    private Tuple<Integer, Boolean> performCompare(String value, String[] nodes) {
        int ival = Integer.valueOf(value);
        int index = 0;
        for(String nodeval : nodes) {
            int cur = Integer.valueOf(nodeval);
            if(ival<=cur) {
                return new Tuple<>(index,ival == cur);
            }
            index++;
        }
        return new Tuple<>(index,false);
    }

    private static final String CHECK1 = "SELECT * FROM TabmOPE WHERE UID = ? ORDER BY ope1 ASC";
    private static final String CHECK2 = "SELECT * FROM TabmOPE WHERE UID = ? ORDER BY ope2 ASC";
    private void testDBOrderForUser(User user) {
        Connection conn = null;
        PreparedStatement check1 = null;
        PreparedStatement check2 = null;
        ResultSet res1 = null;
        ResultSet res2 = null;
        try {
            conn = getConn();
            check1 = conn.prepareStatement(CHECK1);
            check2 = conn.prepareStatement(CHECK2);
            check1.setInt(1, user.getLocalid());
            check2.setInt(1, user.getLocalid());

            res1 = check1.executeQuery();
            res2 = check2.executeQuery();

            checkOrder(res1, "val1");
            checkOrder(res2, "val2");

        } catch (SQLException e) {
            fail(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check1!=null) check1.close();} catch (SQLException e) { e.printStackTrace();}
            try { if(check2!=null) check2.close();} catch (SQLException e) { e.printStackTrace();}
            try { if(res1!=null) res1.close();} catch (SQLException e) { e.printStackTrace();}
            try { if(res2!=null) res2.close();} catch (SQLException e) { e.printStackTrace();}
        }

    }

    private static final String CHECK3 = "SELECT TabmOPE.val1, TabmOPE.ope1 FROM TabmOPE WHERE TabmOPE.val1>=? AND TabmOPE.val1<=? AND TabmOPE.UID=?;";
    private String testQueryValid(User u, int from, int to) {
        Connection conn = null;
        PreparedStatement check3 = null;
        ResultSet res1 = null;
        String res = null;
        try {
            conn = getConn();
            check3 = conn.prepareStatement(CHECK3);
            check3.setInt(1, from);
            check3.setInt(2, to);
            check3.setInt(3, u.getLocalid());

            res1 = check3.executeQuery();
            res = MessageUtil.getRepsonseMessageFromDBResult(res1);
        } catch (SQLException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check3!=null) check3.close();} catch (SQLException e) { e.printStackTrace();}
            try { if(res1!=null) res1.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return res;
    }

    private static final String CHECK4 = "SELECT * FROM TabmOPE WHERE TabmOPE.val1=? AND TabmOPE.val2=? AND TabmOPE.UID=?;";
    private boolean containsRow(User u, int val1, int val2) {
        Connection conn = null;
        PreparedStatement check4 = null;
        ResultSet res1 = null;
        try {
            conn = getConn();
            check4 = conn.prepareStatement(CHECK4);
            check4.setInt(1, val1);
            check4.setInt(2, val2);
            check4.setInt(3, u.getLocalid());

            res1 = check4.executeQuery();

            if(res1.next())
                return true;
        } catch (SQLException e) {
            fail(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check4!=null) check4.close();} catch (SQLException e) { e.printStackTrace();}
            try { if(res1!=null) res1.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return false;
    }

    private static final String CHECK5 = "SELECT Count(TabmOPE.val1) FROM TabmOPE WHERE TabmOPE.UID=?;";
    private int rowCount(User u) {
        Connection conn = null;
        PreparedStatement check4 = null;
        ResultSet res1 = null;
        try {
            conn = getConn();
            check4 = conn.prepareStatement(CHECK5);
            check4.setInt(1, u.getLocalid());
            res1 = check4.executeQuery();

            if(res1.next()) {
                return res1.getInt("Count(TabmOPE.val1)");
            }

        } catch (SQLException e) {
            fail(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check4!=null) check4.close();} catch (SQLException e) { e.printStackTrace();}
            try { if(res1!=null) res1.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return 0;
    }


    private static void checkOrder(ResultSet set, String col) throws SQLException {
        int last = 0, cur = 0;
        if(set.next()) {
            last = set.getInt(col);
        }
        while (set.next()) {
            cur = set.getInt(col);
            assertTrue(last<=cur);
            last = cur;
        }
    }

    private static Connection getConn() throws SQLException {
        new Driver();
        return DriverManager.getConnection("jdbc:mysql://" + DB_HOSTNAME	+ ":" + DB_PORT + "/" + DB_NAME, DB_UNAME, DB_PW);
    }

    private void resetDB() {
        mOPEDBInterface.getInstance().resetCaches();
        Connection conn = null;
        Statement db = null;
        try {
            conn = getConn();
            for(String batch : DB_SCRIPT) {
                try {
                    db = conn.createStatement();
                    db.execute(batch);
                } catch (SQLException e) {
                    //ignore
                } finally {
                    if (db != null) db.close();
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(db!=null) db.close();} catch (SQLException e) { e.printStackTrace();}
        }
    }

    private static final String[] DB_SCRIPT = new String[] {"CREATE  TABLE IF NOT EXISTS `OPETest`.`Users` (\n" +
            "  `id` INT  NOT NULL AUTO_INCREMENT,\n" +
            "  `userid` VARCHAR(100)  NOT NULL,\n" +
            "  `mail` VARCHAR(100) NOT NULL,\n" +
            "  PRIMARY KEY (`id`));\n",
            "DROP TABLE `OPETest`.`TreeIndexes`;",
            "CREATE  TABLE IF NOT EXISTS `OPETest`.`TreeIndexes` (\n" +
            "  `id` INT  NOT NULL AUTO_INCREMENT,\n" +
            "  `table` VARCHAR(100)  NOT NULL,\n" +
            "  `coldet` VARCHAR(100)   NOT NULL,\n" +
            "  `colope` VARCHAR(100)  NOT NULL,\n" +
            "PRIMARY KEY (`id`));",
            "DROP TABLE `OPETest`.`Commands`;",
            "CREATE  TABLE IF NOT EXISTS `OPETest`.`Commands` (\n" +
            "  `id` INT  NOT NULL AUTO_INCREMENT,\n" +
            "  `name` VARCHAR(100)  NOT NULL,\n" +
            "    `typeinfo` TEXT  NOT NULL,\n" +
            "  `description` TEXT NOT NULL,\n" +
            "    `numargs` INT NOT NULL,\n" +
            "    `isquery` BOOL NOT NULL,\n" +
            "PRIMARY KEY (`id`));\n",
            "DROP TABLE `OPETest`.`TabmOPE`;",
            "CREATE  TABLE IF NOT EXISTS `OPETest`.`TabmOPE` (\n" +
            "  `id` INT  NOT NULL AUTO_INCREMENT,\n" +
            "  `UID` INT  NOT NULL,\n" +
            "  `val1` VARCHAR(100) NOT NULL,\n" +
            "  `ope1` BIGINT(8) UNSIGNED NOT NULL,\n" +
            "  `val2` VARCHAR(100) NOT NULL,\n" +
            "  `ope2` BIGINT(8) UNSIGNED NOT NULL,\n" +
            "PRIMARY KEY (`id`));",
            "DROP PROCEDURE storemOPE;",
            "CREATE PROCEDURE storemOPE(IN userid INT, \n" +
            "              IN val1  VARCHAR(100), \n" +
            "              IN ope1  BIGINT(8) UNSIGNED,\n" +
            "              IN val2  VARCHAR(100) ,\n" +
            "              IN ope2  BIGINT(8) UNSIGNED)\n" +
            "BEGIN\n" +
            "  INSERT INTO TabmOPE VALUES (0, userid, val1, ope1, val2, ope2);\n" +
            "END\n;",
            "DROP PROCEDURE rangemOPE;",
            "CREATE PROCEDURE rangemOPE(IN userid INT,\n" +
            "                            IN opeFrom BIGINT(8) UNSIGNED,\n" +
            "                            IN opeTo BIGINT(8) UNSIGNED)\n" +
            "BEGIN\n" +
            "  SELECT TabmOPE.val1, TabmOPE.ope1 FROM TabmOPE WHERE TabmOPE.ope1>=opeFrom AND TabmOPE.ope1<=opeTo AND TabmOPE.UID=userid ORDER BY TabmOPE.id ASC;\n" +
            "END\n;",
            "DROP PROCEDURE deleteOPE;",
            "CREATE PROCEDURE deleteOPE(IN userid INT, \n" +
                    "              IN val1I  VARCHAR(100), \n" +
                    "              IN ope1I  BIGINT(8) UNSIGNED,\n" +
                    "              IN val2I  VARCHAR(100) ,\n" +
                    "              IN ope2I  BIGINT(8) UNSIGNED)\n" +
                    "BEGIN\n" +
                    "  DELETE FROM TabmOPE WHERE TabmOPE.ope2 = ope2I AND TabmOPE.val1 = val1I AND TabmOPE.val2 = val2I AND TabmOPE.ope1 = ope1I AND TabmOPE.UID=userid;\n" +
                    "END\n;",
            "INSERT INTO Commands VALUES(0, 'deleteOPE','u,s,i,s,i','deleteOPE(?,?,?,?,?)',5,FALSE);",
            "INSERT INTO Commands VALUES(0, 'storemOPE','u,s,i,s,i','storemOPE(?,?,?,?,?)',5,FALSE);",
            "INSERT INTO Commands VALUES(0, 'rangemOPE','u,i,i','rangemOPE(?,?,?)',3,TRUE);",
            "INSERT INTO TreeIndexes VALUES(1, 'TabmOPE', 'val1', 'ope1');\n",
            "INSERT INTO TreeIndexes VALUES(2, 'TabmOPE', 'val2', 'ope2');"};


}
