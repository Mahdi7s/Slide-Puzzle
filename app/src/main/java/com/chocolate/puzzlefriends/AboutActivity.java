package com.chocolate.puzzlefriends;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

/**
 * 
 * Screen containing information about the app.
 * 
 * @author David Vavra
 * 
 */
public class AboutActivity extends Activity implements View.OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

        findViewById(R.id.btnInstaPage).setOnClickListener(this);
        findViewById(R.id.btnFacePage).setOnClickListener(this);
	}

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnInstaPage:
                Uri uri = Uri.parse("http://instagram.com/choc01ate_land");
                Intent intent2 = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent2);
                break;
            case R.id.btnFacePage:
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/379632955493149"));
                    startActivity(intent);
                } catch(Exception e){
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/choc01ate")));
                }
                break;
        }
    }
}