/**
 * 
 */
package com.jinlin.draggridview.custom_3rd;

import android.widget.ListAdapter;

/**
 * Use this adapter intead of normal ListAdapter, in case of some item is not
 * reorderable, eg. an special "+" or "more" button in the grid
 * 
 * @author dongxinyu.dxy
 * @author tonywang.wy
 */
public interface DragReorderListAdapter extends ListAdapter {

	/**
	 * 是否是可移动的item
	 * 
	 * @param position
	 * @return
	 */
	public abstract boolean isReorderableItem(int position);

	/**
	 * 是否是可删除的item
	 * 
	 * @param position
	 * @return
	 */
	public abstract boolean isRemovableItem(int position);

}
