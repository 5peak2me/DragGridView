/**
 * 
 */
package com.jinlin.draggridview.custom_3rd;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;

/**
 * 可拖拽排序的gridview，支持添加和删除cell
 * 
 * @author dongxinyu.dxy
 * @author tonywang.wy
 */
public class DragReorderGridView extends GridView {

	private static final int ANIMATION_DURATION = 150; // in ms
	private static final int GRIDVIEW_SCROLL_STEP = 8; // in dp
	private static final int HOVER_AMPLIFY_PERSENCT = -3; // amplify rate when
															// drag start

	private DragReorderListener mDragReorderListener;

	private boolean mEditModeEnabled = false;// 左上角删除按钮是否可用
	private int mEditActionViewId;// 删除按钮的view id

	private boolean mIsEditMode = false;
	private int mEditingPosition = INVALID_POSITION;
	private boolean mIsDragging = false;
	private int mDraggingPosition = INVALID_POSITION;
	private int mFirstDraggingPosition = INVALID_POSITION;
	private int mLastDraggingPosition = INVALID_POSITION;

	private BitmapDrawable mHoverView;
	private int mHoverViewOffsetX = 0;
	private int mHoverViewOffsetY = 0;
	private Rect mHoverViewBounds;

	private int mGridViewScrollStep = 0;

	private int mLastEventY = -1;// 最后touch位置的y坐标
	private int mLastEventX = -1;// 最后touch位置的x坐标

	private boolean mEnableItemClick = true;

	public DragReorderGridView(Context context) {
		this(context, null);
	}

	public DragReorderGridView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DragReorderGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public void setDragReorderListener(int actionViewId,
			DragReorderListener dragReorderListener) {
		mEditModeEnabled = true;
		mEditActionViewId = actionViewId;
		mDragReorderListener = dragReorderListener;
	}

	/**
	 * 返回gridview是否在edit状态<br>
	 * not same as View.isInEditMode()
	 * 
	 * @return
	 */
	public boolean isDragEditMode() {
		return mIsEditMode;
	}

	/**
	 * 初始化<br>
	 * 设置item long click listener<br>
	 * 设置item click listener<br>
	 * 设置scroll listener<br>
	 * 设置mGridViewScrollStep变量
	 */
	private void init() {
		setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (!isReorderable(position)) {
					// return false;
					return true;// 不处理onclick事件
				}

				notifyLongClicked(position);

				if (mEditModeEnabled && isRemovable(position)) {
					quitEditMode();
					enterEditMode(position);
				}

				// 保存开始拖动的位置，在ACTION_UP的时候判断，是否拖动到新位置了
				mFirstDraggingPosition = position;
				startDrag(position);

				return true;
			}
		});

		setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// sometimes don't trigger itemClick,
				// e.g. when the delete showing icon
				if (mEnableItemClick) {
					mDragReorderListener.onItemClicked(position);
				} else {
					mEnableItemClick = true;
				}

			}

		});

		setOnScrollListener(mScrollListener);
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		mGridViewScrollStep = (int) (GRIDVIEW_SCROLL_STEP * metrics.density + 0.5f);
	}

	/**
	 * item左上角delete点击监听器
	 */
	private OnClickListener mEditActionOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			if (mDragReorderListener != null) {
				mDragReorderListener.onEditAction(mEditingPosition);
			}
			quitEditMode();
		}
	};

	/**
	 * 在tiggerPosition位置开始edit模式(左上角delete显示)
	 * 
	 * @param tiggerPosition
	 */
	private void enterEditMode(int tiggerPosition) {
		mEditingPosition = tiggerPosition;
		mIsEditMode = true;
	}

	/**
	 * 取消edit模式(取消左上角的delete按钮)
	 */
	public void quitEditMode() {
		// TODO stopDrag();

		if (!mIsEditMode) {
			return;
		}

		mIsEditMode = false;
		updateEditingPosition(INVALID_POSITION);
		invalidate();
	}

	/**
	 * 开始drag<br>
	 * 设置mIsDragging为true<br>
	 * 创造drag hover drawable<br>
	 * 设置drag item的位置<br>
	 * 设置edit item的位置
	 * 
	 * @param draggingPosition
	 */
	private void startDrag(int draggingPosition) {
		View draggingView = findViewByPosition(draggingPosition);
		if (draggingView == null) {
			return;
		}

		mIsDragging = true;

		updateDraggingPosition(draggingPosition);
		updateEditingPosition(draggingPosition);
		createHoverDrawable(draggingView);
	}

	/**
	 * 停止drag，初始化变量
	 */
	private void stopDrag() {
		if (!mIsDragging) {
			return;
		}

		mIsDragging = false;
		mHoverView = null;
		mIsWaitingForScrollFinish = false;
		mIsMobileScrolling = false;
		notifyDragEnded();
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mHoverView != null) {
			mHoverView.draw(canvas);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			mLastEventX = (int) event.getX();
			mLastEventY = (int) event.getY();

			mEnableItemClick = true;
			if (!mIsEditMode) {
				break;
			}
			// 在edit模式下，取消edit模式时不允许触发onItemClick
			mEnableItemClick = false;
			// layoutChildren();
			// 在edit模式下,再action down则取消edit模式
			quitEditMode();
			break;

		case MotionEvent.ACTION_MOVE:
			if (!mIsDragging) {
				break;
			}

			mLastEventX = (int) event.getX();
			mLastEventY = (int) event.getY();

			hoverViewFollow(mLastEventX, mLastEventY);
			attemptReorder();
			handleScroll();

			return true;

		case MotionEvent.ACTION_UP:
			// 拖动的时候，如果真的拖动了位置，则放手时取消edit模式，不再允许delete
			if (mLastDraggingPosition != mFirstDraggingPosition) {
				quitEditMode();
			}
			finishDrag();
			break;

		case MotionEvent.ACTION_CANCEL:
			stopDrag();
			break;

		default:
			break;
		}

		return super.onTouchEvent(event);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (mIsDragging) {
			for (int i = 0; i < getCount(); i++) {
				setCellDragging(i, i == mDraggingPosition);
			}
		}
		if (mIsEditMode) {
			for (int i = 0; i < getCount(); i++) {
				setCellEditing(i, i == mEditingPosition);
			}
		}
	}

	/**
	 * 拖拽的hover veiw重绘
	 * 
	 * @param x
	 * @param y
	 */
	private void hoverViewFollow(int x, int y) {
		mHoverViewBounds.offsetTo(x + mHoverViewOffsetX, y + mHoverViewOffsetY);
		mHoverView.setBounds(mHoverViewBounds);
		invalidate();
	}

	/**
	 * 判断某位置item是否是可移动的
	 * 
	 * @param position
	 * @return
	 */
	private boolean isReorderable(int position) {
		if (getAdapter() instanceof DragReorderListAdapter) {
			return ((DragReorderListAdapter) getAdapter())
					.isReorderableItem(position);
		} else {
			return true;
		}
	}

	/**
	 * 判断某位置item是否可以删除
	 * 
	 * @param position
	 * @return
	 */
	private boolean isRemovable(int position) {
		if (getAdapter() instanceof DragReorderListAdapter) {
			return ((DragReorderListAdapter) getAdapter())
					.isRemovableItem(position);
		} else {
			return true;
		}
	}

	/**
	 * 根据position获取item view
	 * 
	 * @param position
	 * @return
	 */
	private View findViewByPosition(int position) {
		int firstPosition = this.getFirstVisiblePosition();
		int lastPosition = this.getLastVisiblePosition();

		if (position < firstPosition || position > lastPosition) {
			return null;
		}

		View v = this.getChildAt(position - firstPosition);

		return v;
	}

	/**
	 * Creates the hover cell with the appropriate bitmap and of appropriate
	 * size. The hover cell's BitmapDrawable is drawn on top of the bitmap every
	 * single time an invalidate call is made.
	 */
	private void createHoverDrawable(View v) {
		Bitmap b = snapshotBitmap(v);
		mHoverView = new BitmapDrawable(getResources(), b);

		int w = v.getWidth();
		int h = v.getHeight();
		int top = v.getTop();
		int left = v.getLeft();

		int wAmplified = w * HOVER_AMPLIFY_PERSENCT / 100;
		int hAmplified = h * HOVER_AMPLIFY_PERSENCT / 100;

		mHoverViewBounds = new Rect(left - wAmplified, top - hAmplified, left
				+ w + wAmplified, top + h + hAmplified);

		mHoverViewOffsetX = mHoverViewBounds.left - mLastEventX;
		mHoverViewOffsetY = mHoverViewBounds.top - mLastEventY;

		mHoverView.setBounds(mHoverViewBounds);
	}

	/**
	 * Returns a bitmap showing a screenshot of the view passed in.
	 */
	private Bitmap snapshotBitmap(View v) {
		Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		v.draw(canvas);
		return bitmap;
	}

	/**
	 * 通知客户端对adapter数据重新排序
	 * 
	 * @param from
	 * @param to
	 */
	private void notifyReorder(int from, int to) {
		if (mDragReorderListener == null) {
			return;
		}
		mDragReorderListener.onReorder(from, to);
	}

	/**
	 * 通知客户端drag结束
	 */
	private void notifyDragEnded() {
		if (mDragReorderListener == null) {
			return;
		}
		mDragReorderListener.onDragEnded();
	}

	/**
	 * 调用mDragReorderListener接口的onItemLongClicked方法
	 * {@link DragReorderListener#onItemLongClicked()}
	 */
	private void notifyLongClicked(int position) {
		if (mDragReorderListener == null) {
			return;
		}
		mDragReorderListener.onItemLongClicked(position);
	}

	/**
	 * move过程中重排view和数据
	 */
	private void attemptReorder() {
		int x = mLastEventX;
		int y = mLastEventY;

		final int targetPosition = pointToPosition(x, y);
		if (targetPosition == AdapterView.INVALID_POSITION
				|| targetPosition == mDraggingPosition) {
			return;
		}

		if (!isReorderable(targetPosition)) {
			return;
		}

		final int origPosition = mDraggingPosition;
		notifyReorder(origPosition, targetPosition);
		updateDraggingPosition(targetPosition);
		updateEditingPosition(targetPosition);
		final ViewTreeObserver observer = getViewTreeObserver();
		if (observer != null) {
			observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				@Override
				public boolean onPreDraw() {
					observer.removeOnPreDrawListener(this);
					animationReorder(origPosition, targetPosition);
					return true;
				}
			});
		}

	}

	/**
	 * 重排动画
	 * 
	 * @param oldPosition
	 * @param newPosition
	 */
	private void animationReorder(int oldPosition, int newPosition) {
		boolean isForward = newPosition > oldPosition;
		int fromX;
		int toX;
		int fromY;
		int toY;
		if (isForward) {
			for (int pos = Math.min(oldPosition, newPosition); pos < Math.max(
					oldPosition, newPosition); pos++) {
				View view = findViewByPosition(pos);
				if (view == null) {
					continue;
				}
				if ((pos + 1) % getNumColumnsCompat() == 0) {
					fromX = -view.getWidth() * (getNumColumnsCompat() - 1);
					toX = 0;
					fromY = view.getHeight();
					toY = 0;

				} else {
					fromX = view.getWidth();
					toX = 0;
					fromY = 0;
					toY = 0;
				}
				TranslateAnimation translate = new TranslateAnimation(
						Animation.ABSOLUTE, fromX, Animation.ABSOLUTE, toX,
						Animation.ABSOLUTE, fromY, Animation.ABSOLUTE, toY);
				translate.setDuration(ANIMATION_DURATION);
				translate.setFillEnabled(true);
				translate.setFillAfter(false);
				view.clearAnimation();
				view.startAnimation(translate);
			}
		} else {
			for (int pos = Math.max(oldPosition, newPosition); pos > Math.min(
					oldPosition, newPosition); pos--) {
				View view = findViewByPosition(pos);
				if (view == null) {
					continue;
				}
				if ((pos + getNumColumnsCompat()) % getNumColumnsCompat() == 0) {
					fromX = view.getWidth() * (getNumColumnsCompat() - 1);
					toX = 0;
					fromY = -view.getHeight();
					toY = 0;
				} else {
					fromX = -view.getWidth();
					toX = 0;
					fromY = 0;
					toY = 0;
				}
				TranslateAnimation translate = new TranslateAnimation(
						Animation.ABSOLUTE, fromX, Animation.ABSOLUTE, toX,
						Animation.ABSOLUTE, fromY, Animation.ABSOLUTE, toY);
				translate.setDuration(ANIMATION_DURATION);
				translate.setFillEnabled(true);
				translate.setFillAfter(false);
				view.clearAnimation();
				view.startAnimation(translate);
			}
		}
	}

	/**
	 * 获取gridview的列数
	 * 
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private int getNumColumnsCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return getNumColumns();
		} else {
			int columns = 0;
			int children = getChildCount();
			if (children > 0) {
				int width = getChildAt(0).getMeasuredWidth();
				if (width > 0) {
					columns = getWidth() / width;
				}
			}
			return columns > 0 ? columns : AUTO_FIT;
		}
	}

	/**
	 * 设置被拖拽的item位置
	 * 
	 * @param newPosition
	 */
	private void updateDraggingPosition(int newPosition) {
		if (newPosition != INVALID_POSITION) {
			mLastDraggingPosition = newPosition;
		}
		setCellDragging(mDraggingPosition, false);
		mDraggingPosition = newPosition;
		setCellDragging(mDraggingPosition, true);
	}

	/**
	 * 设置item左上角delete按钮可用
	 * 
	 * @param newPosition
	 */
	private void updateEditingPosition(int newPosition) {
		setCellEditing(mEditingPosition, false);
		mEditingPosition = newPosition;
		setCellEditing(mEditingPosition, true);
	}

	/**
	 * 设置item的左上角delete按钮是否显示
	 * 
	 * @param position
	 * @param isEditing
	 */
	private void setCellEditing(int position, boolean isEditing) {
		View editingCell = findViewByPosition(position);
		if (editingCell == null || !(editingCell instanceof ViewGroup)) {
			return;
		}

		if (!isRemovable(position) && isEditing) {
			// 不可删除的item，不允许设置actionView可见
			return;
		}

		View actionView = ((ViewGroup) editingCell)
				.findViewById(mEditActionViewId);
		if (actionView == null) {
			return;
		}

		actionView.setVisibility(isEditing ? VISIBLE : INVISIBLE);
		actionView.setOnClickListener(isEditing ? mEditActionOnClickListener
				: null);
	}

	/**
	 * 设置被drag的item不可见
	 * 
	 * @param position
	 * @param isDragging
	 */
	private void setCellDragging(int position, boolean isDragging) {
		View cell = findViewByPosition(position);
		if (cell == null) {
			return;
		}
		cell.setVisibility(isDragging ? INVISIBLE : VISIBLE);
	}

	/**
	 * 完成drag<br>
	 * 设置hover到target的动画<br>
	 * 调用{@link DragReorderGridView#stopDrag()}方法
	 */
	private void finishDrag() {
		if (mIsDragging || mIsWaitingForScrollFinish) {

			// If the autoscroller has not completed scrolling, we need to wait
			// for it to
			// finish in order to determine the final location of where the
			// hover cell
			// should be animated to.
			if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
				mIsWaitingForScrollFinish = true;
				return;
			}

			View draggingView = findViewByPosition(mDraggingPosition);
			TranslateAnimation translate = new TranslateAnimation(
					Animation.ABSOLUTE, mHoverViewBounds.left
							- draggingView.getLeft(), Animation.ABSOLUTE, 0,
					Animation.ABSOLUTE, mHoverViewBounds.top
							- draggingView.getTop(), Animation.ABSOLUTE, 0);
			translate.setDuration(ANIMATION_DURATION);
			translate.setFillEnabled(true);
			translate.setFillAfter(false);
			draggingView.clearAnimation();
			draggingView.startAnimation(translate);
			translate.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation arg0) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationRepeat(Animation arg0) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationEnd(Animation arg0) {
					// reset();
				}
			});

			mHoverView = null;
			updateDraggingPosition(INVALID_POSITION);
			invalidate();
		}

		stopDrag();
	}

	private boolean mIsMobileScrolling = false;

	private void handleScroll() {
		mIsMobileScrolling = handleScroll(mHoverViewBounds);
	}

	private boolean handleScroll(Rect r) {
		int offset = computeVerticalScrollOffset();
		int height = getHeight();
		int extent = computeVerticalScrollExtent();
		int range = computeVerticalScrollRange();
		int hoverViewTop = r.top;
		int hoverHeight = r.height();

		if (hoverViewTop <= 0 && offset > 0) {
			smoothScrollBy(-mGridViewScrollStep, 0);
			return true;
		}

		if (hoverViewTop + hoverHeight >= height && (offset + extent) < range) {
			smoothScrollBy(mGridViewScrollStep, 0);
			return true;
		}

		return false;
	}

	private boolean mIsWaitingForScrollFinish = false;
	private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

	/**
	 * This scroll listener is added to the gridview in order to handle cell
	 * swapping when the cell is either at the top or bottom edge of the
	 * gridview. If the hover cell is at either edge of the gridview, the
	 * gridview will begin scrolling. As scrolling takes place, the gridview
	 * continuously checks if new cells became visible and determines whether
	 * they are potential candidates for a cell swap.
	 */
	private OnScrollListener mScrollListener = new OnScrollListener() {

		private int mPreviousFirstVisibleItem = -1;
		private int mPreviousVisibleItemCount = -1;
		private int mCurrentFirstVisibleItem;
		private int mCurrentVisibleItemCount;
		private int mCurrentScrollState;

		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			mCurrentFirstVisibleItem = firstVisibleItem;
			mCurrentVisibleItemCount = visibleItemCount;

			mPreviousFirstVisibleItem = (mPreviousFirstVisibleItem == -1) ? mCurrentFirstVisibleItem
					: mPreviousFirstVisibleItem;
			mPreviousVisibleItemCount = (mPreviousVisibleItemCount == -1) ? mCurrentVisibleItemCount
					: mPreviousVisibleItemCount;

			checkAndHandleFirstVisibleCellChange();
			checkAndHandleLastVisibleCellChange();

			mPreviousFirstVisibleItem = mCurrentFirstVisibleItem;
			mPreviousVisibleItemCount = mCurrentVisibleItemCount;
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			mCurrentScrollState = scrollState;
			mScrollState = scrollState;
			isScrollCompleted();
		}

		/**
		 * This method is in charge of invoking 1 of 2 actions. Firstly, if the
		 * gridview is in a state of scrolling invoked by the hover cell being
		 * outside the bounds of the gridview, then this scrolling event is
		 * continued. Secondly, if the hover cell has already been released,
		 * this invokes the animation for the hover cell to return to its
		 * correct position after the gridview has entered an idle scroll state.
		 */
		private void isScrollCompleted() {
			if (mCurrentVisibleItemCount > 0
					&& mCurrentScrollState == SCROLL_STATE_IDLE) {
				if (mIsDragging && mIsMobileScrolling) {
					handleScroll();
				} else if (mIsWaitingForScrollFinish) {
					finishDrag();
				}
			}
		}

		/**
		 * Determines if the gridview scrolled up enough to reveal a new cell at
		 * the top of the list. If so, then the appropriate parameters are
		 * updated.
		 */
		public void checkAndHandleFirstVisibleCellChange() {
			if (mCurrentFirstVisibleItem != mPreviousFirstVisibleItem) {
				if (mIsDragging && mDraggingPosition != INVALID_POSITION) {
					attemptReorder();
				}
			}
		}

		/**
		 * Determines if the gridview scrolled down enough to reveal a new cell
		 * at the bottom of the list. If so, then the appropriate parameters are
		 * updated.
		 */
		public void checkAndHandleLastVisibleCellChange() {
			int currentLastVisibleItem = mCurrentFirstVisibleItem
					+ mCurrentVisibleItemCount;
			int previousLastVisibleItem = mPreviousFirstVisibleItem
					+ mPreviousVisibleItemCount;
			if (currentLastVisibleItem != previousLastVisibleItem) {
				if (mIsDragging && mDraggingPosition != INVALID_POSITION) {
					attemptReorder();
				}
			}
		}
	};
}
