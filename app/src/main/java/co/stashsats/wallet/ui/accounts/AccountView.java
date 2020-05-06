package co.stashsats.wallet.ui.accounts;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import co.stashsats.session.Conversion;
import co.stashsats.wallet.ui.R;
import co.stashsats.wallet.ui.UI;

public class AccountView extends CardView {

    private View mView;
    private Button mSendButton, mReceiveButton;
    private LinearLayout mBodyLayout, mActionLayout, mSubaccount, mAddSubaccount;
    private TextView mTitleText, mBalanceText, mBalanceUnitText, mBalanceFiatText;

    public AccountView(final Context context) {
        super(context);
        setupInflate(context);
        setupViews(mView);
    }

    public AccountView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setupInflate(context);
        setupViews(mView);
    }

    public void setView(final View view) {
        mView = view;
        setupViews(mView);
    }

    private void setupInflate(final Context context) {
        final LayoutInflater inflater = (LayoutInflater) context
                                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.list_element_wallet, this, true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            getBackground().setAlpha(0);
        else
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

        mView = getRootView();
    }

    private void setupViews(final View view) {
        mSendButton = UI.find(view, R.id.sendButton);
        mReceiveButton = UI.find(view, R.id.receiveButton);
        mBodyLayout = UI.find(view, R.id.body);
        mActionLayout = UI.find(view, R.id.actionLayout);
        mTitleText = UI.find(view, R.id.name);
        mBalanceText = UI.find(view, R.id.mainBalanceText);
        mBalanceUnitText = UI.find(view, R.id.mainBalanceUnitText);
        mBalanceFiatText = UI.find(view, R.id.mainLocalBalanceText);
        mSubaccount= UI.find(view, R.id.subaccount);
        mAddSubaccount = UI.find(view, R.id.addSubaccount);

    }

    // Show actions
    public void hideActions() {
        mActionLayout.setVisibility(GONE);

    }

    public void setTitle(final String text) {
        mTitleText.setText(text);
    }

    public void setBalance(final long satoshi) {
        mBalanceText.setVisibility(VISIBLE);
        mBalanceUnitText.setVisibility(VISIBLE);
        try {
            final String valueBitcoin = Conversion.getBtc(satoshi, false);
            final String valueFiat = Conversion.getFiat(satoshi, true);
            mBalanceText.setText(valueBitcoin);
            mBalanceUnitText.setText(" " + Conversion.getUnit());
            mBalanceFiatText.setText("≈  " + valueFiat);
        } catch (final Exception e) {
            Log.e("", "Conversion error: " + e.getLocalizedMessage());
        }
    }

    // Set on click listener
    @Override
    public void setOnClickListener(final OnClickListener onClickListener) {
        mSendButton.setOnClickListener(onClickListener);
        mReceiveButton.setOnClickListener(onClickListener);
        mBodyLayout.setOnClickListener(onClickListener);
        mBalanceText.setOnClickListener(onClickListener);
        mAddSubaccount.setOnClickListener(onClickListener);
    }

}
