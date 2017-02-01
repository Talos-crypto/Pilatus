package ch.ethz.inf.vs.talosmodule.cryptoalg;

/**
 * This class should wrap the FastEcElGamalCipher to the BC primitives
 */
public class EcElGamalBCWrapper {

    /*private FastECElGamal cipher = null;
    org.spongycastle.jce.spec.ECParameterSpec params = ECNamedCurveTable.getParameterSpec("prime192v1");

    public void init(ECPublicKeyParameters params, ECPrivateKeyParameters privKeyParams) {
        cipher = new FastECElGamal(generatePair(params, privKeyParams), new PRNGImpl());
    }

    private KeyPair generatePair(ECPublicKeyParameters params, ECPrivateKeyParameters privKeyParams) {
        NativeECElGamalCrypto.NativeECELGamalPublicKey pub = new NativeECElGamalCrypto.NativeECELGamalPublicKey(FastECElGamal.CURVE_ID, encodePoint(params.getQ()));
        NativeECElGamalCrypto.NativeECELGamalPrivateKey priv = new NativeECElGamalCrypto.NativeECELGamalPrivateKey(privKeyParams.getD());
        return new KeyPair(pub,priv);
    }

    public ECPair encrypt(BigInteger plaintext) {
        NativeECElGamal.NativeECElgamalCipher ciph = cipher.encrypt(plaintext);
        //ECPoint R = ECPointUtil.decodePoint(params.getCurve(), Hex.decode(ciph.getR()));
        return null;
    }

    public BigInteger decrypt(ECPair ciphertext) {
        return null;
    }

    private static String encodePoint(ECPoint point) {
        return Hex.toHexString(point.getEncoded(true));
    }*/

}
