package com.jinlin.draggridview.custom_3rd;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jinlin.draggridview.R;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends BaseAdapter implements DragReorderListAdapter {

	private List<Item> list;
	private Context mContext;

	public MyAdapter(List<Item> list, Context context) {
		if (list == null) {
			this.list = new ArrayList<Item>();
		} else {
			this.list = list;
		}
		this.mContext = context;
	}

	public List<Item> getData() {
		return list;
	}

	public void removeItem(int position) {
		list.remove(position);
	}

	public void reorder(int from, int to) {
		if (from != to) {
			Item item = list.remove(from);
			list.add(to, item);
			notifyDataSetChanged();
		}
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewGroup result = (ViewGroup) convertView;
		if (result == null) {
			result = (ViewGroup) LayoutInflater.from(mContext).inflate(
					R.layout.item_grid, parent, false);
		}

		TextView textView = (TextView) result.findViewById(R.id.item_title);
		ImageView icon = (ImageView) result.findViewById(R.id.item_img);
		textView.setText(list.get(position).getLabel());
		icon.setImageResource(list.get(position).getIcon());

		return result;
	}

	@Override
	public boolean isReorderableItem(int position) {
		return !list.get(position).isFixed();
	}

	@Override
	public boolean isRemovableItem(int position) {
		return list.get(position).isRemovable();
	}
}
