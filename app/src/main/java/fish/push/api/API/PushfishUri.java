package fish.push.api.API;

import java.util.regex.Pattern;

public class PushfishUri {
    public static final String uriProtocol = "pjet://";
    private static final String reToken = "[a-zA-Z0-9]{4}-[a-zA-Z0-9]{6}-[a-zA-Z0-9]{12}-[a-zA-Z0-9]{5}-[a-zA-Z0-9]{9}";
    private static final String reUri = String.format("^(%s)(%s)($|[\\/?]([^\\s]+)?$)", uriProtocol.replace("/", "\\/"), reToken);

    public static boolean isValidToken(String token) {
        return token.matches("^" + reToken + "$");
    }

    public static boolean isValidUri(String uri) {
        return uri.matches(reUri);
    }

    public static String tokenFromUri(String uri) throws PushfishException {
        if (!isValidToken(uri))
            // Error #2 is the invalid service token error
            throw new PushfishException("Invalid MQTT URI.", 2);

        return Pattern.compile(reUri).matcher(uri).group(2);
    }
}
