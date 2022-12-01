package com.chocolate.puzzlefriends.Utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.chocolate.puzzlefriends.ChunkedImageActivity;
import com.chocolate.puzzlefriends.GameboardView;
import com.chocolate.puzzlefriends.MainActivity;
import com.chocolate.puzzlefriends.R;
import com.chocolate.puzzlefriends.TileView;
import com.chocolate.puzzlefriends.data.TextImageParcelable;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.nereo.multi_image_selector.MultiImageSelectorActivity;
import me.nereo.multi_image_selector.MultiImageSelectorFragment;

/**
 * Created by Mahdi7s on 12/27/2014.
 */
public final class FileUtility {
    public static Uri outputFileUri;

    private static String getUniqueImageFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "pf-" + timeStamp;

        return imageFileName;
    }

    public static Intent getSocialImageIntents(Context context, boolean camera) {
        camera = false;
        Map<String, String> intentSocialPackages = new HashMap<String, String>();
        intentSocialPackages.put("twitter", null);
        intentSocialPackages.put("facebook", null);
        //intentSocialPackages.put("instagram", "/Pictures/Instagram/");
        intentSocialPackages.put("viber", "/viber/media/Viber Images/");
        intentSocialPackages.put("whatsapp", "/WhatsApp/Media/WhatsApp Images/");
        intentSocialPackages.put("hike", "/Hike/Media/hike Images/");
        intentSocialPackages.put("telegram", "/Pictures/Telegram/");
        intentSocialPackages.put("line", "/Pictures/LINE/");
        intentSocialPackages.put("lenzor", null);
        intentSocialPackages.put("tango", "/DCIM/Tango/");

        List<Intent> intentsList = new ArrayList<Intent>();
        if (camera) {
            intentsList.addAll(0, getCameraIntents(context));
        }
        Intent socialIntents = new Intent(Intent.ACTION_SEND);
        socialIntents.setType("image/*");

        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(socialIntents, 0);

        for (String keyName : intentSocialPackages.keySet()) {
            for (ResolveInfo resolveInfo : resolveInfos) {
                String packName = resolveInfo.activityInfo.packageName;
                if (packName.contains(keyName)) {
                    String folderPath = intentSocialPackages.get(keyName);

                    if (folderPath != null && checkSocialDirExists(folderPath)) {
                        Intent intent = new Intent(context, MultiImageSelectorActivity.class);

                        //ComponentName cn =  galleryIntent.resolveActivity(packageManager);
                        //intent.setComponent(cn);

                        //intent.setComponent(new ComponentName(cn.getPackageName(), cn.getClassName()));
                        //intent.setPackage(cn.getPackageName());
                        //intent.setClassName(cn.getPackageName(), cn.getClassName());
                        intent.putExtra("path", Environment.getExternalStorageDirectory().getPath() + folderPath);
                        intent.putExtra(MultiImageSelectorActivity.EXTRA_SHOW_CAMERA, false);
                        intent.putExtra(MultiImageSelectorActivity.EXTRA_SELECT_MODE, MultiImageSelectorActivity.MODE_SINGLE);

                        //intent.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getPath() + folderPath), "*/*");
                        intentsList.add(new LabeledIntent(intent, packName, resolveInfo.loadLabel(packageManager), resolveInfo.getIconResource()));
                    }
                }
            }
        }


        Intent chooserIntent = Intent.createChooser(galleryIntent, "برای وارد کردن عکس یکی را انتخاب کنید");
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentsList.toArray(new Parcelable[]{}));

        return chooserIntent;
    }

    private static boolean checkSocialDirExists(String path) {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + path);
        return file.isDirectory();
    }

    public static Map<String, Object> getImageUserAttrs(Context context, Uri file) throws IOException {
        return QrCodeUtility.getOriginalImageAndQrCode(BitmapFactory.decodeFile(getRealPathFromURI(context, file)));
    }

    public static Map<Class, Object> onImageResult(Context context, int requestCode, int resultCode, Intent data, int reqWidth, int reqHeight) {
        Map<Class, Object> retval = null;
        try {
            if (resultCode == ChunkedImageActivity.RESULT_OK) {
                if (requestCode == ChunkedImageActivity.REQUEST_IMAGE_CAPTURE) {
                    final boolean isCamera;
                    if (data == null) {
                        isCamera = true;
                    } else {
                        final String action = data.getAction();
                        if (action == null) {
                            isCamera = false;
                        } else {
                            isCamera = action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                        }
                    }

                    Uri selectedImageUri;
                    if (isCamera) {
                        selectedImageUri = outputFileUri;
                    } else if (data.getAction() != null && data.getAction().equals(MultiImageSelectorActivity.ACTION_IMAGE_SEL_SP)) {
                        selectedImageUri = Uri.parse("file://" + data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT).get(0));
                    } else {
                        selectedImageUri = data == null ? null : data.getData();
                    }
                    if (selectedImageUri != null) {
                        //Bitmap bmp = BitmapFactory.decodeFile(FileUtility.getRealPathFromURI(context, selectedImageUri));
                        Bitmap bmp = BitmapUtility.decodeScaledBitmapFromSdCard(FileUtility.getRealPathFromURI(context, selectedImageUri), reqWidth, reqHeight);
                        retval = new HashMap<Class, Object>();
                        retval.put(Uri.class, selectedImageUri);
                        retval.put(Bitmap.class, bmp);
                    }
                }
            }
        } catch (Exception ex) {
            retval = null;
        }
        return retval;
    }

    public static Intent getImageIntent(Context context, boolean camera) {
        final List<Intent> cameraIntents = getCameraIntents(context);

        // Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "برای وارد کردن عکس یکی را انتخاب کنید");
        if (camera) {
            // Add the camera options.
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
        }
        return chooserIntent;
    }

    private static List<Intent> getCameraIntents(Context context) {
        // Determine Uri of camera image to save.
        final File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) /*+ File.separator + "PuzzleFriends"*/ + File.separator);
        if (!root.exists())
            root.mkdirs();
        final String fname = getUniqueImageFilename();
        final File sdImageMainDirectory = new File(root, fname);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

        // Camera.
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }
        return cameraIntents;
    }

    public static void cacheAndShareBitmap(Context context, List<Integer> tileOrder, Bitmap bitmap, int mode) {
        // Save this bitmap to a file.
        Point finalImgSize = QrCodeUtility.getFinalBitmapSize(bitmap);
        String qrCodeData = String.format("PF-%d-%d-%s-%d-%d", (int) Math.sqrt(tileOrder.size() - 1), mode, TextUtils.join(",", tileOrder), finalImgSize.x, finalImgSize.y);
        Bitmap finalBitmap = QrCodeUtility.attachQrCodeToBitmap(context, bitmap, qrCodeData);

        shareBitmap(context, finalBitmap, false);
    }

    public static void shareBitmap(Context context, Bitmap finalBitmap, boolean winShare) {
        shareBitmap(context, finalBitmap, winShare, 100);
    }

    public static void shareBitmap(Context context, Bitmap finalBitmap, boolean winShare, int quality) {
        File cache = context.getExternalCacheDir();
        File sharefile = new File(cache, "toshare.jpg");
        try {
            FileOutputStream out = new FileOutputStream(sharefile);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            out.flush();
            out.close();
        } catch (IOException e) {

        }

        shareImage(context, sharefile, winShare);
    }

    private static void shareImage(Context context, File sharefile, boolean winShare) {
        Intent chooserIntent = getSocialIntentChooser(context, sharefile, winShare);
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }

    private static Intent getSocialIntentChooser(Context context, File sharefile, boolean winShare) {
        Map<String, Boolean> intentSocialPackages = new HashMap<String, Boolean>();
        intentSocialPackages.put("twitter", true);
        intentSocialPackages.put("facebook", false);
        //intentSocialPackages.put("instagram", true);
        intentSocialPackages.put("viber", true);
        intentSocialPackages.put("whatsapp", true);
        intentSocialPackages.put("hike", true);
        intentSocialPackages.put("telegram", true);
        intentSocialPackages.put("line", true);
        intentSocialPackages.put("lenzor", true);
        intentSocialPackages.put("tango", true);
        intentSocialPackages.put("android.email", true);


        String shareText = "برای حل جورچین، برنامه بفرمایید جورچین را از کافه بازار دریافت نمایید";
        if (winShare) {
            Intent intent = ((Activity)context).getIntent();
            if(intent != null && intent.getBooleanExtra("gallery", false)){
                shareText = "http://cafebazaar.ir/app/com.chocolate.puzzlefriends";
            } else {
                shareText = "کسی میتونه این رکورد رو بشکنه؟";
            }
        }
        // Now send it out to share
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/jpeg");

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(share, 0);
        List<LabeledIntent> intentsList = new ArrayList<LabeledIntent>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            String packName = resolveInfo.activityInfo.packageName;
            for (String keyName : intentSocialPackages.keySet()) {
                if (packName.contains(keyName)) {
                    boolean canShareText = intentSocialPackages.get(keyName);
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setComponent(new ComponentName(packName, resolveInfo.activityInfo.name));
                    intent.setPackage(packName);
                    intent.setClassName(packName, resolveInfo.activityInfo.name);
                    if (canShareText) {
                        intent.putExtra(Intent.EXTRA_SUBJECT, shareText);
                        intent.putExtra(Intent.EXTRA_TITLE, shareText);
                        intent.putExtra(Intent.EXTRA_TEXT, shareText);
                        intent.setType("text/plain");
                    }
                    if (sharefile != null) {
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + sharefile));
                    }
                    intent.setType("image/jpeg");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    intentsList.add(new LabeledIntent(intent, packName, resolveInfo.loadLabel(packageManager), resolveInfo.icon));
                }
            }
        }
        Intent chooserIntent = Intent.createChooser(intentsList.remove(intentsList.size() - 1), "ارسال جورچین به دوستان");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentsList.toArray(new Parcelable[]{}));
        return chooserIntent;
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {
        /*
        try
        {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = MainActivity.instance.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        catch (Exception e)
        {
            return contentUri.getPath();
        }
        */
        return getPath(context, contentUri);
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(Context context, final Uri uri) {
        //final Context context = MainActivity.instance.getApplicationContext();
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
