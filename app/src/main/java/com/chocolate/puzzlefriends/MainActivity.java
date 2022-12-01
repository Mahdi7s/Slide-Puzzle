package com.chocolate.puzzlefriends;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.chocolate.puzzlefriends.Utils.SfxPlayer;
import com.chocolate.puzzlefriends.data.SfxResource;


public class MainActivity extends Activity implements View.OnClickListener {
    public static MainActivity instance = null;
    private ImageButton soundButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        findViewById(R.id.button_createPuzzle).setOnClickListener(this);
        findViewById(R.id.button_solve).setOnClickListener(this);
        findViewById(R.id.helpButton).setOnClickListener(this);
        findViewById(R.id.aboutButton).setOnClickListener(this);

        soundButton = (ImageButton)findViewById(R.id.toggleButton);
        soundButton.setTag("on");
        soundButton.setOnClickListener(this);

        SfxPlayer.getInstance(this);

        showHelpOnFirstRun();

        setMuteButton(getMuteState());
    }

    @Override
    protected void onResume() {
        super.onResume();

        findViewById(R.id.button_createPuzzle).setEnabled(true);
        findViewById(R.id.button_solve).setEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public void onBackPressed(){
        finish();
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
            case R.id.button_createPuzzle:
                findViewById(R.id.button_createPuzzle).setEnabled(false);
                SfxPlayer.getInstance(this).Play(SfxResource.Button);

                Intent intent = new Intent(this, Difficulty.class);
                intent.putExtra("state", "create_puzzle"); //Optional parameters
                startActivity(intent);
                break;
            case R.id.button_solve:
                findViewById(R.id.button_solve).setEnabled(false);
                SfxPlayer.getInstance(this).Play(SfxResource.Button);

                Intent intent1 = new Intent(this, SolveType.class);
                startActivity(intent1);
                break;
            case R.id.toggleButton:
                toggleMuteState();
                break;
            case R.id.helpButton:
                SfxPlayer.getInstance(this).Play(SfxResource.Button);

                Intent intent2 = new Intent(this, HelpActivity.class);
                startActivity(intent2);
                break;
            case R.id.aboutButton:
                SfxPlayer.getInstance(this).Play(SfxResource.Button);

                Intent intent3 = new Intent(this, AboutActivity.class);
                startActivity(intent3);
                break;
        }
    }

    private void toggleMuteState() {
        if(soundButton.getTag().equals("on")){
            setMuteButton(true);
        } else {
            setMuteButton(false);
        }
    }

    private void setMuteButton(boolean mute){
        soundButton.setImageResource(mute ? R.drawable.btn_sound_off : R.drawable.btn_sound_on);
        soundButton.setTag(mute ? "off" : "on");
        SfxPlayer.muted = mute;
        setMuteState(mute);
    }

    private void showHelpOnFirstRun() {
        if(!isAppFirstRun())return;

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("به بفرمایید جورچین خوش آمدید")
                .setMessage("شما برای بار اول است که بازی را اجرا نموده اید، آیا مایل به مطالعه آموزش بازی هستید؟(این جورچین متفاوت است!!!)")
                .setPositiveButton("بله", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, HelpActivity.class);
                        startActivity(intent);
                    }
                })
                .show();
    }

    private boolean isAppFirstRun(){
        final String key = "firstRun";
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        boolean retval = Boolean.parseBoolean(sharedPreferences.getString(key, Boolean.toString(true)));
        if(retval) {
            SharedPreferences.Editor spEdit = sharedPreferences.edit();
            spEdit.putString(key, Boolean.toString(false));
            spEdit.commit();
        }
        return retval;
    }

    private boolean getMuteState(){
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean("MuteState", false);
    }

    private void setMuteState(boolean mute){
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor spEdit = sharedPreferences.edit();
        spEdit.putBoolean("MuteState", mute);
        spEdit.commit();
    }
}
