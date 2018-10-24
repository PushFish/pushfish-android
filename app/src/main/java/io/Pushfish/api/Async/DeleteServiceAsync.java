package io.Pushfish.api.Async;


import android.os.AsyncTask;

import io.Pushfish.api.DatabaseHandler;
import io.Pushfish.api.PushfishApi.PushfishApi;
import io.Pushfish.api.PushfishApi.PushfishException;
import io.Pushfish.api.PushfishApi.PushfishService;

public class DeleteServiceAsync extends AsyncTask<PushfishService, Void, Void> {
    private DatabaseHandler db;
    private PushfishApi api;
    private io.Pushfish.api.Async.GenericAsyncCallback callback;

    public DeleteServiceAsync(PushfishApi api, DatabaseHandler db) {
        this.api = api;
        this.db = db;
        this.callback = null;
    }

    public void setCallback(GenericAsyncCallback callback) {
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(PushfishService... services) {
        for (PushfishService service : services) {
            try {
                api.deleteSubscription(service.getToken());
                db.removeService(service);
            } catch (PushfishException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (callback != null)
            callback.onComplete();
    }
}
