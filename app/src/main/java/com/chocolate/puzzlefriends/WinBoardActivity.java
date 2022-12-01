package com.chocolate.puzzlefriends;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;

import androidx.core.content.IntentCompat;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chocolate.puzzlefriends.Utils.DataHelper;
import com.chocolate.puzzlefriends.Utils.FileUtility;
import com.chocolate.puzzlefriends.Utils.SfxPlayer;
import com.chocolate.puzzlefriends.data.SfxResource;

import java.io.ByteArrayOutputStream;

import hotchemi.android.rate.AppRate;


public class WinBoardActivity extends Activity {
    private ImageButton shareBtn;
    private TextView moves, userName, time, difficulty;
    private boolean isFromGallery = false;
    private int solveMode = 1;
    private boolean isFirstShare = true;

    private ImageView imgView, mode;

    public static Intent createWinIntent(Context context, Bitmap completedImg, int moves, int secs, int mode, int difficulty, boolean fromGallery) {
        Intent intent = new Intent(context, WinBoardActivity.class);
        intent.putExtra("moves", moves);
        intent.putExtra("secs", secs);
        intent.putExtra("mode", mode);
        intent.putExtra("difficulty", difficulty);
        intent.putExtra("gallery", fromGallery);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        completedImg.compress(Bitmap.CompressFormat.JPEG, 60, stream);
        byte[] byteArray = stream.toByteArray();
        intent.putExtra("image", byteArray);

        return intent;
    }

    private void setupWin() {
        Intent intent = getIntent();
        moves.setText(String.format("%s: %d", "تعداد حرکت", intent.getIntExtra("moves", 0)));
        int secsT = intent.getIntExtra("secs", 0);
        time.setText(String.format("%s: %d:%02d", "مدت زمان", secsT / 60, secsT % 60));
        difficulty.setText(String.format("%s: %d*%d", "دوشواری", intent.getIntExtra("difficulty", 3), intent.getIntExtra("difficulty", 3)));
        solveMode = intent.getIntExtra("mode", 1);
        mode.setImageResource(solveMode == 1 ? R.drawable.mode1_on : R.drawable.mode2_on);
        isFromGallery = intent.getBooleanExtra("gallery", false);
        byte[] byteArray = getIntent().getByteArrayExtra("image");
        Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        imgView.setImageBitmap(bmp);

        findViewById(R.id.adsBanner).setVisibility(isFromGallery ? View.VISIBLE : View.GONE);

        SfxPlayer.getInstance(this).Play(SfxResource.Win);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            imgView.setMaxHeight(findViewById(R.id.layoutWin).getHeight() / 2);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_win_board);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE); // preventing screenshot :)

        initRate();
        final DataHelper dataHelper = DataHelper.getInstance(WinBoardActivity.this);

        moves = (TextView) findViewById(R.id.txtWinMoves);
        userName = (TextView) findViewById(R.id.txtWinMame);
        time = (TextView) findViewById(R.id.txtWinTime);
        difficulty = (TextView) findViewById(R.id.txtWinDifficulty);
        mode = (ImageView) findViewById(R.id.winModeImg);
        imgView = (ImageView) findViewById(R.id.imgImageOrg);

        setupWin();

        shareBtn = (ImageButton) findViewById(R.id.btnWinShare);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareBtn.setVisibility(View.GONE);

                RelativeLayout winLayout = (RelativeLayout) findViewById(R.id.layoutWin);
                winLayout.setDrawingCacheEnabled(true);
                winLayout.buildDrawingCache();

                Bitmap bmp = winLayout.getDrawingCache();
                FileUtility.shareBitmap(WinBoardActivity.this, bmp, true, 60);

                v.setVisibility(View.VISIBLE);

                if (isFromGallery && isFirstShare) {
                    dataHelper.decreaseRemainedGiftPuzzles();
                    isFirstShare = false;
                }
            }
        });
        int diff = getIntent().getIntExtra("difficulty", 3);
        final int givenTokens = isFromGallery ? 0 : (diff - 3 + 1) * (solveMode == 1 ? 2 : 1);

        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/ADastNevis.ttf");
        userName.setTypeface(tf);
        moves.setTypeface(tf);
        time.setTypeface(tf);
        difficulty.setTypeface(tf);

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setCancelable(true);
        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                onBackPressed();
            }
        });
        alert.setTitle("شما برنده شدید");
        if (givenTokens <= 0) {
            alert.setMessage("لطفا نام یا پیام خود را وارد کنید");
        } else {
            alert.setMessage("آفرین! شما " + givenTokens + " توکن جایزه گرفتید ...");
        }

        LinearLayout group = new LinearLayout(this);
        group.setGravity(Gravity.CENTER_HORIZONTAL);
        group.setOrientation(LinearLayout.VERTICAL);
        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setHint("نام یا پیام شما");
        input.setInputType(EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS | EditorInfo.TYPE_TEXT_VARIATION_FILTER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});
        group.addView(input);
        final ImageButton positiveBtn = new ImageButton(this);
        positiveBtn.setAdjustViewBounds(true);
        positiveBtn.setBackgroundColor(Color.TRANSPARENT);
        positiveBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        positiveBtn.setImageResource(R.drawable.btn_free);   // ****** change image if is gallery > free
        group.addView(positiveBtn);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        positiveBtn.setLayoutParams(lp);
        alert.setView(group);

        final AlertDialog alertDialog = alert.create();

        positiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String answer = " " + input.getText().toString() + " ";
                userName.setText(answer);
                dataHelper.setLives(dataHelper.getLives() + givenTokens);
                Toast.makeText(WinBoardActivity.this, String.format("توکن ها: %d", dataHelper.getLives()), Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
                AppRate.showRateDialogIfMeetsConditions(WinBoardActivity.this);
            }
        });

        alertDialog.show();
        input.requestFocus();
    }

    private void initRate() {
        AppRate.with(this)
                .setInstallDays(6) // default 10, 0 means install day.
                .setLaunchTimes(6) // default 10
                .setRemindInterval(2) // default 1
                .setShowNeutralButton(true) // default true
                .setDebug(false) // default false
                .monitor();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_win, menu);
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

    @Override
    public void onBackPressed() {
        final AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        dlg.setTitle("بازگشت به صفحه اصلی");
        dlg.setMessage("مطمئنی که نمیخوای جای دیگه رکوردت رو به اشتراک بذاری؟");
        dlg.setCancelable(true);
        dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        dlg.setPositiveButton("آره مطمئنم", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(WinBoardActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
        dlg.show();
    }
}
