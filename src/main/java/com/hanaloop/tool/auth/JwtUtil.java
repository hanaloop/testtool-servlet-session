package com.hanaloop.tool.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JwtUtil {
    private JwtUtil() {}

    private static String b64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String generateElToken(String userId, long iat, long exp, String aud, String iss, String secret) throws Exception {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = new StringBuilder(128)
                .append('{')
                .append("\"sub\":\"").append(jsonEscape(userId)).append('\"')
                .append(',').append("\"iat\":").append(iat)
                .append(',').append("\"exp\":").append(exp)
                .append(',').append("\"aud\":\"").append(jsonEscape(aud)).append('\"')
                .append(',').append("\"iss\":\"").append(jsonEscape(iss)).append('\"')
                .append('}')
                .toString();

        String header = b64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = b64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        String signature = b64Url(sig);

        return signingInput + "." + signature;
    }

    public static class Claims {
        public String sub;
        public long iat;
        public long exp;
        public String aud;
        public String iss;
    }

    public static Claims verifyAndDecode(String token, String secret) throws Exception {
        if (token == null) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;
        String headerB64 = parts[0];
        String payloadB64 = parts[1];
        String sigB64 = parts[2];

        // Verify signature
        String signingInput = headerB64 + "." + payloadB64;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expectedSig = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        String expectedB64 = b64Url(expectedSig);
        if (!constantTimeEquals(expectedB64, sigB64)) {
            return null;
        }

        // Decode payload
        byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
        String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
        Claims c = new Claims();
        c.sub = extractString(payloadJson, "sub");
        c.aud = extractString(payloadJson, "aud");
        c.iss = extractString(payloadJson, "iss");
        c.iat = extractLong(payloadJson, "iat");
        c.exp = extractLong(payloadJson, "exp");
        return c;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int res = 0;
        for (int i = 0; i < a.length(); i++) {
            res |= a.charAt(i) ^ b.charAt(i);
        }
        return res == 0;
    }

    private static String extractString(String json, String key) {
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        String value = json.substring(q1 + 1, q2);
        // Unescape minimal set
        return value.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\");
    }

    private static long extractLong(String json, String key) {
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return 0L;
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return 0L;
        int start = colon + 1;
        // skip spaces
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)))) end++;
        if (end <= start) return 0L;
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
