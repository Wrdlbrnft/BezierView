package com.github.wrdlbrnft.bezier;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.github.wrdlbrnft.bezier.string.BezierString;
import com.github.wrdlbrnft.bezier.symbols.BezierSymbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with Android Studio
 * User: kapeller
 * Date: 07/06/2017
 */

public class BezierView extends View {

    private static final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();

    private static final float STEP_WIDTH_FACTOR = 0.8f;
    private static final float CENTERING_FACTOR = 0.1f;

    @ColorInt
    private static final int DEFAULT_COLOR = Color.WHITE;
    private static final int DEFAULT_TEXT_SIZE = 96;
    private static final long DEFAULT_DURATION = 400L;
    private static final int DEFAULT_HORIZONTAL_DIGIT_PADDING = 0;
    private static final int DEFAULT_VERTICAL_DIGIT_PADDING = 0;
    private static final float DEFAULT_DIGIT_STROKE_WIDTH = 1.0f;
    private static final int DEFAULT_GRAVITY = Gravity.CENTER;

    private final List<BezierSymbolDrawable> mCurrentDrawables = new ArrayList<>();

    private final Drawable.Callback mDrawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(@NonNull Drawable who) {
            invalidate();
        }

        @Override
        public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {

        }

        @Override
        public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {

        }
    };

    @ColorInt
    private int mTextColor = DEFAULT_COLOR;
    private float mTextSize = DEFAULT_TEXT_SIZE;
    private BezierString mText = BezierString.empty();
    private long mAnimationDuration = DEFAULT_DURATION;
    private float mTextStrokeWidth = DEFAULT_DIGIT_STROKE_WIDTH;
    private int mHorizontalDigitPadding = DEFAULT_HORIZONTAL_DIGIT_PADDING;
    private int mVerticalDigitPadding = DEFAULT_VERTICAL_DIGIT_PADDING;
    private int mGravity = DEFAULT_GRAVITY;

    private final AnimationCoreographer mCoreographer = new AnimationCoreographer();

    public BezierView(Context context) {
        super(context);
    }

    public BezierView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        parseAttributes(context, attrs, 0, 0);
    }

    public BezierView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAttributes(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("WrongConstant")
    private void parseAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs == null) {
            return;
        }

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BezierView, defStyleAttr, defStyleRes);
        try {
            final int textColor = typedArray.getColor(R.styleable.BezierView_textColor, DEFAULT_COLOR);
            setTextColor(textColor);

            final int gravity = typedArray.getInteger(R.styleable.BezierView_android_gravity, DEFAULT_GRAVITY);
            setGravity(gravity);

            final float textSize = typedArray.getDimensionPixelSize(R.styleable.BezierView_textSize, DEFAULT_TEXT_SIZE);
            setTextSize(textSize);

            final String text = typedArray.getString(R.styleable.BezierView_text);
            setText(BezierString.of(text));

            final long animationDuration = typedArray.getInt(R.styleable.BezierView_animationDuration, (int) DEFAULT_DURATION);
            setAnimationDuration(animationDuration);

            final int horizontalDigitPadding = typedArray.getDimensionPixelSize(R.styleable.BezierView_digitPadding_horizontal, DEFAULT_HORIZONTAL_DIGIT_PADDING);
            setHorizontalDigitPadding(horizontalDigitPadding);

            final int verticalDigitPadding = typedArray.getDimensionPixelSize(R.styleable.BezierView_digitPadding_vertical, DEFAULT_VERTICAL_DIGIT_PADDING);
            setVerticalDigitPadding(verticalDigitPadding);

            final float strokeWidth = typedArray.getDimension(R.styleable.BezierView_textStrokeWidth, DEFAULT_DIGIT_STROKE_WIDTH);
            setTextStrokeWidth(strokeWidth);
        } finally {
            typedArray.recycle();
        }
    }

    public void setText(BezierString string) {
        mText = string;
        updateText(string == null ? BezierString.empty() : string);
    }

    public BezierString getText() {
        return mText;
    }

    private void updateText(BezierString string) {
        int counter = 0;
        boolean layoutChanged = false;

        final List<Animator> animators = new ArrayList<>();
        final int drawableCount = mCurrentDrawables.size();
        for (int i = 0, count = string.length(); i < count; i++, counter++) {
            final BezierSymbol symbol = string.symbolAt(i);

            if (i < drawableCount) {
                final BezierSymbolDrawable drawable = mCurrentDrawables.get(i);
                animators.add(drawable.createAnimator(symbol));
            } else {
                layoutChanged = true;
                final BezierSymbolDrawable drawable = new BezierSymbolDrawable();
                drawable.setColor(mTextColor);
                drawable.setStrokeWidth(mTextStrokeWidth);
                drawable.setCallback(mDrawableCallback);
                drawable.setSymbol(symbol);
                mCurrentDrawables.add(drawable);
            }
        }

        int limit = counter;
        while (counter < drawableCount) {
            layoutChanged = true;
            mCurrentDrawables.remove(drawableCount - 1 - counter + limit);
            counter++;
        }

        if (layoutChanged) {
            requestLayout();
        }

        if (!animators.isEmpty()) {
            final AnimatorSet set = new AnimatorSet();
            final Animator[] animatorArray = animators.toArray(new Animator[animators.size()]);
            set.setInterpolator(INTERPOLATOR);
            set.setDuration(mAnimationDuration);
            set.playTogether(animatorArray);
            mCoreographer.queue(set);
        }
    }

    public int getGravity() {
        return mGravity;
    }

    public void setGravity(int gravity) {
        mGravity = gravity;
        requestLayout();
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float textSize) {
        mTextSize = textSize;
        requestLayout();
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(@ColorInt int textColor) {
        mTextColor = textColor;
        updateColor(textColor);
    }

    private void updateColor(@ColorInt int color) {
        for (BezierSymbolDrawable drawable : mCurrentDrawables) {
            drawable.setColor(color);
        }
    }

    public float getTextStrokeWidth() {
        return mTextStrokeWidth;
    }

    public void setTextStrokeWidth(float strokeWidth) {
        mTextStrokeWidth = strokeWidth;
        updateStrokeWidth(strokeWidth);
    }

    private void updateStrokeWidth(float strokeWidth) {
        for (BezierSymbolDrawable drawable : mCurrentDrawables) {
            drawable.setStrokeWidth(strokeWidth);
        }
    }

    public int getHorizontalDigitPadding() {
        return mHorizontalDigitPadding;
    }

    public void setHorizontalDigitPadding(int horizontalDigitPadding) {
        mHorizontalDigitPadding = horizontalDigitPadding;
        requestLayout();
    }

    public int getVerticalDigitPadding() {
        return mVerticalDigitPadding;
    }

    public void setVerticalDigitPadding(int verticalDigitPadding) {
        mVerticalDigitPadding = verticalDigitPadding;
        requestLayout();
    }

    public long getAnimationDuration() {
        return mAnimationDuration;
    }

    public void setAnimationDuration(long duration) {
        mAnimationDuration = duration;
        mCoreographer.setMinDisplayTime(duration * 2 / 5);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int digitWidth = (int) (mTextSize + 0.5f);
        final int digitHeight = (int) (mTextSize + 0.5f);
        final int digitPaddingX = mHorizontalDigitPadding;
        final int digitPaddingY = mVerticalDigitPadding;
        final int stepWidthX = (int) (digitWidth * STEP_WIDTH_FACTOR + 0.5f);

        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();

        final int contentWidth = digitPaddingX + (stepWidthX + digitPaddingX) * mCurrentDrawables.size();
        final int contentHeight = digitHeight + 2 * digitPaddingY;

        final int desiredWidth = contentWidth + paddingLeft + paddingRight;
        final int desiredHeight = contentHeight + paddingTop + paddingBottom;

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        final int width = resolveMeasuredSize(desiredWidth, widthMode, widthSize);
        final int height = resolveMeasuredSize(desiredHeight, heightMode, heightSize);

        final int layoutDirection = ViewCompat.getLayoutDirection(this);
        final int absoluteGravity = Gravity.getAbsoluteGravity(mGravity, layoutDirection);

        final int gravityOffsetX = calculateHorizontalOffset(absoluteGravity, width, contentWidth);
        final int gravityOffsetY = calculateVerticalOffset(absoluteGravity, height, contentHeight);

        final int offsetX = gravityOffsetX + (int) (mTextSize * CENTERING_FACTOR + 0.5f) + paddingLeft;
        final int offsetY = gravityOffsetY + (int) (mTextSize * CENTERING_FACTOR + 0.5f) + paddingTop;

        for (int i = 0; i < mCurrentDrawables.size(); i++) {
            final BezierSymbolDrawable drawable = mCurrentDrawables.get(i);
            final int left = offsetX + digitPaddingX + i * (stepWidthX + digitPaddingX);
            final int top = offsetY + digitPaddingY;
            final int right = left + digitWidth;
            final int bottom = top + digitHeight;
            drawable.setBounds(left, top, right, bottom);
        }

        setMeasuredDimension(width, height);
    }

    private static int resolveMeasuredSize(int desiredSize, int mode, int limit) {

        if (mode == MeasureSpec.EXACTLY) {
            return limit;
        }

        if (mode == MeasureSpec.AT_MOST) {
            return Math.min(desiredSize, limit);
        }

        return desiredSize;
    }

    private static int calculateHorizontalOffset(int gravity, int width, int contentWidth) {
        switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {

            case Gravity.CENTER_HORIZONTAL:
                return (width - contentWidth) / 2;

            case Gravity.RIGHT:
                return width - contentWidth;

            case Gravity.LEFT:
            default:
                return 0;
        }
    }

    private static int calculateVerticalOffset(int gravity, int height, int contentHeight) {
        switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {

            case Gravity.CENTER_VERTICAL:
                return (height - contentHeight) / 2;

            case Gravity.BOTTOM:
                return height - contentHeight;

            case Gravity.TOP:
            default:
                return 0;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (BezierSymbolDrawable currentDrawable : mCurrentDrawables) {
            currentDrawable.draw(canvas);
        }
    }

    private static class AnimationCoreographer {

        private final Runnable mStartNextAnimator = new Runnable() {
            @Override
            public void run() {
                if (mNext != null) {
                    startAnimator(mNext);
                }
            }
        };

        private final Handler mHandler = new Handler(Looper.getMainLooper());

        private Animator mNext = null;
        private Animator mCurrent = null;

        private long mCurrentStartTime;
        private long mMinDisplayTime;

        public void setMinDisplayTime(long minDisplayTime) {
            mMinDisplayTime = minDisplayTime;
        }

        public void queue(Animator animator) {
            mHandler.removeCallbacks(mStartNextAnimator);

            if (mCurrent == null) {
                startAnimator(animator);
                return;
            }

            final long currentDisplayTime = System.currentTimeMillis() - mCurrentStartTime;

            if (mMinDisplayTime < currentDisplayTime) {
                startAnimator(animator);
            } else {
                mNext = animator;
                mHandler.postDelayed(mStartNextAnimator, mCurrentStartTime + mMinDisplayTime - System.currentTimeMillis());
            }
        }

        private void startAnimator(Animator animator) {
            if (mCurrent != null) {
                mCurrent.cancel();
            }
            mCurrent = animator;
            mCurrentStartTime = System.currentTimeMillis();
            mCurrent.start();
        }
    }
}
