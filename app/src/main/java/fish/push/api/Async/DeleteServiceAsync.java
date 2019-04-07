package fish.push.api.Async;


import android.os.AsyncTask;

import fish.push.api.DatabaseHandler;
import fish.push.api.API.PushfishApi;
import fish.push.api.API.PushfishException;
import fish.push.api.API.PushfishService;

public class DeleteServiceAsync extends AsyncTask<PushfishService, Void, Void> {
    private DatabaseHandler db;
    private PushfishApi api;
    private fish.push.api.Async.GenericAsyncCallback callback;

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
