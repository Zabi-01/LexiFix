package lexifix.ui;

import lexifix.model.TrieNode;
import lexifix.engine.Trie;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * TrieVisualizerPanel
 *
 * Custom JPanel that draws the live Trie tree.
 * getPreferredSize() returns the actual pixel bounds of the drawn content
 * so a JScrollPane can scroll both horizontally and vertically.
 */
public class TrieVisualizerPanel extends JPanel {

    // ── palette ──────────────────────────────────────────────────────────────
    private static final Color BG           = new Color(248, 250, 252);
    private static final Color NODE_PATH    = new Color(37,  99,  235);
    private static final Color NODE_END     = new Color(22,  163,  74);
    private static final Color NODE_CHILD   = new Color(96,  165, 250);
    private static final Color NODE_ROOT    = new Color(100, 116, 139);
    private static final Color EDGE_PATH    = new Color(37,   99, 235);
    private static final Color EDGE_CHILD   = new Color(186, 214, 254);
    private static final Color LABEL_LIGHT  = Color.WHITE;
    private static final Color LABEL_DARK   = new Color(30,  41,  59);
    private static final Color TEXT_HINT    = new Color(148, 163, 184);
    private static final Color TICK_COLOR   = new Color(22,  163,  74);

    // ── geometry (scales with the global font size) ────────────────────────
    private int nodeR    = 15;   // node radius
    private int vGap     = 54;   // vertical gap between levels
    private int hSpread  = 200;  // horizontal spread at first child level
    private int startY   = 50;   // y of root dot
    private int margin   = 40;   // left/right margin around drawn content

    // ── label fonts (scale with the global font size) ──────────────────────
    private int letterFontSize = 12;  // node letter
    private int tickFontSize   = 9;   // end-of-word check mark
    private int hintFontSize   = 13;  // "type to begin" hint
    private static final int MAX_EXTRA = 3;    // extra levels shown beyond prefix tip

    // ── state ────────────────────────────────────────────────────────────────
    private Trie   trie;
    private String prefix = "";
    private float  scale  = 1.0f;   // zoom factor (set from popup toolbar)

    // layout data, rebuilt every paint
    private final List<NodeInfo> nodes = new ArrayList<>();
    private final List<EdgeInfo> edges = new ArrayList<>();
    // actual bounding box of drawn content
    private int contentW = 300;
    private int contentH = 400;

    // ── public API ───────────────────────────────────────────────────────────
    public TrieVisualizerPanel(Trie trie) {
        this.trie = trie;
        setBackground(BG);
        setOpaque(true);
    }

    public void update(String newPrefix) {
        this.prefix = (newPrefix == null) ? "" : newPrefix.toLowerCase().trim();
        // force re-layout + re-paint
        revalidate();
        repaint();
    }

    public void setTrie(Trie t) {
        this.trie = t;
        revalidate();
        repaint();
    }

    /** Set zoom level (1.0 = 100 %, 0.5 = 50 %, 2.0 = 200 %). */
    public void setScale(float s) {
        this.scale = Math.max(0.2f, Math.min(3.0f, s));
        revalidate();
        repaint();
    }

    public float getScale() { return scale; }

    /**
     * Rescales node size, spacing and label fonts to follow the global
     * font-size slider. {@code delta} is the change in the main UI font
     * size (in points); a fraction of it is applied to node geometry so the
     * tree grows/shrinks in proportion without becoming unwieldy.
     */
    public void adjustFontSize(int delta) {
        if (delta == 0) return;
        letterFontSize = clamp(letterFontSize + delta, 8, 28);
        tickFontSize   = clamp(tickFontSize   + delta, 6, 22);
        hintFontSize   = clamp(hintFontSize   + delta, 9, 28);

        int geomDelta = (int) Math.round(delta * 0.6);
        nodeR   = clamp(nodeR   + geomDelta, 10, 30);
        vGap    = clamp(vGap    + delta,     30, 90);
        hSpread = clamp(hSpread + delta * 4, 80, 360);

        revalidate();
        repaint();
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * Return the size of the actual drawn content so the parent
     * JScrollPane can set up both scroll bars correctly.
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(
            (int) ((contentW + margin * 2) * scale),
            (int) ((contentH + margin)     * scale));
    }

    // ── painting ─────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (trie == null || trie.getRoot() == null) {
            hint(g2, "Load a dictionary first"); g2.dispose(); return;
        }
        if (prefix.isEmpty()) {
            hint(g2, "Start typing to visualise the Trie"); g2.dispose(); return;
        }
        TrieNode tip = navigate(trie.getRoot(), prefix);
        if (tip == null) {
            hint(g2, "\"" + prefix + "\" not found in Trie"); g2.dispose(); return;
        }

        // ── step 1: compute layout (positions only, no drawing) ──────────────
        nodes.clear();
        edges.clear();

        // We need to know the total width of the child subtree BEFORE deciding
        // the x-centre of the prefix path.  Run a measure pass first.
        int subtreeWidth = measureSubtree(tip, prefix.length(), prefix.length() + MAX_EXTRA);
        int prefixCX     = Math.max(margin + subtreeWidth / 2, margin + nodeR);

        buildPrefixPath(prefixCX);
        int tipY = startY + prefix.length() * vGap;
        buildChildren(tip, prefix.length(), prefixCX, tipY,
                      prefix.length() + MAX_EXTRA);

        // ── step 2: update bounding box & notify scroll pane ─────────────────
        int maxX = margin, maxY = margin;
        int minX = Integer.MAX_VALUE;
        for (NodeInfo n : nodes) {
            maxX = Math.max(maxX, n.cx + nodeR);
            maxY = Math.max(maxY, n.cy + nodeR + 14);   // +14 for ✓ tick
            minX = Math.min(minX, n.cx - nodeR);
        }
        if (minX == Integer.MAX_VALUE) minX = margin;
        int newW = maxX + margin;
        int newH = maxY + margin;
        if (newW != contentW || newH != contentH) {
            contentW = newW;
            contentH = newH;
            revalidate();   // scroll pane recalculates thumb sizes
        }

        // ── step 3: draw edges then nodes ────────────────────────────────────
        g2.scale(scale, scale);
        for (EdgeInfo e : edges) drawEdge(g2, e);
        for (NodeInfo n : nodes) drawNode(g2, n);

        g2.dispose();
    }

    // ── layout: measure ──────────────────────────────────────────────────────

    /**
     * Returns the pixel width that the subtree rooted at `node` will occupy
     * at the given depth level.
     */
    private int measureSubtree(TrieNode node, int depth, int maxDepth) {
        if (node == null || depth >= maxDepth) return nodeR * 2;
        Map<Character, TrieNode> children = node.getChildren();
        if (children.isEmpty()) return nodeR * 2;

        int extraLevel = depth - prefix.length();
        int spread = Math.max(nodeR * 2 + 8, hSpread >> extraLevel);

        int total = 0;
        for (TrieNode child : children.values()) {
            total += measureSubtree(child, depth + 1, maxDepth);
        }
        return Math.max((children.size() - 1) * spread + nodeR * 2, total);
    }

    // ── layout: build ────────────────────────────────────────────────────────

    private void buildPrefixPath(int cx) {
        // root dot (not a letter, just a visual anchor)
        nodes.add(new NodeInfo('\u25CF', cx, startY - vGap / 2,
                               true, false, true));

        TrieNode cur = trie.getRoot();
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            cur    = cur.getChild(c);
            boolean eow = (cur != null && cur.isEndOfWord());
            int y = startY + (i + 1) * vGap;

            NodeInfo prev = nodes.get(nodes.size() - 1);
            nodes.add(new NodeInfo(c, cx, y, true, eow, false));
            edges.add(new EdgeInfo(prev.cx, prev.cy, cx, y, true));
        }
    }

    private void buildChildren(TrieNode node, int depth,
                               int cx, int parentY, int maxDepth) {
        if (node == null || depth >= maxDepth) return;
        Map<Character, TrieNode> children = node.getChildren();
        if (children.isEmpty()) return;

        int n          = children.size();
        int extraLevel = depth - prefix.length();
        int spread     = Math.max(nodeR * 2 + 8, hSpread >> extraLevel);
        int totalW     = (n - 1) * spread;
        int startX     = cx - totalW / 2;

        NodeInfo parent = findNode(cx, parentY);

        int idx = 0;
        for (Map.Entry<Character, TrieNode> entry : children.entrySet()) {
            char     c      = entry.getKey();
            TrieNode child  = entry.getValue();
            int      childX = startX + idx * spread;
            int      childY = parentY + vGap;

            nodes.add(new NodeInfo(c, childX, childY, false,
                                   child.isEndOfWord(), false));
            if (parent != null)
                edges.add(new EdgeInfo(parent.cx, parent.cy,
                                       childX, childY, false));

            buildChildren(child, depth + 1, childX, childY, maxDepth);
            idx++;
        }
    }

    private NodeInfo findNode(int x, int y) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            NodeInfo n = nodes.get(i);
            if (Math.abs(n.cx - x) < 3 && Math.abs(n.cy - y) < 3) return n;
        }
        return null;
    }

    // ── drawing ──────────────────────────────────────────────────────────────

    private void drawEdge(Graphics2D g2, EdgeInfo e) {
        g2.setColor(e.active ? EDGE_PATH : EDGE_CHILD);
        g2.setStroke(new BasicStroke(e.active ? 2.4f : 1.5f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(e.x1, e.y1, e.x2, e.y2);
    }

    private void drawNode(Graphics2D g2, NodeInfo n) {
        if (n.isRoot) {
            g2.setColor(NODE_ROOT);
            g2.fillOval(n.cx - 6, n.cy - 6, 12, 12);
            return;
        }

        Color fill  = pickFill(n);
        Color label = (n.onPath || n.endOfWord) ? LABEL_LIGHT : LABEL_DARK;

        // shadow
        g2.setColor(new Color(0, 0, 0, 20));
        g2.fillOval(n.cx - nodeR + 2, n.cy - nodeR + 3, nodeR * 2, nodeR * 2);
        // fill
        g2.setColor(fill);
        g2.fillOval(n.cx - nodeR, n.cy - nodeR, nodeR * 2, nodeR * 2);
        // border
        g2.setColor(fill.darker());
        g2.setStroke(new BasicStroke(n.onPath ? 2f : 1f));
        g2.drawOval(n.cx - nodeR, n.cy - nodeR, nodeR * 2, nodeR * 2);
        // letter
        g2.setFont(new Font("Segoe UI", Font.BOLD, letterFontSize));
        g2.setColor(label);
        FontMetrics fm = g2.getFontMetrics();
        String lbl = String.valueOf(n.ch).toUpperCase();
        g2.drawString(lbl,
                n.cx - fm.stringWidth(lbl) / 2,
                n.cy + fm.getAscent() / 2 - 1);
        // end-of-word tick below child nodes
        if (n.endOfWord && !n.onPath) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, tickFontSize));
            g2.setColor(TICK_COLOR);
            g2.drawString("✓", n.cx - 3, n.cy + nodeR + 10);
        }
    }

    private Color pickFill(NodeInfo n) {
        if (n.onPath && n.endOfWord) return NODE_END;
        if (n.onPath)                return NODE_PATH;
        if (n.endOfWord)             return NODE_END;
        return NODE_CHILD;
    }

    private void hint(Graphics2D g2, String msg) {
        g2.setFont(new Font("Segoe UI", Font.ITALIC, hintFontSize));
        g2.setColor(TEXT_HINT);
        FontMetrics fm = g2.getFontMetrics();
        int w = getWidth(), h = getHeight();
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
    }

    private TrieNode navigate(TrieNode root, String pref) {
        TrieNode cur = root;
        for (char c : pref.toCharArray()) {
            if (!cur.hasChild(c)) return null;
            cur = cur.getChild(c);
        }
        return cur;
    }

    // ── inner data ───────────────────────────────────────────────────────────

    private static class NodeInfo {
        final char ch; final int cx, cy;
        final boolean onPath, endOfWord, isRoot;
        NodeInfo(char ch, int cx, int cy,
                 boolean onPath, boolean endOfWord, boolean isRoot) {
            this.ch = ch; this.cx = cx; this.cy = cy;
            this.onPath = onPath; this.endOfWord = endOfWord; this.isRoot = isRoot;
        }
    }

    private static class EdgeInfo {
        final int x1, y1, x2, y2; final boolean active;
        EdgeInfo(int x1, int y1, int x2, int y2, boolean active) {
            this.x1=x1; this.y1=y1; this.x2=x2; this.y2=y2; this.active=active;
        }
    }
}
