package ahqpck.hse.safety.util;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Utility class for Base62 encoding/decoding.
 * Provides compact, URL-safe encoding for UUIDs and numbers.
 */
@Slf4j
public class Base62Utils {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;

    /**
     * Encode a UUID to Base62 string.
     * @param uuid the UUID to encode
     * @return Base62 encoded string
     */
    public static String encodeUUID(UUID uuid) {
        if (uuid == null) {
            log.warn("UUID is null, returning empty string");
            return "";
        }
        
        // Get the most significant bits and least significant bits
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        
        // Encode as big integer by combining MSB and LSB
        String msbEncoded = encodeLong(msb);
        String lsbEncoded = encodeLong(lsb);
        
        // Combine them with a separator
        String result = msbEncoded + lsbEncoded;
        log.debug("Encoded UUID {} to Base62: {}", uuid, result);
        return result;
    }

    /**
     * Decode a Base62 string back to UUID.
     * @param encoded the Base62 encoded string
     * @return the decoded UUID
     * @throws IllegalArgumentException if the string is not valid Base62 UUID
     */
    public static UUID decodeUUID(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            log.warn("Encoded string is null or empty");
            return null;
        }
        
        try {
            // Split the encoded string (assuming it's two long values combined)
            int mid = (encoded.length() + 1) / 2;
            String msbEncoded = encoded.substring(0, mid);
            String lsbEncoded = encoded.substring(mid);
            
            long msb = decodeLong(msbEncoded);
            long lsb = decodeLong(lsbEncoded);
            
            UUID uuid = new UUID(msb, lsb);
            log.debug("Decoded Base62 {} to UUID: {}", encoded, uuid);
            return uuid;
        } catch (Exception e) {
            log.error("Failed to decode Base62 UUID: {}", encoded, e);
            throw new IllegalArgumentException("Invalid Base62 UUID format", e);
        }
    }

    /**
     * Encode a long value to Base62 string.
     * @param value the value to encode
     * @return Base62 encoded string
     */
    public static String encodeLong(long value) {
        if (value == 0) {
            return "0";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean negative = value < 0;
        value = Math.abs(value);
        
        while (value > 0) {
            sb.insert(0, ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        
        if (negative) {
            sb.insert(0, '-');
        }
        
        return sb.toString();
    }

    /**
     * Decode a Base62 string to a long value.
     * @param encoded the Base62 encoded string
     * @return the decoded long value
     * @throws IllegalArgumentException if the string is not valid Base62
     */
    public static long decodeLong(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return 0;
        }
        
        try {
            boolean negative = encoded.startsWith("-");
            if (negative) {
                encoded = encoded.substring(1);
            }
            
            long value = 0;
            for (char c : encoded.toCharArray()) {
                int digit = ALPHABET.indexOf(c);
                if (digit == -1) {
                    throw new IllegalArgumentException("Invalid character: " + c);
                }
                value = value * BASE + digit;
            }
            
            return negative ? -value : value;
        } catch (Exception e) {
            log.error("Failed to decode Base62 string: {}", encoded, e);
            throw new IllegalArgumentException("Invalid Base62 format", e);
        }
    }

    /**
     * Generate a new random UUID encoded as Base62.
     * @return Base62 encoded UUID
     */
    public static String generateBase62UUID() {
        UUID uuid = UUID.randomUUID();
        String encoded = encodeUUID(uuid);
        log.debug("Generated Base62 UUID: {}", encoded);
        return encoded;
    }

    /**
     * Validate if a string is a valid Base62 UUID.
     * @param encoded the string to validate
     * @return true if valid Base62 UUID format
     */
    public static boolean isValidBase62UUID(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return false;
        }
        
        try {
            decodeUUID(encoded);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
