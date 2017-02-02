package ch.ethz.inf.vs.talosavaapp.util;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import ch.ethz.inf.vs.talosavaapp.R;
import ch.ethz.inf.vs.talosavaapp.activities.CloudActivityOverview;
import ch.ethz.inf.vs.talosmodule.main.SharedUser;

/**
 * Created by lubu on 04.11.16.
 */

public class UserAdapter extends ArrayAdapter<SharedUser> {

    private ArrayList<SharedUser> items;

    public UserAdapter(Context context, ArrayList<SharedUser> items) {
        super(context, R.layout.list_cloud_layout, items);
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SharedUser item = items.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_cloud_layout, parent, false);
        }
        TextView type = (TextView) convertView.findViewById(R.id.clouddataype);
        TextView content = (TextView) convertView.findViewById(R.id.numtoday);
        ImageView imgView = (ImageView) convertView.findViewById(R.id.cloudimage);
        RelativeLayout layout = (RelativeLayout) convertView.findViewById(R.id.relLayoutList);
        LinearLayout linearLayout = (LinearLayout) convertView.findViewById(R.id.cloudlinlay);

        layout.setBackgroundColor(getContext().getResources().getColor(R.color.lightBG));
        linearLayout.setBackgroundColor(getContext().getResources().getColor(R.color.heavierBG));


        type.setText(String.valueOf(item.getLocalID()));
        content.setText(item.getMail());
        content.setTextColor(Color.BLACK);
        type.setTextColor(Color.BLACK);
        imgView.setBackgroundResource(R.drawable.login);

        return convertView;
    }
}
