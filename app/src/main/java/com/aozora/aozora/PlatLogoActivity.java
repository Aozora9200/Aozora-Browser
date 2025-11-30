package com.aozora.aozora;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

public class PlatLogoActivity extends Activity {
    Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mToast = Toast.makeText(this, "REZZZZZZZ...", Toast.LENGTH_SHORT);

        ImageView content = new ImageView(this);
        content.setImageResource(R.drawable.h_platlogo);
        content.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        setContentView(content);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            mToast.show();
        }
        return super.dispatchTouchEvent(ev);
    }
}
