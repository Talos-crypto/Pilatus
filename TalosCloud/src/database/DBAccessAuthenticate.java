package database;

import util.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
 * Implements the MySQL database API for authentication.
 * Assumes that the table Users and Share exist in the database.
 */
public class DBAccessAuthenticate {
    private static final String SHARED_ID = "shared";

    private final static String CHECK_USER_VALID_SQL = "SELECT * FROM Users WHERE Users.userid = ? AND Users.mail = ?;";
    private final static String CHECK_SHARED = "SELECT * FROM Share WHERE Share.fromid = ? AND Share.toid =?;";
    private final static String GET_COMBINED = "SELECT Users.* FROM Share JOIN Users ON Share.combined=Users.id WHERE Share.fromid = ?;";
    private final static String GET_USER = "SELECT * FROM Users WHERE Users.id = ?;";
    private final static String GET_USER_STRING_ID = "SELECT * FROM Users WHERE Users.userid = ?;";
    private final static String GET_SHARED_USERS = "SELECT Users.* FROM Share JOIN Users ON Share.toid=Users.id WHERE Share.fromid = ?;";
    private final static String GET_ACCESS_USERS = "SELECT Users.* FROM Share JOIN Users ON Share.fromid=Users.id WHERE Share.toid = ?;";
    private final static String GET_SHARE_USERS = "SELECT * FROM Users WHERE Users.id NOT IN (SELECT Share.toid as item FROM Share WHERE Share.fromid=? ) AND Users.id NOT IN (SELECT Share.combined as item FROM Share) AND NOT Users.id=?;";
    private final static String REGISTER_USER_SQL = "INSERT INTO Users VALUES(0,?,?,?)";
    private final static String INSERT_SHARE_RELATION = "INSERT INTO Share VALUES(0,?,?,?,?)";
    private final static String GET_COMMAND_DESC_SQL = "SELECT * FROM Commands WHERE Commands.name = ?;";
    private final static String TYPEINFOR_DELIM = ",";

    /**
     * Register a user.
     * Adds user info to the Users table.
     * @param user the user
     * @return true if ok fals if not ok
     */
    public static boolean registerUser(User user) {
        PreparedStatement check = null;
        Connection conn = null;
        if(checkUserValidDB(user)) {
           return false;
        }
        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            check = conn.prepareStatement(REGISTER_USER_SQL);
            check.setString(1, user.getUserid());
            check.setString(2, user.getMail());
            if(user.getPk() == null)
                check.setBytes(3, new byte[] {1});
            else
                check.setBytes(3, user.getPk());
            check.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) {e.printStackTrace();}
            try { if(check!=null) check.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        return true;
    }

    /**
     * Adds and access relation from user A to user B.
     * User A grants B acces to it's data.
     * @param u the user A
     * @param idOther the user id of user B
     * @param token the PRE token from A to B
     * @return a new USer, which represents the shared User from A to B.
     */
    public static User addAccess(User u, int idOther, byte[] token) {
        User other = getUser(idOther);
        if(other == null)
            return null;
        String sharedID = u.getUserid()+other.getUserid();
        User combined = new User(u.getUserid()+other.getUserid(), SHARED_ID, token);
        if(!registerUser(combined))
            return null;
        combined = getUser(sharedID);
        if(combined == null)
            return null;
        if(addShareRelation(u.getLocalid(), idOther, combined.getLocalid())) {
            return combined;
        } else {
            return null;
        }
    }

    private static boolean addShareRelation(int fromID, int toID, int combinedID) {
        PreparedStatement check = null;
        Connection conn = null;
        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            check = conn.prepareStatement(INSERT_SHARE_RELATION);
            check.setInt(1, fromID);
            check.setInt(2, toID);
            check.setInt(3, combinedID);
            check.setInt(4, 0);
            check.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) {e.printStackTrace();}
            try { if(check!=null) check.close(); } catch (SQLException e) {e.printStackTrace();}
        }
        return true;
    }

    /**
     * Returns the User with the given user id from the Users table.
     * @param id the user id
     * @return the user or null if the user does not exist
     */
    public static User getUser(int id) {
        PreparedStatement check = null;
        Connection conn = null;
        User userout = null;
        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            check = conn.prepareStatement(GET_USER);
            check.setInt(1, id);
            ResultSet res = check.executeQuery();

            if (res.next()) {
                userout = new User(res.getInt("id"), res.getString("userid"), res.getString("mail"), res.getBytes("pk"));
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return  null;
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check!=null) check.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return userout;
    }

    /**
     * Returns a list of shared Users
     * Assuming users share from A to B
     * When sharedAccess is true: given user B returns all A users.
     * When sharedAccess is false: given user A returns all B users.
     * @param user either user A or B
     * @param sharedAccess see above
     * @return a list of Users.
     */
    public static List<User> getSharedUsers(User user, boolean sharedAccess) {
        PreparedStatement check = null;
        Connection conn = null;
        ArrayList<User> result = new ArrayList<>();
        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            if(sharedAccess) {
                check = conn.prepareStatement(GET_ACCESS_USERS);
            } else {
                check = conn.prepareStatement(GET_SHARED_USERS);
            }
            check.setInt(1, user.getLocalid());
            ResultSet res = check.executeQuery();

            while (res.next()) {
                result.add(new User(res.getInt("id"), res.getString("userid"), res.getString("mail"), res.getBytes("pk")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return  null;
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check!=null) check.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return result;
    }

    /**
     * Returns all shared Users(A->B) given the user A.
     * For each share relation a new User is created represting the access relation.
     * @param user the user A
     * @return a list of shared Users
     */
    public static List<User> getCombinedUsers(User user) {
        PreparedStatement check = null;
        Connection conn = null;
        ArrayList<User> result = new ArrayList<>();
        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            check = conn.prepareStatement(GET_COMBINED);
            check.setInt(1, user.getLocalid());
            ResultSet res = check.executeQuery();
            while (res.next()) {
                result.add(new User(res.getInt("id"), res.getString("userid"), res.getString("mail"), res.getBytes("pk")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return  null;
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check!=null) check.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return result;
    }

    /**
     * Returns all the Users, which the provided User can share with.
     * I.e. no share relation already exist.
     * @param u a user.
     * @return a list of users.
     */
    public static List<User> getUsers(User u) {
        PreparedStatement check = null;
        Connection conn = null;
        ArrayList<User> result = new ArrayList<>();
        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            check = conn.prepareStatement(GET_SHARE_USERS);
            check.setInt(1, u.getLocalid());
            check.setInt(2, u.getLocalid());
            ResultSet res = check.executeQuery();

            while (res.next()) {
                result.add(new User(res.getInt("id"), res.getString("userid"), res.getString("mail"), res.getBytes("pk")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return  null;
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check!=null) check.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return result;
    }

    /**
     * Get the user with the given id
     * @param id the id
     * @return a user
     */
    public static User getUser(String id) {
        PreparedStatement check = null;
        Connection conn = null;
        User userout = null;
        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            check = conn.prepareStatement(GET_USER_STRING_ID);
            check.setString(1, id);
            ResultSet res = check.executeQuery();

            if (res.next()) {
                userout = new User(res.getInt("id"), res.getString("userid"), res.getString("mail"), res.getBytes("pk"));
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return  null;
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check!=null) check.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return userout;
    }

    private static class UserState {
        public boolean valid;
        public int localID;
        public byte[] pk;

        public UserState(boolean valid, int localID, byte[] pk) {
            this.valid = valid;
            this.localID = localID;
            this.pk = pk;
        }
    }

    /**
     * Checks if the given user is valid.
     * I.e exist in the Users table.
     * @param user the user
     * @return true if valid else false
     */
    public static boolean checkUserValid(User user) {
        return checkUserValidDB(user);
    }


    public static boolean checkUserValidDB(User user) {
        PreparedStatement check = null;
        Connection conn = null;
        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            check = conn.prepareStatement(CHECK_USER_VALID_SQL);
            check.setString(1, user.getUserid());
            check.setString(2, user.getMail());
            ResultSet res = check.executeQuery();

            if (res.next()) {
                user.setLocalid(res.getInt("id"));
                user.setPk(res.getBytes("pk"));
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return  false;
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check!=null) check.close();} catch (SQLException e) { e.printStackTrace();}
        }
    }

    public static class ShareState {
        public int fromID;
        public int toID;
        public int combined;
        public boolean isReplicated;
        private ShareState(int fromID, int toID, int combined, boolean isReplicated) {
            this.fromID = fromID;
            this.toID = toID;
            this.combined = combined;
            this.isReplicated = isReplicated;
        }
    }

    /**
     * Checks if the given share relation from A to B exist.
     * @param fromid the user id of A
     * @param toID the user id of B
     * @return a ShareState if exists else null
     */
    public static ShareState checkShared(int fromid, int toID) {
        PreparedStatement check = null;
        Connection conn = null;
        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            check = conn.prepareStatement(CHECK_SHARED);
            check.setInt(1, fromid);
            check.setInt(2, toID);
            ResultSet res = check.executeQuery();

            if (res.next()) {
                return new ShareState(res.getInt("fromid"), res.getInt("toid"), res.getInt("combined"), res.getInt("replicated")==1);
            } else {
                return null;
            }
        } catch (SQLException e) {
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check!=null) check.close();} catch (SQLException e) { e.printStackTrace();}
        }
        return null;
    }

    /**
     * Returns a command description given a name of a talos command.
     * @param cmd the name of the command
     * @return a COmmandDescription if found else null
     */
    public static CommandDescription getCommandDescription(String cmd) {
        PreparedStatement check = null;
        Connection conn = null;
        boolean isquery;
        String description = null;
        String typeInfo = null;
        String[] typeinforAr = null;
        int numArg = 0;
        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            check = conn.prepareStatement(GET_COMMAND_DESC_SQL);
            check.setString(1, cmd);
            ResultSet res = check.executeQuery();

            if (res.next()) {
                description = res.getString("description");
                typeInfo = res.getString("typeinfo");
                numArg = res.getInt("numargs");
                isquery = res.getBoolean("isquery");
                typeinforAr = typeInfo.split(TYPEINFOR_DELIM);
                if(numArg!=typeinforAr.length) {
                    return null;
                }
                return new CommandDescription(cmd,description,typeinforAr,numArg,isquery);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return  null;
        } finally {
            try { if(conn!=null) conn.close(); } catch (SQLException e) { e.printStackTrace();}
            try { if(check!=null) check.close();} catch (SQLException e) { e.printStackTrace();}
        }
    }

}
