package utils.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Hash {
	
	public String toHash(String message) throws HashException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = Base64.getEncoder().encode(
				digest.digest(
					message.getBytes(StandardCharsets.UTF_8)
				)
			);
			return new String(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new HashException(e);
		}
	}
	
	public boolean compare(String message, String hash) {
		try {
			return hash.equals(toHash(message));
		} catch (HashException e) {
			return false;
		}
	}

}
