package com.chocolate.puzzlefriends.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import com.chocolate.puzzlefriends.TileView;
import com.chocolate.puzzlefriends.data.Coordinate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by Mahdi7s on 3/22/2015.
 */
public class BitmapUtility {
    public static LinkedList<Integer> getInitOrders(Context context, int chunkNumbers, boolean rand) {
        LinkedList<Integer> orders = new LinkedList<Integer>();
        if (!rand) {
            for (int i = 0; i < chunkNumbers * chunkNumbers; i++) {
                orders.add(i);
            }
            orders.add(chunkNumbers * chunkNumbers - 1);
        } else {
            TileView[][] tiles = new TileView[chunkNumbers][chunkNumbers];
            for (int i = 0; i < chunkNumbers; i++) {
                for (int j = 0; j < chunkNumbers; j++) {
                    TileView tv = new TileView(context, i * chunkNumbers + j);
                    tv.coordinate = new Coordinate(i, j);

                    tiles[i][j] = tv;
                }
            }
            TileView emptyTile = tiles[chunkNumbers - 1][chunkNumbers - 1];
            emptyTile.setEmpty(true);

            Coordinate lastEmptyCoord = emptyTile.coordinate;
            for (int i = 0; i < (chunkNumbers * chunkNumbers * 4); i++) {
                TileView randTile = getRandSideTile(tiles, emptyTile, lastEmptyCoord);

                lastEmptyCoord = emptyTile.coordinate;

                Coordinate coordinate = randTile.coordinate;
                randTile.coordinate = emptyTile.coordinate;
                emptyTile.coordinate = coordinate;
            }

            // ----------------------------------------------------------------------------------------------

            for (int i = 0; i < chunkNumbers; i++) {
                for (int j = 0; j < chunkNumbers; j++) {
                    TileView tv = getTile(tiles, chunkNumbers, i, j);
                    orders.add(tv.originalIndex);
                }
            }
            orders.add(emptyTile.originalIndex);
        }
        return orders;
    }

    private static TileView getTile(TileView[][] tiles, int chunkNumbers, int row, int col) {
        for (int i = 0; i < chunkNumbers; i++) {
            for (int j = 0; j < chunkNumbers; j++) {
                TileView tv = tiles[i][j];
                if (tv.coordinate.row == row && tv.coordinate.column == col)
                    return tv;
            }
        }
        return null;
    }

    private static TileView getRandSideTile(TileView[][] tiles, TileView emptyTile, Coordinate lastEmptyCoord) {
        ArrayList<TileView> retArr = new ArrayList<>();
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles.length; j++) {
                TileView t = tiles[i][j];

                if (emptyTile.coordinate.row > 0)
                    if (t.coordinate.row == emptyTile.coordinate.row - 1 && t.coordinate.column == emptyTile.coordinate.column)
                        if (!t.coordinate.matches(lastEmptyCoord))
                            retArr.add(t);

                if (emptyTile.coordinate.row < tiles.length - 1)
                    if (t.coordinate.row == emptyTile.coordinate.row + 1 && t.coordinate.column == emptyTile.coordinate.column)
                        if (!t.coordinate.matches(lastEmptyCoord))
                            retArr.add(t);

                if (emptyTile.coordinate.column > 0)
                    if (t.coordinate.column == emptyTile.coordinate.column - 1 && t.coordinate.row == emptyTile.coordinate.row)
                        if (!t.coordinate.matches(lastEmptyCoord))
                            retArr.add(t);

                if (emptyTile.coordinate.column < tiles.length - 1)
                    if (t.coordinate.column == emptyTile.coordinate.column + 1 && t.coordinate.row == emptyTile.coordinate.row)
                        if (!t.coordinate.matches(lastEmptyCoord))
                            retArr.add(t);
            }
        }

        return retArr.get((int) (Math.random() * retArr.size()));
    }

    private static void initTiles(TileView[][] tiles, int tileCount) {
        int i = tileCount * tileCount - 1;
        while (i > 0) {
            int j = (int) (Math.random() * i);
            int xi = i % tileCount;
            int yi = (i / tileCount);
            int xj = j % tileCount;
            int yj = (j / tileCount);
            swapTiles(tiles, xi, yi, xj, yj);
            --i;
        }
    }

    private static void swapTiles(TileView[][] tiles, int i, int j, int k, int l) {
        TileView temp = tiles[i][j];
        tiles[i][j] = tiles[k][l];
        tiles[k][l] = temp;
    }

    private static boolean isSolvable(TileView[][] tiles, int tileCount, int width, int height, int emptyRow) {
        if (width % 2 == 1) {
            return (sumInversions(tiles, tileCount) % 2 == 0);
        } else {
            return ((sumInversions(tiles, tileCount) + height - emptyRow) % 2 == 0);
        }
    }

    private static int sumInversions(TileView[][] tiles, int tileCount) {
        int inversions = 0;
        for (int j = 0; j < tileCount; ++j) {
            for (int i = 0; i < tileCount; ++i) {
                inversions += countInversions(tiles, tileCount, i, j);
            }
        }
        return inversions;
    }

    private static int countInversions(TileView[][] tiles, int tileCount, int i, int j) {
        int inversions = 0;
        int tileNum = j * tileCount + i;
        int lastTile = tileCount * tileCount;
        int tileValue = tiles[i][j].coordinate.column * tileCount + tiles[i][j].coordinate.row;
        for (int q = tileNum + 1; q < lastTile; ++q) {
            int k = q % tileCount;
            int l = (q / tileCount);

            int compValue = tiles[k][l].coordinate.row * tileCount + tiles[k][l].coordinate.column;
            if (tileValue > compValue && tileValue != (lastTile - 1)) {
                ++inversions;
            }
        }
        return inversions;
    }

    private static void initEmpty(TileView[][] tiles, int tileCount, TileView emptyTile) {
        for (int j = 0; j < tileCount; ++j) {
            for (int i = 0; i < tileCount; ++i) {
                if (tiles[i][j].coordinate.column == tileCount - 1 && tiles[i][j].coordinate.row == tileCount - 1) {
                    emptyTile.setEmpty(false);
                    emptyTile = tiles[i][i];
                }
            }
        }
    }

    public static Bitmap makePuzzle(ArrayList<TileView> tiles, ArrayList<Integer> orders) {
        Bitmap bmp = ((BitmapDrawable) tiles.get(0).getDrawable()).getBitmap(); // TODO: what we can do?!
        int chunkWidth = bmp.getWidth();
        int chunkHeight = bmp.getHeight();
        int chunkNumbers = (int) Math.sqrt(tiles.size());

        if (orders == null) {
            orders = new ArrayList<Integer>(getInitOrders(null, chunkNumbers, false));
            orders.remove(orders.size() - 1);
        }

        //create a bitmap of a size which can hold the complete image after merging
        Bitmap bitmap = Bitmap.createBitmap(chunkWidth * chunkNumbers, chunkHeight * chunkNumbers, Bitmap.Config.ARGB_4444);

        //create a canvas for drawing all those small images
        Canvas canvas = new Canvas(bitmap);
        int count = 0;
        for (int rows = 0; rows < chunkNumbers; rows++) {
            for (int cols = 0; cols < chunkNumbers; cols++) {
                int order = orders.indexOf(count);
                TileView tile2 = tiles.get(order);
                if (!tile2.isEmpty()) {
                    Bitmap bmp2 = ((BitmapDrawable) tile2.getDrawable()).getBitmap();
                    canvas.drawBitmap(bmp2, chunkWidth * cols, chunkHeight * rows, null);
                }
                count++;
            }
        }

        canvas.save();
        return bitmap;
    }

    public static Bitmap decodeScaledBitmapFromSdCard(String filePath,
                                                      int reqWidth, int reqHeight) {

        reqWidth = Math.max(reqWidth, 300);
        reqHeight = Math.max(reqHeight, 200);

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static Bitmap decodeScaledBitmapFromResource(Resources resources, int resId, int reqWidth, int reqHeight) {
        reqWidth = Math.max(reqWidth, 300);
        reqHeight = Math.max(reqHeight, 200);

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources, resId, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }
}
