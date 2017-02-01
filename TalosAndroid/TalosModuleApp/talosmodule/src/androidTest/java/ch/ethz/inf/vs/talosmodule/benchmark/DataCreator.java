package ch.ethz.inf.vs.talosmodule.benchmark;

import android.test.InstrumentationTestCase;
import android.util.Log;

import org.spongycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.KeyPair;
import java.util.Random;

import ch.ethz.inf.vs.talosmodule.HttpFileTransfer;
import ch.ethz.inf.vs.talosmodule.OutputFile;
import ch.ethz.inf.vs.talosmodule.cryptoalg.CRTEcElGamal;
import ch.ethz.inf.vs.talosmodule.cryptoalg.FastECElGamal;
import ch.ethz.inf.vs.talosmodule.cryptoalg.NativeECElGamalCrypto;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGImpl;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PRNGRC4Stream;
import ch.ethz.inf.vs.talosmodule.cryptoalg.Paillier;
import ch.ethz.inf.vs.talosmodule.cryptoalg.PaillierPriv;
import ch.ethz.inf.vs.talosmodule.util.CDBUtil;
import ch.ethz.inf.vs.talosmodule.util.ContextHolder;

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

public class DataCreator extends InstrumentationTestCase {

    private static boolean USE_HTTP_TRANSFER = true;

    private static final String DELIM = ";";
    private static final String LB = "\n";

    private Random rand = new Random();

    private static void log(String name) {
        Log.d(DataCreator.class.getName(), name);
    }

    private int getRandomInteger() {
        return rand.nextInt();
    }

    private long getRandomLong() {
        return rand.nextLong();
    }
    private byte[] key = new byte[16];

    public void testDataMain() throws Exception {
        createDataFileNoCRT(false, 1000);
        //createDataFileCRT(false,2,1000);
        //createDataFileCRT(false,3,1000);
        //createDataFileCRT(false, 4, 1000);
        //createDataFileCRT(false, 5, 1000);
        //createDataFilePaillier(1024, 1000);
        //createDataFilePaillier(2048, 1000);
    }

    private void createDataFileCRT(boolean is32bit, int crtPartions, int numValues) throws Exception {
        int partSize = computePartionsSize(is32bit, crtPartions);
        int numBits = is32bit ? 32 : 64;

        CRTEcElGamal gamal = createCipher(crtPartions, partSize);
        StringBuilder log = new StringBuilder();
        CRTEcElGamal.CRTEcElGamalCipher bla = gamal.encrypt(BigInteger.ONE);
        String temp = bla.getCipher();
        int pointSize = (temp.length() / (crtPartions*2));
        log.append("#PointSize: ").append(pointSize).append(LB);
        log.append("INSERT INTO <Table> VALUES ");

        for(int round=1; round<=numValues; round++) {
            log.append("(0x");
            BigInteger cur = getRandValue(is32bit);
            CRTEcElGamal.CRTEcElGamalCipher cipher = gamal.encrypt(cur);
            log.append(cipher.getCipher());
            log.append(LB);
            if (round==numValues)
                log.append(")");
            else
                log.append("),").append(LB);
            log("Round " + round + " passed");
        }

        log.append(";");

        String title = "DataFile"+"secp224k1"+"_NUMBITS_"+numBits+"_CRT_"+crtPartions+".sql";
        if(USE_HTTP_TRANSFER)
            HttpFileTransfer.sendData(title + LB + log.toString());
        else {
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    private void createDataFileNoCRT(boolean is32bit, int numValues) throws Exception {
        int numBits = is32bit ? 32 : 64;

        FastECElGamal gamal = new FastECElGamal(FastECElGamal.generateKeys(new PRNGRC4Stream(key)),new PRNGImpl());

        StringBuilder log = new StringBuilder();
        NativeECElGamalCrypto.NativeECElgamalCipher bla = gamal.encrypt(BigInteger.ONE);
        String temp = bla.getR()+bla.getS();
        int ciphSize = temp.length() / 4;
        log.append("#PointSize: ").append(ciphSize).append(LB);
        log.append("INSERT INTO <Table> VALUES ");

        for(int round=1; round<=numValues; round++) {
            log.append("(0x");
            BigInteger cur = getRandValue(is32bit);
            NativeECElGamalCrypto.NativeECElgamalCipher cipher = gamal.encrypt(cur);
            log.append(cipher.getR());
            log.append(cipher.getS());
            log.append(LB);
            if (round==numValues)
                log.append(")");
            else
                log.append("),").append(LB);
            log("Round " + round + " passed");
        }

        log.append(";");

        String title = "DataFile"+"secp160r1"+"_NUMBITS_"+numBits+"_CRT_"+1+".sql";
        if(USE_HTTP_TRANSFER)
            HttpFileTransfer.sendData(title + LB + log.toString());
        else {
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    private void createDataFilePaillier(int secParam, int numValues) throws Exception {
        KeyPair pair = PaillierPriv.keygen(new PRNGRC4Stream(key), secParam, secParam / 4);
        Paillier pailEnc = new PaillierPriv(pair);
        StringBuilder log = new StringBuilder();
        BigInteger pubKey = pailEnc.hompubkey();
        log.append("#PubKey: ");
        log.append(CDBUtil.bytesToHex(BigIntegers.asUnsignedByteArray(pubKey)));
        log.append(LB);
        log.append("INSERT INTO <Table> VALUES ");

        for(int round=1; round<=numValues; round++) {
            log.append("('");
            BigInteger cur = getRandValue(false);
            BigInteger cipher = pailEnc.encrypt(cur);
            log.append(CDBUtil.bytesToHex(BigIntegers.asUnsignedByteArray(cipher)));
            log.append(LB);
            if (round==numValues)
                log.append("')");
            else
                log.append("'),").append(LB);
            log("Round " + round + " passed");
        }

        log.append(";");

        String title = "DataFile"+"_Paillier"+"_SEC_"+secParam+".sql";
        if(USE_HTTP_TRANSFER)
            HttpFileTransfer.sendData(title + LB + log.toString());
        else {
            OutputFile benchFile = new OutputFile(title);
            benchFile.writeToFile(log.toString());
        }
    }

    private BigInteger getRandValue(boolean is32bit) {
        if(is32bit) {
            return BigInteger.valueOf(getRandomInteger());
        } else {
            return BigInteger.valueOf(getRandomLong());
        }
    }

    private CRTEcElGamal createCipher(int crtPartions, int partSize) {
        return new CRTEcElGamal(CRTEcElGamal.generateKeys(new PRNGRC4Stream(key),partSize,crtPartions),new PRNGImpl());
    }

    private int computePartionsSize(boolean is32bit, int crtPartions) {
        if(is32bit) {
            return (int) ((32.0/((double)crtPartions)) +1);
        } else {
            return (int) ((64.0/((double)crtPartions)) +1);
        }
    }


    @Override
    public void setUp() throws Exception {
        super.setUp();
        ContextHolder.setContext(this.getInstrumentation().getContext());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        FastECElGamal.TableLoader.freeTables();
    }

}
