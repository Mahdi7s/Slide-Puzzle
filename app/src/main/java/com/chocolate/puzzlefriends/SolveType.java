package com.chocolate.puzzlefriends;

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.chocolate.puzzlefriends.Utils.SfxPlayer;
import com.chocolate.puzzlefriends.data.SfxResource;


public class SolveType extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solve_type);

        findViewById(R.id.button_sFriends).setOnClickListener(this);
        findViewById(R.id.button_sGallery).setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_solve_type, menu);
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
            case R.id.button_sFriends:
                findViewById(R.id.button_sFriends).setEnabled(false);
                SfxPlayer.getInstance(this).Play(SfxResource.Button);

                Intent intent = new Intent(this, SolvePuzzle.class);
                intent.putExtra("state", "friends"); //Optional parameters
                SolvePuzzle.puzzleLoaded = false;
                startActivity(intent);
                break;
            case R.id.button_sGallery:
                findViewById(R.id.button_sGallery).setEnabled(false);
                SfxPlayer.getInstance(this).Play(SfxResource.Button);

                Intent intent2 = new Intent(this, Difficulty.class);
                intent2.putExtra("state", "gallery"); //Optional parameters
                startActivity(intent2);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        findViewById(R.id.button_sFriends).setEnabled(true);
        findViewById(R.id.button_sGallery).setEnabled(true);
    }
}
