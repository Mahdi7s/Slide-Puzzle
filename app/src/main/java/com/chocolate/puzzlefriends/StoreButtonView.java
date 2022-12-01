package com.chocolate.puzzlefriends;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chocolate.puzzlefriends.Utils.DataHelper;
import com.chocolate.puzzlefriends.Utils.SfxPlayer;
import com.chocolate.puzzlefriends.data.SfxResource;

import org.joda.time.DateTime;

/**
 * Created by choc01ate on 5/7/2015.
 */
public class StoreButtonView extends RelativeLayout implements View.OnClickListener {
    private TextView tokensText;
    private DataHelper dataHelper;
    private ImageView notification;
    private DateTime lastUpdated;
    private static boolean isInitialized = false;

    private AsyncTask<String, Integer, DataHelper> asyncTask;
    private int animCounter = 0;

    public StoreButtonView(Context context, AttributeSet attrSet) {
        super(context, attrSet);

        setVisibility(GONE);

        tokensText = new TextView(getContext());
        notification = new ImageView(getContext());

        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.btn_store);
        setBackgroundResource(R.drawable.btn_store);

        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/ADastNevis.ttf");
        tokensText.setTypeface(tf, Typeface.BOLD);
        tokensText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        addView(tokensText);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        tokensText.setLayoutParams(params);
        tokensText.setPadding(dpToPx(36), dpToPx(4), 0, 0);
        tokensText.setTextColor(Color.BLACK);

        notification.setImageResource(R.drawable.notification);
        addView(notification);
        params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        notification.setLayoutParams(params);
        notification.setVisibility(VISIBLE);

        final Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim);
        final Handler timeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(++animCounter >= 5) {
                    StoreButtonView.this.startAnimation(animation);
                    animCounter = 0;
                }

                if(dataHelper != null) {
                    DateTime luDate = dataHelper.getLastUpdatedDate();
                    if (isInitialized && ((luDate != null && lastUpdated.isBefore(luDate)) || dataHelper.isStoreDirty())) {
                        updateView(dataHelper);
                    }
                }

                this.sendMessageDelayed(this.obtainMessage(), 1 * 1000);
            }
        };
        timeHandler.sendMessageDelayed(timeHandler.obtainMessage(), 0);
        lastUpdated = DateTime.now();

        setOnClickListener(this);
    }

    @Override
    public void onAttachedToWindow() {
        new AsyncTask<String, Integer, DataHelper>() {
            @Override
            protected DataHelper doInBackground(String... params) {
                dataHelper = DataHelper.getInstance(StoreButtonView.this.getContext());
                dataHelper.getLives();

                return dataHelper;
            }

            @Override
            protected void onPostExecute(DataHelper dataHelper) {
                updateView(dataHelper);

                isInitialized = true;
            }
        }.execute();
    }

    private void updateView(DataHelper dataHelper) {
        int tokens = dataHelper.getLives();
//        if (tokens == DataHelper.UnlimitedLives) {
//            setVisibility(GONE);
//            setEnabled(false);
//            return;
//        }
        tokensText.setText(tokens + "");
        notification.setVisibility(dataHelper.isStoreDirty() ? VISIBLE : INVISIBLE);

        setVisibility(VISIBLE);
        lastUpdated = DateTime.now();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (asyncTask != null) {
            asyncTask.cancel(true);
            asyncTask = null;
        }
    }

    @Override
    public void onClick(View v) {
        SfxPlayer.getInstance(getContext()).Play(SfxResource.Button);

        Intent intent4 = new Intent(getContext(), StoreActivity.class);
        getContext().startActivity(intent4);
    }

    private int dpToPx(int dp){
        int padding_in_dp = dp;  // 6 dps
        final float scale = getResources().getDisplayMetrics().density;
        int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
        return padding_in_px;
    }
}
