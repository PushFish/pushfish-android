package fish.push.api.Async;


import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import fish.push.api.DatabaseHandler;
import fish.push.api.API.PushfishApi;
import fish.push.api.API.PushfishService;
import fish.push.api.API.PushfishException;
import fish.push.api.SubscriptionsAdapter;

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
