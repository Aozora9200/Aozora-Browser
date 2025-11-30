package com.aozora.aozora;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.airbnb.lottie.LottieAnimationView;

public class TouchEffectView extends FrameLayout {

    private long lastTouchTime = 0;
    private static final long TRAIL_INTERVAL = 16; // ms間隔で小エフェクトを生成（約60fps）

    public TouchEffectView(Context context) {
        super(context);
        init();
    }

    public TouchEffectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);

        // ★ sw600dp判定：600dp以上なら true
        boolean isSw600dp = getResources().getConfiguration().smallestScreenWidthDp >= 600;

        // 値を分岐
        float effectValue = isSw600dp ? 0.2f : 0.6f;

        // ここがポイント：全イベントを監視するが、消費はしない
        setOnTouchListener((v, event) -> {
            PointF point = new PointF(event.getX(), event.getY());

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    SharedPreferences setupprefs = getContext().getSharedPreferences("AppPrefs", getContext().MODE_PRIVATE);
                    boolean touchEffect = setupprefs.getBoolean("touchEffect", true);
                    if (touchEffect) {
                        showEffect(point, effectValue, 1.0f);
                        lastTouchTime = System.currentTimeMillis();
                    }
                    break;
            }

            // false → 他のViewにタッチを渡す（スワイプやボタンも動作する）
            return false;
        });
    }

    private void showEffect(PointF point, float scale, float speed) {
        LottieAnimationView lottie = new LottieAnimationView(getContext());
        lottie.setAnimation("tap_effect.json"); // assets/tap_effect.json
        lottie.setRepeatCount(0);
        lottie.setSpeed(speed);

        int size = (int) (300 * scale);
        LayoutParams params = new LayoutParams(size, size);
        params.leftMargin = (int) (point.x - size / 2f);
        params.topMargin = (int) (point.y - size / 2f);
        addView(lottie, params);

        lottie.playAnimation();

        lottie.addAnimatorUpdateListener(animation -> {
            if (animation.getAnimatedFraction() >= 1.0f) {
                removeView(lottie);
            }
        });
    }
}