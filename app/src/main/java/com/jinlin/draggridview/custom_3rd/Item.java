package com.jinlin.draggridview.custom_3rd;

public class Item {
	private String label;
	private int icon;
	private boolean isFixed = false;// 固定的item,不可移动,不可删除
	private boolean isRemovable = true;// 不可删除的item，但可以移动

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getIcon() {
		return icon;
	}

	public void setIcon(int icon) {
		this.icon = icon;
	}

	public boolean isFixed() {
		return isFixed;
	}

	public void setFixed(boolean isFixed) {
		this.isFixed = isFixed;
	}

	public boolean isRemovable() {
		return isRemovable;
	}

	public void setRemovable(boolean isRemovable) {
		this.isRemovable = isRemovable;
	}

}
