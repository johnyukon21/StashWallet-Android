package co.stashsats.wallet.ui.components;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.cardview.widget.CardView;

import co.stashsats.wallet.ui.R;

public class CircularButton extends CardView {

    private LinearLayout mLinearLayout;

    private static float DEFAULT_RADIUS = 3;
    private static float DEFAULT_ELEVATION = 5;
    private static int DEFAULT_DURATION = 300;

    private ProgressBar mProgressBar;
    private Button mButton;
    private TransitionDrawable mTransStartLoading;
    private TransitionDrawable mTransStopLoading;
    private int mBackgroundColor;
    private int mLayoutWidth;
    private int mSelectableItemBackground;

    public CircularButton(Context context) {
        super(context);
    }

    public CircularButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircularButton);

        setLayoutTransition(new LayoutTransition());

        setRadius(getPx(DEFAULT_RADIUS));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(getPx(DEFAULT_ELEVATION));
        }

        mLinearLayout = new LinearLayout(context);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);

        // set selectable background
        final TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                                                 typedValue, true);
        mSelectableItemBackground = typedValue.resourceId;
        mLinearLayout.setBackgroundResource(mSelectableItemBackground);

        // create button
        mButton = new Button(context);
        mButton.setBackgroundColor(Color.TRANSPARENT);
        mButton.setClickable(false);
        mButton.setPadding((int)getPx(15), mButton.getPaddingTop(), (int)getPx(15),
                           mButton.getPaddingBottom());
        final String text = typedArray.getString(R.styleable.CircularButton_text);
        mButton.setText(text);
        mButton.setTextColor(typedArray.getColor(R.styleable.CircularButton_textColor, Color.BLACK));

        // create progressbar
        mProgressBar = new ProgressBar(context);
        mProgressBar.setVisibility(View.GONE);

        // animation transaction
        final LayoutTransition layoutTransition = getLayoutTransition();
        layoutTransition.setDuration(DEFAULT_DURATION);
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

        this.setOnClickListener(view -> {
            if (isClickable()) {
                startLoading();
            }
        });

        // set background color animations
        mBackgroundColor = typedArray.getColorStateList(R.styleable.CardView_cardBackgroundColor)
                           .getDefaultColor();
        final ColorDrawable[] color1 = {new ColorDrawable(mBackgroundColor),
                                        new ColorDrawable(Color.WHITE)};
        mTransStartLoading = new TransitionDrawable(color1);
        final ColorDrawable[] color2 = {new ColorDrawable(mSelectableItemBackground), new
                                        ColorDrawable(mBackgroundColor)};
        mTransStopLoading = new TransitionDrawable(color2);

        // set progressbar for API < lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mProgressBar.setBackgroundColor(Color.WHITE);
            mProgressBar.getIndeterminateDrawable().setColorFilter(
                mBackgroundColor, PorterDuff.Mode.SRC_IN);
        }

        typedArray.recycle();

        // get the width set
        final int[] width = new int[] { android.R.attr.layout_width };
        final TypedArray typedArray1 = context.obtainStyledAttributes(attrs, width);
        mLayoutWidth = typedArray1.getLayoutDimension(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        typedArray1.recycle();

        mLinearLayout.addView(mButton);
        mLinearLayout.addView(mProgressBar);
        addView(mLinearLayout);
    }

    public CircularButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private float getPx(float dim) {
        final Resources resources = getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dim, resources.getDisplayMetrics());
    }

    public void startLoading() {
        // set width to wrap
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        requestLayout();

        setClickable(false);
        mButton.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setCardBackgroundColor(Color.WHITE);
            mLinearLayout.setBackgroundColor(mSelectableItemBackground);
            setRadius(getPx(30));
            return;
        }
        setRadius(getPx(23));

        mLinearLayout.setBackground(mTransStartLoading);
        mTransStartLoading.startTransition(DEFAULT_DURATION);

        // workaround to set the correct background on the end of animation
        new Handler().postDelayed(() ->
                                  mLinearLayout.getBackground().setColorFilter(Color.WHITE,
                                                                               PorterDuff.Mode.MULTIPLY),
                                  DEFAULT_DURATION);
    }

    public void stopLoading() {
        // restore original width
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = mLayoutWidth;
        requestLayout();

        mButton.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        setRadius(getPx(DEFAULT_RADIUS));
        setClickable(true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setCardBackgroundColor(mBackgroundColor);
            mLinearLayout.setBackgroundColor(mBackgroundColor);
            return;
        }
        mLinearLayout.setBackground(mTransStopLoading);
        mTransStopLoading.startTransition(DEFAULT_DURATION);

        // workaround to set the correct background on the end of animation
        new Handler().postDelayed(() ->
                                  mLinearLayout.setBackgroundResource(mSelectableItemBackground), DEFAULT_DURATION);
    }

    public void setComplete(final Boolean complete) {
        if (complete)
            stopLoading();
        else
            startLoading();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setComplete(enabled);
    }

    public boolean isLoading() {
        return !isClickable();
    }

    public void setText(final String text) {
        if (mButton == null)
            return;
        mButton.setText(text);
    }

    public void setText(final int resId) {
        if (mButton == null)
            return;
        mButton.setText(resId);
    }
}
