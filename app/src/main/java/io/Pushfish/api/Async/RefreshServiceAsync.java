package io.Pushfish.api.Async;

import android.os.AsyncTask;

import io.Pushfish.api.DatabaseHandler;
import io.Pushfish.api.PushfishApi.PushfishApi;
import io.Pushfish.api.PushfishApi.PushfishService;
import io.Pushfish.api.PushfishApi.PushfishException;


public class RefreshServiceAsync extends AsyncTask<Void, Void, PushfishService[]> {
    private DatabaseHandler db;
    private PushfishApi api;
    private RefreshServiceCallback callback;

    public RefreshServiceAsync(PushfishApi api, DatabaseHandler db) {
        this.api = api;
        this.db = db;
        this.callback = null;
    }

    public void setCallback(RefreshServiceCallback callback) {
        this.callback = callback;
    }


    @Override
    protected PushfishService[] doInBackground(Void... voids) {
        try {
            PushfishService[] subscription = this.api.listSubscriptions();
            db.refreshServices(subscription);
            return subscription;
        } catch (PushfishException e) {
            e.printStackTrace();
        }
        return new PushfishService[0];
    }

    @Override
    protected void onPostExecute(PushfishService[] services) {
        super.onPostExecute(services);
        for (PushfishService service : services)
            new DownloadServiceLogoAsync(api.getContext()).execute(service);
        if (callback != null)
            callback.onComplete(services);
    }
}
