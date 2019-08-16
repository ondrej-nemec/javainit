package utils.security;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.regex.Pattern;

public class Slugify {
	
	private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

	public static String toSlug(final String string) {
		 String nowhitespace = WHITESPACE.matcher(string).replaceAll("_");
		 String normalized = Normalizer.normalize(nowhitespace, Form.NFD);
		 String slug = NONLATIN.matcher(normalized).replaceAll("");
		 return slug.toLowerCase(Locale.ENGLISH);
	}
	
}
