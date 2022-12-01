package com.chocolate.puzzlefriends.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.chocolate.puzzlefriends.R;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by MDB on 4/25/2015.
 */
public class DataHelper {
    //public static final int UnlimitedLives = -100;

    private static DataHelper instance = null;
    private static String mPassword = null;
    private static AesCbcWithIntegrity.SecretKeys secretKeys = null;

    private Context appContext;
    private SharedPreferences sharedPreferences;
    private final int freeTokenHours = 8;
    private final int defaultRemainedPuzzles = 0;
    private final String livesKey = "lives";
    private final String freeTokenKey = "freeToken";
    private final String giftGottenKey = "giftGotten";
    private final String remainedGiftKey = "giftGotten";

    private DateTimeFormatter dateTimeFormatter = DateTimeFormat.mediumDateTime();
    private DateTime lastUpdatedDate;

    private Map<String, String> dataMap;

    private DataHelper(Context context) {
        this.appContext = context.getApplicationContext();
        sharedPreferences = this.appContext.getSharedPreferences("app_data", Context.MODE_PRIVATE);
        dataMap = new HashMap<>();
    }

    public static DataHelper getInstance(Context context){
        if(instance == null){
            instance = new DataHelper(context);
        }
        return instance;
    }

    public DateTime getLastUpdatedDate(){
        return lastUpdatedDate;
    }

    public boolean purchase(int tokenCount){
        int lives = getLives();
//        if(lives == UnlimitedLives){
//            return true;
//        }else{
            if(lives - tokenCount >= 0){
                setLives(lives - tokenCount);
                return true;
            }
            return false;
//        }
    }

    public int getLives() {
        int lives = 0;
        try {
            lives = Integer.parseInt(getData(livesKey));
        }
        catch (Exception ex){
            lives = 50;
            setData(livesKey, lives+"");
        }
        return lives;
    }

    public void setLives(int lives) {
        //if(getLives() != UnlimitedLives) {
            setData(livesKey, lives + "");
        //}
    }

    // ------------------------Free Token-------------------------

    public boolean canGetFreeToken() {
        DateTime lastFreeToken = getLastFreeTokenDate();
        return lastFreeToken.plusHours(freeTokenHours).isBeforeNow();
    }

    public int getFreeToken() {
        int retval = getLives()+10;
        setLives(retval);
        resetLastFreeTokenDate();
        return retval;
    }

    private DateTime getLastFreeTokenDate() {
        DateTime retval = null;
        try {
            retval = DateTime.parse(getData(freeTokenKey), dateTimeFormatter);
        } catch (Exception e) {
            e.printStackTrace();
            retval = resetLastFreeTokenDate();
        }
        return retval;
    }

    private DateTime resetLastFreeTokenDate(){
        DateTime date = DateTime.now();
        setData(freeTokenKey, date.toString(dateTimeFormatter));
        return date;
    }

    // ------------------------ Gift -------------------------

    public boolean canGetGift(){
        return !isGiftGotten() && getRemainedPuzzlesToGift() <= 0;
    }

    public boolean isGiftGotten(){
        boolean retval = Boolean.parseBoolean(getData(giftGottenKey));
        return retval;
    }

    public void setGiftGotten() {
        setData(giftGottenKey, Boolean.toString(true));
    }

    public int getGift(){
        int retval = getLives()+300;
        setLives(retval);
        setData(giftGottenKey, Boolean.toString(true));
        return retval;
    }

    public int getRemainedPuzzlesToGift(){
        int retval = defaultRemainedPuzzles;
        try {
            retval = Integer.parseInt(getData(remainedGiftKey));
        }
        catch (Exception ex){
            ex.printStackTrace();
            retval = defaultRemainedPuzzles;
            setData(remainedGiftKey, defaultRemainedPuzzles+"");
        }
        return retval;
    }

    public int decreaseRemainedGiftPuzzles() {
        int retval = getRemainedPuzzlesToGift()-1;
        if(retval >= 0) {
            setData(remainedGiftKey, retval + "");
        }
        return retval;
    }

    public boolean isStoreDirty() {
        return canGetFreeToken() || canGetGift();
    }

    // ------------------------ Helpers -------------------------

    public void setData(String prop, String val) {
        SharedPreferences.Editor spEdit = sharedPreferences.edit();
        spEdit.putString(prop, encrypt(val));
        spEdit.commit();
        dataMap.put(prop, val);
        lastUpdatedDate = DateTime.now();
    }

    public String getData(String prop) {
        if(dataMap.containsKey(prop)){
            return dataMap.get(prop);
        }

        String retval = sharedPreferences.getString(prop, null);
        if(retval != null){
            retval = decrypt(retval);
        }
        return retval;
    }

    private String encrypt(String str) {
        String retval = null;
        try {
            String passStr = getPassword(true);
            AesCbcWithIntegrity.CipherTextIvMac cipher = AesCbcWithIntegrity.encrypt(str, getSecretKeys());
            retval = cipher.toString();
        }
        catch (GeneralSecurityException ex){
            Log.d("Encryption", ex.getMessage());
        }
        catch (UnsupportedEncodingException ex2){
            Log.d("Encryption", ex2.getMessage());
        }
        return retval;
    }

    private String decrypt(String str) {
        String retval = null;
        try {
            AesCbcWithIntegrity.CipherTextIvMac cipher = new AesCbcWithIntegrity.CipherTextIvMac(str);
            String password = getPassword(false);
            retval = AesCbcWithIntegrity.decryptString(cipher, getSecretKeys());
        }
        catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return retval;
    }

    private AesCbcWithIntegrity.SecretKeys getSecretKeys() throws InvalidKeyException {
        if(secretKeys == null){
            secretKeys = AesCbcWithIntegrity.keys(getPassword(false));
        }
        return secretKeys;
    }

    private String getPassword(boolean updateSalt) {
        if(mPassword == null) {
            String salt = sharedPreferences.getString("salt", null);

            try {
                if (updateSalt || salt == null) {
                    salt = AesCbcWithIntegrity.saltString(AesCbcWithIntegrity.generateSalt());
                    SharedPreferences.Editor spE = sharedPreferences.edit();
                    spE.putString("salt", salt);
                    spE.commit();
                }

                AesCbcWithIntegrity.SecretKeys password = AesCbcWithIntegrity.generateKeyFromPassword(appContext.getString(R.string.enc_key), salt);
                mPassword = AesCbcWithIntegrity.keyString(password);
            } catch (GeneralSecurityException ex) {
                Log.d("salt ex", ex.getMessage());
            }
        }
        return mPassword;
    }
}
