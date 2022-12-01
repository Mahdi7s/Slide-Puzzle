package com.chocolate.puzzlefriends;

import android.animation.Animator;
import android.animation.FloatEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chocolate.puzzlefriends.Utils.SfxPlayer;
import com.chocolate.puzzlefriends.Utils.TileSlicer;
import com.chocolate.puzzlefriends.data.Coordinate;
import com.chocolate.puzzlefriends.data.SfxResource;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 
 * Layout handling creation and interaction of the game tiles. Captures gestures
 * and performs animations.
 * 
 * Based on:
 * https://github.com/thillerson/Android-Slider-Puzzle/blob/master/src/
 * com/tackmobile/GameboardView.java
 * 
 * @author David Vavra
 * 
 */
public class GameboardViewMode2 extends RelativeLayout implements OnTouchListener {
    public SolvePuzzle solvePuzzle = null;

    public int GRID_SIZE = 3; // 4x4
    private Bitmap original = null;

    private int moves = 0;
    public boolean isSolving = true;

    public enum Direction {
        X, Y
    }; // movement along x or y axis

    private int tileSizeX, tileSizeY;
    public ArrayList<TileView> tiles;
    private TileView tile1, tile2;
    public boolean boardCreated;
    private RectF gameboardRect;
    public LinkedList<Integer> correctOrders = null;
    public LinkedList<Integer> tileOrder;

    public TextView txtMoves = null;

    public GameboardViewMode2(Context context, AttributeSet attrSet) {
        super(context, attrSet);
    }

    private boolean layouted = false, setuped = false;
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        layouted = true;

        startGame(false);
    }

    public void setupImage(Bitmap bmp, int chunkSize, boolean fake) {
        GRID_SIZE = chunkSize;
        original = bmp;
        setuped = true;

        startGame(fake);
    }

    private void startGame(boolean fake) {
        if (fake || (!boardCreated && setuped && layouted)) {
            determineGameboardSizes();
            fillTiles();
            boardCreated = !fake;
        }
    }

    /**
     * Detect game board size and tile size based on current screen.
     */
    private void determineGameboardSizes() {
        int margin = 10;
        int viewWidth = getWidth() - margin;
        int viewHeight = getHeight() - margin;
        // fit in portrait or landscape
        int bmpX = original.getWidth(),
            bmpY = original.getHeight();

//        if (viewWidth > viewHeight) {
//            if(bmpX > bmpY) { // this is nice
//                tileSizeY = Math.min(viewHeight, bmpY) / GRID_SIZE;
//                tileSizeX = bmpX*tileSizeY/bmpY;
//            } else {
//                tileSizeX = Math.min(viewWidth, bmpX) / GRID_SIZE;
//                tileSizeY = bmpY * tileSizeX / bmpX;
//            }
//        } else {
//            if(bmpX > bmpY) { // this is nice
//                tileSizeX = Math.min(viewWidth, bmpX) / GRID_SIZE;
//                tileSizeY = bmpY * tileSizeX / bmpX;
//            } else {
//                tileSizeY = Math.min(viewHeight, bmpY) / GRID_SIZE;
//                tileSizeX = bmpX*tileSizeY/bmpY;
//            }
//        }

        float ratio = Math.min((float)viewWidth / bmpX, (float)viewHeight / bmpY);
        tileSizeX = (int) (bmpX*ratio / GRID_SIZE);
        tileSizeY = (int) (bmpY*ratio / GRID_SIZE);

        int gameboardSizeX = (int) (tileSizeX * GRID_SIZE);
        int gameboardSizeY = (int) (tileSizeY * GRID_SIZE);
        // center gameboard
        int gameboardTop = (viewHeight / 2 - gameboardSizeY / 2) + margin/2;
        int gameboardLeft = (viewWidth / 2 - gameboardSizeX / 2) + margin/2;
        gameboardRect = new RectF(gameboardLeft, gameboardTop, gameboardLeft + gameboardSizeX, gameboardTop
                + gameboardSizeY);
    }

    /**
     * Fills game board with tiles sliced from the globe image.
     */
    public void fillTiles() {
        removeAllViews();

        TileSlicer tileSlicer = new TileSlicer(original, GRID_SIZE, true, 2, getContext());

        // order slices
        if (tileOrder == null) {
            tileSlicer.randomizeSlices();
        } else {
            tileSlicer.setSliceOrder(tileOrder, -1);
        }
        // fill game board with slices
        tiles = new ArrayList<TileView>();
        for (int rowI = 0; rowI < GRID_SIZE; rowI++) {
            for (int colI = 0; colI < GRID_SIZE; colI++) {
                TileView tile = tileSlicer.getTile();
                tile.coordinate = new Coordinate(rowI, colI);
                tiles.add(tile);
                placeTile(tile);
            }
        }
    }

    /**
     * Places tile on appropriate place in the layout.
     *
     * @param tile
     *            Tile to place
     */
    private void placeTile(TileView tile) {
        LayoutParams params = getTileLayoutParams(tile);
        addView(tile, params);
        tile.setOnTouchListener(this);
    }

    private LayoutParams getTileLayoutParams(TileView tile) {
        Rect tileRect = rectForCoordinate(tile.coordinate);
        LayoutParams params = new LayoutParams(tileSizeX, tileSizeY);
        params.topMargin = tileRect.top;
        params.leftMargin = tileRect.left;
        return params;
    }

    /**
     * Handling of touch events. High-level logic for moving tiles on the game
     * board.
     */
    public boolean onTouch(View v, MotionEvent event) {
        if(!isSolving) return false;

        TileView touchedTile = (TileView) v;
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if(tile1 == null) {
                tile1 = touchedTile;
                tile1.setBackgroundColor(Color.GREEN);

                SfxPlayer.getInstance(solvePuzzle).Play(SfxResource.Selecting);
            } else if(tile2 == null) {
                tile2 = touchedTile;
                if(!tile1.coordinate.matches(tile2.coordinate)){
                    SfxPlayer.getInstance(solvePuzzle).Play(SfxResource.Selecting);

                    // change tiles
                    Coordinate c1 = tile1.coordinate;
                    tile1.coordinate = tile2.coordinate;
                    tile2.coordinate = c1;

                    tile1.setLayoutParams(getTileLayoutParams(tile1));
                    tile2.setLayoutParams(getTileLayoutParams(tile2));

                    tile1.setBackgroundColor(Color.TRANSPARENT);

                    countTheMove();
                }
                else {
                    tile1.setBackgroundColor(Color.TRANSPARENT);
                }
                tile1 = tile2 = null;
            }
        }

        return true;
    }

    private void countTheMove() {
        // hey we have one move!
        if(txtMoves != null){
            txtMoves.setText(String.format("%d", ++moves));

            int correctTiles = 0;
            for(TileView tv:tiles){
                int tOrder = tv.coordinate.row*GRID_SIZE + tv.coordinate.column;
                int cOrder = correctOrders.get(tv.originalIndex);

                if(tOrder == cOrder) {
                    ++correctTiles;
                }
            }

            if(correctTiles == tiles.size()) {
                Toast.makeText(solvePuzzle, "تبریک! شما برنده شدید", Toast.LENGTH_SHORT).show();
                solvePuzzle.showWin(moves, 2);
            }
        }
    }

    /**
     * @param coordinate
     *            coordinate of the tile
     * @return tile at given coordinate
     */
    private TileView getTileAtCoordinate(Coordinate coordinate) {
        for (TileView tile : tiles) {
            if (tile.coordinate.matches(coordinate)) {
                return tile;
            }
        }
        return null;
    }

    /**
     * @param coordinate
     * @return Rectangle for given coordinate
     */
    private Rect rectForCoordinate(Coordinate coordinate) {
        int gameboardY = (int) Math.floor(gameboardRect.top);
        int gameboardX = (int) Math.floor(gameboardRect.left);

        Bitmap bmp = ((BitmapDrawable) tiles.get(0).getDrawable()).getBitmap();
        int top = (coordinate.row * tileSizeY) + gameboardY;
        int left = ((coordinate.column * tileSizeX)) + gameboardX;
        return new Rect(left, top, left + tileSizeX, top + tileSizeY);
    }

    /**
     * Sets tile locations from previous state.
     *
     * @param tileLocations
     *            list of integers marking order
     */
    public void setTileOrder(LinkedList<Integer> tileLocations) {
        this.tileOrder = tileLocations;
    }
}
