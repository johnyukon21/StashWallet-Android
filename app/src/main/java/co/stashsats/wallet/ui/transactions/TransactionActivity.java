package co.stashsats.wallet.ui.transactions;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import co.stashsats.sdk.data.BalanceData;
import co.stashsats.sdk.data.BumpTxData;
import co.stashsats.sdk.data.NetworkData;
import co.stashsats.sdk.data.TransactionData;
import co.stashsats.session.Conversion;
import co.stashsats.wallet.ui.LoggedActivity;
import co.stashsats.wallet.ui.R;
import co.stashsats.wallet.ui.UI;
import co.stashsats.wallet.ui.components.CharInputFilter;
import co.stashsats.wallet.ui.preferences.PrefKeys;
import co.stashsats.wallet.ui.send.SendAmountActivity;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;

import co.stashsats.session.Session;

public class TransactionActivity extends LoggedActivity implements View.OnClickListener  {

    private static final String TAG = TransactionActivity.class.getSimpleName();

    private TextView mMemoTitle;
    private TextView mMemoSave;
    private TextView mMemoText;
    private TextView mUnconfirmedText;
    private TextView mStatusIncreaseFee;
    private Button mExplorerButton;
    private Dialog mSummary;
    private Dialog mTwoFactor;
    private ImageView mStatusIcon;

    private TransactionData mTxItem;

    private Disposable memoDisposable, bumpDisposable;


    @Override
    protected int getMainViewId() { return R.layout.activity_transaction; }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_OK);
        UI.preventScreenshots(this);

        setTitleBackTransparent();

        mMemoTitle = UI.find(this, R.id.txMemoTitle);
        mMemoSave = UI.find(this, R.id.txMemoSave);
        mMemoText = UI.find(this, R.id.txMemoText);
        mExplorerButton = UI.find(this, R.id.txExplorer);
        mUnconfirmedText = UI.find(this, R.id.txUnconfirmedText);
        mStatusIncreaseFee = UI.find(this, R.id.status_increase_fee);
        mStatusIcon = UI.find(this, R.id.status_icon);

        try {
            mTxItem = (TransactionData) getIntent().getSerializableExtra("TRANSACTION");
        } catch (final Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            finishOnUiThread();
            return;
        }

        // Set txid
        final TextView hashText = UI.find(this, R.id.txHashText);
        hashText.setText(mTxItem.getTxhash());

        // Set explorer button
        final String blockExplorerTx = "https://blahblahblah";
        openInBrowser(mExplorerButton, mTxItem.getTxhash(), blockExplorerTx);

        // Set title: incoming, outgoing, redeposited
        final String title;
        if (mTxItem.getTxType() == TransactionData.TYPE.OUT)
            title = getString(R.string.id_sent);
        else if (mTxItem.getTxType() == TransactionData.TYPE.REDEPOSIT)
            title = getString(R.string.id_redeposited);
        else
            title = getString(R.string.id_received);
        setTitle(title);

        final String confirmations;
        final int confirmationsColor;
        final int currentBlock = Session.getSession().getBlockHeight();
        mStatusIcon.setVisibility(View.GONE);
        if (mTxItem.getConfirmations(currentBlock) == 0) {
            confirmations = getString(R.string.id_unconfirmed);
            confirmationsColor = R.color.red;
        } else {
            confirmations = getString(R.string.id_completed);
            confirmationsColor = R.color.green;
            mStatusIcon.setVisibility(View.VISIBLE);
        }
        mUnconfirmedText.setText(confirmations);
        mUnconfirmedText.setTextColor(getResources().getColor(confirmationsColor));

        // Set amount
        final boolean negative = mTxItem.getTxType() != TransactionData.TYPE.IN;
        final String neg = negative ? "-" : "";
        final TextView amountText = UI.find(this, R.id.txAmountText);

        try {
            final BalanceData balance = Session.getSession().convertBalance(mTxItem.getSatoshi().get("btc"));
            final String btc = Conversion.getBtc(balance, true);
            final String fiat = Conversion.getFiat(balance, true);
            amountText.setText(String.format("%s%s / %s%s", neg, btc, neg, fiat));
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // Set date/time
        final TextView dateText = UI.find(this, R.id.txDateText);
        final String date = mTxItem.getLocalizedDate(DateFormat.LONG);
        final String time = mTxItem.getLocalizedTime(DateFormat.SHORT);
        dateText.setText(date + ", " + time);

        // Set fees
        showFeeInfo(mTxItem.getFee(), mTxItem.getTransactionVsize(), mTxItem.getFeeRate());
        UI.hide(mStatusIncreaseFee);
        if (mTxItem.getTxType() == TransactionData.TYPE.OUT || mTxItem.getTxType() == TransactionData.TYPE.REDEPOSIT ||
            mTxItem.isSpent()) {
            if (mTxItem.getConfirmations(currentBlock) == 0)
                showUnconfirmed();
        }

        // Set recipient / received on
        final TextView recipientText = UI.find(this, R.id.txRecipientText);
        final TextView recipientTitle = UI.find(this, R.id.txRecipientTitle);
        if (!TextUtils.isEmpty(mTxItem.getAddressee())) {
            recipientText.setText(mTxItem.getAddressee());
        }

        UI.hideIf(mTxItem.getTxType() == TransactionData.TYPE.REDEPOSIT, UI.find(this, R.id.txRecipientReceiverView));
        UI.hideIf(mTxItem.getTxType() == TransactionData.TYPE.REDEPOSIT, UI.find(this, R.id.amountView));
        UI.hideIf(mTxItem.getTxType() == TransactionData.TYPE.IN, recipientText);
        UI.hideIf(mTxItem.getTxType() == TransactionData.TYPE.IN, recipientTitle);

        // Memo
        CharInputFilter.setIfNecessary(mMemoText);
        if (!TextUtils.isEmpty(mTxItem.getMemo())) {
            mMemoText.setText(mTxItem.getMemo());
        }

        mMemoSave.setOnClickListener(this);
        mMemoText.addTextChangedListener(new TextWatcher() {

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mMemoSave.setVisibility(View.VISIBLE);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void afterTextChanged(Editable s) { }
        });

        // The following are needed to effectively loose focus and cursor from mMemoText
        mMemoTitle.setFocusable(true);
        mMemoTitle.setFocusableInTouchMode(true);
    }

    private void showFeeInfo(final long fee, final long vSize, final long feeRate) {
        final TextView feeText = UI.find(this, R.id.txFeeInfoText);
        try {
            final String btcFee = Conversion.getBtc(fee, true);
            feeText.setText(String.format("%s (%s)", btcFee, UI.getFeeRateString(feeRate)));
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
    }

    private void showUnconfirmed() {

        UI.show(mStatusIncreaseFee);
        mStatusIncreaseFee.setOnClickListener(this);
        mStatusIcon.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSummary = UI.dismiss(this, mSummary);
        mTwoFactor = UI.dismiss(this, mTwoFactor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mMemoText);
        UI.unmapClick(mMemoSave);
        UI.unmapClick(mStatusIncreaseFee);

        if (memoDisposable != null)
            memoDisposable.dispose();
        if (bumpDisposable != null)
            bumpDisposable.dispose();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_transaction, menu);
        return true;
    }

    @Override
    public void onClick(final View v) {
        if (v == mMemoSave)
            onMemoSaveClicked();
        else if (v == mStatusIncreaseFee)
            onBumpFeeButtonClicked();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_share:
            final Intent sendIntent = new Intent(Intent.ACTION_SEND);
            final String blockExplorerTx = "https://blahblahblah";
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    blockExplorerTx+ mTxItem.getTxhash());
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
            return true;
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void onFinishedSavingMemo() {
        runOnUiThread(() -> {
            mMemoSave.setVisibility(View.GONE);
            hideKeyboardFrom(mMemoText);
            mMemoTitle.requestFocus();
        });
    }

    private void onMemoSaveClicked() {
        final String newMemo = UI.getText(mMemoText);
        if (newMemo.equals(mTxItem.getMemo())) {
            onFinishedSavingMemo();
            return;
        }

        memoDisposable = Observable.just(Session.getSession())
                         .observeOn(Schedulers.computation())
                         .map((session) -> {
            return session.changeMemo(mTxItem.getTxhash(), newMemo);
        })
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe((res) -> {
            if (res)
                onFinishedSavingMemo();
            else
                UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_LONG);
        }, (e) -> {
            e.printStackTrace();
            UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_LONG);
        });
    }

    private void openInBrowser(final Button button, final String identifier, final String url) {
        button.setOnClickListener(v -> {
            if (TextUtils.isEmpty(url))
                return;

            String domain = url;
            try {
                domain = new URI(url).getHost();
            } catch (final URISyntaxException e) {
                e.printStackTrace();
            }

            if (TextUtils.isEmpty(domain))
                return;

            final String stripped = domain.startsWith("www.") ? domain.substring(4) : domain;
            final Uri uri = Uri.parse(TextUtils.concat(url, identifier).toString());
//            final boolean dontAskAgain = cfg().getBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL, false);
//            if (dontAskAgain) {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
//            } else {
//                new MaterialDialog.Builder(this)
//                .checkBoxPromptRes(R.string.id_dont_ask_me_again, false,
//                                   (buttonView,
//                                    isChecked) -> cfg().edit().putBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL,
//                                                                          isChecked).apply())
//                .content(getString(R.string.id_are_you_sure_you_want_to_view, stripped))
//                .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
//                .positiveText(android.R.string.ok)
//                .negativeText(android.R.string.cancel)
//                .cancelable(false)
//                .onNegative((dialog, which) -> cfg().edit().putBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL,
//                                                                       false).apply())
//                .onPositive((dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, uri)))
//                .build().show();
//            }
        });
    }

    private void onBumpFeeButtonClicked() {
        Log.d(TAG,"onBumpFeeButtonClicked");

        startLoading();
        final String txhash = mTxItem.getTxhash();
        final int subaccount = mTxItem.getSubaccount() ==
                               null ? getActiveAccount() : mTxItem.getSubaccount();

        bumpDisposable = Observable.just(Session.getSession())
                         .observeOn(Schedulers.computation())
                         .map((session) -> {
            return session.getTransactionsRaw(subaccount, 0, 30).resolve();
        })
                         .map((txListObject) -> {
            return Session.getSession().findTransactionRaw((ArrayNode) txListObject.get(
                                                       "transactions"), txhash);
        })
                         .map((txToBump) -> {
            final JsonNode feeRate = txToBump.get("fee_rate");
            BumpTxData bumpTxData = new BumpTxData();
            bumpTxData.setPreviousTransaction(txToBump);
            bumpTxData.setFeeRate(feeRate.asLong());
            bumpTxData.setSubaccount(subaccount);
            Log.d(TAG,"createTransactionRaw(" + bumpTxData.toString() + ")");
            return bumpTxData;
        })
                         .map((bumpTxData) -> {
            return Session.getSession().createTransactionRaw(null, bumpTxData).resolve();
        })
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe((tx) -> {
            stopLoading();
            final Intent intent = new Intent(this, SendAmountActivity.class);
            removeUtxosIfTooBig(tx);
            intent.putExtra(PrefKeys.INTENT_STRING_TX, tx.toString());
            startActivity(intent);
            finish();
        }, (e) -> {
            e.printStackTrace();
            stopLoading();
            UI.toast(this, e.getMessage(), Toast.LENGTH_LONG);
            Log.e(TAG,e.getMessage());
        });
    }


}
