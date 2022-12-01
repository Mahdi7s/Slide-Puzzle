package com.chocolate.puzzlefriends;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chocolate.puzzlefriends.Utils.SfxPlayer;
import com.chocolate.puzzlefriends.Utils.TileSlicer;
import com.chocolate.puzzlefriends.data.Coordinate;
import com.chocolate.puzzlefriends.data.SfxResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import nineoldandroids.animation.Animator;
import nineoldandroids.animation.FloatEvaluator;
import nineoldandroids.animation.ObjectAnimator;

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
public class GameboardView extends RelativeLayout implements OnTouchListener {
    public SolvePuzzle solvePuzzle = null;


    public int GRID_SIZE = 3; // 4x4
    private Bitmap original = null;

    public boolean isSolving = false;
    private int moves = 0;

    public int emptyTileOrder = -1;

    public enum Direction {
        X, Y
    }; // movement along x or y axis

    private int tileSizeX, tileSizeY, tileSizeAvg;
    public ArrayList<TileView> tiles;
    private TileView emptyTile, movedTile;
    public boolean boardCreated;
    private RectF gameboardRect;
    private PointF lastDragPoint;
    private ArrayList<GameTileMotionDescriptor> currentMotionDescriptors;
    public LinkedList<Integer> correctOrders = null;
    public LinkedList<Integer> tileOrder;

    public TextView txtMoves = null;

    public GameboardView(Context context, AttributeSet attrSet) {
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
        int margin = isSolving ? 10 : 10; //seems the top margin have problem
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

        tileSizeAvg = (tileSizeX + tileSizeY)/2;
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

        TileSlicer tileSlicer = new TileSlicer(original, GRID_SIZE, isSolving, 1, getContext());
        // order slices
        if (tileOrder == null) {
            tileSlicer.randomizeSlices();
        } else {
            tileSlicer.setSliceOrder(tileOrder, emptyTileOrder);
        }
        // fill game board with slices
        tiles = new ArrayList<TileView>();
        for (int rowI = 0; rowI < GRID_SIZE; rowI++) {
            for (int colI = 0; colI < GRID_SIZE; colI++) {
                TileView tile = tileSlicer.getTile();
                tile.setLayoutParams(new ViewGroup.LayoutParams(tileSizeX, tileSizeY));
                tile.coordinate = new Coordinate(rowI, colI);
                if (tile.isEmpty()) {
                    emptyTile = tile;
                }
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
        Rect tileRect = rectForCoordinate(tile.coordinate);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(tileSizeX, tileSizeY);
        params.topMargin = tileRect.top;
        params.leftMargin = tileRect.left;
        addView(tile, params);
        tile.setOnTouchListener(this);
    }

    /**
     * Handling of touch events. High-level logic for moving tiles on the game
     * board.
     */
    public boolean onTouch(View v, MotionEvent event) {
        if(!isSolving || (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN
                || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP)) return true;

        TileView touchedTile = (TileView) v;
        if (touchedTile.isEmpty() || !touchedTile.isInRowOrColumnOf(emptyTile)) {
            return false;
        } else {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if(movedTile != null) return true;
                // start of the gesture
                movedTile = touchedTile;
                currentMotionDescriptors = getTilesBetweenEmptyTileAndTile(movedTile);
                movedTile.numberOfDrags = 0;
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                // during the gesture
                if (lastDragPoint != null && movedTile != null) {
                    followFinger(event);
                }
                lastDragPoint = new PointF(event.getRawX(), event.getRawY());
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                if(movedTile == null) {
                    currentMotionDescriptors = null;
                    lastDragPoint = null;
                    movedTile = null;
                    return true;
                }
                // end of gesture
                // reload the motion descriptors in case of position change
                currentMotionDescriptors = getTilesBetweenEmptyTileAndTile(movedTile);

                // if drag was over 50% or it's click, do the move
                if (lastDragMovedAtLeastHalfWay() || isClick()) {
                    animateTilesToEmptySpace();
                } else {
                    animateTilesBackToOrigin();
                }
                currentMotionDescriptors = null;
                lastDragPoint = null;
                movedTile = null;
            }
            return true;
        }
    }

    /**
     * @return Whether last drag moved with the tile more than 50% of its size
     */
    private boolean lastDragMovedAtLeastHalfWay() {
        if (lastDragPoint != null && currentMotionDescriptors != null && currentMotionDescriptors.size() > 0) {
            GameTileMotionDescriptor firstMotionDescriptor = currentMotionDescriptors.get(0);
            if (firstMotionDescriptor.axialDelta > tileSizeAvg / 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detects click - either true click (no drags) or small involuntary drag
     *
     * @return Whether last gesture was a click
     */
    private boolean isClick() {
        if(!isSolving) return true;

        if (lastDragPoint == null) {
            return true; // no drags
        }
        // just small amount of MOVE events counts as click
        if (currentMotionDescriptors != null && currentMotionDescriptors.size() > 0 && movedTile.numberOfDrags < 10) {
            GameTileMotionDescriptor firstMotionDescriptor = currentMotionDescriptors.get(0);
            // just very small drag counts as click
            if (firstMotionDescriptor.axialDelta < tileSizeAvg / 20) {
                return true;
            }
        }
        return false;
    }

    /**
     * Follows finger while dragging all currently moved tiles. Allows movement
     * only along x axis for row and y axis for column.
     *
     * @param event
     */
    private void followFinger(MotionEvent event) {
        if(!isSolving) return;

        boolean impossibleMove = true;
        float dxEvent = event.getRawX() - lastDragPoint.x;
        float dyEvent = event.getRawY() - lastDragPoint.y;
        TileView tile;
        movedTile.numberOfDrags++;
        for (GameTileMotionDescriptor descriptor : currentMotionDescriptors) {
            tile = descriptor.tile;
            Pair<Float, Float> xy = getXYFromEvent(tile, dxEvent, dyEvent, descriptor.direction);
            // detect if this move is valid
            RectF candidateRect = new RectF(xy.first, xy.second, xy.first + tile.getWidth(), xy.second
                    + tile.getHeight());
            ArrayList<TileView> tilesToCheck = null;
            if (tile.coordinate.row == emptyTile.coordinate.row) {
                tilesToCheck = allTilesInRow(tile.coordinate.row);
            } else if (tile.coordinate.column == emptyTile.coordinate.column) {
                tilesToCheck = allTilesInColumn(tile.coordinate.column);
            }

            boolean candidateRectInGameboard = (gameboardRect.contains(candidateRect));
            boolean collides = collidesWithTitles(candidateRect, tile, tilesToCheck);

            impossibleMove = impossibleMove && (!candidateRectInGameboard || collides);
        }
        if (!impossibleMove) {
            // perform the move for all moved tiles in the descriptors
            for (GameTileMotionDescriptor descriptor : currentMotionDescriptors) {
                tile = descriptor.tile;
                Pair<Float, Float> xy = getXYFromEvent(tile, dxEvent, dyEvent, descriptor.direction);
                tile.setXY(xy.first, xy.second);
            }
        }
    }

    /**
     * Computes new x,y coordinates for given tile in given direction (x or y).
     *
     * @param tile
     * @param dxEvent
     *            change of x coordinate from touch gesture
     * @param dyEvent
     *            change of y coordinate from touch gesture
     * @param direction
     *            x or y direction
     * @return pair of first x coordinates, second y coordinates
     */
    private Pair<Float, Float> getXYFromEvent(TileView tile, float dxEvent, float dyEvent, Direction direction) {
        float dxTile = 0, dyTile = 0;
        if (direction == Direction.X) {
            dxTile = tile.getXPos() + dxEvent;
            dyTile = tile.getYPos();
        }
        if (direction == Direction.Y) {
            dyTile = tile.getYPos() + dyEvent;
            dxTile = tile.getXPos();
        }
        return new Pair<Float, Float>(dxTile, dyTile);
    }

    /**
     * @param candidateRect
     *            rectangle to check
     * @param tile
     *            tile belonging to rectangle
     * @param tilesToCheck
     *            list of tiles to check
     * @return Whether candidateRect collides with any tilesToCheck
     */
    private boolean collidesWithTitles(RectF candidateRect, TileView tile, ArrayList<TileView> tilesToCheck) {
        RectF otherTileRect;
        for (TileView otherTile : tilesToCheck) {
            if (!otherTile.isEmpty() && otherTile != tile) {
                otherTileRect = new RectF(otherTile.getXPos(), otherTile.getYPos(), otherTile.getXPos()
                        + otherTile.getWidth(), otherTile.getYPos() + otherTile.getHeight());
                if (RectF.intersects(otherTileRect, candidateRect)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int motionsCount = 0;
    /**
     * Performs animation of currently moved tiles into empty space. Happens
     * when valid tile is clicked or is dragged over 50%.
     */
    private void animateTilesToEmptySpace() {
        emptyTile.setXY(movedTile.getXPos(), movedTile.getYPos());
        //Coordinate emptyCoor = new Coordinate(emptyTile.coordinate.row, emptyTile.coordinate.column);
        emptyTile.coordinate = movedTile.coordinate;
        //movedTile.coordinate = emptyCoor;

        motionsCount = currentMotionDescriptors.size();
        ObjectAnimator animator;
        for (final GameTileMotionDescriptor motionDescriptor : currentMotionDescriptors) {
            animator = ObjectAnimator.ofObject(motionDescriptor.tile, motionDescriptor.direction.toString(),
                    new FloatEvaluator(), motionDescriptor.from, motionDescriptor.to);
            animator.setDuration(16);
            animator.addListener(new Animator.AnimatorListener() {

                public void onAnimationStart(Animator animation) {
                    SfxPlayer.getInstance(solvePuzzle).Play(SfxResource.Sliding);
                }

                public void onAnimationCancel(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    motionDescriptor.tile.coordinate = motionDescriptor.finalCoordinate;
                    motionDescriptor.tile.setXY(motionDescriptor.finalRect.left, motionDescriptor.finalRect.top);

                    if(--motionsCount <= 0)
                    {
                        countTheMove();
                    }
                }
            });
            animator.start();
        }
    }

    /**
     * Performs animation of currently moved tiles back to origin. Happens when
     * the drag was less than 50%.
     */
    private void animateTilesBackToOrigin() {
        ObjectAnimator animator;
        if (currentMotionDescriptors != null) {
            for (final GameTileMotionDescriptor motionDescriptor : currentMotionDescriptors) {
                animator = ObjectAnimator.ofObject(motionDescriptor.tile, motionDescriptor.direction.toString(),
                        new FloatEvaluator(), motionDescriptor.currentPosition(), motionDescriptor.originalPosition());
                animator.setDuration(16);
                animator.addListener(new Animator.AnimatorListener() {

                    public void onAnimationStart(Animator animation) {
                    }

                    public void onAnimationCancel(Animator animation) {
                    }

                    public void onAnimationRepeat(Animator animation) {
                    }

                    public void onAnimationEnd(Animator animation) {
                        motionDescriptor.tile.setXY(motionDescriptor.originalRect.left,
                                motionDescriptor.originalRect.top);
                    }
                });
                animator.start();
            }
        }
    }

    private void countTheMove() {
        // hey we have one move!
        if (txtMoves != null) {
            txtMoves.setText(String.format("%d", ++moves));

            int correctTiles = 0;
            for (TileView tv : tiles) {
                if (tv.coordinate.matches(emptyTile.coordinate)) continue;

                int tOrder = tv.coordinate.row * GRID_SIZE + tv.coordinate.column;
                int cOrder = correctOrders.get(tv.originalIndex);

                if (tOrder == cOrder) {
                    ++correctTiles;
                }
            }
            String msg;
            if (correctTiles == tiles.size()-1) {
                Toast.makeText(solvePuzzle, "هورااا", Toast.LENGTH_SHORT).show();
                solvePuzzle.showWin(moves, 1);
            }
        }
    }
    /**
     * Finds tiles between checked tile and empty tile and initializes motion
     * descriptors for those tiles.
     *
     * @param tile
     *            A tile to be checked
     * @return list of tiles between checked tile and empty tile
     */
    private ArrayList<GameTileMotionDescriptor> getTilesBetweenEmptyTileAndTile(TileView tile) {
        ArrayList<GameTileMotionDescriptor> descriptors = new ArrayList<GameTileMotionDescriptor>();
        Coordinate coordinate, finalCoordinate;
        TileView foundTile;
        GameTileMotionDescriptor motionDescriptor;
        Rect finalRect, currentRect;
        float axialDelta;
        if (tile.isToRightOf(emptyTile)) {
            // add all tiles left of the tile
            for (int i = tile.coordinate.column; i > emptyTile.coordinate.column; i--) {
                coordinate = new Coordinate(tile.coordinate.row, i);
                foundTile = (tile.coordinate.matches(coordinate)) ? tile : getTileAtCoordinate(coordinate);
                finalCoordinate = new Coordinate(tile.coordinate.row, i - 1);
                currentRect = rectForCoordinate(foundTile.coordinate);
                finalRect = rectForCoordinate(finalCoordinate);
                axialDelta = Math.abs(foundTile.getXPos() - currentRect.left);
                motionDescriptor = new GameTileMotionDescriptor(foundTile, Direction.X, foundTile.getXPos(),
                        finalRect.left);
                motionDescriptor.finalCoordinate = finalCoordinate;
                motionDescriptor.finalRect = finalRect;
                motionDescriptor.axialDelta = axialDelta;
                descriptors.add(motionDescriptor);
            }
        } else if (tile.isToLeftOf(emptyTile)) {
            // add all tiles right of the tile
            for (int i = tile.coordinate.column; i < emptyTile.coordinate.column; i++) {
                coordinate = new Coordinate(tile.coordinate.row, i);
                foundTile = (tile.coordinate.matches(coordinate)) ? tile : getTileAtCoordinate(coordinate);
                finalCoordinate = new Coordinate(tile.coordinate.row, i + 1);
                currentRect = rectForCoordinate(foundTile.coordinate);
                finalRect = rectForCoordinate(finalCoordinate);
                axialDelta = Math.abs(foundTile.getXPos() - currentRect.left);
                motionDescriptor = new GameTileMotionDescriptor(foundTile, Direction.X, foundTile.getXPos(),
                        finalRect.left);
                motionDescriptor.finalCoordinate = finalCoordinate;
                motionDescriptor.finalRect = finalRect;
                motionDescriptor.axialDelta = axialDelta;
                descriptors.add(motionDescriptor);
            }
        } else if (tile.isAbove(emptyTile)) {
            // add all tiles bellow the tile
            for (int i = tile.coordinate.row; i < emptyTile.coordinate.row; i++) {
                coordinate = new Coordinate(i, tile.coordinate.column);
                foundTile = (tile.coordinate.matches(coordinate)) ? tile : getTileAtCoordinate(coordinate);
                finalCoordinate = new Coordinate(i + 1, tile.coordinate.column);
                currentRect = rectForCoordinate(foundTile.coordinate);
                finalRect = rectForCoordinate(finalCoordinate);
                axialDelta = Math.abs(foundTile.getYPos() - currentRect.top);
                motionDescriptor = new GameTileMotionDescriptor(foundTile, Direction.Y, foundTile.getYPos(),
                        finalRect.top);
                motionDescriptor.finalCoordinate = finalCoordinate;
                motionDescriptor.finalRect = finalRect;
                motionDescriptor.axialDelta = axialDelta;
                descriptors.add(motionDescriptor);
            }
        } else if (tile.isBelow(emptyTile)) {
            // add all tiles above the tile
            for (int i = tile.coordinate.row; i > emptyTile.coordinate.row; i--) {
                coordinate = new Coordinate(i, tile.coordinate.column);
                foundTile = (tile.coordinate.matches(coordinate)) ? tile : getTileAtCoordinate(coordinate);
                finalCoordinate = new Coordinate(i - 1, tile.coordinate.column);
                currentRect = rectForCoordinate(foundTile.coordinate);
                finalRect = rectForCoordinate(finalCoordinate);
                axialDelta = Math.abs(foundTile.getYPos() - currentRect.top);
                motionDescriptor = new GameTileMotionDescriptor(foundTile, Direction.Y, foundTile.getYPos(),
                        finalRect.top);
                motionDescriptor.finalCoordinate = finalCoordinate;
                motionDescriptor.finalRect = finalRect;
                motionDescriptor.axialDelta = axialDelta;
                descriptors.add(motionDescriptor);
            }
        }
        return descriptors;
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
     * @param row
     *            number of row
     * @return list of tiles in the row
     */
    private ArrayList<TileView> allTilesInRow(int row) {
        ArrayList<TileView> tilesInRow = new ArrayList<TileView>();
        for (TileView tile : tiles) {
            if (tile.coordinate.row == row) {
                tilesInRow.add(tile);
            }
        }
        return tilesInRow;
    }

    /**
     * @param column
     *            number of column
     * @return list of tiles in the column
     */
    private ArrayList<TileView> allTilesInColumn(int column) {
        ArrayList<TileView> tilesInColumn = new ArrayList<TileView>();
        for (TileView tile : tiles) {
            if (tile.coordinate.column == column) {
                tilesInColumn.add(tile);
            }
        }
        return tilesInColumn;
    }

    /**
     * @param coordinate
     * @return Rectangle for given coordinate
     */
    private Rect rectForCoordinate(Coordinate coordinate) {
        int gameboardY = (int) Math.floor(gameboardRect.top);
        int gameboardX = (int) Math.floor(gameboardRect.left);

        int top = (coordinate.row * tileSizeY) + gameboardY; //(coordinate.row == 0 ? gameboardY : 0);
        int left = (coordinate.column * tileSizeX) + gameboardX;//(coordinate.column == 0 ? gameboardX : 0);
        return new Rect(left, top, left + tileSizeX, top + tileSizeY);
    }

    /**
     * Returns current tile locations. Useful for preserving state when
     * orientation changes.
     *
     * @return current tile locations
     */
    public LinkedList<Integer> getTileOrder() {
        LinkedList<Integer> tileLocations = new LinkedList<Integer>();
        for (int rowI = 0; rowI < GRID_SIZE; rowI++) {
            for (int colI = 0; colI < GRID_SIZE; colI++) {
                TileView tile = getTileAtCoordinate(new Coordinate(colI, rowI));
                if(tile!= null)
                    tileLocations.add(tile.originalIndex);
                else
                    tileLocations.add(-1);
            }
        }
        return tileLocations;
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

    /**
     * Describes movement of the tile. It is used to move several tiles at once.
     */
    public class GameTileMotionDescriptor {

        public Rect finalRect, originalRect;
        public Direction direction; // x or y
        public TileView tile;
        public float from, to, axialDelta;
        public Coordinate finalCoordinate;

        public GameTileMotionDescriptor(TileView tile, Direction direction, float from, float to) {
            super();
            this.tile = tile;
            this.from = from;
            this.to = to;
            this.direction = direction;
            this.originalRect = rectForCoordinate(tile.coordinate);
        }

        /**
         * @return current position of the tile
         */
        public float currentPosition() {
            if (direction == Direction.X) {
                return tile.getXPos();
            } else if (direction == Direction.Y) {
                return tile.getYPos();
            }
            return 0;
        }

        /**
         * @return original position of the tile. It is used in movement to
         *         original position.
         */
        public float originalPosition() {
            if (direction == Direction.X) {
                return originalRect.left;
            } else if (direction == Direction.Y) {
                return originalRect.top;
            }
            return 0;
        }

    }
}
