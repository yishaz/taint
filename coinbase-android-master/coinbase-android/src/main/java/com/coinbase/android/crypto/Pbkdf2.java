package com.coinbase.android.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactorySpi;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *  Source obtained from crypto-gwt. Apache 2 License.
 *  https://code.google.com/p/crypto-gwt/source/browse/crypto-gwt/src/main/java/com/googlecode/
 *  cryptogwt/provider/Pbkdf2.java
 */

public class Pbkdf2 extends SecretKeyFactorySpi  {

    private static int CHUNK_SIZE = 200;

    private static class Pbkdf2State {
        final Mac prf;
        final int hLen;
        final int l;
        final int c;
        final byte[] output;
        final byte[] salt;
        byte[] block;
        int currentIteration;
        int i;
        public byte[] u;
        Pbkdf2State(Mac prf, byte[] salt, int c, int bitsToGenerate) {
            this.prf = prf;
            this.hLen = prf.getMacLength();
            this.c = c;
            this.salt = salt;
            int dkLen = bitsToGenerate / 8;
            l = (dkLen + hLen - 1) / hLen;
            output = new byte[dkLen];
            i = 1;
            currentIteration = 0;
        }
    }

    public static class HmacSHA1 extends Pbkdf2 {
        protected HmacSHA1() throws NoSuchAlgorithmException {
            super("PBKDF2WithHmacSHA1", "HmacSHA1");
        }
    }

    private String prfAlgorithm;

    private String algorithm;

    protected Pbkdf2(String algorithm, String prf) throws NoSuchAlgorithmException {
        this.algorithm = algorithm;
        this.prfAlgorithm = prf;
    }

    byte[] generate(char[] password, byte[] salt, int c, int bitsToGenerate) {
        Pbkdf2State state = initState(password, salt, c, bitsToGenerate);
        for (; state.i <= state.l; state.i++) {
            f(state, c);
        }
        return state.output;

    }

    private Pbkdf2State initState(char[] password, byte[] salt, int c,
                                  int bitsToGenerate) {
        Mac prf = null;
        SecretKeySpec key =
                new SecretKeySpec(ByteArrayUtils.toBytes(password), prfAlgorithm);
        try {
            prf = Mac.getInstance(prfAlgorithm);
            prf.init(key);
        } catch (GeneralSecurityException e) {
            assert false : "Unexpected exception: " + e;
        }
        assert prf != null;
        Pbkdf2State state = new Pbkdf2State(prf, salt, c, bitsToGenerate);
        return state;
    }


    private void f(Pbkdf2State state, int chunkSize) {
        Mac prf = state.prf;
        int c = state.c;
        int i = state.i;
        int offset = (i-1) * state.hLen;

        if (isFirstIteration(state)) {
            initF(state, prf, i);
        }

        for(int chunk=0; !isFComplete(state, c) && chunk < chunkSize; chunk++, state.currentIteration++) {
            state.u = prf.doFinal(state.u);
            ByteArrayUtils.xor(state.block, 0, state.u, 0, state.u.length);
        }
        if (isFComplete(state, c)) {
            finalizeF(state, offset);
        }
    }

    private void finalizeF(Pbkdf2State state, int offset) {
        System.arraycopy(state.block, 0, state.output, offset, Math.min(state.block.length,
                state.output.length - offset));
        state.currentIteration = 0;
    }

    private void initF(Pbkdf2State state, Mac prf, int i) {
        state.block = new byte[prf.getMacLength()];
        prf.update(state.salt);
        prf.update(ByteArrayUtils.toBytes(i));
        state.u = prf.doFinal();
        System.arraycopy(state.u, 0, state.block, 0, state.u.length);
        state.currentIteration = 2;
    }

    private boolean isFComplete(Pbkdf2State state, int c) {
        return state.currentIteration > c;
    }

    private boolean isFirstIteration(Pbkdf2State state) {
        return state.currentIteration == 0;
    }

    @Override
    protected SecretKey engineGenerateSecret(KeySpec keySpec)
            throws InvalidKeySpecException {
        PBEKeySpec pbeKeySpec = (PBEKeySpec) keySpec;
        return new PBESecretKeyImpl(this.algorithm,
                pbeKeySpec,
                generate(pbeKeySpec.getPassword(),
                        pbeKeySpec.getSalt(),
                        pbeKeySpec.getIterationCount(),
                        pbeKeySpec.getKeyLength()));
    }

    @Override
    protected KeySpec engineGetKeySpec(SecretKey secretKey, Class aClass) throws InvalidKeySpecException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SecretKey engineTranslateKey(SecretKey secretKey) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }
}
