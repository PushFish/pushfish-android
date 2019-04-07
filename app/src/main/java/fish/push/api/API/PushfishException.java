package fish.push.api.API;

public class PushfishException extends Exception {
    public int code;

    public PushfishException(String message, int code) {
        super(message);
        this.code = code;
    }
}
