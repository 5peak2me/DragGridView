package com.jinlin.draggridview.custom_5th;

import android.view.View;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pasqualeanatriello on 01/03/14.
 */

public class CellRecicleBin {


    /**
     * Our view cache. We want to arrange Views depending on their type
     */

    private HashMap<Float, List<View>> mCache;


    public CellRecicleBin() {
        mCache = new HashMap<>();
    }

    public View get(float type) {
        List<View> itemsForType = mCache.get(type);
        if (itemsForType != null && itemsForType.size() > 0) {
            return itemsForType.remove(0);
        }
        return null;
    }

    public void put(View view, float ratio) {
        List<View> itemsForType = mCache.get(ratio);
        if (itemsForType == null) {
            itemsForType = new LinkedList<View>();
            mCache.put(ratio, itemsForType);
        }
        itemsForType.add(view);
    }

}
