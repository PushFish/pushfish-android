package fish.push.api.Async;

import fish.push.api.API.PushfishMessage;

import java.util.ArrayList;


public interface ReceivePushCallback {
    void receivePush(ArrayList<PushfishMessage> msg);
}
