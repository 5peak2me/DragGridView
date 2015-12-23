package com.jinlin.draggridview.custom_5th;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewPropertyAnimator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by pasqualeanatriello on 01/03/14.
 */
public class DynamicGridView extends ViewGroup implements GestureDetector.OnGestureListener {

    private DynamicGridAdapter mAdapter;

    // The list of views this DynamicGridView is currently displaying
    private List<List<ItemRepresentation>> mColumns;
    private Set<ItemRepresentation> mAnimatingItems;
    private List<Integer> mColumnsMinVisibleIndex;
    private List<Integer> mColumnsMaxVisibleIndex;
    private ItemRepresentation mSelectedItem;
    private int mNumberOfColumns;
    private int mCurrTop;
    private Scroller mScroller;
    private Rect mTmpRect;
    private CellRecicleBin mCellCache;
    private GestureDetector mGestureDetector;
    private FlingRunnable mFlingRunnable;
    private float mDownY;
    private float mDownX;
    private GlobalAnimator mGlobalAnimator;
    private int mTouchSlop;
    private boolean mIsScrolling;
    private int mSelectedItemColumn;
    private int mSelectedColumnIndex;
    private int mFirstVisibleItemInFirstColumn;
    private int mOffsetOfFirstItem;
    private DataSetObserver mDataSetObserver;
    private OnItemClickListener mItemClickListener;
    private ItemRepresentation mHighlitedItem;

    public interface OnItemClickListener {
        public void onItemClicked(int column, int row, View view);
    }

    /**
     * Static class to represent the position the view should have inside the ViewGroup.
     * This is useful to animate view movements
     */
    private static class ItemRepresentation {
        public ItemRepresentation() {
            animating = false;
        }

        public View view;
        public int viewTop;
        public int viewLeft;
        public int viewHeight;
        public int globalIndex;
        public boolean animating;

    }

    private class GlobalAnimator implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

        private static final long ANIMATION_DURATION = 200;
        private ValueAnimator mValueAnimator;
        private boolean mAnimating;
        private float lastUpdateValue;

        public void start() {
            mAnimating = true;
            lastUpdateValue = .0f;
            mValueAnimator = ValueAnimator.ofFloat(.0f, 1.0f).setDuration(ANIMATION_DURATION);
            mValueAnimator.addUpdateListener(this);
            mValueAnimator.addListener(this);
            mValueAnimator.setInterpolator(new LinearInterpolator());
            mValueAnimator.start();
        }

        public void stop() {
            if (mValueAnimator != null) {
                mValueAnimator.removeListener(this);
                mValueAnimator.cancel();
            }
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            //Let's animate some views!
            int finalLeft, finalTop, currentLeft, currentTop, remainingOffsetLeft, remainingOffsetTop,
                    alreadyUpdatedLeft, alreadyUpdatedTop, stepValueX, stepValueY;
            float animationElapsedTime = valueAnimator.getAnimatedFraction();

            for (ItemRepresentation item : mAnimatingItems) {
                finalLeft = item.viewLeft;
                finalTop = item.viewTop + mCurrTop;
                currentLeft = item.view.getLeft();
                currentTop = item.view.getTop();
                remainingOffsetLeft = finalLeft - currentLeft;
                remainingOffsetTop = finalTop - currentTop;
                //Let's calculate the part of the animation this view already did
                alreadyUpdatedLeft = (int) ((remainingOffsetLeft * lastUpdateValue) / (1f - lastUpdateValue));
                alreadyUpdatedTop = (int) ((remainingOffsetTop * lastUpdateValue) / (1f - lastUpdateValue));
                //and now the single step size
                stepValueX = (int) ((remainingOffsetLeft + alreadyUpdatedLeft) * (animationElapsedTime - lastUpdateValue));
                stepValueY = (int) ((remainingOffsetTop + alreadyUpdatedTop) * (animationElapsedTime - lastUpdateValue));
                item.view.offsetLeftAndRight(stepValueX);
                item.view.offsetTopAndBottom(stepValueY);
            }
            lastUpdateValue = animationElapsedTime;
        }

        @Override
        public void onAnimationStart(Animator animator) {

        }

        @Override
        public void onAnimationEnd(Animator animator) {

            //Let's make sure the animation completed and we don't have any pixel left due to approximation
            int finalLeft, finalTop, currentLeft, currentTop, remainingOffsetLeft, remainingOffsetTop;
            for (ItemRepresentation item : mAnimatingItems) {
                finalLeft = item.viewLeft;
                finalTop = item.viewTop + mCurrTop;
                currentLeft = item.view.getLeft();
                currentTop = item.view.getTop();
                remainingOffsetLeft = finalLeft - currentLeft;
                remainingOffsetTop = finalTop - currentTop;
                item.view.offsetLeftAndRight(remainingOffsetLeft);
                item.view.offsetTopAndBottom(remainingOffsetTop);
                item.animating = false;
            }
            mAnimatingItems.clear();
            mAnimating = false;
        }

        @Override
        public void onAnimationCancel(Animator animator) {

        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }

        public boolean isAnimating() {
            return mAnimating;
        }

    }


    public DynamicGridView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public DynamicGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public DynamicGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }


    public void setItemClickListener(OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }


    public DynamicGridAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(DynamicGridAdapter adapter) {
        if (!(adapter instanceof DynamicGridAdapter)) {
            throw new IllegalArgumentException("Adapter for DynamicGridView must extend DynamicGridAdapter");
        }
        mAdapter = (DynamicGridAdapter) adapter;
        mAdapter.setDataSetObserver(mDataSetObserver);
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (mAdapter != null && !mAdapter.isEmpty()) {
            clearItemsAndInitColumns();
            generateColumns();
            if (changed) {
                mCurrTop = findTopFromRestoredState();
            }

            fillGridFromCurrentTop();
        }
    }


    private int findTopFromRestoredState() {
        if (mColumns.get(0).size() < mFirstVisibleItemInFirstColumn) {
            return 0;
        }
        return (mColumns.get(0).get(mFirstVisibleItemInFirstColumn).viewTop - mOffsetOfFirstItem) * -1;
    }


    /**
     * generates the empty columns geometry so that we know the position of every cell
     */
    private void generateColumns() {
        for (int index = 0; index < mAdapter.getNumberOfColumns(); index++) {

            for (int row = 0; row < mAdapter.getCountForColumn(index); row++) {
                float proportion = mAdapter.getAspectRatioForItem(index, row);
                int cellWidth = getWidth() / mNumberOfColumns;
                int cellHeight = (int) (cellWidth / proportion);
                ItemRepresentation item = new ItemRepresentation();

                item.viewTop = getBottomForColumn(mColumns.get(index));
                item.viewLeft = index * (getWidth() / mNumberOfColumns);
                item.viewHeight = cellHeight;
                item.globalIndex = index;
                mColumns.get(index).add(item);
            }
        }
    }

    private void fillGridFromCurrentTop() {
        for (int i = 0; i < mNumberOfColumns; i++) {
            fillColumnFromCurrentTop(i);
        }

    }


    private void fillGridBottom() {
        for (int i = 0; i < mNumberOfColumns; i++) {
            fillColumnBottom(i, false);
        }
    }


    private void generateViewRepresentationAndAddView(int index, int column, boolean animated) {
        if (mColumns.get(column).size() == 0) {
            return;
        }
        ItemRepresentation item = mColumns.get(column).get(index);
        //int itemType = mAdapter.getItemViewType(column,index);
        float proportion = mAdapter.getAspectRatioForItem(column, index);
        View v = mAdapter.getView(column, index, mCellCache.get(proportion), this);
        v.setSelected(false);
        int cellWidth = getWidth() / mNumberOfColumns;
        int cellHeight = (int) (cellWidth / proportion);
        int width = MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY);
        int height = MeasureSpec.makeMeasureSpec(cellHeight, MeasureSpec.EXACTLY);
        v.measure(width, height);
        item.view = v;
        LayoutParams lp = new LayoutParams(cellWidth, cellHeight);
        addViewInLayout(v, getChildCount(), lp, true);
        if (!animated) {
            v.layout(item.viewLeft, item.viewTop + mCurrTop, item.viewLeft + cellWidth, item.viewTop + mCurrTop + cellHeight);
        } else {
            item.animating = true;
            mAnimatingItems.add(item);
            v.layout(item.viewLeft, getBottom() - 1, item.viewLeft + cellWidth, getBottom() + cellHeight - 1);
        }
    }


    private int getLastVisibleItemBottomForColumn(int i) {
        if (mColumnsMaxVisibleIndex.get(i) >= 0) {
            ItemRepresentation item = mColumns.get(i).get(mColumnsMaxVisibleIndex.get(i));
            return item.viewTop + mCurrTop + item.viewHeight;
        }

        return 0;
    }

    private int getBottomForColumn(List<ItemRepresentation> itemRepresentations) {
        if (itemRepresentations.size() > 0) {
            ItemRepresentation lastItem = itemRepresentations.get(itemRepresentations.size() - 1);
            return lastItem.viewTop + lastItem.viewHeight;
        }

        return 0;
    }

    private void clearItemsAndInitColumns() {
        if (getChildCount() > 0) {
            removeViewsInLayout(0, getChildCount());
        }
        initDataStructures();
    }


    private void init(Context context, AttributeSet attrs, int defStyle) {
        mTmpRect = new Rect();
        mDataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                requestLayout();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                requestLayout();
            }
        };
        mCurrTop = 0;

        mCellCache = new CellRecicleBin();
        mGestureDetector = new GestureDetector(context, this, new Handler());
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mTouchSlop = vc.getScaledTouchSlop();
        mGlobalAnimator = new GlobalAnimator();
    }

    private void initDataStructures() {
        mNumberOfColumns = mAdapter.getNumberOfColumns();
        mColumns = new ArrayList<List<ItemRepresentation>>();
        mAnimatingItems = new HashSet<ItemRepresentation>();
        mColumnsMinVisibleIndex = new ArrayList<Integer>();
        mColumnsMaxVisibleIndex = new ArrayList<Integer>();
        for (int i = 0; i < mNumberOfColumns; i++) {
            mColumns.add(new LinkedList<ItemRepresentation>());
            mColumnsMinVisibleIndex.add(0);
            mColumnsMaxVisibleIndex.add(-1);
        }
    }

    private float canScroll(float distanceY) {
        return distanceY > 0 ? canScrollDown(distanceY) : canScrollUp(distanceY);
    }

    private float canScrollDown(float distanceY) {

        float limit = 0;
        for (int i = 0; i < mNumberOfColumns; i++) {
            limit = Math.max(limit, canScrollDown(distanceY, i));
        }
        return Math.min(limit, distanceY);
    }

    private float canScrollDown(float distanceY, int i) {
        if (mColumnsMaxVisibleIndex.get(i) == mColumns.get(i).size() - 1) {
            List<ItemRepresentation> column = mColumns.get(i);
            if (column.size() != 0 && column.size() > mColumnsMaxVisibleIndex.get(i) && column.get(mColumnsMaxVisibleIndex.get(i)).view != null) {
                float maxScroll = column.get(mColumnsMaxVisibleIndex.get(i)).view.getBottom() - getHeight();
                return maxScroll;
            } else {
                return 0;
            }

        }
        return distanceY;

    }

    private float canScrollUp(float distanceY) {
        if (mCurrTop < 0) {
            return Math.max(mCurrTop, distanceY);
        }
        return 0;
    }


    private void scroll(float distanceY) {
        distanceY = canScroll(distanceY);
        if (distanceY != 0) {
            if (distanceY > 0) {
                scrollUp(distanceY);
            } else {
                scrollDown(distanceY);
            }
        }

    }

    private void scrollUp(float distanceY) {
        offsetViews(-distanceY);
        mCurrTop -= (int) distanceY;
        removeFromTop();
        fillGridBottom();
    }

    private void removeFromTop() {
        for (int i = 0; i < mNumberOfColumns; i++) {
            List<ItemRepresentation> columnList = mColumns.get(i);

            boolean removing = true;
            while (columnList.size() > 0 && columnList.size() > mColumnsMinVisibleIndex.get(i) && removing) {
                ItemRepresentation item = columnList.get(mColumnsMinVisibleIndex.get(i));
                if (!item.animating && item.view != null && (item.viewTop + item.viewHeight + mCurrTop) < 0) {
                    removeViewInLayout(item.view);
                    recycleItem(item.view, i, mColumnsMinVisibleIndex.get(i));
                    item.view = null;
                    mColumnsMinVisibleIndex.set(i, mColumnsMinVisibleIndex.get(i) + 1);
                } else {
                    removing = false;
                }
            }
        }

    }


    private void removeFromBottom() {
        for (int i = 0; i < mNumberOfColumns; i++) {
            List<ItemRepresentation> columnList = mColumns.get(i);
            boolean removing = true;
            while (columnList.size() > 0 && removing) {
                int row = mColumnsMaxVisibleIndex.get(i);
                ItemRepresentation item = columnList.get(row);
                if (!item.animating && item.view != null && (item.viewTop + mCurrTop) > getHeight()) {
                    removeViewInLayout(item.view);
                    recycleItem(item.view, i, row);
                    item.view = null;
                    mColumnsMaxVisibleIndex.set(i, mColumnsMaxVisibleIndex.get(i) - 1);
                } else {
                    removing = false;
                }
            }
        }

    }


    private void scrollDown(float distanceY) {
        offsetViews(-distanceY);
        mCurrTop -= (int) distanceY;
        removeFromBottom();
        fillGridTop();
    }

    private void fillGridTop() {
        for (int i = 0; i < mNumberOfColumns; i++) {
            fillColumnTop(i);
        }

    }

    private void fillColumnTop(int i) {
        int minItem = mColumnsMinVisibleIndex.get(i);
        List<ItemRepresentation> column = mColumns.get(i);
        while (minItem >= column.size() || (minItem > 0 && column.get(minItem).view.getTop() >= 0)) {
            mColumnsMinVisibleIndex.set(i, --minItem);
            generateViewRepresentationAndAddView(minItem, i, false);
        }
    }

    private void fillColumnBottom(int i, boolean animated) {
        if (mColumns.get(i).size() == 0) {
            return;
        }
        while (mColumnsMaxVisibleIndex.get(i) < (mColumns.get(i).size() - 1) && getLastVisibleItemBottomForColumn(i) < getHeight()) {
            mColumnsMaxVisibleIndex.set(i, mColumnsMaxVisibleIndex.get(i) + 1);
            generateViewRepresentationAndAddView(mColumnsMaxVisibleIndex.get(i), i, animated);

        }
    }


    private void fillColumnFromCurrentTop(int column) {
        int minItem = findMinimumVisibleItemForColumn(column);
        mColumnsMinVisibleIndex.set(column, minItem);
        mColumnsMaxVisibleIndex.set(column, minItem);
        generateViewRepresentationAndAddView(mColumnsMaxVisibleIndex.get(column), column, false);
        fillColumnBottom(column, false);
    }

    private int findMinimumVisibleItemForColumn(int columnIdx) {
        List<ItemRepresentation> column = mColumns.get(columnIdx);
        Iterator<ItemRepresentation> iterator = column.iterator();
        int idx = 0;
        boolean found = false;
        while (iterator.hasNext() && !found) {
            ItemRepresentation item = iterator.next();
            int itemBottom = item.viewTop + item.viewHeight + mCurrTop;
            if (itemBottom > 0) {
                found = true;
            } else {
                idx++;
            }
        }
        return idx;
    }


    private void recycleItem(View view, int column, int row) {
        mCellCache.put(view, mAdapter.getAspectRatioForItem(column, row));
    }

    private void offsetViews(float distanceY) {

        for (int i = 0; i < mNumberOfColumns; i++) {
            List<ItemRepresentation> columnList = mColumns.get(i);
            if (columnList.size() > 0) {
                for (int j = mColumnsMinVisibleIndex.get(i); j <= mColumnsMaxVisibleIndex.get(i); j++) {
                    ItemRepresentation item = columnList.get(j);
                    if (!item.animating) {
                        item.view.offsetTopAndBottom((int) distanceY);
                    }
                }
            }
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        final int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mIsScrolling = false;
            return false;
        }

        //We want to intercept vertical scroll events
        if (action == MotionEvent.ACTION_MOVE) {
            if (mIsScrolling) {
                return true;
            }
            if (Math.abs(event.getRawY() - mDownY) > mTouchSlop || Math.abs(event.getRawX() - mDownX) > mTouchSlop) {
                mIsScrolling = true;
                return true;
            }

        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            if (mSelectedItem != null) {
                mAnimatingItems.add(mSelectedItem);
                mSelectedItem.view.setSelected(false);
                startExpandAnimation();
                mSelectedItem = null;
                startAnimatingViews();
            } else if (mHighlitedItem != null && mHighlitedItem.view != null) {
                mHighlitedItem.view.setSelected(false);
                if (!mIsScrolling) {
                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClicked(mSelectedItemColumn, mSelectedColumnIndex, mHighlitedItem.view);
                    }
                }
                mHighlitedItem = null;
            }
            mIsScrolling = false;
        }

        if (mSelectedItem != null && action == MotionEvent.ACTION_MOVE) {
            mSelectedItem.animating = true;
            mSelectedItem.view.offsetTopAndBottom((int) (event.getRawY() - mDownY));
            mSelectedItem.view.offsetLeftAndRight((int) (event.getX() - mDownX));
            mDownY = event.getRawY();
            mDownX = event.getRawX();
            checkNewPositionForSelecetView();
        }


        return mGestureDetector.onTouchEvent(event);
    }

    private void checkNewPositionForSelecetView() {
        int itemColumn = findTouchedColumn(mSelectedItem.view.getLeft() + mSelectedItem.view.getWidth() / 2);
        if (mSelectedItemColumn == itemColumn) {
            moveInsideColumn();
        } else {
            moveInAnotherColumn(itemColumn);
        }
    }


    private void moveInsideColumn() {
        //Let's just check the predecessor and the successor
        List<ItemRepresentation> column = mColumns.get(mSelectedItemColumn);

        if (mSelectedColumnIndex > 0) {
            ItemRepresentation previousItem = column.get(mSelectedColumnIndex - 1);
            int prevItemYcenter = previousItem.viewTop + previousItem.viewHeight / 2 + mCurrTop;
            if ((mSelectedItem.view.getTop() + mSelectedItem.viewHeight / 2) < prevItemYcenter) {
                //Let's swap them. this is easy and will not cause any other item to move
                column.remove(mSelectedColumnIndex);
                column.add(mSelectedColumnIndex - 1, mSelectedItem);
                mAdapter.moveUp(mSelectedItemColumn, mSelectedColumnIndex);
                mSelectedItem.viewTop = previousItem.viewTop;
                previousItem.animating = true;
                previousItem.viewTop = mSelectedItem.viewTop + mSelectedItem.viewHeight;
                mAnimatingItems.add(previousItem);
                startAnimatingViews();
                mSelectedColumnIndex = mSelectedColumnIndex - 1;
                //We are done. Nothing more to do here
                return;
            }
        }
        if (mSelectedColumnIndex < column.size() - 1) {
            ItemRepresentation nextItem = column.get(mSelectedColumnIndex + 1);
            int prevItemYcenter = nextItem.viewTop + nextItem.viewHeight / 2 + mCurrTop;
            if ((mSelectedItem.view.getTop() + mSelectedItem.viewHeight / 2) > prevItemYcenter) {
                //Let's swap them. this is easy and will not cause any other item to move
                column.remove(mSelectedColumnIndex);
                column.add(mSelectedColumnIndex + 1, mSelectedItem);
                mAdapter.moveDown(mSelectedItemColumn, mSelectedColumnIndex);
                nextItem.viewTop = mSelectedItem.viewTop;
                mSelectedItem.viewTop = nextItem.viewTop + nextItem.viewHeight;
                nextItem.animating = true;
                mAnimatingItems.add(nextItem);
                startAnimatingViews();
                mSelectedColumnIndex = mSelectedColumnIndex + 1;
                //We are done. Nothing more to do here
                return;
            }
        }
    }

    private void moveInAnotherColumn(int newColumn) {
        //a bit trickier here first thing let's remove the item from its own column and update the indexes
        List<ItemRepresentation> oldColumn = mColumns.get(mSelectedItemColumn);
        mColumnsMaxVisibleIndex.set(mSelectedItemColumn, mColumnsMaxVisibleIndex.get(mSelectedItemColumn) - 1);
        int newColumnMax = mColumnsMaxVisibleIndex.get(mSelectedItemColumn);
        oldColumn.remove(mSelectedItem);
        for (int i = mSelectedColumnIndex; i < oldColumn.size(); i++) {
            ItemRepresentation previous = i == 0 ? null : oldColumn.get(i - 1);
            ItemRepresentation item = oldColumn.get(i);
            item.viewTop = previous == null ? 0 : previous.viewTop + previous.viewHeight;
            if (i <= newColumnMax) {
                item.animating = true;
                mAnimatingItems.add(item);
            }
        }

        //now let's find a suitable spot in the new column
        List<ItemRepresentation> column = mColumns.get(newColumn);
        int minIndexIntoNewColumn = mColumnsMinVisibleIndex.get(newColumn);
        int maxIndexIntoNewColumn = mColumnsMaxVisibleIndex.get(newColumn);
        int index = -1;
        if (column.size() > 0) {
            boolean indexFound = false;
            int selectedItemCenterY = mSelectedItem.view.getTop() + mSelectedItem.view.getHeight() / 2;
            for (int i = minIndexIntoNewColumn; i <= maxIndexIntoNewColumn && !indexFound; i++) {
                ItemRepresentation item = column.get(i);
                if (selectedItemCenterY > (item.viewTop + mCurrTop) && selectedItemCenterY < (item.viewTop + item.viewHeight + mCurrTop)) {
                    index = i;
                }
            }
            if (index == -1) {
                index = maxIndexIntoNewColumn + 1;
            }
        } else {
            mColumnsMinVisibleIndex.set(newColumn, 0);
            index = 0;
        }
        column.add(index, mSelectedItem);
        mSelectedItem.viewLeft = newColumn * mSelectedItem.view.getWidth();
        maxIndexIntoNewColumn++;
        mColumnsMaxVisibleIndex.set(newColumn, maxIndexIntoNewColumn);
        //TODO optimize me please
        mSelectedItem.viewTop = index == 0 ? 0 : column.get(index - 1).viewTop + column.get(index - 1).viewHeight;
        //And now let's update the top of all items below the newly inserted
        for (int i = index + 1; i < column.size(); i++) {
            ItemRepresentation item = column.get(i);
            ItemRepresentation prev = column.get(i - 1);
            item.viewTop = prev.viewTop + prev.viewHeight;
            if (i <= maxIndexIntoNewColumn) {
                item.animating = true;
                mAnimatingItems.add(item);
            }
        }
        //let's update indexes and we are almost done
        mAdapter.moveItemFromTo(mSelectedItemColumn, mSelectedColumnIndex, newColumn, index);
        fillColumnBottom(mSelectedItemColumn, true);
        mSelectedItemColumn = newColumn;
        mSelectedColumnIndex = index;
        //let's animate everything and now we are really done
        startAnimatingViews();
    }

    private void startAnimatingViews() {
        mGlobalAnimator.stop();
        mGlobalAnimator.start();
    }


    @Override
    public boolean onDown(MotionEvent motionEvent) {
        cancelFling();
        mSelectedItemColumn = findTouchedColumn(motionEvent.getX());
        mSelectedColumnIndex = findTouchedItemInColumn(mSelectedItemColumn, motionEvent.getY());
        if (mSelectedColumnIndex >= 0) {
            mHighlitedItem = mColumns.get(mSelectedItemColumn).get(mSelectedColumnIndex);
            mHighlitedItem.view.setSelected(true);
        }
        mDownY = motionEvent.getRawY();
        mDownX = motionEvent.getRawX();
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float distanceX, float distanceY) {
        scroll(distanceY);
        mIsScrolling = true;
        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        if (mGlobalAnimator.isAnimating())
            return;

        if (mHighlitedItem != null) {
            mSelectedItem = mHighlitedItem;
            detachViewFromParent(mSelectedItem.view);
            attachViewToParent(mSelectedItem.view, getChildCount(), mSelectedItem.view.getLayoutParams());
            startShrinkAnimation();
        }

    }

    private void startShrinkAnimation() {
        ViewPropertyAnimator.animate(mSelectedItem.view).scaleX(.9f).scaleY(.9f).setDuration(200).start();
    }

    private void startExpandAnimation() {
        ViewPropertyAnimator.animate(mSelectedItem.view).scaleX(1f).scaleY(1f).setDuration(200).start();

    }

    private int findTouchedItemInColumn(int columnIdx, float y) {
        int minIndex = mColumnsMinVisibleIndex.get(columnIdx);
        int maxIndex = mColumnsMaxVisibleIndex.get(columnIdx);
        List<ItemRepresentation> column = mColumns.get(columnIdx);
        for (int i = minIndex; i <= maxIndex && i < column.size(); i++) {
            ItemRepresentation item = column.get(i);
            item.view.getHitRect(mTmpRect);
            if (mTmpRect.contains(mTmpRect.left + 1, (int) y)) {
                return i;
            }
        }
        return -1;
    }

    private int findTouchedColumn(float x) {
        int columnWidth = getWidth() / mNumberOfColumns;
        return (int) (x / columnWidth);
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float velocityX, float velocityY) {
        cancelFling();
        mScroller = new Scroller(getContext());
        mScroller.fling(0, 0, 0, (int) velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        mFlingRunnable = new FlingRunnable(mScroller.getCurrY());

        ViewCompat.postOnAnimation(this, mFlingRunnable);
        return true;
    }

    private void cancelFling() {
        if (mFlingRunnable != null) {
            mFlingRunnable.cancelFling();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelFling();
        super.onDetachedFromWindow();
    }

    private class FlingRunnable implements Runnable {

        private int lastY;
        private boolean cancelled;
        private int numberOfStops = 0;

        public FlingRunnable(int startY) {
            lastY = startY;
            cancelled = false;
        }

        @Override
        public void run() {

            if (cancelled) {
                return;
            }
            mScroller.computeScrollOffset();
            int y = mScroller.getCurrY();
            int delta = lastY - y;
            if (delta != 0 && !cancelled) {
                scroll(delta);
                lastY = y;
            } else {
                numberOfStops++;
                if (numberOfStops > 3) {
                    mScroller.abortAnimation();
                    return;
                }
            }
            ViewCompat.postOnAnimation(DynamicGridView.this, this);

        }


        public void cancelFling() {
            mScroller.forceFinished(true);
            cancelled = true;
        }
    }


    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.firstVisibleItemInFirstColumn = mColumnsMinVisibleIndex.get(0);
        ss.offset = mColumns.get(0).get(ss.firstVisibleItemInFirstColumn).view.getTop();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mFirstVisibleItemInFirstColumn = ss.firstVisibleItemInFirstColumn;
        this.mOffsetOfFirstItem = ss.offset;
    }


    static class SavedState extends BaseSavedState {
        int firstVisibleItemInFirstColumn;
        int offset;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.firstVisibleItemInFirstColumn = in.readInt();
            this.offset = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.firstVisibleItemInFirstColumn);
            out.writeInt(this.offset);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
