package ru.infernoproject.core.common.srp;

import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6ServerSession;
import ru.infernoproject.core.common.config.ConfigFile;

import java.math.BigInteger;

public class SRP6Engine {

    private final SRP6CryptoParams cryptoParams;

    private static final BigInteger DEFAULT_N = new BigInteger(
        "ade5b502d60ded86245b81f7b1b1bd23911275aa7ecffc6f66233fe2bf4ec9baa54b30e2eee8af091e75814a43acc384b217cd646347d3b17c16cb3c470026bf8f207e04dff56df12bb273233b6b61828fa14c6c3f2c1650da3adb6b2defd8799a608f058cbb9fd0f782710736262787999575e9906b549dbd0c3900376c64c017644eb8ac1ddc437056587dc56f393324cb9b8fe9c8035de726bdb3615ed5127d2f8a9f4ca3ef274b9e09fdec1f9ee4e9e353ee6d0c369c1d0f3b545955e401e553aa2e4a0d26b87de3f6afa40e4c7383b4b49358815561a8126a8e026c36108e5fcecbdb3cc06ac2ea6bba599e87a8f23c2db176c4822ed5325d15f1fae89d", 16
    );
    private static final BigInteger DEFAULT_g = BigInteger.valueOf(2L);
    private static final String DEFAULT_H = "SHA-1";

    public SRP6Engine(ConfigFile config) {
        cryptoParams = new SRP6CryptoParams(
            config.getBigInt("crypto.N", DEFAULT_N),
            config.getBigInt("crypto.g", DEFAULT_g),
            config.getString("crypto.hash", DEFAULT_H)
        );
    }

    public SRP6ServerSession getSession() {
        return new SRP6ServerSession(cryptoParams);
    }

    public SRP6CryptoParams getCryptoParams() {
        return cryptoParams;
    }
}
