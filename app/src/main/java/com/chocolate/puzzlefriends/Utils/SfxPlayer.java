package com.chocolate.puzzlefriends.Utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

import com.chocolate.puzzlefriends.R;
import com.chocolate.puzzlefriends.data.SfxResource;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ariana Gostar on 4/19/2015.
 */
public class SfxPlayer {
    private static SfxPlayer instance = null;
    public static boolean muted = false;

    private Map<SfxResource, Integer> sfxIds;
    private SoundPool soundPool;

    public static SfxPlayer getInstance(Context context){
        if(instance == null){
            instance = new SfxPlayer(context);
        }
        return instance;
    }

    public void Play(SfxResource sfxResource) {
        if(!muted) {
            soundPool.play(sfxIds.get(sfxResource), 1, 1, 0, 0, 1);
        }
    }

    private SfxPlayer(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createNewSoundPool();
        }else{
            createOldSoundPool();
        }

        sfxIds = new HashMap<>();
        sfxIds.put(SfxResource.Button, soundPool.load(context, R.raw.sfx_button, 1));
        sfxIds.put(SfxResource.Random, soundPool.load(context, R.raw.sfx_random, 1));
        sfxIds.put(SfxResource.Selecting, soundPool.load(context, R.raw.sfx_selecting, 1));
        sfxIds.put(SfxResource.Sliding, soundPool.load(context, R.raw.sfx_sliding, 1));
        sfxIds.put(SfxResource.Tip, soundPool.load(context, R.raw.sfx_tip, 1));
        sfxIds.put(SfxResource.Win, soundPool.load(context, R.raw.sfx_win, 1));
        sfxIds.put(SfxResource.Store, soundPool.load(context, R.raw.sfx_store, 1));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void createNewSoundPool(){
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
    }

    @SuppressWarnings("deprecation")
    protected void createOldSoundPool(){
        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC,0);
    }
}
