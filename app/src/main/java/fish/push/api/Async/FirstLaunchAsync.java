package fish.push.api.Async;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.Date;

import fish.push.api.API.PushfishApi;
import fish.push.api.API.PushfishException;
import fish.push.api.API.PushfishMessage;
import fish.push.api.API.PushfishService;
import fish.push.api.DatabaseHandler;
import fish.push.api.R;
import fish.push.api.SettingsActivity;

public class FirstLaunchAsync extends AsyncTask<Context, Void, Void> {

    @Override
    protected Void doInBackground(Context... params) {
        Context context = params[0];

        try {
            Resources resources = context.getResources();

            PushfishApi api = new PushfishApi(context, SettingsActivity.getRegisterUrl(context));
            DatabaseHandler db = new DatabaseHandler(context);

            PushfishService service;
            String serviceToken = resources.getString(R.string.pushfish_announce_service);
            try {
                service = api.addSubscription(serviceToken);
            } catch (PushfishException e) {
                // If it's telling us that we are already subscribed
                // to that service then just ignore the error
                if (e.code != 4) {
                    throw e;
                } else {
                    service = new PushfishService(serviceToken, "Pushfish Announcements", new Date());
                }
            }

            PushfishMessage message = new PushfishMessage(
                    service, resources.getString(R.string.pushfish_welcome_message),
                    resources.getString(R.string.pushfish_welcome_title), new Date()
            );

            db.addService(service);
            db.addMessage(message);

            context.sendBroadcast(new Intent("PushfishMessageRefresh"));
            new RefreshServiceAsync(api, db).execute();
        } catch (PushfishException e) {
            Toast.makeText(context, "Could not register to welcome service: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        return null;
    }
}
