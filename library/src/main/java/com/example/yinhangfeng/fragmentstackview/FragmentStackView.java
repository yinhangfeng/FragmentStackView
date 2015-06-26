package com.example.yinhangfeng.fragmentstackview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import java.util.ArrayList;

/**
 * Created by yhf on 2015/6/24.
 */
public class FragmentStackView extends ViewGroup {
    private static final String TAG = "FragmentStackView";
    static final boolean DEBUG = true;

    private static final int DEFAULT_SCRIM_COLOR = 0x77000000;

    private static final Interpolator sQuinticInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    public static final int STATE_IDLE = 0;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SETTLING = 2;

    private int mDragState;

    private final int mTouchSlop;
    private final int mMaxVelocity;
    private final int mMinVelocity;

    //遮罩
    private int mScrimColor = DEFAULT_SCRIM_COLOR;
    private Paint mScrimPaint = new Paint();

    //阴影
    private Drawable mShadow;

    /** 当前激活的上层View */
    private View aboveActiveView;
    /** 当前激活的下层View */
    private View behindActiveView;
    private float behindOffsetScale = 0.33f;

    private ViewScroller mViewScroller = new ViewScroller();
    //是否处于用户不可操作的动画中
    private boolean forcedAnimation;

    private FragmentManager fragmentManager;
    private ArrayList<Fragment> fragmentStack = new ArrayList<>();

    public FragmentStackView(Context context) {
        this(context, null);
    }

    public FragmentStackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FragmentStackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMaxVelocity = vc.getScaledMaximumFlingVelocity();
        mMinVelocity = vc.getScaledMinimumFlingVelocity();
        setWillNotDraw(true);
    }

    /**
     * Fragment入栈
     * @param fragment 目标Fragment
     * @param tag tag
     * @param animate 是否动画过程
     */
    public void push(Fragment fragment, String tag, boolean animate) {
        if(forcedAnimation) {
            Log.w(TAG, "push forcedAnimation");
            return;
        }
        fragmentStack.add(fragment);
        fragmentManager.beginTransaction()
                .add(getId(), fragment, tag)
                .commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
        if(animate) {
            forcedAnimation = true;
            setTopActiveView();
            setActiveViewOffsetRatio(1);
            smoothScroll(false, 0);
        } else {
            dispatchOnPush();
        }
    }

    public void pop(boolean animate) {
        if(forcedAnimation) {
            Log.w(TAG, "pop forcedAnimation");
            return;
        }
        int size = fragmentStack.size();
        if(size == 0) {
            return;
        }
        if(animate) {
            forcedAnimation = true;
            setTopActiveView();
            setActiveViewOffsetRatio(0);
            smoothScroll(true, 0);
        } else {
            dispatchOnPop();
        }
    }

    /**
     * 设置阴影
     */
    public void setShadow(Drawable shadowDrawable) {
        mShadow = shadowDrawable;
        invalidate();
    }

    /**
     * 设置阴影
     */
    public void setShadow(@DrawableRes int resId) {
        setShadow(getResources().getDrawable(resId));
    }

    /**
     * 处理push结束
     */
    private void dispatchOnPush() {
        int size = fragmentStack.size();
        if(size > 1) {
            Fragment behindFragment = fragmentStack.get(size - 2);
            if(behindFragment.isHidden()) {
                behindActiveView.setVisibility(View.GONE);
            } else {
                fragmentManager.beginTransaction()
                        .hide(behindFragment)
                        .commitAllowingStateLoss();
                fragmentManager.executePendingTransactions();
            }
        }
        resetActiveView();
        forcedAnimation = false;
        ensureTopViewOffset();
    }

    /**
     * 处理pop结束
     */
    private void dispatchOnPop() {
        Log.i(TAG, "dispatchOnPop1 " + getChildCount());
        int size = fragmentStack.size();
        if(size > 0) {
            Fragment fragment = fragmentStack.get(size - 1);
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.remove(fragment);
            if(size > 1) {
                ft.show(fragmentStack.get(size - 2));
            }
            ft.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
            fragmentStack.remove(size - 1);
        }
        Log.i(TAG, "dispatchOnPop2 " + getChildCount());
        resetActiveView();
        forcedAnimation = false;
        ensureTopViewOffset();
    }

    /**
     * 设置栈顶的两个View为activeView
     */
    private void setTopActiveView() {
        aboveActiveView = null;
        behindActiveView = null;
        int childCount = getChildCount();
        if(childCount > 0) {
            aboveActiveView = getChildAt(childCount - 1);
            if(childCount > 1) {
                behindActiveView = getChildAt(childCount - 2);
                if(behindActiveView.getVisibility() != View.VISIBLE) {
                    behindActiveView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void resetActiveView() {
        aboveActiveView = null;
        behindActiveView = null;
    }

    //设置当前的偏移比率
    private void setActiveViewOffsetRatio(float aboveOffsetRatio) {
        if(aboveOffsetRatio < 0) {
            aboveOffsetRatio = 0;
        } else if(aboveOffsetRatio > 1) {
            aboveOffsetRatio = 1;
        }
        int newLeft = (int) (aboveOffsetRatio * getWidth());
        int dx = newLeft - aboveActiveView.getLeft();
        aboveActiveView.offsetLeftAndRight(dx);
        ((LayoutParams) aboveActiveView.getLayoutParams()).offsetRatio = aboveOffsetRatio;
        setBehindActiveViewOffset(aboveOffsetRatio);
        invalidate();
    }

    //偏移当前激活的View
    private void offsetActiveView(int dx) {
        if(dx == 0) {
            return;
        }
        aboveActiveView.offsetLeftAndRight(dx);
        float aboveOffsetRatio = (float) aboveActiveView.getLeft() / getWidth();
        ((LayoutParams) aboveActiveView.getLayoutParams()).offsetRatio = aboveOffsetRatio;
        setBehindActiveViewOffset(aboveOffsetRatio);
        invalidate();
    }

    //设置behindActiveView的offset 与aboveActiveView联动
    private void setBehindActiveViewOffset(float aboveOffsetRatio) {
        if(behindActiveView != null) {
            float behindOffsetRatio = (aboveOffsetRatio - 1) * behindOffsetScale;
            ((LayoutParams) behindActiveView.getLayoutParams()).offsetRatio = behindOffsetRatio;
            int newBehindLeft = (int) (behindOffsetRatio * getWidth());
            behindActiveView.offsetLeftAndRight(newBehindLeft - behindActiveView.getLeft());
        }
    }

    /**
     * 确保栈顶View的位置
     */
    private void ensureTopViewOffset() {
        int childCount = getChildCount();
        if(childCount == 0) {
            return;
        }
        View topView = getChildAt(childCount - 1);
        ((LayoutParams) topView.getLayoutParams()).offsetRatio = 0;
        int left = topView.getLeft();
        if(left != 0) {
            topView.offsetLeftAndRight(-left);
            invalidate();
        }
    }

    /**
     * 平滑滚动当前激活的view
     * @param open true: pop上层, false: 恢复或push上层
     * @param velocity 初速度
     */
    private void smoothScroll(boolean open, int velocity) {
        int startX = aboveActiveView.getLeft();
        int endX = open ? getWidth() : 0;
        // TODO: 2015/6/26 计算duration
        int duration = 3000;
        mViewScroller.startScroll(startX, endX - startX, duration);
    }

    private void setDragState(int state) {
        if(mDragState == state) {
            return;
        }
        mDragState = state;
        if(state == STATE_IDLE && aboveActiveView != null) {
            LayoutParams lp = (LayoutParams) aboveActiveView.getLayoutParams();
            if(lp.offsetRatio == 1) {
                dispatchOnPop();
            } else if(lp.offsetRatio == 0) {
                dispatchOnPush();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if(widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            //FragmentStackView必须固定宽高
            if(isInEditMode()) {
                //防止在界面编辑器中报错
                if(widthMode == MeasureSpec.AT_MOST) {
                    widthMode = MeasureSpec.EXACTLY;
                } else if(widthMode == MeasureSpec.UNSPECIFIED) {
                    widthMode = MeasureSpec.EXACTLY;
                    widthSize = 300;
                }
                if(heightMode == MeasureSpec.AT_MOST) {
                    heightMode = MeasureSpec.EXACTLY;
                } else if(heightMode == MeasureSpec.UNSPECIFIED) {
                    heightMode = MeasureSpec.EXACTLY;
                    heightSize = 300;
                }
            } else {
                throw new IllegalArgumentException("FragmentStackView must be measured with MeasureSpec.EXACTLY.");
            }
        }

        setMeasuredDimension(widthSize, heightSize);

        //测量子View
        int foundDrawers = 0;
        final int childCount = getChildCount();
        for(int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if(child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            //不考虑子View lp.width,必须填充除margin外的父View区域
            final int contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
            final int contentHeightSpec = MeasureSpec.makeMeasureSpec(heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
            child.measure(contentWidthSpec, contentHeightSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = r - l;
        final int childCount = getChildCount();
        for(int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if(child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childLeft = (int) (lp.offsetRatio * width);
            child.layout(childLeft, lp.topMargin, childLeft + child.getMeasuredWidth(), lp.topMargin + child.getMeasuredHeight());
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final boolean result = super.drawChild(canvas, child, drawingTime);
        if(aboveActiveView != null) {
            float opacity = 1 - ((LayoutParams) aboveActiveView.getLayoutParams()).offsetRatio;
            if(opacity > 0) {
                if (child == behindActiveView) {
                    //在behindActiveView的上面盖上遮罩
                    final int baseAlpha = (mScrimColor & 0xff000000) >>> 24;
                    final int imag = (int) (baseAlpha * opacity);
                    final int color = imag << 24 | (mScrimColor & 0xffffff);
                    mScrimPaint.setColor(color);
                    canvas.drawRect(0, 0, getWidth(), getHeight(), mScrimPaint);
                }
                if(mShadow != null && child == aboveActiveView) {
                    //在aboveActiveView边缘画上阴影
                    int childLeft = child.getLeft();
                    mShadow.setBounds(childLeft - mShadow.getIntrinsicWidth(), child.getTop(), childLeft, child.getBottom());
                    mShadow.setAlpha((int) (0xff * opacity));
                    mShadow.draw(canvas);
                }
            }
        }
        return result;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(forcedAnimation) {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent event) {
        return super.onInterceptHoverEvent(event);
        // TODO: 2015/6/25
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
        // TODO: 2015/6/25
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if(checkLayoutParams(params)) {
            ((LayoutParams) params).offsetRatio = 0;
        }
        super.addView(child, index, params);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof ViewGroup.MarginLayoutParams ? new LayoutParams((MarginLayoutParams) p) : new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        /**
         * 子View左侧相对于父View的偏移比例[-1, 1]
         * -1:左侧不可见
         * 0:一般状态
         * 1:右侧不可见
         */
        float offsetRatio;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    private class ViewScroller implements Runnable {

        private Scroller mScroller;
        private int lastX;

        public ViewScroller() {
            mScroller = new Scroller(getContext(), sQuinticInterpolator);
        }

        @Override
        public void run() {
            if(mScroller.computeScrollOffset()) {
                int x = mScroller.getCurrX();
                int dx = x - lastX;
                lastX = x;
                offsetActiveView(dx);
            }
            if(mScroller.isFinished()) {
                setDragState(STATE_IDLE);
            } else {
                postOnAnimation(this);
            }
        }

        public void startScroll(int startX, int dx, int duration) {
            setDragState(STATE_SETTLING);
            lastX = startX;
            mScroller.startScroll(startX, 0, dx, 0, duration);
            postOnAnimation(this);
        }

        public void stop() {
            removeCallbacks(this);
            mScroller.abortAnimation();
        }
    }
}
