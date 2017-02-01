package ch.ethz.inf.vs.talosmodule.cryptoalg;

import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

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
 * Implementation of the public part of the EC-ELGamal Crypto-System.
 * ECC operations from SpongyCastle. (bad performance)
 */
public class ECELGamal {

    private ECCurve curve;
    private ECPoint generator;
    private ECPoint Y;

    private IPRNG rand;


    public ECELGamal(ELGamalPublicKey pub, IPRNG rand) {
        this.curve = pub.getDomain().getCurve();
        this.generator = pub.getDomain().getG();
        this.Y = pub.getY();
        this.rand = rand;
    }

    public static KeyPair generateKeys(IPRNG rand, String curveName) {
        X9ECParameters params = SECNamedCurves.getByName(curveName);
        ECDomainParameters ecp = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());

        BigInteger x = rand.getRandMod(ecp.getCurve().getField().getCharacteristic());

        ECPoint Y = ecp.getG().multiply(x);

        return new KeyPair(new ELGamalPublicKey(ecp, Y), new ELGamalPrivateKey(x));
    }

    public static KeyPair generateKeys(IPRNG rand) {
        return generateKeys(rand, "secp192k1");
    }

    public ELGamalCipher encrypt(BigInteger num) {
        ECPoint R, S, M;
        BigInteger k;

        k = rand.getRandMod(curve.getOrder());
        M = mappingFunc(num);
        R = generator.multiply(k);
        S = M.add(Y.multiply(k));
        return new ELGamalCipher(R, S);
    }

    protected ECPoint mappingFunc(BigInteger message) {
        return generator.multiply(message);
    }

    protected BigInteger invMappingFunc(ECPoint y, long maxIt) {
        //TODO
        long res = 0;
        boolean found = false;
        for (long iter = 1; iter < maxIt / 2; iter++) {
            if (generator.multiply(BigInteger.valueOf(iter)).equals(y)) {
                res = iter;
                break;
            } else if ((generator.multiply(BigInteger.valueOf(-iter)).equals(y))) {
                res = -iter;
                break;
            }
        }
        assert (!found);
        return BigInteger.valueOf(res);
    }

    public static class ELGamalCipher {
        public final ECPoint R;
        public final ECPoint S;

        public ELGamalCipher(ECPoint R, ECPoint S) {
            this.R = R;
            this.S = S;
        }

        public ELGamalCipher add(ELGamalCipher other) {
            return new ELGamalCipher(this.R.add(other.R), this.S.add(other.S));
        }

        public byte[] encode() {
            return null;
        }
    }

    public static class ELGamalPublicKey implements PublicKey {

        private final ECDomainParameters domain;

        public final ECPoint Y;

        private ELGamalPublicKey(ECDomainParameters domain, ECPoint Y) {
            this.domain = domain;
            this.Y = Y;
        }

        public ECDomainParameters getDomain() {
            return domain;
        }

        public ECPoint getY() {
            return Y;
        }

        @Override
        public String getAlgorithm() {
            return "EC-ElGamal";
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public byte[] getEncoded() {
            return new byte[0];
        }
    }

    public static class ELGamalPrivateKey implements PrivateKey {

        private final BigInteger x;

        public ELGamalPrivateKey(BigInteger x) {
            this.x = x;
        }

        public BigInteger getX() {
            return x;
        }

        @Override
        public String getAlgorithm() {
            return "EC-ELGamal";
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public byte[] getEncoded() {
            return new byte[0];
        }
    }


}
