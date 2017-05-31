package ru.infernoproject.tests.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class CryptoHelper {

    private final byte[] serverSalt;
    private static final Random random = new Random();

    public CryptoHelper(byte[] serverSalt) {
        this.serverSalt = serverSalt;
    }

    public byte[] generateSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        return salt;
    }

    public byte[] calculateVerifier(String login, String password, byte[] clientSalt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            md.update(serverSalt);
            md.update(String.format("%s:%s", login, password).getBytes());
            md.update(clientSalt);

            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] calculateChallenge(String login, String password, byte[] vector, byte[] clientSalt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            md.update(vector);
            md.update(calculateVerifier(login, password, clientSalt));
            md.update(serverSalt);

            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
