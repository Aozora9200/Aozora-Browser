package com.aozora.aozora;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class FlowLayout extends ViewGroup {

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int x = 0, y = 0, rowHeight = 0;

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            if (x + child.getMeasuredWidth() > width) {
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }
            x += child.getMeasuredWidth();
            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
        }
        y += rowHeight;
        setMeasuredDimension(width, y);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = 0, y = 0, rowHeight = 0;

        int width = r - l;

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (x + childWidth > width) {
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }

            child.layout(x, y, x + childWidth, y + childHeight);
            x += childWidth;
            rowHeight = Math.max(rowHeight, childHeight);
        }
    }
}