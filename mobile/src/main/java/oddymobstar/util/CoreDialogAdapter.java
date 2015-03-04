package oddymobstar.util;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import oddymobstar.crazycourier.R;
import oddymobstar.database.DBHelper;

/**
 * Created by root on 27/02/15.
 */
public class CoreDialogAdapter extends CursorAdapter {

    private Context context;
    private int layout = R.layout.core_list_item;
    private int source;

    public CoreDialogAdapter(Context context, Cursor cursor, boolean autoRequery, int source) {
        super(context, cursor, autoRequery);

        this.source = source;
        this.context = context;

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflator = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        View v = inflator.inflate(layout, null);
        bindView(v, context, cursor);
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView tv = (TextView) view.findViewById(R.id.core_item_name);

        switch (source) {

            case CoreDialog.MY_TOPICS:
                tv.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TOPIC_KEY)) + " - " + cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TOPIC_NAME)));
                break;
            case CoreDialog.GLOBAL_TOPICS:
                tv.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TOPIC_KEY)) + " - " + cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TOPIC_NAME)));
                break;
            case CoreDialog.CONFIG:
                tv.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.CONFIG_NAME)) + " - " + cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.CONFIG_VALUE)));
                break;
            case CoreDialog.MY_ALLIANCES:
                tv.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.ALLIANCE_KEY)) + " - " + cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.ALLIANCE_NAME)));
                break;
        }


    }
}
