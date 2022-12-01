package com.chocolate.puzzlefriends.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Ariana Gostar on 3/30/2015.
 */
public class TextImageParcelable implements Parcelable {
    private String text;
    private Uri image;

    /**
     * Constructs a Question from values
     */
    public TextImageParcelable (Uri image, String text) {
        this.text = text;
        this.image = image;
    }

    /**
     * Constructs a Question from a Parcel
     * @param parcel Source Parcel
     */
    public TextImageParcelable (Parcel parcel) {
        this.text = parcel.readString();
        //this.image = parcel.readParcelable(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Required method to write to Parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeParcelable(image, flags);
    }

    // Method to recreate a Question from a Parcel
    public static Creator<TextImageParcelable> CREATOR = new Creator<TextImageParcelable>() {

        @Override
        public TextImageParcelable createFromParcel(Parcel source) {
            return new TextImageParcelable(source);
        }

        @Override
        public TextImageParcelable[] newArray(int size) {
            return new TextImageParcelable[size];
        }

    };
}
