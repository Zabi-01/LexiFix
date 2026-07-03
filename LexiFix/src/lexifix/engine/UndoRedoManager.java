package lexifix.engine;

import java.util.Stack;

/**
 * UndoRedoManager - Stack-based Undo/Redo for text input
 *
 * DATA STRUCTURE: Stack (LIFO — Last In First Out)
 *
 * Two stacks:
 *   undoStack → holds previous states (push on every change)
 *   redoStack → holds undone states (push when undo is called)
 *
 * OOP: Encapsulation — stacks are private, accessed via push/undo/redo methods
 */
public class UndoRedoManager {

    private Stack<String> undoStack;
    private Stack<String> redoStack;

    private static final int MAX_HISTORY = 100;

    public UndoRedoManager() {
        this.undoStack = new Stack<>();
        this.redoStack = new Stack<>();
        undoStack.push(""); // initial empty state
    }

    /**
     * Push a new state onto the undo stack.
     * Clears redo stack (new action invalidates redo history).
     *
     * FIX: When trimming the stack on overflow, we no longer insert a
     * phantom empty string at the bottom. Instead we simply stop popping
     * once the stack reaches MAX_HISTORY/2, preserving the oldest real state.
     */
    public void push(String state) {
        if (!undoStack.isEmpty() && undoStack.peek().equals(state)) return;

        undoStack.push(state);
        redoStack.clear();

        // Trim oldest entries when stack exceeds MAX_HISTORY
        if (undoStack.size() > MAX_HISTORY) {
            Stack<String> temp = new Stack<>();
            // Save the top half (newest states) into temp
            while (undoStack.size() > MAX_HISTORY / 2) {
                temp.push(undoStack.pop());
            }
            // Discard the bottom half (oldest states) by clearing what remains
            undoStack.clear();
            // Restore the newest states back onto the (now empty) undoStack
            while (!temp.isEmpty()) {
                undoStack.push(temp.pop());
            }
        }
    }

    /**
     * Undo — pop from undoStack, push to redoStack
     * Returns the previous state, or null if nothing to undo
     * O(1)
     */
    public String undo() {
        if (undoStack.size() <= 1) return null;

        String current = undoStack.pop();
        redoStack.push(current);
        return undoStack.peek();
    }

    /**
     * Redo — pop from redoStack, push back to undoStack
     * Returns the redone state, or null if nothing to redo
     * O(1)
     */
    public String redo() {
        if (redoStack.isEmpty()) return null;

        String state = redoStack.pop();
        undoStack.push(state);
        return state;
    }

    /**
     * Peek at current state without modifying stacks
     */
    public String current() {
        return undoStack.isEmpty() ? "" : undoStack.peek();
    }

    /**
     * Reset everything
     */
    public void reset() {
        undoStack.clear();
        redoStack.clear();
        undoStack.push("");
    }

    public boolean canUndo() { return undoStack.size() > 1; }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public int undoCount() { return undoStack.size() - 1; }
    public int redoCount() { return redoStack.size(); }

    public Stack<String> getUndoStack() { return undoStack; }
    public Stack<String> getRedoStack() { return redoStack; }
}
