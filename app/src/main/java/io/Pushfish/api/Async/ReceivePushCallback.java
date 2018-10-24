package io.Pushfish.api.Async;

import io.Pushfish.api.PushfishApi.PushfishMessage;

import java.util.ArrayList;


public interface ReceivePushCallback {
    void receivePush(ArrayList<PushfishMessage> msg);
}
