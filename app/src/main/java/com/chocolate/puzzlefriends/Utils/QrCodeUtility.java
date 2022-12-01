package com.chocolate.puzzlefriends.Utils;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.provider.CalendarContract;
import android.text.TextUtils;

import com.chocolate.puzzlefriends.MainActivity;
import com.chocolate.puzzlefriends.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.aztec.encoder.Encoder;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.QRCode;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ariana Gostar on 4/8/2015.
 */
public class QrCodeUtility {
    public static int QrCodeSize = 49;
    private static int margin = 0;// one pix for left, top & one pix for right , bottom
    private static int qrSegTopMargin = 0;

    public static Bitmap attachQrCodeToBitmap(Context context, Bitmap originalBmp, String qrCode){
        return attachQrCodeToBitmap(context, originalBmp, convertToQrCodeBitmap(qrCode));
    }

    public static Bitmap attachQrCodeToBitmap(Context context, Bitmap originalBmp, Bitmap qrBmp){
        Bitmap retBmp = Bitmap.createBitmap(originalBmp.getWidth() + margin, originalBmp.getHeight() + qrSegTopMargin + qrBmp.getHeight() + margin, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(retBmp);
        canvas.drawBitmap(originalBmp, margin, margin, null);

        Paint rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setColor(Color.WHITE);
        canvas.drawRect(margin, margin + originalBmp.getHeight() + qrSegTopMargin, originalBmp.getWidth() - margin,
                originalBmp.getHeight() + qrSegTopMargin + QrCodeSize + margin, rectPaint);

        canvas.drawBitmap(qrBmp, margin, margin + originalBmp.getHeight() + qrSegTopMargin, null);

        Bitmap adsBmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.ads);
        adsBmp = Bitmap.createScaledBitmap(adsBmp, Math.min(originalBmp.getWidth() - (QrCodeSize + 10), adsBmp.getWidth()), Math.min(QrCodeSize, adsBmp.getHeight()), true);
        canvas.drawBitmap(adsBmp, ((originalBmp.getWidth()-QrCodeSize)/2 - adsBmp.getWidth()/2) + QrCodeSize, originalBmp.getHeight() + (QrCodeSize/2 - adsBmp.getHeight()/2), rectPaint);

        canvas.save();

        return retBmp;
    }

    public static Point getFinalBitmapSize(Bitmap originalBmp){
        return new Point(originalBmp.getWidth() + margin, originalBmp.getHeight() + qrSegTopMargin + QrCodeSize + margin);
    }

    public static Map<String, Object> getOriginalImageAndQrCode(Bitmap wholeBmp){
        Map<String, Object> retval = null;
        try {
            Bitmap qrCodeBmp = Bitmap.createBitmap(wholeBmp, 0, wholeBmp.getHeight() - (QrCodeSize + margin), QrCodeSize + margin, QrCodeSize + margin);
            String qrCodeData = getQrCodeData(qrCodeBmp);
            ArrayList<Integer> orders = new ArrayList<Integer>();
            if (qrCodeData != null && qrCodeData.startsWith("PF-")) {
                String[] slices = qrCodeData.split("-");
                Integer chunkNum = Integer.parseInt(slices[1]);
                Integer mode = Integer.parseInt(slices[2]);
                for (String order : slices[3].split(",")) {
                    orders.add(Integer.parseInt(order));
                }
                retval = new HashMap<String, Object>();
                retval.put("gridSize", chunkNum);
                retval.put("mode", mode);
                retval.put("orders", orders);
                retval.put("imgRes", slices[4] + "*" + slices[5]);
                int prevW = Integer.parseInt(slices[4]),
                        prevH = Integer.parseInt(slices[5]);
                int newH = ((prevH - QrCodeSize) * wholeBmp.getHeight()) / prevH;
                Bitmap originalBmp = Bitmap.createBitmap(wholeBmp, 0, 0, wholeBmp.getWidth(), newH);
                retval.put("originalImage", originalBmp);
            }
        }
        catch (Exception ex){
            retval = null;
        }
        return retval;
    }

    public static String getQrCodeData(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        bitmap.recycle();
        bitmap = null;
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
        MultiFormatReader reader = new MultiFormatReader();
        try {
            Result result = reader.decode(bBitmap);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    // http://codeisland.org/2013/generating-qr-codes-with-zxing/
    public static Bitmap convertToQrCodeBitmap(String data){
        QRCodeWriter writer = new QRCodeWriter();
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
            hints.put(EncodeHintType.MARGIN, 1); /* default = 4 */
            BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, QrCodeSize, QrCodeSize, hints);
            return toBitmap(matrix);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Writes the given Matrix on a new Bitmap object.
     * @param matrix the matrix to write.
     * @return the new {@link android.graphics.Bitmap}-object.
     */
    private static Bitmap toBitmap(BitMatrix matrix){
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                bmp.setPixel(x, y, matrix.get(x,y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }
}
