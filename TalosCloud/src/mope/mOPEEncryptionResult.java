package mope;

import mopetree.mOPEUpdateSummary;

import java.math.BigInteger;

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
 * Represents the result of a mOPE interaction with the client user.
 * The result is a path in the tree encoded as an integer.
 */
public class mOPEEncryptionResult {

    /**
     * The value of the node (normally deterministic encrypted)
     */
    private String value;

    /**
     * Path in the tree
     */
    private BigInteger encoding;

    /**
     * Since the tree should be balanced on each
     * insert mOPE encodings change. An mOPEUpdateTask updates the encodings
     * in the database.
     */
    private mOPEUpdateTask update = null;

    mOPEEncryptionResult(String value, BigInteger encoding) {
        this.value = value;
        this.encoding = encoding;
    }

    mOPEEncryptionResult(String value, BigInteger encoding, mOPEUpdateTask update) {
        this.value = value;
        this.encoding = encoding;
        this.update = update;
    }

    public boolean hasUpdateTask() {
        return update != null;
    }

    public String getValue() {
        return value;
    }

    public BigInteger getEncoding() {
        return encoding;
    }

    public mOPEUpdateTask getUpdate() {
        return update;
    }
}
