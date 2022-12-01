package com.chocolate.puzzlefriends.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.chocolate.puzzlefriends.TileView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * Slices original bitmap into tiles and adds border. Provides randomized or
 * ordered access to tiles.
 * 
 * Based on
 * https://github.com/thillerson/Android-Slider-Puzzle/blob/master/src/com
 * /tackmobile/TileServer.java
 * 
 * @author David Vavra
 */
public class TileSlicer {

    private Bitmap original;
    private int tileSizeX, tileSizeY;
    private int gridSize;
    private boolean isSolving;
    private List<Bitmap> slices;
    private int lastSliceServed;
    private List<Integer> sliceOrder;
    private Context context;
    private int mode = 1;

    /**
     * Initializes TileSlicer.
     *
     * @param original
     *            Bitmap which should be sliced
     * @param gridSize
     *            Grid size, for example 4 for 4x4 grid
     */
    public TileSlicer(Bitmap original, int gridSize, boolean isSolving, int mode, Context context) {
        super();
        this.original = original;
        this.gridSize = gridSize;
        this.isSolving = isSolving;
        this.tileSizeX = original.getWidth() / gridSize;
        this.tileSizeY = original.getHeight() / gridSize;
        this.context = context;
        this.mode = mode;
        slices = new LinkedList<Bitmap>();
        sliceOriginal();
    }

    /**
     * Slices original bitmap and adds border to slices.
     */
    private void sliceOriginal() {
        int x, y;
        Bitmap bitmap;
        lastSliceServed = 0;
        for (int rowI = 0; rowI < gridSize; rowI++) {
            for (int colI = 0; colI < gridSize; colI++) {
                // don't slice last part - empty slice
                //if (isSolving && mode == 1 && rowI == gridSize - 1 && colI == gridSize - 1) {
                //    continue;
                //} else {
                    x = colI * tileSizeX;
                    y = rowI * tileSizeY;
                    // slice
                    bitmap = Bitmap.createBitmap(original, x, y, tileSizeX, tileSizeY);
                    if(isSolving) {
                        // draw border lines
                        Canvas canvas = new Canvas(bitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.parseColor("#fbfdff"));
                        int endX = tileSizeX - 1;
                        int endY = tileSizeY - 1;
                        canvas.drawLine(0, 0, 0, endY, paint);
                        canvas.drawLine(0, endY, endX, endY, paint);
                        canvas.drawLine(endX, endY, endX, 0, paint);
                        canvas.drawLine(endX, 0, 0, 0, paint);
                    }
                    slices.add(bitmap);
                //}
            }
        }
        // remove reference to original bitmap
        original = null;
    }

    /**
     * Randomizes slices in case no previous state is available.
     */
    public void randomizeSlices() {
        // randomize first slices
        Collections.shuffle(slices);
        // last one is empty slice
        slices.add(null);
        sliceOrder = null;
    }

    /**
     * Sets slice order in case of previous instance is available, eg. from
     * screen rotation.
     *
     * @param order
     *            list of integers marking order of slices
     */
    public void setSliceOrder(List<Integer> order, int emptyOrder) {
        List<Bitmap> newSlices = new LinkedList<Bitmap>();
        for (int i = 0;i<order.size();i++) {
            int o = order.get(i);
            if(isSolving) {
                if (o != emptyOrder) {
                    newSlices.add(slices.get(o));
                } else if (mode == 1) {
                    // empty slice
                    newSlices.add(null);
                }
            } else {
                newSlices.add(slices.get(o));
            }
        }
        sliceOrder = order;
        slices = newSlices;
    }

    /**
     * Serves slice and creates a tile for gameboard.
     *
     * @return TileView with the image or null if there are no more slices
     */
    public TileView getTile() {// 3,5,6,4,7,8,1,0,2
        TileView tile = null;
        if (slices.size() > 0) {
            int originalIndex;
            if (sliceOrder == null) {
                originalIndex = lastSliceServed++;
            } else {
                originalIndex = sliceOrder.get(lastSliceServed++);
            }
            tile = new TileView(context, originalIndex);
            if (slices.get(0) == null) {
                // empty slice
                tile.setEmpty(true);
            }
            tile.setImageBitmap(slices.remove(0));
        }
        return tile;
    }
}