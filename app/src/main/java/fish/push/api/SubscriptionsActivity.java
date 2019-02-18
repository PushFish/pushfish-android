package fish.push.api;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.Arrays;

import fish.push.api.Async.AddServiceAsync;
import fish.push.api.Async.DeleteServiceAsync;
import fish.push.api.Async.GenericAsyncCallback;
import fish.push.api.Async.RefreshServiceAsync;
import fish.push.api.Async.RefreshServiceCallback;
import fish.push.api.PushfishApi.PushfishApi;
import fish.push.api.PushfishApi.PushfishException;
import fish.push.api.PushfishApi.PushfishService;
import fish.push.api.PushfishApi.PushfishUri;

public class SubscriptionsActivity extends ListActivity {
    private fish.push.api.PushfishApi.PushfishApi api;
    private DatabaseHandler db;
    private SubscriptionsAdapter adapter;
    private BroadcastReceiver receiver;
    private SwipeRefreshLayout refreshLayout;
    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refreshServices();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriptions);
        this.refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        this.refreshLayout.setEnabled(true);
        this.refreshLayout.setOnRefreshListener(refreshListener);

        this.api = new PushfishApi(getApplicationContext(), fish.push.api.SettingsActivity.getRegisterUrl(this));
        this.db = new DatabaseHandler(getApplicationContext());

        adapter = new fish.push.api.SubscriptionsAdapter(this);
        setListAdapter(adapter);

        adapter.upDateEntries(new ArrayList<PushfishService>(Arrays.asList(db.getAllServices())));
        registerForContextMenu(findViewById(android.R.id.list));

        Uri pushfishUri = getIntent().getData();
        if (pushfishUri != null) {
            try {
                String host = pushfishUri.getHost();
                Log.d("PushfishService", "Host: " + host);
                parseTokenOrUri(host);
            } catch (PushfishException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (NullPointerException ignore) {
            }
        }

        if (adapter.getCount() == 0 && !this.refreshLayout.isRefreshing()) {
            refreshServices();
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.upDateEntries(new ArrayList<PushfishService>(Arrays.asList(db.getAllServices())));
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(receiver, new IntentFilter("PushfishIconDownloaded"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.subscriptions_context_menu, menu);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        openContextMenu(v);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        PushfishService service = (PushfishService) adapter.getItem(Math.round(info.id));
        switch (item.getItemId()) {
            case R.id.action_copy_token:
                MiscUtil.WriteToClipboard(service.getToken(), "Pushfish Token", this);
                Toast.makeText(this, "Copied token to clipboard", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_delete:
                DeleteServiceAsync deleteServiceAsync = new DeleteServiceAsync(api, db);
                deleteServiceAsync.setCallback(new GenericAsyncCallback() {
                    @Override
                    public void onComplete(Object... objects) {
                        adapter.upDateEntries(new ArrayList<PushfishService>(Arrays.asList(db.getAllServices())));
                    }
                });
                deleteServiceAsync.execute(service);
                return true;
            case R.id.action_clear_notifications:
                db.cleanService(service);
                sendBroadcast(new Intent("PushfishMessageRefresh"));
                return true;
            case R.id.action_share:
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, "pjet://" + service.getToken() + "/");
                startActivity(Intent.createChooser(share, "Share service"));
                return true;
            case R.id.action_show_qr:
                try {
                    BitMatrix matrix = new QRCodeWriter().encode(service.getToken(), BarcodeFormat.QR_CODE, 1000, 1000);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    View view = getLayoutInflater().inflate(R.layout.dialog_display_qr, null);

                    ImageView image = (ImageView) view.findViewById(R.id.image_qr);
                    image.setImageBitmap(MiscUtil.matrixToBitmap(matrix));

                    builder.setView(view).show();
                } catch (WriterException e) {
                    Toast.makeText(getApplicationContext(), "Couldn't generate qr code", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.subscriptions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.actions_new:
                final String[] items = new String[]{
                        "Scan QR", "Enter token"
                };
                final Activity thisActivity = this;
                new AlertDialog.Builder(this)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (i == 0) {
                                    new IntentIntegrator(thisActivity).initiateScan(IntentIntegrator.QR_CODE_TYPES);
                                } if (i == 1) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                                    builder.setTitle("Public token");
                                    final EditText input = new EditText(thisActivity);

                                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                                    builder.setView(input);
                                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                parseTokenOrUri(input.getText().toString());
                                            } catch (PushfishException e) {
                                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                            } catch (NullPointerException ignore) {
                                            }

                                        }
                                    });
                                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    });

                                    builder.show();
                                }

                            }
                        })
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void parseTokenOrUri(String token) throws PushfishException {
        token = token.trim();
        if (PushfishUri.isValidUri(token)) {
            try {
                token = PushfishUri.tokenFromUri(token);
            } catch (PushfishException ignore) {
            }
        }
        if (!PushfishUri.isValidToken(token))
            throw new PushfishException("Invalid MQTT topic.", 2);

        AddServiceAsync addServiceAsync = new AddServiceAsync(api, db, adapter);
        addServiceAsync.execute(token);
    }

    // Used for parsing the QR code scanner result
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        try {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (scanResult == null)
                return;
            parseTokenOrUri(scanResult.getContents().trim());
        } catch (PushfishException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (NullPointerException ignore) {
        }
    }

    private void refreshServices() {
        RefreshServiceCallback callback = new RefreshServiceCallback() {
            @Override
            public void onComplete(PushfishService[] services) {
                adapter.upDateEntries(new ArrayList<PushfishService>(Arrays.asList(services)));
                refreshLayout.setRefreshing(false);
            }
        };
        RefreshServiceAsync refresh = new RefreshServiceAsync(api, db);
        refresh.setCallback(callback);
        refreshLayout.setRefreshing(true);
        refresh.execute();
    }
}
