package com.jinlin.draggridview.custom_5th;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by pasqualeanatriello on 01/03/14.
 *
 * The adapter for the DynamicGridView similar to the BaseAdapter concept
 */
public abstract class DynamicGridAdapter {


    private DataSetObserver mDataSetObserver;


    public abstract int getNumberOfColumns();

    public abstract int getCountForColumn(int column);

    public abstract boolean isEmpty();

    public abstract View getView(int column, int row, View convertView, ViewGroup container);

    // aspect ratio defined as width/height
    public abstract float getAspectRatioForItem(int column, int row);

    public void setDataSetObserver(DataSetObserver dataSetObserver) {
        mDataSetObserver = dataSetObserver;
    }

    public void notifyDatasetChanged() {
        if (mDataSetObserver != null) {
            mDataSetObserver.onChanged();
        }
    }

    public void notifyDatasetInvalidated() {
        if (mDataSetObserver != null) {
            mDataSetObserver.onInvalidated();
        }
    }

    public abstract void moveItemFromTo(int col1, int row1, int col2, int row2);

    public abstract void moveUp(int col,int row);
    public abstract void moveDown(int col,int row);

}
