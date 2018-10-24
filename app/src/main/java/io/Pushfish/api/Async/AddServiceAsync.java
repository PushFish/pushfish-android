package io.Pushfish.api.Async;


import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import io.Pushfish.api.DatabaseHandler;
import io.Pushfish.api.PushfishApi.PushfishApi;
import io.Pushfish.api.PushfishApi.PushfishService;
import io.Pushfish.api.PushfishApi.PushfishException;
import io.Pushfish.api.SubscriptionsAdapter;

public class AddServiceAsync extends AsyncTask<String, Void, PushfishService> {
    private PushfishApi api;
    private SubscriptionsAdapter adapter;
    private DatabaseHandler db;
    private PushfishException exception;

    public AddServiceAsync(PushfishApi api, DatabaseHandler db, SubscriptionsAdapter adapter) {
        this.api = api;
        this.adapter = adapter;
        this.db = db;
    }

    @Override
    protected PushfishService doInBackground(String... strings) {
        try {
            return api.addSubscription(strings[0]);
        } catch (PushfishException e) {
            Log.e("ServAsync", e.getMessage());
            exception = e;
            return null;
        }
    }

    @Override
    protected void onPostExecute(PushfishService service) {
        if (service != null) {
            new DownloadServiceLogoAsync(api.getContext()).execute(service);
            db.addService(service);
            adapter.addEntry(service);
        } else {
            String message = exception.getMessage();
            Toast.makeText(api.getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
