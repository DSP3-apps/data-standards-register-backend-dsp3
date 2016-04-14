package uk.gov.mint;

import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

//TODO: Copied from alpha register to generate hash, We need to figure out what hashing technique is required
public class Digest {
    public static String shasum(String raw) {
        try {
            String head = "blob " + raw.getBytes(StandardCharsets.UTF_8).length + "\0";

            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update((head + raw).getBytes(StandardCharsets.UTF_8));
            return new String(Hex.encodeHex(md.digest()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
