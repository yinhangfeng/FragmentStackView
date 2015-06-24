package com.example.yinhangfeng.fragmentstackview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * Created by yhf on 2015/6/24.
 */
public class FragmentStackView extends ViewGroup {
    private static final String TAG = "FragmentStackView";
    static final boolean DEBUG = true;

    public FragmentStackView(Context context) {
        super(context);
    }

    public FragmentStackView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FragmentStackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO: 2015/6/24
    }
}
