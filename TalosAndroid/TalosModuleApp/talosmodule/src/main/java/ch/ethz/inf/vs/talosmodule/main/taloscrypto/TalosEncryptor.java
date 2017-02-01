package ch.ethz.inf.vs.talosmodule.main.taloscrypto;

import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.TalosColumn;
import ch.ethz.inf.vs.talosmodule.main.values.TalosCipher;
import ch.ethz.inf.vs.talosmodule.main.values.TalosValue;
import ch.ethz.inf.vs.talosmodule.main.mOPEOperationType;

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

public interface TalosEncryptor {

    /**
     * Encrypts a TalosValue randomly, given the column in the database.
     * The key is derived from the column and the master secret.
     * @param val the value to encrypt
     * @param column the corresponding column of the value
     * @return returns the encrypted ciphertext
     * @throws TalosModuleException if encryption fails
     */
    public TalosCipher encryptRND(TalosValue val, TalosColumn column) throws TalosModuleException;

    /**
     * Encrypts a TalosValue deterministically, given the column in the database.
     * The key is derived from the column and the master secret.
     * @param val the value to encrypt
     * @param column the corresponding column of the value
     * @return returns the encrypted ciphertext
     * @throws TalosModuleException if encryption fails
     */
    public TalosCipher encryptDET(TalosValue val, TalosColumn column) throws TalosModuleException;

    /**
     * Encrypts a TalosValue homomorphically, given the column in the database.
     * The key is derived from the column and the master secret.
     * @param val the value to encrypt
     * @param column the corresponding column of the value
     * @return returns the encrypted ciphertext
     * @throws TalosModuleException if encryption fails
     */
    public TalosCipher encryptHOM(TalosValue val, TalosColumn column) throws TalosModuleException;

    /**
     * Encrypts a TalosValue homomorphically wit pre, given the column in the database.
     * Allows sharing
     * @param val the value to encrypt
     * @param column the corresponding column of the value
     * @return returns the encrypted ciphertext
     * @throws TalosModuleException if encryption fails
     */
    public TalosCipher encryptHOMPRE(TalosValue val, TalosColumn column) throws TalosModuleException;

    /**
     * Encrypts a TalosValue homomorphically, given the column in the database.
     * The key is derived from the column and the master secret.
     * @param val the value to encrypt
     * @param detColumn the corresponding deterministic encrypted column of the value
     * @param indexOfTree the index of the tree (id stored in the meta-tables of the database)
     * @param type the mOPE operation type (insert, delete, update, query)
     * @return returns the encrypted ciphertext
     * @throws TalosModuleException
     */
    public TalosCipher encryptOPE(TalosValue val, TalosColumn detColumn, int indexOfTree, mOPEOperationType type) throws TalosModuleException;

}
