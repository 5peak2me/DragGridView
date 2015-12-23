/*
 * This is a modified version of a class from the Android Open Source Project. 
 * The original copyright and license information follows.
 * 
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jinlin.draggridview.custom_6th;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * A DragView is a special view used by a DragController. During a drag operation, what is actually moving
 * on the screen is a DragView. A DragView is constructed using a bitmap of the view the user really
 * wants to move.
 */
@SuppressLint("ViewConstructor")
public class DragView extends View
{
    private Bitmap mBitmap;

    private Paint dragViewPaint;

    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager mWindowManager;

    /**
     * Construct the drag view.
     * <p>
     * The registration point is the point inside our view that the touch events should
     * be centered upon.
     *
     * @param context A context
     * @param view The view that we're dragging around.  We scale it up when we draw it.
     */
    public DragView(Context context, View view) {
        super(context);

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        mBitmap = getBitmapFromView(view);

        dragViewPaint = new Paint();
        dragViewPaint.setColor(Color.RED);
    }

    /**
     * Creates a drawing cache for a given view temporarily and uses it to get the drawing cache
     * as a bitmap. It then returns the drawing cache to it's normal state.
     * @param view the view from which a bitmap will be obtained
     * @return a bitmap representing the view.
     */
    public Bitmap getBitmapFromView(View view){
        boolean isDrawingCacheEnabled = view.isDrawingCacheEnabled();
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = view.getDrawingCache();
        Bitmap result = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * 1.2f), (int) (bitmap.getHeight() * 1.2f), true);
        result = rotateBitmap(result, 5);
        view.setDrawingCacheEnabled(isDrawingCacheEnabled);

        return result;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mBitmap.getWidth(), mBitmap.getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0.0f, 0.0f, dragViewPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBitmap.recycle();
    }

    /**
     * Create a window containing this view and show it.
     *
     * @param windowToken obtained from v.getWindowToken() from one of your views
     * @param touchX the x coordinate the user touched in screen coordinates
     * @param touchY the y coordinate the user touched in screen coordinates
     */
    public void show(IBinder windowToken, int touchX, int touchY) {
        WindowManager.LayoutParams lp;
        int pixelFormat;

        pixelFormat = PixelFormat.TRANSLUCENT;

        lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                touchX - mBitmap.getWidth()/2, touchY - mBitmap.getHeight()/2,
                WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                pixelFormat);

        lp.gravity = Gravity.LEFT | Gravity.TOP;
        lp.token = windowToken;
        lp.setTitle("DragView");
        mLayoutParams = lp;

        mWindowManager.addView(this, lp);
    }

    /**
     * Move the window containing this view.
     *
     * @param touchX the x coordinate the user touched in screen coordinates
     * @param touchY the y coordinate the user touched in screen coordinates
     */
    public void move(int touchX, int touchY) {
        // update the X and Y position
        WindowManager.LayoutParams lp = mLayoutParams;
        lp.x = touchX - mBitmap.getWidth()/2;
        lp.y = touchY - mBitmap.getHeight()/2;
        mWindowManager.updateViewLayout(this, lp);
    }

    /**
     * @return the X center of this view as it is being dragged
     */
    public int getDragX(){
        return mLayoutParams.x;
    }

    /**
     * @return the Y center of this view as it is being dragged
     */
    public int getDragY(){
        return mLayoutParams.y;
    }

    /**
     * Removes the drag view
     */
    public void remove() {
        mWindowManager.removeView(this);
    }
}

