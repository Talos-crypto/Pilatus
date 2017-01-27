package database;

import com.google.common.cache.*;
import mope.mOPEException;
import mope.mOPEInteractionTree;
import mope.mOPETreeIndex;
import mope.mOPEUpdateTask;
import mopetree.Tuple;
import util.SystemLogger;
import util.User;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
 * Implements the database mOPE API.
 * Assumes the table TreeIndexes exists in the database for storing information about
 * the mOPE tree's existing in the data tables.
 */
public final class mOPEDBInterface implements ImOPEDBInterface {

    private static final String LOAD_TREE_INDEX = "SELECT * FROM TreeIndexes WHERE id = ?;";

    private static mOPEDBInterface version = null;

    private static final String DB_TREE_INDEX_TABLE = "table";
    private static final String DB_TREE_INDEX_COLDET = "coldet";
    private static final String DB_TREE_INDEX_COLOPE = "colope";

    private mOPEDBInterface() {
    }

    public static synchronized mOPEDBInterface getInstance() {
        if(version==null)
            version = new mOPEDBInterface();
        return version;
    }

    /**
     * Cache for tree indexes
     */
    private LoadingCache<Integer, mOPETreeIndex> treeIndexCache = buildTreeIndexCache();

    private LoadingCache<CachedTreeKey, mOPEInteractionTree> treeCache = buildTreeCache();


    private LoadingCache<Integer, mOPETreeIndex> buildTreeIndexCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(100)
                .build(new CacheLoader<Integer,mOPETreeIndex>() {
                    @Override
                    public mOPETreeIndex load(Integer index) throws Exception {
                        return loadTreeIndexFromDB(index);
                    }
                });
    }

    private LoadingCache<CachedTreeKey, mOPEInteractionTree> buildTreeCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<CachedTreeKey, mOPEInteractionTree>() {
                    @Override
                    public void onRemoval(RemovalNotification<CachedTreeKey, mOPEInteractionTree> removalNotification) {
                        if(removalNotification.wasEvicted()) {
                            mOPEInteractionTree tree = removalNotification.getValue();
                            try {
                                tree.enterForUpdate();
                                tree.invalidate();
                                SystemLogger.log("Tree deleted from Cache");
                                tree.quitUpdate();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                })
                .build(new CacheLoader<CachedTreeKey, mOPEInteractionTree>() {
                    @Override
                    public mOPEInteractionTree load(CachedTreeKey key) throws Exception {
                        return loadTreeFromDB(key.user, key.index);
                    }
                });
    }

    @Override
    public void refreshCachedTree(User user, mOPETreeIndex index) {
        treeCache.invalidate(new CachedTreeKey(user, index));
    }


    private mOPETreeIndex loadTreeIndexFromDB(int index) throws mOPEException {
        Connection conn = null;
        PreparedStatement cStmt = null;
        ResultSet res = null;
        mOPETreeIndex treeIndex = null;

        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            cStmt = conn.prepareStatement(LOAD_TREE_INDEX);
            cStmt.setInt(1, index);
            res = cStmt.executeQuery();
            if(res.next()) {
                treeIndex = new mOPETreeIndex(index,
                        res.getString(DB_TREE_INDEX_TABLE),
                        res.getString(DB_TREE_INDEX_COLOPE),
                        res.getString(DB_TREE_INDEX_COLDET));
            } else {
                throw new mOPEException("Tree with index "+index+" does not exist");
            }
        } catch (SQLException e) {
            throw new mOPEException("Error on DB access ", e);
        } finally {
            close(res, cStmt, conn);
        }
        return treeIndex;
    }

    /**
     * Loads an mOPE tree information from the DB given it's unique id called index
     * @param index the mOPE tree id
     * @return the mOPE tree index information
     * @throws mOPEException
     */
    @Override
    public mOPETreeIndex loadTreeIndex(int index) throws mOPEException {
        try {
            return treeIndexCache.get(index);
        } catch (ExecutionException e) {
            throw new mOPEException("Cache load failed "+index, e);
        }
    }

    public void resetCaches() {
        treeIndexCache.invalidateAll();
        treeCache.invalidateAll();
    }

    private mOPEInteractionTree loadTreeFromDB(User user, mOPETreeIndex index) throws mOPEException {
        Connection conn = null;
        PreparedStatement cStmt = null;
        ResultSet res = null;
        mOPEInteractionTree tree = null;

        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            cStmt = conn.prepareStatement(index.getPreparedStatement());
            cStmt.setInt(1, user.getLocalid());
            res = cStmt.executeQuery();
            tree = new mOPEInteractionTree(user, index, res);
        } catch (SQLException e) {
            throw new mOPEException("DB tree load failed ", e);
        } finally {
            close(res, cStmt, conn);
        }

        return tree;
    }

    /**
     * Loads a mOPE tree from the database, given the user and the tree id.
     * @param user the executing user
     * @param treeID the unique tree id.
     * @return an mOPE tree
     * @throws mOPEException
     */
    @Override
    public mOPEInteractionTree loadTree(User user, int treeID) throws mOPEException {
        mOPEInteractionTree res = null;
        mOPETreeIndex index = loadTreeIndex(treeID);
        try {
            res = treeCache.get(new CachedTreeKey(user, index));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Executes a talos command, which contains mOPE operations on the database.
     * Updates the mOPE tree in the database with the new changed mOPE tree
     * @param user the executing user
     * @param tasks the update tasks
     * @param cmd a descption of the talos command that wants to update the tree
     * @param args the arguments o the task
     * @return
     * @throws mOPEException
     */
    @Override
    public boolean performmOPEUpdate(User user, List<mOPEUpdateTask> tasks, CommandDescription cmd, String[] args) throws mOPEException {
        Connection conn = null;
        List<PreparedStatement> cStmts = new ArrayList<>();
        CallableStatement call = null;

        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            conn.setAutoCommit(false);
            for(mOPEUpdateTask task : tasks) {
                String stmt = task.getTree().getIndex().getPreparedStatementUpdate();
                for(Tuple<BigInteger, String> update : task.getUpdateSummary()) {
                    PreparedStatement pstmt = conn.prepareStatement(stmt);
                    pstmt.setBigDecimal(1, new BigDecimal(update.a));
                    pstmt.setString(2, update.b);
                    pstmt.setInt(3,user.getLocalid());
                    cStmts.add(pstmt);
                    pstmt.executeUpdate();
                }
            }
            call = DBAccessAPI.getCmdPrepareStmt(conn, user, cmd, args);
            call.executeUpdate();
            conn.commit();

        } catch (SQLException e) {
            if(conn!=null)
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            throw new mOPEException("DB tree load failed ", e);
        } finally {
            if(conn!=null)
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            for(PreparedStatement stmt : cStmts)
                close(null, stmt, null);
            close(null, call, null);
            close(null, null, conn);
        }

        return true;
    }

    /**
     * Checks the number of occurences in the data table, with the provided user,
     *  the given mOPE tree index and the encoding.
     * @param user
     * @param index
     * @param encoding
     * @return
     * @throws mOPEException
     */
    @Override
    public int checkNumOccurences(User user, mOPETreeIndex index, BigInteger encoding) throws mOPEException {
        Connection conn = null;
        PreparedStatement cStmt = null;
        ResultSet res = null;
        int numOcc = 0;

        try {
            conn = MySqlConnectionPool.getInstance().getConnection();
            cStmt = conn.prepareStatement(index.getPreparedStatementCheck());
            cStmt.setBigDecimal(1, new BigDecimal(encoding));
            cStmt.setInt(2, user.getLocalid());
            res = cStmt.executeQuery();

            while (res.next())
                numOcc++;
        } catch (SQLException e) {
            throw new mOPEException("NumOccurences failed ", e);
        } finally {
            close(res, cStmt, conn);
        }

        return numOcc;
    }

    private void close(ResultSet rs, Statement ps, Connection conn) {
        if (rs!=null) {
            try {
                rs.close();

            } catch(SQLException e) {
                e.printStackTrace();
            }
        }
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    private final class CachedTreeKey {

        User user;
        mOPETreeIndex index;
        String uniqueID;

        CachedTreeKey(User user, mOPETreeIndex index) {
            this.user = user;
            this.index = index;
            generateUniqueID();
        }

        private void generateUniqueID() {
            StringBuilder sb = new StringBuilder();
            sb.append(user.getLocalid())
                    .append(index.getTableName())
                    .append(index.getColumnDET())
                    .append(index.getColumnOPE());
            this.uniqueID = sb.toString();
        }

        @Override
        public int hashCode() {
            return uniqueID.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CachedTreeKey) {
                CachedTreeKey other = (CachedTreeKey) obj;
                return other.uniqueID.equals(this.uniqueID);
            }
            return false;
        }
    }



}
