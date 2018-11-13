package com.example.overscroller.over;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.OverScroller;

/**
 * @author : liujian
 * @since : 2018/10/13
 */
public class OverLinearLayout extends LinearLayout {
    private static final int mOverScrollDistance = 900;
    private OverScroller scroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    private boolean mIsBeingDragged;
    private int mLastMotionY;
    private int mLastMotionX;

    private int measureHeight;

    public OverLinearLayout(Context context) {
        this(context, null);
    }

    public OverLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        scroller = new OverScroller(context);
        mVelocityTracker = VelocityTracker.obtain();
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //        int action = ev.getAction();
        //        if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged) {
        //            return true;
        //        }
        //        switch (action) {
        //            case MotionEvent.ACTION_DOWN:
        //                int y = (int) ev.getY();
        //                int x = (int) ev.getX();
        //                if (!inChild(x, y)) {
        //                    mIsBeingDragged = false;
        //                    break;
        //                }
        //                mLastMotionY = y;
        //                mLastMotionX = x;
        //                break;
        //            case MotionEvent.ACTION_MOVE:
        //                int deltaY = (int) Math.abs(ev.getY() - mLastMotionY);
        //                int deltaX = (int) Math.abs(ev.getX() - mLastMotionX);
        //                if (deltaY > mTouchSlop && deltaY > deltaX) {
        //                    mIsBeingDragged = true;
        //                    if (getParent() != null) {
        //                        getParent().requestDisallowInterceptTouchEvent(true);
        //                    }
        //                    mLastMotionY = (int) ev.getY();
        //                }
        //                break;
        //        }
        //        return mIsBeingDragged;
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(event);
                mLastMotionX = (int) event.getX();
                mLastMotionY = (int) event.getY();
                mIsBeingDragged = false;

                if (!scroller.isFinished()) {
                    scroller.forceFinished(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                initVelocityTrackerIfNotExists();
                mVelocityTracker.addMovement(event);
                int deltaX = (int) (mLastMotionX - event.getX());
                int deltaY = (int) (mLastMotionY - event.getY());
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop && Math.abs(deltaY) > Math.abs(deltaX)) {
                    mIsBeingDragged = true;
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }

                if (mIsBeingDragged) {
                    if ((getScrollY() <= 0 && deltaY < 0)) {
                        float scrollRate = getScrollRate(getScrollY(), mOverScrollDistance);
                        deltaY *= scrollRate;
                        Log.i("LIUJIAN", "scrollY1: " + getScrollY());
                        Log.i("LIUJIAN", "onTouchEvent1: " + scrollRate);
                    }
                    if ((getScrollY() >= getScrollRange() && deltaY > 0)) {
                        deltaY *= getScrollRate(getScrollY() - getScrollRange(), mOverScrollDistance);
                        Log.i("LIUJIAN", "scrollY1: " + (getScrollY() - getScrollRange()));
                        Log.i("LIUJIAN", "onTouchEvent1: " + getScrollRate(getScrollY() - getScrollRange(), mOverScrollDistance));
                    }
                    overScrollBy(0, deltaY, 0, getScrollY(), 0, getScrollRange(), 0, mOverScrollDistance, true);
                    mLastMotionX = (int) event.getX();
                    mLastMotionY = (int) event.getY();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    float yVelocity = mVelocityTracker.getYVelocity();
                    if (scroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange())) {
                        ViewCompat.postInvalidateOnAnimation(this);
                    } else if (Math.abs(yVelocity) > mMinimumVelocity) {
                        //scroll相关的坐标系与正常坐标系相反
                        doFling((int) -yVelocity);
                    }
                }
                recycleVelocityTracker();
                break;
            case MotionEvent.ACTION_CANCEL:
                recycleVelocityTracker();
                break;
        }
        super.onTouchEvent(event);
        return true;
    }


    private static final float INIT_RATE = 0.4f;
    private android.view.animation.Interpolator interpolator = new LinearInterpolator();

    private float getScrollRate(int scrollY, int totalDistance) {
        float q = 1 - interpolator.getInterpolation(Math.abs(scrollY) * 1.0f / Math.abs(totalDistance));
        return INIT_RATE * q;
    }

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        int newScrollX = scrollX + deltaX;
        int newScrollY = scrollY + deltaY;


        // Clamp values if at the limits and record
        final int left = -maxOverScrollX;
        final int right = maxOverScrollX + scrollRangeX;
        final int top = -maxOverScrollY;
        final int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (!scroller.isFinished()) {
            super.scrollTo(scrollX, scrollY);
            if (clampedY) {
                scroller.springBack(this.getScrollX(), this.getScrollY(), 0, 0, 0, getScrollRange());
            }
        } else {
            super.scrollTo(scrollX, scrollY);
        }
    }

    private void doFling(int yVelocity) {
        scroller.fling(0, getScrollY(), 0, yVelocity, 0, 0, 0, getScrollRange(), 0, mOverScrollDistance);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = scroller.getCurrX();
            int y = scroller.getCurrY();
            int range = getScrollRange();
            if (oldX != x || oldY != y) {
                overScrollBy(x - oldX, y - oldY, oldX, oldY, 0, range, 0, mOverScrollDistance, false);
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private boolean inChild(int x, int y) {
        int scrollY = getScrollY();
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (x >= childAt.getLeft() && x <= childAt.getRight() && y <= childAt.getBottom() - scrollY && y >= childAt.getTop() - scrollY) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.i("LIUJIAN", "onSizeChanged: " + h);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(measureHeight, MeasureSpec.UNSPECIFIED));
    }

    private int getScrollRange() {
        return getHeight() - measureHeight;
    }
}
