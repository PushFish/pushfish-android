package fish.push.api;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Arrays;

import fish.push.api.Async.FirstLaunchAsync;
import fish.push.api.Async.ReceivePushAsync;
import fish.push.api.Async.ReceivePushCallback;
import fish.push.api.PushfishApi.PushfishApi;
import fish.push.api.PushfishApi.PushfishMessage;

public class PushListActivity extends ListActivity {
    private PushfishApi api;
    private DatabaseHandler db;
    private PushListAdapter adapter;
    private BroadcastReceiver receiver;
    private SwipeRefreshLayout refreshLayout;
    private MqttAsyncClient mqttClient;
    private MqttConnectOptions mqttOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_list);
        this.api = new PushfishApi(getApplicationContext(), SettingsActivity.getRegisterUrl(this));
        this.db = new DatabaseHandler(getApplicationContext());
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isFirstLaunch = preferences.getBoolean("first_launch", true);
        if (isFirstLaunch) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("first_launch", false);
            editor.apply();
            new FirstLaunchAsync().execute(getApplicationContext());
        }
        configureRefreshListener();
        loadListView();
        configureMessaging();
        configureBroadcastReceiver();
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        if (adapter.getSelected() == position) {
            adapter.clearSelected();
        } else {
            adapter.setSelected(position);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(receiver, new IntentFilter("PushfishMessageRefresh"));
        registerReceiver(receiver, new IntentFilter("PushfishIconDownloaded"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePushList();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void updatePushList() {
        adapter.upDateEntries(new ArrayList<>(Arrays.asList(db.getAllMessages())));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.push_list, menu);
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.action_subscriptions:
                intent = new Intent(getApplicationContext(), SubscriptionsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_settings:
                intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void configureRefreshListener() {
        this.refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        this.refreshLayout.setEnabled(true);
        this.refreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        ReceivePushAsync receivePushAsync = new ReceivePushAsync(api, adapter);
                        receivePushAsync.setCallBack(new ReceivePushCallback() {
                            @Override
                            public void receivePush(ArrayList<PushfishMessage> messages) {
                                refreshLayout.setRefreshing(false);
                            }
                        });
                        refreshLayout.setRefreshing(true);
                        receivePushAsync.execute();
                    }
                });
    }

    private void loadListView() {
        adapter = new PushListAdapter(this);
        setListAdapter(adapter);
        this.getListView().setLongClickable(true);
        this.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                PushfishMessage message = (PushfishMessage) adapter.getItem(position);

                MiscUtil.WriteToClipboard(message.getMessage(), "Pushfish message", getApplicationContext());
                Toast.makeText(getApplicationContext(), "Copied message to clipboard", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void configureMessaging() {
        try {
            constructConnectionOptions();
            configureMqttClient();
            subscribeToMqttTopic();
            mqttClient.connect(mqttOptions);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void configureMqttClient() {
        try {
            mqttClient = new MqttAsyncClient("tcp://test-fish.push.api.push.fish:1883", "android", new MemoryPersistence());
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.i(PushListActivity.class.getName(), "MQTT Connection successful!");
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.w(PushListActivity.class.getName(), "MQTT Connection was lost!");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handleMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribeToMqttTopic() {
        try {
            mqttClient.subscribe("myTopic", 2, new IMqttMessageListener() { // TODO: @paynemiller once API team defines topic, subscribe to it.
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handleMessage(topic, message);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(String topic, MqttMessage message) {
        Log.i(PushListActivity.class.getName(), "Message Arrived!" +
                "\n Topic: ".concat(topic)
                        .concat("\n Message: ".concat(message.toString())));
    }

    private void constructConnectionOptions() {
        mqttOptions = new MqttConnectOptions();
        mqttOptions.setMqttVersion(3);
        mqttOptions.setCleanSession(true);
        mqttOptions.setKeepAliveInterval(60);
    }

    private void configureBroadcastReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updatePushList();
            }
        };
    }
}
