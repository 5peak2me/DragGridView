package com.jinlin.draggridview.custom_3rd;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.jinlin.draggridview.R;

import java.util.ArrayList;
import java.util.List;

public class Custom3rdActivity extends AppCompatActivity {

	private DragReorderGridView mGridView;
	private MyAdapter mAdapter;
	private List<Item> mItems;

	private String[] sCheeseStrings = { "沪深市场", "港股行情", "天汇宝2号", "融资融券",
			"新股发行", "手机开户", "全部行情", "自选资讯", "港股通", "场内基金" };
	private int[] sCheeseIcons = { R.mipmap.ht_iggt, R.mipmap.ht_ihssc,
			R.mipmap.ht_iqbhq, R.mipmap.ht_iqqhq, R.mipmap.ht_irwz,
			R.mipmap.ht_irzrj, R.mipmap.ht_isjkh, R.mipmap.ht_ithbeh,
			R.mipmap.ht_iwdzx, R.mipmap.ht_ixgsg };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_3rd);

		mGridView = (DragReorderGridView) findViewById(R.id.grid);

		initData();

		mAdapter = new MyAdapter(mItems, this);
		mGridView.setAdapter(mAdapter);
		mGridView
				.setDragReorderListener(R.id.item_delete, mDragReorderListener);
	}

	private void initData() {
		mItems = new ArrayList<Item>();
		int count = sCheeseStrings.length;
		for (int i = 0; i < count; i++) {
			Item item = new Item();
			item.setLabel(sCheeseStrings[i]);
			item.setIcon(sCheeseIcons[i]);
			if (i < 3) {
				item.setRemovable(false);
			}
			mItems.add(item);
		}

		Item addBtn = new Item();
		addBtn.setLabel("");
		addBtn.setIcon(R.mipmap.add_func);
		addBtn.setFixed(true);
		mItems.add(addBtn);

	}

	@Override
	public void onBackPressed() {
		if (mGridView.isDragEditMode()) {
			mGridView.quitEditMode();
			return;
		}
		super.onBackPressed();
	}

	private DragReorderListener mDragReorderListener = new DragReorderListener() {

		@Override
		public void onReorder(int fromPosition, int toPosition) {
			((MyAdapter) mGridView.getAdapter()).reorder(fromPosition,
					toPosition);
		}

		@Override
		public void onDragEnded() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onItemLongClicked(int position) {
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(50);
		}

		@Override
		public void onItemClicked(int position) {
			if (mItems.get(position).getLabel().equals("")) {
				Item item = new Item();
				item.setIcon(R.mipmap.icon);
				int insertPos = mItems.size() - 1;
				item.setLabel("" + insertPos);
				mItems.add(insertPos, item);
				mAdapter.notifyDataSetChanged();
			} else {
				Toast.makeText(Custom3rdActivity.this,
						"click item " + mItems.get(position).getLabel(),
						Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		public void onEditAction(int position) {
			Toast.makeText(Custom3rdActivity.this,
					"deleting " + mAdapter.getData().get(position).getLabel(),
					Toast.LENGTH_SHORT).show();
			mAdapter.removeItem(position);
			mAdapter.notifyDataSetChanged();

		}

	};

}
