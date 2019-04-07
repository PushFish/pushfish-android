package fish.push.api.API;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fish.push.api.HttpUtil;

public class PushfishApi {
    private String baseurl;
    private int lastCheck = 0;
    private String uuid;
    private Context context;

    public PushfishApi(Context context) {
        this(context, "http://api.push.fish");
    }

    public PushfishApi(Context context, String url) {
        this.context = context;
        this.baseurl = url;
        this.uuid = (new DeviceUuidFactory(context)).getDeviceUuid().toString();
    }

    public Context getContext() {
        return this.context;
    }

    public String getUuid() {
        return this.uuid;
    }

    public Date getLastCheck() {
        return new Date((long) this.lastCheck * 1000);
    }

    public PushfishService addSubscription(String service) throws PushfishException {
        Map<String, String> data = this.getBaseMap();
        data.put("service", service);

        try {
            JSONObject obj = this.apiHttpPost("/subscription", data).getJSONObject("service");
            PushfishService srv = new PushfishService(obj.getString("public"), obj.getString("name"), new Date((long) obj.getInt("created") * 1000));
            srv.setIcon(obj.getString("icon"));
            return srv;
        } catch (JSONException ignore) {
            throw new PushfishException("Unknown error", -1);
        }
    }

    public void deleteSubscription(String service) throws PushfishException {
        Map<String, String> data = this.getBaseMap();
        data.put("service", service);
        this.apiHttpDelete("/subscription", data);
    }

    public PushfishService[] listSubscriptions() throws PushfishException {
        Map<String, String> data = this.getBaseMap();
        JSONObject obj = this.apiHttpGet("/subscription", data);
        try {
            JSONArray lstn = obj.getJSONArray("subscriptions");
            PushfishService[] srv = new PushfishService[lstn.length()];
            for (int i = 0; i < srv.length; i++) {
                JSONObject o = lstn.getJSONObject(i).getJSONObject("service");
                srv[i] = new PushfishService(o.getString("public"), o.getString("name"), new Date((long) o.getInt("created") * 1000));
                srv[i].setIcon(o.getString("icon"));
            }
            return srv;
        } catch (JSONException ignore) {
            throw new PushfishException("Unknown error", -1);
        }
    }

    public void sendMessage(PushfishMessage msg) throws PushfishException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("service", msg.getService().getToken());
        data.put("message", msg.getMessage());
        data.put("title", msg.getTitle());
        data.put("level", msg.getLevel() + "");
        this.apiHttpPost("/message", data);
    }

    public PushfishMessage[] getNewMessage() throws PushfishException {
        this.lastCheck = Math.round(System.currentTimeMillis() / 1000);
        try {
            JSONArray json = this.apiHttpGet("/message", this.getBaseMap()).getJSONArray("messages");
            PushfishMessage[] msg = new PushfishMessage[json.length()];
            for (int i = 0; i < json.length(); i++) {
                JSONObject AzMsg = json.getJSONObject(i);
                JSONObject AzServ = AzMsg.getJSONObject("service");
                PushfishService srv = new PushfishService(
                        AzServ.getString("public"),
                        AzServ.getString("name"),
                        new Date((long) AzServ.getInt("created") * 1000)
                );
                msg[i] = new PushfishMessage(
                        srv,
                        AzMsg.getString("message"),
                        AzMsg.getString("title"),
                        AzMsg.getInt("timestamp")
                );
                msg[i].setLevel(AzMsg.getInt("level"));
                msg[i].setLink(AzMsg.getString("link"));
            }
            return msg;
        } catch (JSONException ignore) {
            throw new PushfishException("Unable to parse data", 101);
        }
    }

    private Map<String, String> getBaseMap() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("uuid", this.uuid);
        return data;
    }

    private JSONObject apiHttpPost(String route, Map<String, String> data) throws PushfishException {
        String url = this.baseurl + route;
        String resp = "";
        try {
            resp = HttpUtil.Post(url, data);
            JSONObject json = new JSONObject(resp);
            if (json.has("error")) {
                JSONObject err = json.getJSONObject("error");
                throw new PushfishException(err.getString("message"), err.getInt("id"));
            }
            return json;
        } catch (IOException ignore) {
            throw new PushfishException("There was a network issue", 100);
        } catch (JSONException ignore) {
            throw new PushfishException("Unable to parse data: " + resp, 101);
        }
    }

    private JSONObject apiHttpGet(String route, Map<String, String> data) throws PushfishException {
        String url = this.baseurl + route;
        String resp = "";
        try {
            resp = HttpUtil.Get(url, data);
            JSONObject json = new JSONObject(resp);
            if (json.has("error")) {
                JSONObject err = json.getJSONObject("error");
                throw new PushfishException(err.getString("message"), err.getInt("id"));
            }
            return json;
        } catch (IOException ignore) {
            throw new PushfishException("There was a network issue", 100);
        } catch (JSONException ignore) {
            throw new PushfishException("Unable to parse data: " + resp, 101);
        }
    }

    private JSONObject apiHttpDelete(String route, Map<String, String> data) throws PushfishException {
        String url = this.baseurl + route;
        String resp = "";
        try {
            resp = HttpUtil.Delete(url, data);
            JSONObject json = new JSONObject(resp);
            if (json.has("error")) {
                JSONObject err = json.getJSONObject("error");
                throw new PushfishException(err.getString("message"), err.getInt("id"));
            }
            return json;
        } catch (IOException ignore) {
            throw new PushfishException("There was a network issue", 100);
        } catch (JSONException ignore) {
            throw new PushfishException("Unable to parse data: " + resp, 101);
        }
    }
}
