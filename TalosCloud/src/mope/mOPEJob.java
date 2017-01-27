package mope;

import database.ImOPEDBInterface;
import database.mOPEDBInterface;
import mopetree.mOPEUpdateSummary;
import util.User;

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
 * Created by lukas on 31.12.15.
 */
public class mOPEJob {

    public final static int QUERY = 1;
    public final static int INSERT = 2;
    public final static int UPDATE = 3;
    public final static int DELETE = 4;

    int type;
    private String value;
    private String detHash;

    private int argIndexInCmd;
    private int treeIndex;

    mOPEInteractionTree tree;

    private mOPEEncryptionResult result = null;

    public mOPEJob(int type, String value, String detHash, int argIndexInCmd, int treeIndex) {
        this.type = type;
        this.value = value;
        this.argIndexInCmd = argIndexInCmd;
        this.detHash = detHash;
        this.treeIndex = treeIndex;
    }

    public mOPEJob(int type, String value, int argIndexInCmd, int treeIndex) {
        this.type = type;
        this.value = value;
        this.argIndexInCmd = argIndexInCmd;
        this.detHash = "";
        this.treeIndex = treeIndex;
    }

    public String getValue() {
        return value;
    }

    public mOPEEncryptionResult getResult() throws mOPEException {
        if(hasResult())
            return result;
        throw new mOPEException("No result for this Job");
    }

    public void loadTree(User u, ImOPEDBInterface db) {
        this.tree = db.loadTree(u, treeIndex);
    }

    public boolean hasResult() {
        return result != null;
    }

    public void setResult(mOPEEncryptionResult result) {
        this.result = result;
    }

    public boolean isUpdate() {
        return !(type == QUERY);
    }

    public int getArgIndexInCmd() {
        return argIndexInCmd;
    }

    public String getDetHash() {
        return detHash;
    }
}
