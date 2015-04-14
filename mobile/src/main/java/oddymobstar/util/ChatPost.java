package oddymobstar.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;

import oddymobstar.crazycourier.R;

/**
 * Created by root on 13/04/15.
 */
public class ChatPost extends LinearLayout {


    public ChatPost(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        inflater.inflate(R.layout.chat_post, this, true);


    }

    public ChatPost(Context context) {
        super(context, null);
    }


    public String getPost() {

        return ((EditText) findViewById(R.id.post_text)).getText().toString();

    }

    public void cancelPost() {
        ((EditText) findViewById(R.id.post_text)).setText("");
    }

}
