package io.remonode.notifytest;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * RFC 6238 TOTP — the same code an authenticator app shows, and the same code remonode's "2FA Code (TOTP)"
 * node generates: HMAC-SHA1, 6 digits, 30-second steps. Those are the node's defaults, so a workflow pasted
 * with just the setup key produces codes this app accepts.
 */
final class Totp {

    private static final int DIGITS = 6;
    private static final long PERIOD_SECONDS = 30;
    // One step either side, as real services allow: the code is generated on remonode's worker and typed a
    // second or two later on a phone whose clock is its own, and a code that expires between the two is the
    // run failing on timing rather than on login.
    private static final int WINDOW = 1;

    private Totp() {}

    static boolean verify(String base32Secret, String code, long nowMillis) {
        String digits = code == null ? "" : code.replaceAll("\\s", "");
        if (!digits.matches("\\d{" + DIGITS + "}")) return false;
        byte[] key = base32Decode(base32Secret);
        long step = nowMillis / 1000 / PERIOD_SECONDS;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            if (generate(key, step + i).equals(digits)) return true;
        }
        return false;
    }

    private static String generate(byte[] key, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] h = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int o = h[h.length - 1] & 0x0f;
            int bin = ((h[o] & 0x7f) << 24) | ((h[o + 1] & 0xff) << 16) | ((h[o + 2] & 0xff) << 8) | (h[o + 3] & 0xff);
            int otp = bin % (int) Math.pow(10, DIGITS);
            return String.format(Locale.US, "%0" + DIGITS + "d", otp);
        } catch (GeneralSecurityException e) {
            // HmacSHA1 is guaranteed on every Android version; if it is somehow missing, no code verifies.
            return "";
        }
    }

    private static byte[] base32Decode(String s) {
        String in = s.toUpperCase(Locale.US).replaceAll("[\\s=]", "");
        ByteBuffer out = ByteBuffer.allocate(in.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char c : in.toCharArray()) {
            int v = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(c);
            if (v < 0) continue;
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                out.put((byte) (buffer >> (bits - 8)));
                bits -= 8;
            }
        }
        byte[] b = new byte[out.position()];
        out.flip();
        out.get(b);
        return b;
    }
}
