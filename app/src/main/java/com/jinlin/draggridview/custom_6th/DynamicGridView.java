package com.jinlin.draggridview.custom_6th;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;

/**
 * Created by fernandinho on 10/29/14.
 *
 * Important: To properly use the DynamicGridView you must use the ViewHolder patterns in your
 * adapter. Not doing so wil result in weird visibility bugs (views disappearing/re-appearing)
 */
public class DynamicGridView extends GridView implements AdapterView.OnItemLongClickListener, View.OnDragListener {

    public static final String TAG = "DynamicGridView";

    public final static float SCROLL_BOUND_UP = 0.20f;
    public final static float SCROLL_BOUND_DOWN = 0.80f;
    public final static int SCROLL_SPEED = 8;

    private OnDragListener onDragListener;
    private OnDropListener onDropListener;

    private int currentlyDraggedOverPosition;
    private int lastDraggedOverPosition;

    private Rect mTouchFrame;

    private int isAnimationRunning;
    private boolean pendingDrop;

    public DynamicGridView(Context context) {
        super(context);
        init();
    }

    public DynamicGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DynamicGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        onDragListener = new DefOnDragListener();
        onDropListener = new DefOnDropListener();
        setOnItemLongClickListener(this);
        setOnDragListener(this);

        currentlyDraggedOverPosition = -1;
        isAnimationRunning = 0;
        pendingDrop = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {



        return super.onTouchEvent(event);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        startDragAt(position, view);
        return true;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {

        int action = event.getAction();
        switch (action){
            case DragEvent.ACTION_DRAG_STARTED:
                log("drag started");
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                log("drag ended");
                dropItem(event);
                return true;
            case DragEvent.ACTION_DROP:
                dropItem(event);
                log("dropping");
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                log("drag exited");
                return true;
            case DragEvent.ACTION_DRAG_LOCATION:
                updateDrag(event);
                return true;
            default:
                return false;
        }
    }

    /**
     * Starts a drag operation at the given position
     * @param position
     * @param view
     */
    public void startDragAt(int position, View view){
        currentlyDraggedOverPosition = position;
        lastDraggedOverPosition = position;
        getAdapter().setDraggingPosition(position);
        ClipData clipData = ClipData.newPlainText("label", "text");
        DragShadowBuilder shadowBuilder = new DragShadowBuilder(view);
        view.startDrag(clipData, shadowBuilder, null,0);
        getAdapter().notifyDataSetChanged();
    }

    private void dropItem(DragEvent event){
        log("dropping with %d pending animations",isAnimationRunning);
        currentlyDraggedOverPosition = -1;
        lastDraggedOverPosition = -1;
        getAdapter().setDraggingPosition(-1);
        getAdapter().setDraggingOver(-1);
        getAdapter().notifyDataSetChanged();
        pendingDrop = false;
    }

    private void updateDrag(DragEvent event){
        int pos = pointToPosition((int)event.getX(), (int)event.getY());

        if(pos != INVALID_POSITION && pos != currentlyDraggedOverPosition ){
            lastDraggedOverPosition = currentlyDraggedOverPosition;
            currentlyDraggedOverPosition = pos;
            getAdapter().setDraggingOver(pos);
            getAdapter().notifyDataSetChanged();
            swapItems(currentlyDraggedOverPosition, lastDraggedOverPosition);
        }
    }

    public void swapItems(final int current, final int last){

        SwapDirection direction = getSwapDirection(current, last);

        log("Moved  " + direction + " from " + current + " to " + last);

        getAdapter().swap(current, last);
        getAdapter().setDraggingPosition(current);
        getAdapter().notifyDataSetChanged();
    }


    public View getViewByPosition(int pos) {
        final int firstListItemPosition = getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return getAdapter().getView(pos, null, this);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return getChildAt(childIndex);
        }
    }

    private int getXDirectionSign(SwapDirection direction) {
        if(direction == SwapDirection.left){
            return 1;
        }
        else if (direction == SwapDirection.right){
            return -1;
        }
        return 0;
    }

    private int getYDirectionSign(SwapDirection direction) {
        if(direction == SwapDirection.up){
            return 1;
        }
        else if (direction == SwapDirection.down){
            return -1;
        }
        return 0;
    }

    private TranslateAnimation createTranslationAnimations(View view, float startX, float endX, float startY, float endY) {
        TranslateAnimation animation = new TranslateAnimation(startX, endX, startY, endY);
        animation.setDuration(100);
        view.setAnimation(animation);
        view.setAlpha(1.0f);
        return animation;
    }

    private SwapDirection getSwapDirection(int pos1, int pos2){
        int[] pos1XY = getXYPos(pos1);
        int[] pos2XY = getXYPos(pos2);

        int xDiff = pos1XY[0] - pos2XY[0];
        int yDiff = pos1XY[1] - pos2XY[1];
        if(xDiff > 0){
            return SwapDirection.right;
        }
        else if (xDiff < 0){
            return SwapDirection.left;
        }
        else if (yDiff > 0){
            return SwapDirection.down;
        }
        else if (yDiff < 0){
            return SwapDirection.up;
        }
        return null;
    }

    private int[] getXYPos(int pos){
        int columns = getNumColumns();
        int y = pos/columns;
        int x = pos%columns - 1;
        return new int[]{x,y};
    }

    /**
     * Maps a point to a position in the list.
     *
     * @param x X in local coordinate
     * @param y Y in local coordinate
     * @return The position of the item which contains the specified point, or
     *         {@link #INVALID_POSITION} if the point does not intersect an item.
     */
    public int pointToPosition(int x, int y) {
        Rect frame = mTouchFrame;
        if (frame == null) {
            mTouchFrame = new Rect();
            frame = mTouchFrame;
        }

        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(x, y)) {
                return getFirstVisiblePosition() + i;
            }
        }
        return INVALID_POSITION;
    }

    /**
     * Equivalent to {@link #setAdapter(android.widget.ListAdapter)}
     */
    public void setAdapter(DynamicGridAdapter<?> adapter) {
        this.setAdapter((ListAdapter)adapter);
    }

    @Override
    public DynamicGridAdapter<?> getAdapter() {
        return (DynamicGridAdapter)super.getAdapter();
    }

    private static void log(String format, Object... args){
        String formatted = String.format(format, args);
        Log.d(TAG, formatted);
    }

    public interface OnDragListener {

        /**
         * Called when a drag operation is started
         *
         * @param pos the position where the drag operation was started
         */
        public void onDragStarted(int pos);

        /**
         * This method is called while the user is dragging the view around
         *
         * @param initalPosition the position in the adapter where the drag operation was started
         * @param initial the initial motion event that triggered the drag operation
         * @param current the current motion event
         */
        public void onDragged(int initalPosition, MotionEvent initial, MotionEvent current);
    }

    public interface OnDropListener {

        /**
         * Called when a dragged item is dropped
         *
         * @param from the original position where the item drag was started
         * @param to the final position where the item was dropped
         */
        public void onDrop(int from, int to);
    }

    /**
     * Implementation of OnDragListener that simply logs the methods
     */
    public static class DefOnDragListener implements OnDragListener{
        @Override
        public void onDragStarted(int pos) {
            log("Drag started at %d ", pos);
        }

        @Override
        public void onDragged(int pos, MotionEvent initial, MotionEvent current) {
            log("Dragging from %d to (%.2f, %.2f)", pos, current.getX(), current.getY());
        }
    }

    /**
     * Implementation of OnDropListener that simply logs the methods
     */
    public static class DefOnDropListener implements OnDropListener {
        @Override
        public void onDrop(int from, int to) {
            log("Dropped from %d to %d", from, to);
        }
    }

    public static enum SwapDirection{
        left, up, right, down
    }

}
