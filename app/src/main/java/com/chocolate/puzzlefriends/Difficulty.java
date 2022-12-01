package com.chocolate.puzzlefriends;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.chocolate.puzzlefriends.Utils.SfxPlayer;
import com.chocolate.puzzlefriends.data.SfxResource;

public class Difficulty extends Activity implements View.OnClickListener {
    private String state = "";
    private LinearLayout layoutMode;
    private ImageButton rbMode1;
    private ImageButton rbMode2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_difficulty);

        state = getIntent().getStringExtra("state");

        ImageView imgBrain = (ImageView)findViewById(R.id.imgBrain);
        imgBrain.setVisibility(View.VISIBLE);

        layoutMode = (LinearLayout)findViewById(R.id.layoutMode);
        layoutMode.setVisibility(View.GONE);
        if(state.equals("gallery")) {
            imgBrain.setVisibility(View.GONE);
            layoutMode.setVisibility(View.VISIBLE);

            rbMode1 = (ImageButton) findViewById(R.id.btnMode11);
            rbMode2 = (ImageButton) findViewById(R.id.btnMode22);

            rbMode1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeGameMode();
                }
            });
            rbMode2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeGameMode();
                }
            });
        }

        findViewById(R.id.button_d3).setOnClickListener(this);
        findViewById(R.id.button_d4).setOnClickListener(this);
        findViewById(R.id.button_d5).setOnClickListener(this);


 */
    }

    private void changeGameMode(){
        SfxPlayer.getInstance(this).Play(SfxResource.Button);

        if(rbMode1.getTag().equals("checked")){
            rbMode1.setImageResource(R.drawable.mode1_off);
            rbMode2.setImageResource(R.drawable.mode2_on);
            rbMode1.setTag("unchecked");
            rbMode2.setTag("checked");
        }else{
            rbMode1.setImageResource(R.drawable.mode1_on);
            rbMode2.setImageResource(R.drawable.mode2_off);
            rbMode1.setTag("checked");
            rbMode2.setTag("unchecked");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_difficulty, menu);
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
    protected void onResume() {
        super.onResume();

        findViewById(R.id.button_d3).setEnabled(true);
        findViewById(R.id.button_d4).setEnabled(true);
        findViewById(R.id.button_d5).setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        v.setEnabled(false);
        SfxPlayer.getInstance(this).Play(SfxResource.Button);
        int difficulty = 0;
        switch (v.getId()){
            case R.id.button_d3:
                difficulty = 3;
                break;
            case R.id.button_d4:
                difficulty = 4;
                break;
            case R.id.button_d5:
                difficulty = 5;
                break;
        }

        if(difficulty>0)
        {
            if(state.equals("create_puzzle") || state.equals("intent_image")) {
                Intent intent = new Intent(this, ChunkedImageActivity.class);
                intent.putExtra("state", state);
                intent.putExtra("chunkNumbers", difficulty);
                intent.setData(getIntent().getData());
                ChunkedImageActivity.puzzleLoaded = false;
                startActivity(intent);
            } else if(state.equals("gallery")){
                Intent intent2 = new Intent(this, SolvePuzzle.class);
                intent2.putExtra("state", state);
                intent2.putExtra("chunkNumbers", difficulty);
                intent2.putExtra("mode", rbMode1.getTag().equals("checked") ? 1 : 2);
                startActivity(intent2);
            }
        }
    }
}
