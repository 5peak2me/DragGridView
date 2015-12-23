package com.jinlin.draggridview.custom_6th;

/**
 * Enum that stored supported scroll directions.
 */
public enum ScrollDirection {
    up(-1), down(1), none(0);

    private int direction;

    ScrollDirection(int direction){
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }
}