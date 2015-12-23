package com.jinlin.draggridview.custom_5th;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jinlin.draggridview.custom_5th.Custom5thActivity.Item;

import java.util.List;


/**
 * Created by pasqualeanatriello on 06/03/14.
 */
public class TestAdapter extends DynamicGridAdapter {


    private List<List<Item>> mItems;


    public TestAdapter(List<List<Item>> items) {
        mItems = items;
    }


    @Override
    public int getNumberOfColumns() {
        return 4;
    }

    @Override
    public int getCountForColumn(int column) {
        return mItems.get(column).size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }


    @Override
    public View getView(int column, int row, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = new TextView(viewGroup.getContext());
        }

        TextView tv = (TextView) view;
        Item item=mItems.get(column).get(row);

        StateListDrawable sld = new StateListDrawable();
        ColorDrawable cd = new ColorDrawable(item.color);
        ColorDrawable cd2 = new ColorDrawable(Color.argb(255, 255, 100, 100));
        sld.addState(new int[]{android.R.attr.state_selected}, cd2);
        sld.addState(new int[]{}, cd);
        tv.setBackgroundDrawable(sld);
        tv.setText(item.label);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    @Override
    public float getAspectRatioForItem(int column, int row) {
        return mItems.get(column).get(row).ratio;
    }

    @Override
    public void moveItemFromTo(int col1, int row1, int col2, int row2) {
        Item i = mItems.get(col1).remove(row1);
        mItems.get(col2).add(row2, i);
    }

    @Override
    public void moveUp(int col, int row) {
        Item i = mItems.get(col).remove(row);
        mItems.get(col).add(row - 1, i);
    }

    @Override
    public void moveDown(int col, int row) {
        Item i = mItems.get(col).remove(row);
        mItems.get(col).add(row + 1, i);
    }

}