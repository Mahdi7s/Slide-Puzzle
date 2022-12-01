package com.chocolate.puzzlefriends;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.chocolate.puzzlefriends.Utils.SfxPlayer;
import com.chocolate.puzzlefriends.data.SfxResource;

/**
 * Created by Ariana Gostar on 3/28/2015.
 */
public class IntentSelection extends Activity implements View.OnClickListener {
    //public static IntentSelection instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intent_selection);

        findViewById(R.id.btnCreateImgPuzzle).setOnClickListener(this);
        findViewById(R.id.btnSolveImgPuzzle).setOnClickListener(this);

        //instance = this;
    }

    @Override
    public void onClick(View v) {
        Intent intent = getIntent();
        // Figure out what to do based on the intent type
        if (intent.getType().indexOf("image/") != -1) {
            v.setEnabled(false);
            SfxPlayer.getInstance(this).Play(SfxResource.Button);

            Uri data = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
//            try {
                    switch (v.getId()) {
                        case R.id.btnCreateImgPuzzle:

                            intent = new Intent(this, Difficulty.class);
                            intent.putExtra("state", "intent_image"); //Optional parameters
                            intent.setData(data);
                            startActivity(intent);
                            break;
                        case R.id.btnSolveImgPuzzle:
                            if(data != null /*&& FileUtility.isValidPuzzleImage(this, data)*/) {
                                intent = new Intent(this, SolvePuzzle.class);
                                intent.putExtra("state", "solve_img"); //Optional parameters
                                intent.setData(data);
                                startActivity(intent);
                            }else {
                                Toast.makeText(this, "خطا: جورچین انتخاب شده توسط برنامه پشتیبانی نمیشود", Toast.LENGTH_LONG).show();
                            }
                            break;
                    }
//                } catch (IOException e) {
//                    Toast.makeText(this, "خطا در بارگزاری عکس", Toast.LENGTH_LONG).show();
//                    e.printStackTrace();
//                }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        findViewById(R.id.btnCreateImgPuzzle).setEnabled(true);
        findViewById(R.id.btnSolveImgPuzzle).setEnabled(true);
    }
}
