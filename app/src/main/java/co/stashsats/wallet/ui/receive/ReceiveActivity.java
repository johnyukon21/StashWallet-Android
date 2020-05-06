package co.stashsats.wallet.ui.receive;

import android.R.color;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import co.stashsats.wallet.QrBitmap;
import co.stashsats.wallet.ui.LoggedActivity;
import co.stashsats.wallet.ui.R;
import co.stashsats.wallet.ui.R.drawable;
import co.stashsats.wallet.ui.R.id;
import co.stashsats.wallet.ui.R.layout;
import co.stashsats.wallet.ui.R.string;
import co.stashsats.wallet.ui.UI;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import co.stashsats.session.Session;
import co.stashsats.session.Conversion;

public class ReceiveActivity extends LoggedActivity implements TextWatcher {


    private TextView mAddressText;
    private ImageView mAddressImage;
    private EditText mAmountText;
    private Button mUnitButton;

    private Boolean mIsFiat = false;
    private String mCurrentAddress = "";
    private ObjectNode mCurrentAmount;
    private BitmapWorkerTask mBitmapWorkerTask;
    private boolean isGenerationOnProgress = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layout.activity_receive);
        UI.preventScreenshots(this);
        setTitleBackTransparent();

        mAddressImage = UI.find(this, id.receiveQrImageView);
        mAddressText = UI.find(this, id.receiveAddressText);
        mAmountText = UI.find(this, id.amountEditText);
        UI.localeDecimalInput(mAmountText);
        mUnitButton = UI.find(this, id.unitButton);

        mUnitButton.setOnClickListener((final View v) -> {
            onCurrencyClick();
        });

        UI.find(this, id.shareAddressButton).setOnClickListener((final View v) -> {
            onShareClicked();
        });

        UI.attachHideKeyboardListener(this, UI.find(this, id.content));
        generateAddress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinishing())
            return;

        mAmountText.addTextChangedListener(this);
        mUnitButton.setText(mIsFiat ? Conversion.getFiatCurrency() : Conversion.getUnit());
        mUnitButton.setPressed(!mIsFiat);
        mUnitButton.setSelected(!mIsFiat);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        final int id = R.menu.receive_menu;
        getMenuInflater().inflate(id, menu);
        menu.findItem(R.id.action_generate_new).setIcon(drawable.ic_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (isGenerationOnProgress)
            return true;
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case id.action_generate_new:
            generateAddress();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (isGenerationOnProgress)
            return;
        super.onBackPressed();
    }

    private void updateAddressText() {
        final Integer satoshi = mCurrentAmount != null ? mCurrentAmount.get("satoshi").asInt(0) : 0;
        mAddressText.setText(satoshi == 0 ? mCurrentAddress : getAddressUri(mCurrentAddress, mCurrentAmount));
    }

    private void updateQR() {
        if (mBitmapWorkerTask != null)
            mBitmapWorkerTask.cancel(true);
        mBitmapWorkerTask = new BitmapWorkerTask();
        mBitmapWorkerTask.execute();
    }

    public void generateAddress() {
        // mark generation new address as ongoing
        isGenerationOnProgress = true;
        Observable.just(Session.getSession())
        .subscribeOn(Schedulers.computation())
        .map((session) -> {
            final int subaccount = getActiveAccount();
            final JsonNode jsonResp = Session.getSession().getReceiveAddress(subaccount);
            return jsonResp;
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe((res) -> {
            final String address = res.get("address").asText();
            final Long pointer = res.get("pointer").asLong(0);
            mCurrentAddress = address;
            try {
                updateAddressText();
                updateQR();
            } catch (final Exception e) {
                Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
            }

            isGenerationOnProgress = false;
            return;
        }, (final Throwable e) -> {
            UI.toast(this, string.id_operation_failure, Toast.LENGTH_LONG);
            isGenerationOnProgress = false;
        });
    }



    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        final String key = mIsFiat ? "fiat" : getBitcoinUnitClean();
        try {
            final NumberFormat us = Conversion.getNumberFormat(8, Locale.US);
            final Number number = us.parse(mAmountText.getText().toString());
            final String value = String.valueOf(number);
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode amount = mapper.createObjectNode();
            amount.put(key, value.isEmpty() ? "0" : value);
            // avoid updating the view if changing from fiat to btc or vice versa
            if (mCurrentAmount == null || !mCurrentAmount.get(key).asText().equals(value)) {
                mCurrentAmount = Session.getSession().convert(amount);
                updateAddressText();
                updateQR();
            }
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void afterTextChanged(final Editable s) {}

    public void onCurrencyClick() {
        if (mCurrentAmount == null)
            return;

        try {
            mIsFiat = !mIsFiat;
            setAmountText(mAmountText, mIsFiat, mCurrentAmount);
        } catch (final ParseException e) {
            mIsFiat = !mIsFiat;
            UI.popup(this, R.string.id_your_favourite_exchange_rate_is).show();
            return;
        }

        // Toggle unit display and selected state
        mUnitButton.setText(mIsFiat ? Conversion.getFiatCurrency() : Conversion.getUnit());
        mUnitButton.setPressed(!mIsFiat);
        mUnitButton.setSelected(!mIsFiat);
    }

    public void onShareClicked() {
        if (TextUtils.isEmpty(mCurrentAddress))
            return;

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, UI.getText(mAddressText));
        intent.setType("text/plain");
        startActivity(intent);
    }

    public void onCopyClicked(final String label, final String data, final int toast) {
        if (data == null || data.isEmpty())
            return;

        final ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, data));
        UI.toast(this, toast, Toast.LENGTH_LONG);
    }

    private String getAddressUri(final String address, final ObjectNode amount) {
        final String qrCodeText;
        if (amount == null || amount.get("satoshi").asLong() == 0 || TextUtils.isEmpty(address)) {
            qrCodeText = address;
        } else {
            String s = amount.get("btc").asText();
            s = s.contains(".") ? s.replaceAll("0*$","").replaceAll("\\.$","") : s;
            qrCodeText = String.format(Locale.US,"bitcoin:%s?amount=%s", address, s);
        }
        return qrCodeText;
    }

    class BitmapWorkerTask extends AsyncTask<Object, Object, Bitmap> {
        final ObjectNode amount;
        final String address;
        final int qrCodeBackground = 0; // Transparent background

        BitmapWorkerTask() {
            amount = mCurrentAmount;
            address = mCurrentAddress;
        }

        @Override
        protected Bitmap doInBackground(final Object ... integers) {
            Log.d(TAG, " doInBackground(" + address + ")");
            try {
                return new QrBitmap(getAddressUri(address, amount), qrCodeBackground).getQRCode();
            } catch (final Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            if (bitmap == null)
                return;
            Log.d(TAG, "onPostExecute (" + address + ")");
            if (TextUtils.isEmpty(address)) {
                mAddressImage.setImageDrawable(getResources().getDrawable(color.transparent));
                mAddressImage.setOnClickListener(null);
                mAddressText.setOnClickListener(null);
            } else {
                final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
                bitmapDrawable.setFilterBitmap(false);
                mAddressImage.setImageDrawable(bitmapDrawable);
                mAddressImage.setOnClickListener((final View v) -> onCopyClicked("address", UI.getText(
                                                                                     mAddressText),
                                                                                 string.id_address_copied_to_clipboard));
                mAddressText.setOnClickListener((final View v) -> onCopyClicked("address", UI.getText(mAddressText),
                                                                                string.id_address_copied_to_clipboard));
            }
        }
    }
}
