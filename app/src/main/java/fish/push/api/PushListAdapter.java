package fish.push.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import fish.push.api.API.PushfishMessage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class PushListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater layoutInflater;
    private ArrayList<PushfishMessage> entries = new ArrayList<>();
    private DateFormat dateFormat;
    private int selectedIndex = -1;

    public PushListAdapter(Context context) {
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
        this.dateFormat = new SimpleDateFormat("d MMM HH:mm");
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Object getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RelativeLayout itemView;
        if (convertView == null) {
            itemView = (RelativeLayout) layoutInflater.inflate(
                    R.layout.fragment_pushlist, parent, false
            );
        } else {
            itemView = (RelativeLayout) convertView;
        }
        configureViewForPosition(itemView, position);
        return itemView;
    }

    private void configureViewForPosition(View itemView, int position) {
        TextView dateText = (TextView) itemView.findViewById(R.id.push_date);
        TextView titleText = (TextView) itemView.findViewById(R.id.push_title);
        TextView descriptionText = (TextView) itemView.findViewById(R.id.push_description);
        ImageView iconImage = (ImageView) itemView.findViewById(R.id.push_icon_image);

        String title = entries.get(position).getTitle();
        if (title.equals(""))
            title = entries.get(position).getService().getName();
        String description = entries.get(position).getMessage();
        Date pushDate = entries.get(position).getLocalTimestamp();
        Bitmap icon = entries.get(position).getService().getIconBitmapOrDefault(context);

        dateText.setText(this.dateFormat.format(pushDate));
        titleText.setText(title);
        descriptionText.setText(description);
        iconImage.setImageBitmap(icon);

        TypedValue value = new TypedValue();
        DisplayMetrics metrics = new DisplayMetrics();

        context.getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, value, true);
        ((WindowManager) (context.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getMetrics(metrics);

        int lineCount = selectedIndex == position ? descriptionText.getLineCount() : 0;
        int minHeight = (int) TypedValue.complexToDimension(value.data, metrics);
        int prefHeight = (lineCount + 2) * descriptionText.getLineHeight();
        itemView.getLayoutParams().height = prefHeight > minHeight ? prefHeight : minHeight;
    }

    public int getSelected() {
        return selectedIndex;
    }

    public void setSelected(int selectedIndex) {
        this.selectedIndex = selectedIndex;
        notifyDataSetChanged();
    }

    public void clearSelected() {
        setSelected(-1);
    }

    public void addEntries(ArrayList<PushfishMessage> entries) {
        Collections.reverse(entries);
        for (PushfishMessage entry : entries)
            this.entries.add(0, entry);
        notifyDataSetChanged();
    }

    public void addEntry(PushfishMessage entry) {
        this.entries.add(0, entry);
        notifyDataSetChanged();
    }

    public void updateEntries(ArrayList<PushfishMessage> entries) {
        Collections.reverse(entries);
        this.entries = entries;
        notifyDataSetChanged();
    }
}