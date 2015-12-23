package com.jinlin.draggridview.custom_6th;

import java.util.Stack;

/**
 * A simple class that manages the swap history stack
 */
public class SwapHistory {

    private Stack<GridSwapOperation> opStack;
    private Swapper swapper;

    public SwapHistory(Swapper swapper) {
        this.opStack = new Stack<GridSwapOperation>();
        this.swapper = swapper;
    }

    /**
     * Pushes a new element on to the Swap history
     * @param gridSwapOperation the element that will be pushed
     * @return the size of the stack
     */
    public int push(GridSwapOperation gridSwapOperation){
        opStack.push(gridSwapOperation);
        return opStack.size();
    }

    /**
     * Equivalent to {@link #push(com.authy.dynamicgridview.SwapHistory.GridSwapOperation)}
     */
    public int push(int from, int to){
        return push(new GridSwapOperation(from, to));
    }

    /**
     * Pops the op stack until no elements are left and swaps all elements back into position
     */
    public void reverseOps(){
        while(!opStack.isEmpty()){
            GridSwapOperation gridSwapOp = opStack.pop();
            swapper.swapItems(gridSwapOp.from, gridSwapOp.to);
        }
    }

    /**
     * An abstraction for a class that provides a swap operation over its elements.
     */
    public interface Swapper {

        /**
         * Swaps to elements in the underlying data structure.
         * Correct implementations should follow these guidelines:
         * 1. Both pos1 and pos2 must be >= 0
         * 2. swapItems(pos1, pos2) should be equivalent to swapItems(pos2, pos1)
         * 3. swapItems(A, A) should do nothing.
         * 4. The operation should be reversible i.e. swapItems(A,B); swipeItems(B,A) should do
         * nothing
         *
         * @param pos1 the first item's position
         * @param pos2 the second item's position
         */
        public void swapItems(int pos1, int pos2);
    }

    /**
     * Represents a drag operation in the grid.
     * A drag operation is represented as a pair of positions ({@code from}, {@code to}) where
     * {@code from} indicates the original position and {@code to} the final position.
     */
    public static class GridSwapOperation {

        private int from;
        private int to;

        public GridSwapOperation(int from, int to) {
            this.from = from;
            this.to = to;
        }

        /**
         * @return the first item's position
         */
        public int getFrom() {
            return from;
        }

        /**
         * @return the second item's position
         */
        public int getTo() {
            return to;
        }
    }
}