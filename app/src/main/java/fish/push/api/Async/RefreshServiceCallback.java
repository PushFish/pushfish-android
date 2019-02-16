package fish.push.api.Async;

import fish.push.api.PushfishApi.PushfishService;


public interface RefreshServiceCallback {
    void onComplete(PushfishService[] services);
}
