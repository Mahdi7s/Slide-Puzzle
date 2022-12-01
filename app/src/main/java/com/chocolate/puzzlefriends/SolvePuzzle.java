package com.chocolate.puzzlefriends;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chocolate.puzzlefriends.Utils.BitmapUtility;
import com.chocolate.puzzlefriends.Utils.FileUtility;
import com.chocolate.puzzlefriends.Utils.SfxPlayer;
import com.chocolate.puzzlefriends.data.SfxResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class SolvePuzzle extends Activity implements View.OnClickListener {
    private final int[] galleryImages = {R.drawable.gallery_batman, R.drawable.gallery_brave, R.drawable.gallery_catdog, R.drawable.gallery_ice_age,
        R.drawable.gallery_madagascar, R.drawable.gallery_monster_inc, R.drawable.gallery_popeye, R.drawable.gallery_rango, R.drawable.gallery_road_runner,
        R.drawable.gallery_sonic, R.drawable.gallery_spong_bob, R.drawable.gallery_street_fighter, R.drawable.gallery_streets_of_rage,
        R.drawable.gallery_tmnt, R.drawable.gallery_tweety, R.drawable.gallery_wreck_it_raph};

    public static boolean puzzleLoaded = false;
    private static String state = null;
    private RelativeLayout gameboardParent = null;
    private LinearLayout galleryLayout = null;

    private GameboardView gameboardView = null;
    private GameboardViewMode2 gameboardViewMode2 = null;

    private ImageView imageView = null;
    public Bitmap originalImg = null;
    private ImageButton btnHelp = null;
    private TextView txtMoves, txtTime;

    public int timeSecs = 0;

    private Handler timeHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            ++timeSecs;
            txtTime.setText(String.format("%d:%02d", timeSecs/60, timeSecs%60));
            this.sendMessageDelayed(this.obtainMessage(), 1*1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solve_puzzle);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE); // preventing screenshot :)

        txtMoves = (TextView)findViewById(R.id.txtMoves);
        txtTime = (TextView)findViewById(R.id.txtTime);

        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/ADastNevis.ttf");
        txtMoves.setTypeface(tf);
        txtTime.setTypeface(tf);

        btnHelp = (ImageButton)findViewById(R.id.btnHelp);
        btnHelp.setOnClickListener(this);

        gameboardView = (GameboardView)findViewById(R.id.gameboard);
        gameboardView.solvePuzzle = this;

        gameboardViewMode2 = (GameboardViewMode2)findViewById(R.id.gameboard2);
        gameboardViewMode2.solvePuzzle = this;

        gameboardParent = (RelativeLayout)gameboardView.getParent();

        imageView = (ImageView)findViewById(R.id.imgHelp);

        gameboardView.txtMoves = txtMoves;
        gameboardViewMode2.txtMoves = txtMoves;

        galleryLayout = (LinearLayout)findViewById(R.id.galleryLayout);
        galleryLayout.setVisibility(View.GONE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) return;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initSolving();
            }
        }, 10);
    }

    private void initSolving() {
        final Intent intent = getIntent();

        if(state == null && !puzzleLoaded) {
            state = intent.getStringExtra("state");
        } else if(state != null && state.equals("friends")) {
            return;
        }

        switch (state){
            case "friends":
                Intent openImage = FileUtility.getSocialImageIntents(this, false);
                startActivityForResult(openImage, ChunkedImageActivity.REQUEST_IMAGE_CAPTURE);
                break;
            case "gallery":
                final Handler timeHandler2 = new Handler() {

                    @Override
                    public void handleMessage(Message msg) {
                        if(!SolvePuzzle.this.hasWindowFocus()){
                            this.sendMessageDelayed(this.obtainMessage(), 100);
                            return;
                        }
                        galleryLayout.setVisibility(View.VISIBLE);
                        final int mode = intent.getIntExtra("mode", 1);
                        final int difficulty = intent.getIntExtra("chunkNumbers", 3);

                        ImageButton btnRand = (ImageButton)findViewById(R.id.btnNewRandomImage);
                        btnRand.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                SfxPlayer.getInstance(SolvePuzzle.this).Play(SfxResource.Random);
                                selectRandomPuzzle(difficulty, mode);
                            }
                        });
                        ImageButton btnOK = (ImageButton)findViewById(R.id.btnOkImage);
                        btnOK.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                SfxPlayer.getInstance(SolvePuzzle.this).Play(SfxResource.Button);

                                galleryLayout.setVisibility(View.GONE);

                                gameboardView.isSolving = true;
                                gameboardViewMode2.isSolving = true;

                                timeHandler.sendMessageDelayed(timeHandler.obtainMessage(), 1*1000);
                            }
                        });
                        selectRandomPuzzle(difficulty, mode);

                        ScaleAnimation scale = new ScaleAnimation(0.6f, 1, 0.6f, 1, ScaleAnimation.RELATIVE_TO_SELF, .5f, ScaleAnimation.RELATIVE_TO_SELF, .5f);
                        scale.setDuration(1000);
                        scale.setRepeatCount(Animation.INFINITE);
                        scale.setRepeatMode(Animation.REVERSE);
                        scale.setInterpolator(new OvershootInterpolator());

                        btnRand.startAnimation(scale);
                        btnOK.startAnimation(scale);
                    }
                };
                timeHandler2.sendMessageDelayed(timeHandler2.obtainMessage(), 100);
                break;
            case "solve_img": // we came here from an intent , get image...
                Uri bmpUri = intent.getData();
                Bitmap bmp  = BitmapUtility.decodeScaledBitmapFromSdCard(FileUtility.getRealPathFromURI(this, bmpUri), gameboardView.getWidth(), gameboardView.getHeight());
                Map<Class, Object> retval = new HashMap<Class, Object>();
                retval.put(Uri.class, bmpUri);
                retval.put(Bitmap.class, bmp);

                startSolving(retval);
                break;
        }
    }

    private void selectRandomPuzzle(int difficulty, int mode) {
        if(btnHelp.getTag().equals("checked")){
            toggleHelp();
        }

        gameboardView.isSolving = true;
        gameboardViewMode2.isSolving = true;

        int imgId = galleryImages[(int) (Math.random() * galleryImages.length)];
        Bitmap bmp = BitmapUtility.decodeScaledBitmapFromResource(getResources(), imgId, gameboardView.getWidth(), gameboardView.getHeight());
        ArrayList<Integer> ordersArr = new ArrayList<Integer>(BitmapUtility.getInitOrders(this, difficulty, true));
        int emptyOrder = ordersArr.remove(ordersArr.size()-1);
        setOriginalImage(bmp, ordersArr, difficulty, true);
        if (mode == 1) {
            gameboardView.emptyTileOrder = emptyOrder;
            setupMode1(bmp, ordersArr, difficulty, true);
        } else if(mode == 2) {
            setupMode2(bmp, ordersArr, difficulty, true);
        }

        gameboardView.isSolving = false;
        gameboardViewMode2.isSolving = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Map<Class, Object> map = FileUtility.onImageResult(this, requestCode, resultCode, data, gameboardView.getWidth(), gameboardView.getHeight());
        puzzleLoaded = startSolving(map);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        state = null;
    }


    private boolean startSolving(Map<Class, Object> map) {
        if (map != null) { // split image
            try {
                Bitmap bmp = (Bitmap) map.get(Bitmap.class);
                Uri uri = (Uri) map.get(Uri.class);
                ArrayList<Integer> orders = null;
                int mode = 1;

                Map<String, Object> map1 = FileUtility.getImageUserAttrs(this, uri);
                orders = (ArrayList<Integer>) map1.get("orders");
                int emptyORder = orders.remove(orders.size()-1);
                mode = (int) map1.get("mode");
                bmp = (Bitmap) map1.get("originalImage");

                int chunkNumbers = (int) map1.get("gridSize"); //Math.sqrt(orders.size());
                setOriginalImage(bmp, orders, chunkNumbers, false);

                if (mode == 1) {
                    gameboardView.emptyTileOrder = orders.indexOf(emptyORder);
                    setupMode1(bmp, orders, chunkNumbers, false);
                } else if (mode == 2) {
                    setupMode2(bmp, orders, chunkNumbers, false);
                }

                timeHandler.sendMessageDelayed(timeHandler.obtainMessage(), 1 * 1000);

                return true;
            } catch (Exception e) {
                e.printStackTrace();

                Toast.makeText(this, "جورچین انتخاب شده پشتیبانی نمیشود", Toast.LENGTH_LONG).show();
                onBackPressed();
                return false;
            }
        } else {
            Toast.makeText(this, "بروز خطا در دریافت عکس", Toast.LENGTH_LONG).show();
            onBackPressed();
            return false;
        }
    }

    private void setupMode1(Bitmap bmp, ArrayList<Integer> orders, int chunkNumbers, boolean replaceInitOrders) {
        gameboardView.boardCreated = false;
        gameboardView.isSolving = true;
        LinkedList<Integer> initOrders = BitmapUtility.getInitOrders(this, chunkNumbers, false);
        int emptyOrder = initOrders.removeLast();

        gameboardView.tileOrder = replaceInitOrders ? new LinkedList<>(orders) : initOrders;
        gameboardView.correctOrders = replaceInitOrders ? initOrders : new LinkedList<Integer>(orders);
        gameboardView.setupImage(bmp, chunkNumbers, false);

        gameboardParent.removeView(gameboardViewMode2);
    }

    private void setupMode2(Bitmap bmp, ArrayList<Integer> orders, int chunkNumbers, boolean replaceInitOrders) {
        gameboardViewMode2.boardCreated = false;
        LinkedList<Integer> initOrders = BitmapUtility.getInitOrders(this, chunkNumbers, false);
        int emptyOrder = initOrders.removeLast();

        gameboardViewMode2.tileOrder = replaceInitOrders ? new LinkedList<>(orders) : initOrders;
        gameboardViewMode2.correctOrders = replaceInitOrders ? initOrders : new LinkedList<Integer>(orders);
        gameboardViewMode2.setupImage(bmp, chunkNumbers, false);

        gameboardParent.removeView(gameboardView);
    }

    private void setOriginalImage(Bitmap bmp, ArrayList<Integer> orders, int chunkNumbers, boolean replaceInitOrders) {
        gameboardView.isSolving = false;
        LinkedList<Integer> initOrders = BitmapUtility.getInitOrders(this, chunkNumbers, false);
        int emptyOrder = initOrders.removeLast();
        gameboardView.emptyTileOrder = emptyOrder;
        gameboardView.tileOrder = replaceInitOrders ? new LinkedList<>(orders) : initOrders;
        gameboardView.setupImage(bmp, chunkNumbers, true);
        originalImg = BitmapUtility.makePuzzle(gameboardView.tiles, orders);
    }

    public void showWin(int moves, int mode){
        startActivity(WinBoardActivity.createWinIntent(this, originalImg, moves, timeSecs, mode, (int) Math.sqrt(gameboardView.tileOrder.size()), state.equals("gallery")));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_solve_puzzle, menu);
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
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnHelp:
                SfxPlayer.getInstance(this).Play(SfxResource.Tip);

                toggleHelp();
                break;
        }
    }

    private void toggleHelp() {
        imageView.setImageBitmap(originalImg);
        if(btnHelp.getTag().equals("unchecked")){
            imageView.setVisibility(View.VISIBLE);

            btnHelp.setImageResource(R.drawable.btn_hide);
            btnHelp.setTag("checked");
        }else{
            imageView.setVisibility(View.INVISIBLE);

            btnHelp.setImageResource(R.drawable.btn_show);
            btnHelp.setTag("unchecked");
        }
    }
}
