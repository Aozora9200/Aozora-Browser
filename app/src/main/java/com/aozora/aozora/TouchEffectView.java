package com.aozora.aozora;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.appcompat.view.WindowCallbackWrapper;

import java.util.ArrayList;
import java.util.Random;

public class TouchEffectView extends View {

    private static final int COLOR_MAIN = 0xFFCAEFF8; // 元のLottieと同じ色
    private static final int COLOR_CORE = 0xFFF2FBFD; // 軌跡の芯(明るい色)
    private static final long RING_MS = 450;
    private static final long BURST_MS = 600;
    private static final long SPARKLE_MS = 420;
    private static final long TRAIL_MS = 320;         // 軌跡が消えるまでの時間
    private static final int BURST_COUNT = 6;
    private static final int MAX_PARTICLES = 96;
    private static final float SPARKLE_STEP_DP = 16f; // 何dp動くごとに火花を出すか

    private static class Ring { float x, y; long start; }
    private static class Particle {
        float x, y, angle, dist, rot, spin, size;
        long start, life;
    }
    private static class Pt { float x, y; long time; }
    private static class Trail {
        final ArrayList<Pt> pts = new ArrayList<>();
        boolean active = true;
        float lastEmitX, lastEmitY;
    }

    private final ArrayList<Ring> rings = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final ArrayList<Trail> trails = new ArrayList<>();
    private final SparseArray<Trail> activeTrails = new SparseArray<>(); // pointerId -> Trail
    private final Random rnd = new Random();
    private final Path path = new Path();

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float dp;
    private final SharedPreferences prefs;

    public TouchEffectView(Context context) {
        this(context, null);
    }

    public TouchEffectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        setClickable(false);
        setFocusable(false);
        setWillNotDraw(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(COLOR_MAIN);
        fillPaint.setStyle(Paint.Style.FILL);
        trailPaint.setStyle(Paint.Style.STROKE);
        trailPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    // ===== Windowに取り付ける(Activity / Dialog 共通) =====
    @SuppressLint("RestrictedApi")
    public static TouchEffectView attach(Window window) {
        ViewGroup decor = (ViewGroup) window.getDecorView();
        TouchEffectView view = new TouchEffectView(window.getContext());
        decor.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // タッチを消費せず、全イベントを先に覗く(スクロール中のMOVEも届く)
        window.setCallback(new WindowCallbackWrapper(window.getCallback()) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                view.onGlobalTouch(event);
                return super.dispatchTouchEvent(event);
            }
        });
        return view;
    }

    // このViewはタッチを一切受け取らない
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    // ===== タッチ処理 =====
    public void onGlobalTouch(MotionEvent ev) {
        long now = SystemClock.uptimeMillis();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (!prefs.getBoolean("touchEffect", true)) return;
                int index = ev.getActionIndex();
                float x = ev.getX(index), y = ev.getY(index);
                spawnBurst(x, y, now);

                Trail t = new Trail();
                addPoint(t, x, y, now);
                t.lastEmitX = x;
                t.lastEmitY = y;
                trails.add(t);
                activeTrails.put(ev.getPointerId(index), t);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < ev.getPointerCount(); i++) {
                    Trail t = activeTrails.get(ev.getPointerId(i));
                    if (t == null) continue;
                    // 取りこぼしを防ぎ、軌跡を滑らかにする
                    for (int h = 0; h < ev.getHistorySize(); h++) {
                        addPoint(t, ev.getHistoricalX(i, h), ev.getHistoricalY(i, h),
                                ev.getHistoricalEventTime(h));
                    }
                    float x = ev.getX(i), y = ev.getY(i);
                    addPoint(t, x, y, now);

                    float dx = x - t.lastEmitX, dy = y - t.lastEmitY;
                    if (dx * dx + dy * dy > (SPARKLE_STEP_DP * dp) * (SPARKLE_STEP_DP * dp)) {
                        spawnSparkle(x, y, now);
                        t.lastEmitX = x;
                        t.lastEmitY = y;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                int id = ev.getPointerId(ev.getActionIndex());
                Trail t = activeTrails.get(id);
                if (t != null) {
                    t.active = false; // 以降は自然にフェードアウト
                    activeTrails.remove(id);
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                for (int i = 0; i < activeTrails.size(); i++) {
                    activeTrails.valueAt(i).active = false;
                }
                activeTrails.clear();
                break;
            }
        }
        postInvalidateOnAnimation();
    }

    private void addPoint(Trail t, float x, float y, long time) {
        Pt p = new Pt();
        p.x = x; p.y = y; p.time = time;
        t.pts.add(p);
    }

    private void spawnBurst(float x, float y, long now) {
        Ring r = new Ring();
        r.x = x; r.y = y; r.start = now;
        rings.add(r);

        float step = (float) (Math.PI * 2 / BURST_COUNT);
        float offset = rnd.nextFloat() * step;
        for (int i = 0; i < BURST_COUNT; i++) {
            addParticle(x, y, offset + step * i + (rnd.nextFloat() - 0.5f) * 0.4f,
                    dp * (28 + rnd.nextFloat() * 22), dp * (4 + rnd.nextFloat() * 3), BURST_MS, now);
        }
    }

    private void spawnSparkle(float x, float y, long now) {
        addParticle(x, y, rnd.nextFloat() * (float) (Math.PI * 2),
                dp * (8 + rnd.nextFloat() * 10), dp * (2 + rnd.nextFloat() * 2), SPARKLE_MS, now);
    }

    private void addParticle(float x, float y, float angle, float dist, float size, long life, long now) {
        if (particles.size() >= MAX_PARTICLES) return;
        Particle p = new Particle();
        p.x = x; p.y = y; p.angle = angle; p.dist = dist; p.size = size;
        p.rot = rnd.nextFloat() * 6.28f;
        p.spin = (rnd.nextFloat() - 0.5f) * 8f;
        p.start = now; p.life = life;
        particles.add(p);
    }

    // ===== 描画 =====
    @Override
    protected void onDraw(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        boolean alive = false;

        // リング + 中心のフラッシュ
        for (int i = rings.size() - 1; i >= 0; i--) {
            Ring r = rings.get(i);
            float t = (now - r.start) / (float) RING_MS;
            if (t >= 1f) { rings.remove(i); continue; }
            alive = true;
            float e = 1f - (1f - t) * (1f - t) * (1f - t); // ease-out

            ringPaint.setStrokeWidth(dp * (3.5f * (1f - e) + 0.6f));
            ringPaint.setAlpha((int) (255 * (1f - t)));
            canvas.drawCircle(r.x, r.y, dp * (6 + 38 * e), ringPaint);

            if (t < 0.35f) {
                float f = t / 0.35f;
                fillPaint.setColor(COLOR_MAIN);
                fillPaint.setAlpha((int) (150 * (1f - f)));
                canvas.drawCircle(r.x, r.y, dp * (4 + 16 * f), fillPaint);
            }
        }

        // 軌跡
        for (int i = trails.size() - 1; i >= 0; i--) {
            Trail tr = trails.get(i);
            while (tr.pts.size() > (tr.active ? 1 : 0)
                    && now - tr.pts.get(0).time > TRAIL_MS) {
                tr.pts.remove(0);
            }
            if (tr.pts.isEmpty()) { trails.remove(i); continue; }
            alive = true;
            drawTrail(canvas, tr, now);
        }

        // 三角パーティクル
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            float t = (now - p.start) / (float) p.life;
            if (t >= 1f) { particles.remove(i); continue; }
            alive = true;
            float e = 1f - (1f - t) * (1f - t) * (1f - t);
            float cx = p.x + (float) Math.cos(p.angle) * p.dist * e;
            float cy = p.y + (float) Math.sin(p.angle) * p.dist * e;
            float rot = p.rot + p.spin * t;
            float s = p.size * (1f - 0.5f * t);

            path.reset();
            for (int k = 0; k < 3; k++) {
                float a = rot + k * 2.0944f; // 120度
                float px = cx + (float) Math.cos(a) * s;
                float py = cy + (float) Math.sin(a) * s;
                if (k == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
            path.close();
            fillPaint.setColor(COLOR_MAIN);
            fillPaint.setAlpha((int) (255 * (1f - t * t)));
            canvas.drawPath(path, fillPaint);
        }

        if (alive) postInvalidateOnAnimation();
    }

    private void drawTrail(Canvas canvas, Trail tr, long now) {
        int n = tr.pts.size();
        for (int k = 1; k < n; k++) {
            Pt a = tr.pts.get(k - 1), b = tr.pts.get(k);
            float life = 1f - (now - b.time) / (float) TRAIL_MS;
            if (life <= 0f) continue;
            if (life > 1f) life = 1f;

            // 外側のグロー
            trailPaint.setColor(COLOR_MAIN);
            trailPaint.setAlpha((int) (80 * life));
            trailPaint.setStrokeWidth(dp * (1.5f + 7f * life) * 2.2f);
            canvas.drawLine(a.x, a.y, b.x, b.y, trailPaint);

            // 内側の明るい芯
            trailPaint.setColor(COLOR_CORE);
            trailPaint.setAlpha((int) (230 * life));
            trailPaint.setStrokeWidth(dp * (0.8f + 3.5f * life));
            canvas.drawLine(a.x, a.y, b.x, b.y, trailPaint);
        }

        // 指先の光(押している間だけ)
        if (tr.active && n > 0) {
            Pt head = tr.pts.get(n - 1);
            fillPaint.setColor(COLOR_MAIN);
            fillPaint.setAlpha(110);
            canvas.drawCircle(head.x, head.y, dp * 6f, fillPaint);
            fillPaint.setColor(COLOR_CORE);
            fillPaint.setAlpha(230);
            canvas.drawCircle(head.x, head.y, dp * 2.5f, fillPaint);
        }
    }
}