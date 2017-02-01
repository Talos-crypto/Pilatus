package ch.ethz.inf.vs.talosmodule.main;

import android.content.Context;

import java.util.ArrayList;

import ch.ethz.inf.vs.talosmodule.communication.TalosServer;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.taloscrypto.TalosDecryptor;
import ch.ethz.inf.vs.talosmodule.main.taloscrypto.TalosEncryptor;
import ch.ethz.inf.vs.talosmodule.main.values.TalosCipher;
import ch.ethz.inf.vs.talosmodule.main.values.TalosDataType;
import ch.ethz.inf.vs.talosmodule.main.values.TalosValue;

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

public class ExampleTalosModule {

    private TalosModuleFactory factory;

    private TalosServer server;

    public ExampleTalosModule(Context con, String ip, int port) {
        this.factory = new TalosModuleFactory(con);
        this.server = factory.createServer(ip, port, "ExampleResource");
    }

    public ExampleTalosModule(Context con, String ip, int port, int certResource) {
        this.factory = new TalosModuleFactory(con);
        this.server = factory.createServer(ip, port, certResource, "ExampleResource");
    }

    public boolean insertTest(User user, int a, long b, String c) throws TalosModuleException{
        TalosColumn aColRND = new TalosColumn("ColumnA_RND","TokenARND");
        TalosColumn aColHOM = new TalosColumn("ColumnA_HOM","TokenAHOM");
        TalosColumn bColDET = new TalosColumn("ColumnB_DET","TokenBDET");
        TalosColumn cColRND = new TalosColumn("ColumnC_RND","TokenCRND");

        TalosValue valA = TalosValue.createTalosValue(a);
        TalosValue valB = TalosValue.createTalosValue(b);
        TalosValue valC = TalosValue.createTalosValue(c);

        TalosCipher[] ciphers = new TalosCipher[4];

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = encryptor.encryptRND(valA, aColRND);
        ciphers[1] = encryptor.encryptHOM(valA, aColHOM);
        ciphers[2] = encryptor.encryptDET(valB, bColDET);
        ciphers[3] = encryptor.encryptRND(valC, cColRND);

        TalosCommand cmd = new TalosCommand("insertTest", ciphers);
        server.execute(user, cmd);

        return true;
    }

    public boolean insertOPE(User user, int a) throws TalosModuleException{
        TalosColumn valDET = new TalosColumn("VAL_DET","VAL_DET");

        TalosValue valA = TalosValue.createTalosValue(a);

        TalosCipher[] ciphers = new TalosCipher[2];

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = encryptor.encryptDET(valA, valDET);
        ciphers[1] = encryptor.encryptOPE(valA, valDET, 1, mOPEOperationType.INSERT);

        TalosCommand cmd = new TalosCommand("insertOPE", ciphers);
        server.execute(user, cmd);
        return true;
    }

    public TalosResult queryOPE(User user, int from, int to) throws TalosModuleException{
        TalosColumn valDET = new TalosColumn("VAL_DET","VAL_DET");

        TalosValue fromVal = TalosValue.createTalosValue(from);
        TalosValue toVal = TalosValue.createTalosValue(to);

        TalosCipher[] ciphers = new TalosCipher[2];

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = encryptor.encryptOPE(fromVal, valDET, 1, mOPEOperationType.QUERY);
        ciphers[1] = encryptor.encryptOPE(toVal, valDET, 1, mOPEOperationType.QUERY);

        TalosCommand cmd = new TalosCommand("queryOPE", ciphers);
        TalosCipherResultData encResult = server.execute(user, cmd);

        TalosDecryptor decryptor = factory.createTalosDecryptor();

        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resA = decryptor.decryptDET(row.get("VAL_DET"), TalosDataType.INT_32, valDET);
            resrow.put("VAL", resA);
            rows.add(resrow);
        }

        return new TalosResultSet(rows);
    }

    public TalosResult searchTest(User user, int bSearch) throws TalosModuleException{
        TalosColumn aColRND = new TalosColumn("ColumnA_RND","TokenARND");
        TalosColumn bColDET = new TalosColumn("ColumnB_DET","TokenBDET");
        TalosColumn cColRND = new TalosColumn("ColumnC_RND","TokenCRND");

        TalosValue valB = TalosValue.createTalosValue(bSearch);

        TalosCipher[] ciphers = new TalosCipher[1];

        TalosEncryptor encryptor = factory.createTalosEncryptor();
        ciphers[0] = encryptor.encryptDET(valB, bColDET);

        TalosCommand cmd = new TalosCommand("searchTest", ciphers);
        TalosCipherResultData encResult = server.execute(user, cmd);

        TalosDecryptor decryptor = factory.createTalosDecryptor();

        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resA = decryptor.decryptRND(row.get("ColumnA_RND"), TalosDataType.INT_32, aColRND);
            TalosValue resB = decryptor.decryptDET(row.get("ColumnB_DET"), TalosDataType.INT_64, bColDET);
            TalosValue resC = decryptor.decryptRND(row.get("ColumnC_RND"), TalosDataType.STR, cColRND);
            resrow.put("ColumnA", resA);
            resrow.put("ColumnB", resB);
            resrow.put("ColumnC", resC);
            rows.add(resrow);
        }

        return new TalosResultSet(rows);
    }

    public TalosResult computeSum(User user) throws TalosModuleException{
        TalosColumn aColHOM = new TalosColumn("ColumnA_HOM","TokenAHOM");

        TalosCipher[] ciphers = new TalosCipher[0];
        TalosCommand cmd = new TalosCommand("computeSum", ciphers);

        TalosCipherResultData encResult = server.execute(user, cmd);

        TalosDecryptor decryptor = factory.createTalosDecryptor();

        ArrayList<TalosResultSetRow> rows = new ArrayList<>();
        for(TalosCipherResultRow row : encResult) {
            TalosResultSetRow resrow = new TalosResultSetRow();
            TalosValue resA = decryptor.decryptHOM(row.get("ECElGamal_Agr(TestTable.ColumnA_HOM)"), TalosDataType.INT_32, aColHOM);
            resrow.put("SUM(ColumnA)", resA);
            rows.add(resrow);
        }
        return new TalosResultSet(rows);
    }



}
