package com.chocolate.puzzlefriends;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.content.IntentCompat;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.chocolate.puzzlefriends.Utils.BitmapUtility;
import com.chocolate.puzzlefriends.Utils.DataHelper;
import com.chocolate.puzzlefriends.Utils.FileUtility;
import com.chocolate.puzzlefriends.Utils.SfxPlayer;
import com.chocolate.puzzlefriends.data.SfxResource;

//This activity will display the small image chunks into a grid view
public class ChunkedImageActivity extends Activity implements View.OnClickListener {
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    private int chunkNumbers = 3;
    private GameboardView gameboard;
    private ImageButton rbMode1, rbMode2;
    private boolean isFirstShare = true;
    private boolean isRandomized = false;

    public static boolean puzzleLoaded = false;
    private static String state = null;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_image_grid);

        findViewById(R.id.btnRandomSlice).setOnClickListener(this);
        findViewById(R.id.btnSharePuzzle).setOnClickListener(this);

        gameboard = (GameboardView) findViewById(R.id.gameboard);

        rbMode1 = (ImageButton) findViewById(R.id.btnMode1);
        rbMode2 = (ImageButton) findViewById(R.id.btnMode2);

        rbMode1.setOnClickListener(this);
        rbMode2.setOnClickListener(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) return;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initPuzzling();
            }
        }, 10);
    }

    private void initPuzzling() {
        final Intent intent = getIntent();
        if(state == null && !puzzleLoaded) {
            state = intent.getStringExtra("state");
            chunkNumbers = intent.getIntExtra("chunkNumbers", 3);
        } else if(state != null && state.equals("create_puzzle")) {
            return;
        }

        switch (state) {
            case "create_puzzle":
                Intent openImage = FileUtility.getSocialImageIntents(this, true);
                startActivityForResult(openImage, REQUEST_IMAGE_CAPTURE);
                break;
            case "intent_image":
                Uri bmpUri = intent.getData();
                Bitmap bmp = BitmapUtility.decodeScaledBitmapFromSdCard(FileUtility.getRealPathFromURI(this, bmpUri), gameboard.getWidth(), gameboard.getHeight());
                Map<Class, Object> retval = new HashMap<Class, Object>();
                retval.put(Uri.class, bmpUri);
                retval.put(Bitmap.class, bmp);

                startPuzzleMaking(retval);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Map<Class, Object> map = FileUtility.onImageResult(this, requestCode, resultCode, data, gameboard.getWidth(), gameboard.getHeight());
        puzzleLoaded = startPuzzleMaking(map);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        state = null;
    }

    private boolean startPuzzleMaking(final Map<Class, Object> map) {
        isFirstShare = true;
        try { // split image
            Bitmap bmp = (Bitmap) map.get(Bitmap.class);
            if (bmp.getWidth() < 300 || bmp.getHeight() < 200) {
                Toast.makeText(ChunkedImageActivity.this, "حداقل سایز عکس باید ۲۰۰*۳۰۰ باشد", Toast.LENGTH_LONG).show();
                onBackPressed();
            }
            gameboard.isSolving = false;
            LinkedList<Integer> ordersArr = BitmapUtility.getInitOrders(ChunkedImageActivity.this, chunkNumbers, false);
            int emptyOrder = ordersArr.removeLast();

            gameboard.setTileOrder(ordersArr);
            gameboard.setupImage(bmp, chunkNumbers, false);

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();

            Toast.makeText(ChunkedImageActivity.this, "خطا در دریافت عکس", Toast.LENGTH_LONG).show();
            onBackPressed();
            return false;
        }
    }

    private void makePuzzleBitmapAndShare() throws IOException {
        Bitmap bitmap = BitmapUtility.makePuzzle(gameboard.tiles, null); // TODO : look we use the tiles of resized image
        ArrayList<Integer> orders = new ArrayList<>(gameboard.tileOrder);
        orders.add(gameboard.emptyTileOrder);
        FileUtility.cacheAndShareBitmap(this, orders, bitmap, rbMode1.getTag().equals("checked") ? 1 : 2);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnRandomSlice:
                SfxPlayer.getInstance(this).Play(SfxResource.Random);
                RandGame();
                isRandomized = true;
                break;
            case R.id.btnSharePuzzle:
                if(!isRandomized){
                    Toast.makeText(this, "عکس را به هم بریزید...", Toast.LENGTH_LONG).show();
                    return;
                }
                SfxPlayer.getInstance(this).Play(SfxResource.Button);
                Toast.makeText(getApplicationContext(), "در حال آماده سازی جورچین...", Toast.LENGTH_LONG).show();
                    if (isFirstShare) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
                        alert.setCancelable(true);
                        alert.setTitle("ارسال جورچین");
                        alert.setMessage("آیا برای ارسال جورچین مطمئنید؟");

                        LinearLayout group = new LinearLayout(this);
                        group.setGravity(Gravity.CENTER_HORIZONTAL);
                        group.setOrientation(LinearLayout.VERTICAL);
                        // Set an EditText view to get user input
                        final ImageButton positiveBtn = new ImageButton(this);
                        positiveBtn.setAdjustViewBounds(true);
                        positiveBtn.setBackgroundColor(Color.TRANSPARENT);
                        positiveBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        positiveBtn.setImageResource(chunkNumbers == 3 ?  R.drawable.btn_one_token : chunkNumbers == 4 ? R.drawable.btn_two_token : R.drawable.btn_three_token);
                        group.addView(positiveBtn);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        positiveBtn.setLayoutParams(lp);
                        alert.setView(group);

                        final AlertDialog alertDialog = alert.create();
                        final DataHelper dataHelper = DataHelper.getInstance(this);
                        positiveBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int purchase = chunkNumbers == 3 ? 65 : chunkNumbers == 4 ? 85 : 105;
                                if (dataHelper.purchase(purchase)) {
                                    try {
                                        ChunkedImageActivity.this.makePuzzleBitmapAndShare();
                                        Toast.makeText(ChunkedImageActivity.this, String.format("توکن ها: %d", dataHelper.getLives()), Toast.LENGTH_SHORT).show();
                                        isFirstShare = false;
                                        alertDialog.dismiss();
                                    }
                                    catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(ChunkedImageActivity.this, "خطا در آماده سازی جورچین", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Intent intent = new Intent(ChunkedImageActivity.this, StoreActivity.class);
                                    startActivity(intent);
                                }
                            }
                        });
                        alertDialog.show();
                    } else {
                        try {
                            makePuzzleBitmapAndShare();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "خطا در آماده سازی جورچین", Toast.LENGTH_SHORT).show();
                        }
                    }
                break;
            case R.id.btnMode1:
            case R.id.btnMode2:
                SfxPlayer.getInstance(this).Play(SfxResource.Button);
                if (rbMode1.getTag().equals("checked")) {
                    rbMode1.setImageResource(R.drawable.mode1_off);
                    rbMode2.setImageResource(R.drawable.mode2_on);
                    rbMode1.setTag("unchecked");
                    rbMode2.setTag("checked");
                } else {
                    rbMode1.setImageResource(R.drawable.mode1_on);
                    rbMode2.setImageResource(R.drawable.mode2_off);
                    rbMode1.setTag("checked");
                    rbMode2.setTag("unchecked");
                }
                break;
        }
    }

    private void RandGame() {
        LinkedList<Integer> orders = BitmapUtility.getInitOrders(this, chunkNumbers, true);
        gameboard.emptyTileOrder = orders.removeLast();
        gameboard.setTileOrder(orders);
        gameboard.fillTiles();
    }

    @Override
    public void onBackPressed() {
        if(isFirstShare) {
            super.onBackPressed();
            return;
        }

        final  AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        dlg.setTitle("بازگشت به صفحه اصلی");
        dlg.setMessage("مطمئنی که نمیخوای جای دیگه جورچینت رو به اشتراک بذاری؟");
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
                Intent intent = new Intent(ChunkedImageActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
        dlg.show();
    }
}
