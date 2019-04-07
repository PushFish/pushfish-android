package fish.push.api.Async;

import fish.push.api.API.PushfishService;


public interface RefreshServiceCallback {
    void onComplete(PushfishService[] services);
}
