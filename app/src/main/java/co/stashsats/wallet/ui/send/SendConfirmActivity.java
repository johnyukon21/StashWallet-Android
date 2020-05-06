package co.stashsats.wallet.ui.send;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import co.stashsats.wallet.ui.GaActivity;
import co.stashsats.wallet.ui.LoggedActivity;
import co.stashsats.wallet.ui.R;
import co.stashsats.wallet.ui.UI;
import co.stashsats.wallet.ui.components.CharInputFilter;
import co.stashsats.wallet.ui.preferences.PrefKeys;

import co.stashsats.session.Session;
import co.stashsats.session.Conversion;

public class SendConfirmActivity extends LoggedActivity implements SwipeButton.OnActiveListener {
    private static final String TAG = SendConfirmActivity.class.getSimpleName();
    private final ObjectMapper mObjectMapper = new ObjectMapper();

    private ObjectNode mTxJson;
    private SwipeButton mSwipeButton;

    private Disposable setupDisposable;
    private Disposable sendDisposable;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_confirm);
        UI.preventScreenshots(this);
        setTitleBackTransparent();
        mSwipeButton = UI.find(this, R.id.swipeButton);

        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        setTitle(R.string.id_send);

        startLoading();
        setupDisposable = Observable.just(Session.getSession())
                          .observeOn(AndroidSchedulers.mainThread())
                          .map((session) -> {
            return mObjectMapper.readValue(getIntent().getStringExtra(PrefKeys.INTENT_STRING_TX), ObjectNode.class);
        })
                          .observeOn(Schedulers.computation())
                          .map((tx) -> {
            // FIXME: If we didn't pass in the full transaction (with utxos)
            // then this call will go to the server. So, we should do it in
            // the background and display a wait icon until it returns
            return Session.getSession().createTransactionRaw(this, tx)
            .resolve();
        })
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe((tx) -> {
            mTxJson = tx;
            stopLoading();
            setup();
        }, (e) -> {
            e.printStackTrace();
            stopLoading();
            UI.toast(this, e.getLocalizedMessage(), Toast.LENGTH_LONG);
            setResult(Activity.RESULT_CANCELED);
            finishOnUiThread();
        });
    }

    private void setup() {
        // Setup views fields
        final TextView noteTextTitle = UI.find(this, R.id.sendMemoTitle);
        final TextView noteText = UI.find(this, R.id.noteText);
        final TextView addressText = UI.find(this, R.id.addressText);

        final JsonNode address = mTxJson.withArray("addressees").get(0);
        final String currentRecipient = address.get("address").asText();

        addressText.setText(currentRecipient);
        noteText.setText(mTxJson.get("memo") == null ? "" : mTxJson.get("memo").asText());
        CharInputFilter.setIfNecessary(noteText);

        // Set currency & amount
        final long amount = mTxJson.get("satoshi").asLong();
        final long fee = mTxJson.get("fee").asLong();
        final TextView sendAmount = UI.find(this, R.id.sendAmount);
        final TextView sendFee = UI.find(this, R.id.sendFee);
        sendAmount.setText(getFormatAmount(amount));
        sendFee.setText(getFormatAmount(fee));

        mSwipeButton.setOnActiveListener(this);
    }

    private String getFormatAmount(final long amount) {
        try {
            return String.format("%s / %s",
                                 Conversion.getBtc(amount, true),
                                 Conversion.getFiat(amount, true));
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (setupDisposable != null)
            setupDisposable.dispose();
        if (sendDisposable != null)
            sendDisposable.dispose();
    }

    @Override
    public void onActive() {
        startLoading();
        mSwipeButton.setEnabled(false);

        final GaActivity activity = this;
        final TextView noteText = UI.find(this, R.id.noteText);
        final String memo = noteText.getText().toString();
        mTxJson.put("memo", memo);

        sendDisposable = Observable.just(Session.getSession())
                         .observeOn(Schedulers.computation())
                         .map((session) -> {
            return session.signTransactionRaw(mTxJson).resolve();
        })
                         .map((tx) -> {
             Session.getSession().sendTransactionRaw(activity, tx).resolve();
            return tx;
        })
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe((tx) -> {
            mTxJson = tx;
            UI.toast(activity, R.string.id_transaction_sent, Toast.LENGTH_LONG);
            stopLoading();
            activity.setResult(Activity.RESULT_OK);
            activity.finishOnUiThread();
        }, (e) -> {
            e.printStackTrace();
            stopLoading();
            final Resources res = getResources();
            final String msg = UI.i18n(res, e.getMessage());
            UI.toast(activity, msg, Toast.LENGTH_LONG);
            if (msg.equals(res.getString(R.string.id_transaction_already_confirmed))) {
                activity.setResult(Activity.RESULT_OK);
                activity.finishOnUiThread();
            } else {
                mSwipeButton.setEnabled(true);
                mSwipeButton.moveButtonBack();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
