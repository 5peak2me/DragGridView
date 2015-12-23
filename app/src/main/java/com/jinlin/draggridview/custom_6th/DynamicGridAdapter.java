package com.jinlin.draggridview.custom_6th;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by fernandinho on 10/27/14.
 */
public abstract class DynamicGridAdapter<T> extends BaseAdapter {

    private List<T> data;
    private int draggingPosition;
    private int draggingOver;

    public DynamicGridAdapter(){
        super();
        data = new ArrayList<T>();
        draggingPosition = -1;
        draggingOver = -1;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public T getItem(int position) {
        return data.get(position);
    }

    public void add(T item){
        data.add(item);
    }

    public void addAll(Collection<T> items){
        data.addAll(items);
        notifyDataSetChanged();
    }

    public void setAll(Collection<T> items){
        data.clear();
        data.addAll(items);
        notifyDataSetChanged();
    }

    public void addAll(T... items){
        addAll(Arrays.asList(items));
    }

    public void setAll(T... items){
        setAll(Arrays.asList(items));
    }

    public void swap(int pos1, int pos2){
        T first = data.get(pos1);
        T second = data.get(pos2);
        data.set(pos1, second);
        data.set(pos2, first);
    }

    @Override
    public long getItemId(int position) {
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = getViewItem(position, convertView, parent);
        view.setVisibility(draggingPosition == position ? View.INVISIBLE : View.VISIBLE);
        view.setAlpha(draggingOver == position ? 0.5f : 1.0f);
        return view;
    }

    public abstract View getViewItem(int position, View convertView, ViewGroup parent);

    public void setDraggingPosition(int draggingPosition) {
        this.draggingPosition = draggingPosition;
    }

    public void setDraggingOver(int draggingOver) {
        this.draggingOver = draggingOver;
    }
}
