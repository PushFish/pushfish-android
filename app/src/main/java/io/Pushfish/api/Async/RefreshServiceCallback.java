package io.Pushfish.api.Async;

import io.Pushfish.api.PushfishApi.PushfishService;


public interface RefreshServiceCallback {
    void onComplete(PushfishService[] services);
}
