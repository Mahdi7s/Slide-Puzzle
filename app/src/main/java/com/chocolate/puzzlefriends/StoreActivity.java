package com.chocolate.puzzlefriends;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.chocolate.puzzlefriends.Utils.DataHelper;
import com.chocolate.puzzlefriends.Utils.SfxPlayer;
import com.chocolate.puzzlefriends.data.SfxResource;

import util.IabHelper;
import util.IabResult;
import util.Inventory;
import util.Purchase;


public class StoreActivity extends Activity implements View.OnClickListener {
    // (arbitrary) request code for the purchase flow
    private static final int RC_REQUEST = 10001;
    private static final String TAG = "STORE";
    private static final String SKU_Gift = "token_gift";
    private static final String SKU_Lives1 = "token_15";
    private static final String SKU_Lives2 = "token_35";
    private static final String SKU_Lives3 = "token_85";
    private static final String SKU_Lives4 = "token_035";
    //private static final String SKU_LivesUnlimited = "token_unlimited_test";//"token_unlimited";

    private View storeClickedBtn = null;

    private DataHelper dataHelper;
    private IabHelper mHelper;
    private int mLives = 0;
    private boolean /*mIsUnlimited,*/ mGiftGotten;

    private TextView txtTokensCount;
    private ImageButton btnGift, btnLives1, btnLives2, btnLives3, btnLives4;

    private ProgressDialog mProgressDialog;
    private boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        mProgressDialog = new ProgressDialog(this);

        btnGift = (ImageButton)findViewById(R.id.btnLives0);
        btnLives1 = (ImageButton)findViewById(R.id.btnLives1);
        btnLives2 = (ImageButton)findViewById(R.id.btnLives2);
        btnLives3 = (ImageButton)findViewById(R.id.btnLives3);
        btnLives4 = (ImageButton)findViewById(R.id.btnLives4);
        txtTokensCount = (TextView)findViewById(R.id.txtTokensCount);
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/ADastNevis.ttf");
        txtTokensCount.setTypeface(tf);

        btnGift.setOnClickListener(this);
        btnLives1.setOnClickListener(this);
        btnLives2.setOnClickListener(this);
        btnLives3.setOnClickListener(this);
        btnLives4.setOnClickListener(this);

        dataHelper = DataHelper.getInstance(this);

        loadData();
        updateUi();

        if(dataHelper.canGetFreeToken()){
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setCancelable(true);
            alert.setTitle("توکن رایگان");
            alert.setMessage("این هم 10 توکن رایگان برای شما...\n");
            alert.setPositiveButton("دریافت توکن ها", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dataHelper.getFreeToken();
                    loadData();
                    updateUi();
                    SfxPlayer.getInstance(StoreActivity.this).Play(SfxResource.Store);
                    dialog.dismiss();
                }
            });
            alert.show();
        }
    }

    private interface OnTaskCompleteListener {
        void onTaskComplete();
    }

    private void initializePurchase() {
        if(isInitialized/* || mIsUnlimited*/) return;
    /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
     * (that you got from the Google Play developer console). This is not your
     * developer public key, it's the *app-specific* public key.
     *
     * Instead of just storing the entire literal string here embedded in the
     * program,  construct the key at runtime from pieces or
     * use bit manipulation (for example, XOR with some other string) to hide
     * the actual key.  The key itself is not secret information, but we don't
     * want to make it easy for an attacker to replace the public key with one
     * of their own and then fake messages from the server.
     */
        String base64EncodedPublicKey = getString(R.string.bazaar_public_key);

        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        setWaitScreen(true);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    setWaitScreen(false);
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) {
                    setWaitScreen(false);
                    return;
                }

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) {
                setWaitScreen(false);
                return;
            }

            // Is it a failure?
            if (result.isFailure()) {
                setWaitScreen(false);
                complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            //Purchase premiumPurchase = inventory.getPurchase(SKU_LivesUnlimited);
            //mIsUnlimited = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));

            //if(!mIsUnlimited) {
                Purchase giftPurchase = inventory.getPurchase(SKU_Gift);
                mGiftGotten = (giftPurchase != null && verifyDeveloperPayload(giftPurchase));
                if(mGiftGotten){
                    dataHelper.setGiftGotten();
                }
            //}

            updateUi();
            setWaitScreen(false);
            isInitialized = true;
            if(storeClickedBtn != null){
                doPurchase(storeClickedBtn);
            }
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                //complain("Error purchasing: " + result);
                setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                return;
            }

            Log.d(TAG, "Purchase successful.");

            switch (purchase.getSku()) {
                case SKU_Gift:
                    Log.d(TAG, "Purchase is gift. Starting gift consumption.");
                    mLives = dataHelper.getGift();
                    mGiftGotten = dataHelper.isGiftGotten();
                    updateUi();
                    setWaitScreen(false);
                    break;
                case SKU_Lives1:
                    Log.d(TAG, "Purchase is lives. Starting live consumption.");
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                    break;
                case SKU_Lives2:
                    Log.d(TAG, "Purchase is lives. Starting live consumption.");
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                    break;
                case SKU_Lives3:
                    Log.d(TAG, "Purchase is lives. Starting live consumption.");
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                    break;
                case SKU_Lives4:
                    Log.d(TAG, "Purchase is lives. Starting live consumption.");
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                    break;
//                case SKU_LivesUnlimited:
//                    // bought the premium upgrade!
//                    Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
//                    //alert("Thank you for upgrading to premium!");
//                    mIsUnlimited = true;
//                    mLives = DataHelper.UnlimitedLives;
//                    saveData();
//                    updateUi();
//                    setWaitScreen(false);
//                    break;
            }
        }
    };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                Log.d(TAG, "Consumption successful. Provisioning.");
                String alertMsg = "هم اکنون شما بینهایت توکن دارید!!!";
                int addedTokens = getLivesOf(purchase.getSku());
//                if(addedTokens == DataHelper.UnlimitedLives){
//                    mIsUnlimited = true;
//                    mLives = DataHelper.UnlimitedLives;
//                }else {
                    mLives += addedTokens;
                    alertMsg = addedTokens + " توکن به موجودی شما اضافه شد";
//                }
                saveData();
                SfxPlayer.getInstance(StoreActivity.this).Play(SfxResource.Store);
                alert(alertMsg);
            } else {
                complain("Error while consuming: " + result);
            }
            updateUi();
            setWaitScreen(false);
            Log.d(TAG, "End consumption flow.");
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_store, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    // updates UI to reflect model
    public void updateUi() {
//        if(mIsUnlimited) {
//            txtTokensCount.setText(" نامحدود ");
//
//            btnGift.setEnabled(false);
//            btnLives1.setEnabled(false);
//            btnLives2.setEnabled(false);
//            btnLives3.setEnabled(false);
//            btnLivesUnlimited.setEnabled(false);
//        } else {
            txtTokensCount.setText(" = " + mLives);
//        }

        if(dataHelper.isGiftGotten()){
            btnGift.setVisibility(View.GONE);
        }
    }

    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set) {
        if(set && !mProgressDialog.isShowing()){
            mProgressDialog.setMessage("لطفا صبر کنید...");
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }else{
            mProgressDialog.dismiss();
        }
    }

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
        alert("خطا- لطفا اتصال به اینترنت خود را چک کنید");
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setCancelable(true);
        bld.setMessage(message);
        bld.setNeutralButton("خب", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

    void saveData() {
        dataHelper.setLives(mLives);
        Log.d(TAG, "Saved data: lives = " + String.valueOf(mLives));
    }

    void loadData() {
        mLives = dataHelper.getLives();
        mGiftGotten = dataHelper.isGiftGotten();
        Log.d(TAG, "Loaded data: lives = " + String.valueOf(mLives));
    }

    private int getLivesOf(String pack) {
        int retval = 0;
        switch (pack) {
            case SKU_Gift:
                retval = 300;
                break;
            case SKU_Lives1:
                retval = 650;
                break;
            case SKU_Lives2:
                retval = 1200;
                break;
            case SKU_Lives3:
                retval = 2600;
                break;
            case SKU_Lives4:
                retval = 5500;
                break;
//            case SKU_LivesUnlimited:
//                retval = DataHelper.UnlimitedLives;
//                break;
        }
        return retval;
    }

    @Override
    public void onClick(final View v) {
        if(isInitialized){
            doPurchase(v);
        }else{
            storeClickedBtn = v;
            initializePurchase();
        }
    }

    private void doPurchase(View v) {
        String payload = "payload";
        switch (v.getId()) {
            case R.id.btnLives1:
                setWaitScreen(true);
                Log.d(TAG, "Launching purchase flow for gas.");

                mHelper.launchPurchaseFlow(this, SKU_Lives1, RC_REQUEST,
                        mPurchaseFinishedListener, payload);
                break;
            case R.id.btnLives2:
                setWaitScreen(true);
                Log.d(TAG, "Launching purchase flow for gas.");

                mHelper.launchPurchaseFlow(this, SKU_Lives2, RC_REQUEST,
                        mPurchaseFinishedListener, payload);
                break;
            case R.id.btnLives3:
                setWaitScreen(true);
                Log.d(TAG, "Launching purchase flow for gas.");

                mHelper.launchPurchaseFlow(this, SKU_Lives3, RC_REQUEST,
                        mPurchaseFinishedListener, payload);
                break;
            case R.id.btnLives4:
                setWaitScreen(true);
                Log.d(TAG, "Launching purchase flow for gas.");

                mHelper.launchPurchaseFlow(this, SKU_Lives4, RC_REQUEST,
                        mPurchaseFinishedListener, payload);
                break;
//            case R.id.btnLivesUnlimited:
//                Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
//                setWaitScreen(true);
//
//                mHelper.launchPurchaseFlow(this, SKU_LivesUnlimited, RC_REQUEST,
//                        mPurchaseFinishedListener, payload);
//                break;
            case R.id.btnLives0: // gift button
                if(dataHelper.canGetGift()) {
                    Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
                    setWaitScreen(true);

                    mHelper.launchPurchaseFlow(this, SKU_Gift, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                } else if(!mGiftGotten) {
                    String msg = "برای گرفتن هدیه، " + dataHelper.getRemainedPuzzlesToGift() + " جورچین را از گالری حل کرده و به اشتراک بگذارید";
                    alert(msg);
                }
                break;
        }
        storeClickedBtn = null;
    }
}
