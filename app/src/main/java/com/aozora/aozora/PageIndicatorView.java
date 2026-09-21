package com.aozora.aozora;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

/**
 * ViewPager 用のページインジケーター（Pixel 風：選択中だけ横長のピルになるドット）。
 * Material Components に依存しないので Theme.Holo のままで使えます。
 *
 * 使い方（adapter を setAdapter した後に呼ぶ）:
 *   PageIndicatorView dots = findViewById(R.id.pageIndicator);
 *   dots.setViewPager(viewPager);
 */
public class PageIndicatorView extends View implements ViewPager.OnPageChangeListener {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final float dotSize;      // ドットの直径
    private final float gap;          // ドット間の余白
    private final float activeWidth;  // 選択中ピルの幅

    private int activeColor = 0xFFFFFFFF;
    private int inactiveColor = 0x66FFFFFF;

    private int count = 0;
    private float current = 0f;       // position + offset（スワイプ中は小数になる）

    public PageIndicatorView(Context context) {
        this(context, null);
    }

    public PageIndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = context.getResources().getDisplayMetrics().density;
        dotSize = 8f * density;
        gap = 8f * density;
        activeWidth = 20f * density;
    }

    /** adapter を設定した後に呼んでください。 */
    public void setViewPager(ViewPager pager) {
        PagerAdapter adapter = pager.getAdapter();
        count = adapter == null ? 0 : adapter.getCount();
        current = pager.getCurrentItem();
        pager.addOnPageChangeListener(this);
        requestLayout();
        invalidate();
    }

    public void setColors(int activeColor, int inactiveColor) {
        this.activeColor = activeColor;
        this.inactiveColor = inactiveColor;
        invalidate();
    }

    private float contentWidth() {
        if (count <= 0) return 0f;
        return (count - 1) * dotSize + activeWidth + (count - 1) * gap;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = (int) Math.ceil(contentWidth()) + getPaddingLeft() + getPaddingRight();
        int h = (int) Math.ceil(dotSize) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (count <= 0) return;
        float x = (getWidth() - contentWidth()) / 2f;
        float cy = getHeight() / 2f;
        float r = dotSize / 2f;
        for (int i = 0; i < count; i++) {
            // 現在位置に近いほど 1 に近づく（スワイプ中は隣り合う2つの重みの合計が 1）
            float weight = Math.max(0f, 1f - Math.abs(i - current));
            float w = dotSize + (activeWidth - dotSize) * weight;
            paint.setColor(blend(inactiveColor, activeColor, weight));
            rect.set(x, cy - r, x + w, cy + r);
            canvas.drawRoundRect(rect, r, r, paint);
            x += w + gap;
        }
    }

    private static int blend(int from, int to, float t) {
        int a = (int) (((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t));
        int r = (int) (((from >>> 16) & 0xFF) + ((((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t));
        int g = (int) (((from >>> 8) & 0xFF) + ((((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t));
        int b = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ---- ViewPager.OnPageChangeListener ----
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        current = position + positionOffset;
        invalidate();
    }

    @Override
    public void onPageSelected(int position) { }

    @Override
    public void onPageScrollStateChanged(int state) { }
}
