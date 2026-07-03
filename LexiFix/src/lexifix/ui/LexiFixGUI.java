package lexifix.ui;

import lexifix.engine.Dictionary;
import lexifix.engine.AutocompleteEngine;
import lexifix.engine.SpellCorrectEngine;
import lexifix.engine.SpellChecker;
import lexifix.engine.UndoRedoManager;
import lexifix.engine.BigramGraph;
import lexifix.model.WordEntry;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import javax.swing.AbstractAction;
import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * LexiFixGUI - Clean light-theme UI with adjustable font size
 * Panels: Autocomplete (Trie) | Spell Check (DP+HashMap) |
 *         Phrase Suggestions (Graph) | Undo/Redo (Stack)
 */
public class LexiFixGUI extends JFrame {

    // ── Light Palette ────────────────────────────────────────────────────────
    private static final Color BG_APP       = new Color(245, 247, 250);
    private static final Color BG_CARD      = Color.WHITE;
    private static final Color BG_INPUT     = new Color(250, 251, 253);
    private static final Color BG_ROW_ALT   = new Color(248, 249, 252);
    private static final Color BG_TOPBAR    = Color.WHITE;

    private static final Color BLUE         = new Color(37, 99, 235);
    private static final Color BLUE_LIGHT   = new Color(219, 234, 254);
    private static final Color BLUE_TEXT    = new Color(29, 78, 216);
    private static final Color GREEN        = new Color(22, 163, 74);
    private static final Color GREEN_LIGHT  = new Color(220, 252, 231);
    private static final Color RED          = new Color(220, 38, 38);
    private static final Color RED_LIGHT    = new Color(254, 226, 226);
    private static final Color AMBER        = new Color(180, 110, 0);
    private static final Color AMBER_LIGHT  = new Color(254, 243, 199);
    private static final Color PURPLE       = new Color(124, 58, 237);
    private static final Color PURPLE_LIGHT = new Color(237, 233, 254);
    private static final Color ORANGE       = new Color(194, 65, 12);
    private static final Color ORANGE_LIGHT = new Color(255, 237, 213);

    private static final Color TEXT_MAIN    = new Color(15, 23, 42);
    private static final Color TEXT_SUB     = new Color(100, 116, 139);
    private static final Color TEXT_MUTED   = new Color(148, 163, 184);
    private static final Color BORDER       = new Color(226, 232, 240);
    private static final Color BORDER_FOCUS = new Color(147, 197, 253);
    private static final Color DIVIDER      = new Color(241, 245, 249);

    // ── Dynamic fonts (rebuilt when size changes) ─────────────────────────
    private int fontSize = 14;
    private static final int FONT_SIZE_MIN = 10;
    private static final int FONT_SIZE_MAX = 28;
    private Font fontTitle, fontHeading, fontBody, fontMono, fontBadge, fontSmall;

    // Card header rows whose preferred/maximum height depends on fontTitle's
    // size — rescaled whenever the global font size changes.
    private final List<JPanel> headerRows = new ArrayList<>();

    // ── Engines ──────────────────────────────────────────────────────────────
    private Dictionary         dictionary;
    private AutocompleteEngine autocompleteEngine;
    private SpellCorrectEngine spellCorrectEngine;
    private UndoRedoManager    undoRedo;
    private BigramGraph        bigramGraph;

    // ── UI Components ────────────────────────────────────────────────────────
    private JTextField inputField;
    private JLabel     statusLabel, wordCountLabel, spellStatusIcon;
    private JSlider    fontSlider;
    private JLabel     fontSizeLabel;

    // Autocomplete
    private DefaultListModel<String> autocompleteModel;
    private JList<String>            autocompleteList;
    private JLabel                   trieInfoLabel;

    // Spell check
    private DefaultListModel<String> correctionModel;
    private JList<String>            correctionList;
    private JLabel                   editDistanceLabel, hashInfoLabel;
    private JTextArea                infoArea;

    // History
    private DefaultListModel<String> historyModel;
    private JList<String>            historyList;
    private JLabel                   historyCountLabel;

    // Phrase suggestions
    private DefaultListModel<String> phraseModel;
    private JList<String>            phraseList;
    private JLabel                   graphInfoLabel;

    // Undo/Redo
    private DefaultListModel<String> undoStackModel;
    private JList<String>            undoStackList;
    private DefaultListModel<String> redoStackModel;
    private JList<String>            redoStackList;
    private JLabel                   undoCountLabel;
    private JButton                  btnUndo, btnRedo;

    // Panels that need font refresh
    private JPanel topBar, mainPanel, bottomBar;
    private TrieVisualizerPanel trieVisualizer;
    private TrieVisualizerPanel popupTrieVisualizer; // mirrors live trie in the popup

    private JFileChooser fileChooser;
    private Timer        debounceTimer;
    private boolean      isUndoRedoAction = false;

    // ── Constructor ──────────────────────────────────────────────────────────
    public LexiFixGUI() {
        rebuildFonts();
        initEngines();
        initUI();
        loadDefaultDictionary();
    }

    private void rebuildFonts() {
        fontTitle   = new Font("Segoe UI", Font.BOLD,  fontSize + 8);
        fontHeading = new Font("Segoe UI", Font.BOLD,  fontSize + 1);
        fontBody    = new Font("Segoe UI", Font.PLAIN, fontSize);
        fontMono    = new Font("Consolas", Font.PLAIN, fontSize);
        fontBadge   = new Font("Segoe UI", Font.BOLD,  fontSize - 1);
        fontSmall   = new Font("Segoe UI", Font.PLAIN, fontSize - 2);
    }

    private void initEngines() {
        dictionary         = new Dictionary();
        autocompleteEngine = new AutocompleteEngine(dictionary, 8);
        spellCorrectEngine = new SpellCorrectEngine(dictionary, 5);
        undoRedo           = new UndoRedoManager();
        bigramGraph        = new BigramGraph();
        bigramGraph.loadDefaultPhrases();
    }

    // ── UI BUILD ─────────────────────────────────────────────────────────────
    private void initUI() {
        setTitle("LexiFix  —  Autocomplete · Spell Check · Undo/Redo · Phrase Suggestions");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 860);
        setMinimumSize(new Dimension(1200, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_APP);
        setLayout(new BorderLayout(0, 0));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        debounceTimer = new Timer(120, e -> processInput());
        debounceTimer.setRepeats(false);
    }

    // ── TOP BAR ──────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        topBar = new JPanel(new BorderLayout(16, 0));
        topBar.setBackground(BG_TOPBAR);
        topBar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));

        // Logo
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoPanel.setBackground(BG_TOPBAR);
        JLabel logo = new JLabel("</> LexiFix");
        logo.setFont(fontTitle);
        logo.setForeground(BLUE);
        JLabel version = new JLabel("  v2");
        version.setFont(fontBody);
        version.setForeground(TEXT_MUTED);
        logoPanel.add(logo);
        logoPanel.add(version);

        // Center: spell icon + input + undo/redo + word count
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        center.setBackground(BG_TOPBAR);

        spellStatusIcon = new JLabel("O");
        spellStatusIcon.setFont(new Font("Segoe UI", Font.BOLD, fontSize + 4));
        spellStatusIcon.setForeground(TEXT_MUTED);
        spellStatusIcon.setToolTipText("Green = correct  Red = misspelled");

        inputField = new JTextField(36);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, fontSize + 2));
        inputField.setBackground(BG_INPUT);
        inputField.setForeground(TEXT_MAIN);
        inputField.setCaretColor(BLUE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        inputField.setToolTipText("Type here — autocomplete, spell check and suggestions update live");

        btnUndo = makePillButton("↩ Undo", AMBER, AMBER_LIGHT);
        btnRedo = makePillButton("↪ Redo", GREEN, GREEN_LIGHT);
        btnUndo.setEnabled(false);
        btnRedo.setEnabled(false);
        btnUndo.setToolTipText("Ctrl+Z");
        btnRedo.setToolTipText("Ctrl+Y");
        btnUndo.addActionListener(e -> performUndo());
        btnRedo.addActionListener(e -> performRedo());

        wordCountLabel = makeChip("0 words", PURPLE, PURPLE_LIGHT);

        center.add(spellStatusIcon);
        center.add(inputField);
        center.add(btnUndo);
        center.add(btnRedo);
        center.add(wordCountLabel);

        // Right: font size control
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setBackground(BG_TOPBAR);
        JLabel fontIcon = new JLabel("A");
        fontIcon.setFont(new Font("Segoe UI", Font.BOLD, fontSize - 2));
        fontIcon.setForeground(TEXT_SUB);
        fontSizeLabel = new JLabel(fontSize + "px");
        fontSizeLabel.setFont(fontBadge);
        fontSizeLabel.setForeground(TEXT_SUB);
        fontSlider = new JSlider(FONT_SIZE_MIN, FONT_SIZE_MAX, fontSize);
        fontSlider.setBackground(BG_TOPBAR);
        fontSlider.setPreferredSize(new Dimension(110, 28));
        fontSlider.setToolTipText("Adjust font size");
        fontSlider.addChangeListener(e -> {
            int newSize = fontSlider.getValue();
            int delta   = newSize - fontSize;
            if (delta == 0) return;
            fontSize = newSize;
            fontSizeLabel.setText(fontSize + "px");
            rebuildFonts();
            applyFontsGlobally(delta);
        });
        JLabel fontIconLg = new JLabel("A");
        fontIconLg.setFont(new Font("Segoe UI", Font.BOLD, fontSize + 2));
        fontIconLg.setForeground(TEXT_SUB);

        rightPanel.add(fontIcon);
        rightPanel.add(fontSlider);
        rightPanel.add(fontIconLg);
        rightPanel.add(fontSizeLabel);

        // Keyboard shortcuts — use InputMap/ActionMap so bindings fire reliably
        // regardless of focus or Swing's default text-field key handling.
        InputMap  im = inputField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = inputField.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");

        am.put("undo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { performUndo(); }
        });
        am.put("redo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { performRedo(); }
        });

        inputField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    autocompleteList.requestFocus();
                    if (autocompleteList.getModel().getSize() > 0) autocompleteList.setSelectedIndex(0);
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String sentence = inputField.getText().trim();
                    if (sentence.split("\\s+").length >= 2) {
                        bigramGraph.learnFromText(sentence);
                        graphInfoLabel.setText("Graph: " + bigramGraph.getVocabularySize()
                            + " nodes, " + bigramGraph.getTotalBigrams() + " edges (learned!)");
                        setStatus("Learned: " + sentence, ORANGE);
                    }
                }
            }
        });

        inputField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { onTextChanged(); }
            public void removeUpdate(DocumentEvent e)  { onTextChanged(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        topBar.add(logoPanel,  BorderLayout.WEST);
        topBar.add(center,     BorderLayout.CENTER);
        topBar.add(rightPanel, BorderLayout.EAST);
        return topBar;
    }

    // ── MAIN PANEL ───────────────────────────────────────────────────────────
    private JPanel buildMainPanel() {
        // Left: 4 original panels in equal columns
        JPanel leftPanels = new JPanel(new GridLayout(1, 4, 10, 0));
        leftPanels.setBackground(BG_APP);
        leftPanels.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 6));
        leftPanels.add(buildAutocompletePanel());
        leftPanels.add(buildSpellPanel());
        leftPanels.add(buildPhrasePanel());
        leftPanels.add(buildUndoRedoPanel());

        // Right: Trie Visualizer with fixed preferred width
        JPanel rightPanel = buildTrieVisualizerPanel();
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setBackground(BG_APP);
        rightWrapper.setBorder(BorderFactory.createEmptyBorder(14, 6, 14, 14));
        rightWrapper.add(rightPanel, BorderLayout.CENTER);
        rightWrapper.setPreferredSize(new Dimension(460, 0));
        rightWrapper.setMinimumSize(new Dimension(360, 0));

        // Split pane: resize weight 1.0 means left takes extra space on resize
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanels, rightWrapper);
        split.setResizeWeight(1.0);
        split.setDividerSize(6);
        split.setDividerLocation(1110);
        split.setBorder(null);
        split.setBackground(BG_APP);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_APP);
        mainPanel.add(split, BorderLayout.CENTER);
        return mainPanel;
    }

    // ── AUTOCOMPLETE PANEL ───────────────────────────────────────────────────
    private JPanel buildAutocompletePanel() {
        JPanel card = makeCard();

        JPanel header = makeCardHeader("[Search]  Autocomplete", BLUE, BLUE_LIGHT,
            "Trie Prefix Tree  •  O(L)");
        trieInfoLabel = makeInfoChip("Waiting for input…", BLUE, BLUE_LIGHT);

        autocompleteModel = new DefaultListModel<>();
        autocompleteList  = buildList(autocompleteModel, BLUE, BLUE_LIGHT);
        autocompleteList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String s = autocompleteList.getSelectedValue();
                if (s != null) applyWord(extractWord(s));
            }
        });
        autocompleteList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String s = autocompleteList.getSelectedValue();
                    if (s != null) applyWord(extractWord(s));
                }
            }
        });

        // History sub-section
        JPanel histSep   = makeSeparator();
        JPanel histHeader = makeCardHeader("[History]  Search History", AMBER, AMBER_LIGHT,
            "LinkedList  •  LIFO display");
        historyCountLabel = makeInfoChip("0 items", AMBER, AMBER_LIGHT);
        historyModel = new DefaultListModel<>();
        historyList  = buildList(historyModel, AMBER, AMBER_LIGHT);
        historyList.setVisibleRowCount(4);

        card.add(header);
        card.add(Box.createVerticalStrut(6));
        card.add(trieInfoLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(styledScroll(autocompleteList, BLUE,  220));
        card.add(Box.createVerticalStrut(12));
        card.add(histSep);
        card.add(Box.createVerticalStrut(10));
        card.add(histHeader);
        card.add(Box.createVerticalStrut(6));
        card.add(historyCountLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(styledScroll(historyList, AMBER, 130));
        return card;
    }

    // ── SPELL CHECK PANEL ────────────────────────────────────────────────────
    private JPanel buildSpellPanel() {
        JPanel card = makeCard();

        JPanel header = makeCardHeader("[Edit]  Spell Corrections", RED, RED_LIGHT,
            "Levenshtein DP  •  HashMap O(1)");
        editDistanceLabel = makeInfoChip("Edit distance: —", RED, RED_LIGHT);
        hashInfoLabel     = makeInfoChip("HashMap: —", RED, RED_LIGHT);

        correctionModel = new DefaultListModel<>();
        correctionList  = buildList(correctionModel, RED, RED_LIGHT);
        correctionList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String s = correctionList.getSelectedValue();
                if (s != null) applyWord(extractWord(s));
            }
        });

        // Analysis sub-section
        JPanel anaSep    = makeSeparator();
        JPanel anaHeader = makeCardHeader("[Stats]  Analysis", PURPLE, PURPLE_LIGHT, "");

        infoArea = new JTextArea(6, 0);
        infoArea.setFont(fontMono);
        infoArea.setBackground(DIVIDER);
        infoArea.setForeground(TEXT_MAIN);
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        infoArea.setText("Type to begin…");

        // Buttons
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        btnRow.setBackground(BG_CARD);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnLoad = makePillButton("[Load]  Load Dict",  BLUE,  BLUE_LIGHT);
        JButton btnAdd  = makePillButton("[+]  Add Word",   GREEN, GREEN_LIGHT);
        btnLoad.addActionListener(e -> loadDictionaryFromFile());
        btnAdd.addActionListener(e -> addWordDialog());
        btnRow.add(btnLoad);
        btnRow.add(btnAdd);

        card.add(header);
        card.add(Box.createVerticalStrut(6));
        card.add(editDistanceLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(hashInfoLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(styledScroll(correctionList, RED,    180));
        card.add(Box.createVerticalStrut(12));
        card.add(anaSep);
        card.add(Box.createVerticalStrut(10));
        card.add(anaHeader);
        card.add(Box.createVerticalStrut(6));
        card.add(styledScroll(infoArea, PURPLE,        180));
        card.add(Box.createVerticalStrut(10));
        card.add(btnRow);
        return card;
    }

    // ── PHRASE PANEL ─────────────────────────────────────────────────────────
    private JPanel buildPhrasePanel() {
        JPanel card = makeCard();

        JPanel header = makeCardHeader("[Phrases]  Phrase Suggestions", ORANGE, ORANGE_LIGHT,
            "Graph (Adjacency Map)  •  Bigrams");
        graphInfoLabel = makeInfoChip("Graph: 0 nodes, 0 edges", ORANGE, ORANGE_LIGHT);

        phraseModel = new DefaultListModel<>();
        phraseList  = buildList(phraseModel, ORANGE, ORANGE_LIGHT);
        phraseList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String s = phraseList.getSelectedValue();
                if (s != null) applyNextWord(extractWord(s));
            }
        });

        // Explanation box
        JPanel expSep    = makeSeparator();
        JPanel expHeader = makeCardHeader("[Info]  How it works", ORANGE, ORANGE_LIGHT, "");
        JTextArea expArea = new JTextArea();
        expArea.setFont(fontSmall);
        expArea.setBackground(DIVIDER);
        expArea.setForeground(TEXT_SUB);
        expArea.setEditable(false);
        expArea.setLineWrap(true);
        expArea.setWrapStyleWord(true);
        expArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        expArea.setText(
            "GRAPH DATA STRUCTURE\n" +
            "Nodes = words\n" +
            "Edges = one word follows another\n" +
            "Weight = frequency of the pair\n\n" +
            "Type  'data '  then watch\n" +
            "suggestions appear.\n\n" +
            "Graph learns as you type!"
        );

        JButton btnLearn = makePillButton("[Learn]  Teach a Sentence", ORANGE, ORANGE_LIGHT);
        btnLearn.addActionListener(e -> {
            String current = inputField.getText().trim();
            if (current.split("\\s+").length >= 2) {
                bigramGraph.learnFromText(current);
                graphInfoLabel.setText("Graph: " + bigramGraph.getVocabularySize()
                    + " nodes, " + bigramGraph.getTotalBigrams() + " edges (learned!)");
                setStatus("Learned: " + current, ORANGE);
            } else {
                learnSentenceDialog();
            }
        });

        card.add(header);
        card.add(Box.createVerticalStrut(6));
        card.add(graphInfoLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(styledScroll(phraseList, ORANGE,      200));
        card.add(Box.createVerticalStrut(12));
        card.add(expSep);
        card.add(Box.createVerticalStrut(10));
        card.add(expHeader);
        card.add(Box.createVerticalStrut(6));
        card.add(styledScroll(expArea, ORANGE,         160));
        card.add(Box.createVerticalStrut(10));
        card.add(btnLearn);
        return card;
    }

    // ── TRIE VISUALIZER PANEL ────────────────────────────────────────────────
    private JPanel buildTrieVisualizerPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(BG_CARD);

        final Color TRIE_GREEN       = new Color(20, 120, 90);
        final Color TRIE_GREEN_LIGHT = new Color(209, 250, 229);

        // Header bar matching style of other panels
        JPanel header = makeCardHeader("\uD83C\uDF33  Trie Visualizer", TRIE_GREEN,
                TRIE_GREEN_LIGHT, "Prefix Tree  •  Live");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Legend chip under header
        JLabel legend = makeInfoChip("Blue = prefix path  |  Green = word end  |  Sky = children",
                TRIE_GREEN, TRIE_GREEN_LIGHT);

        // ── Full View button ─────────────────────────────────────────────────
        JButton btnFullView = makePillButton("\u26F6  Full View", TRIE_GREEN, TRIE_GREEN_LIGHT);
        btnFullView.setToolTipText("Open the complete Trie in a full-size window");
        btnFullView.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnFullView.addActionListener(e -> openFullTrieView());

        // Top section: header + legend + full-view button
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(BG_CARD);
        top.setBorder(BorderFactory.createEmptyBorder(14, 14, 6, 14));
        top.add(header);
        top.add(Box.createVerticalStrut(6));
        top.add(legend);
        top.add(Box.createVerticalStrut(8));
        top.add(btnFullView);

        // Visualizer canvas
        trieVisualizer = new TrieVisualizerPanel(dictionary.getTrie());
        trieVisualizer.setBackground(new Color(248, 250, 252));

        JScrollPane scroll = new JScrollPane(trieVisualizer,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scroll.getViewport().setBackground(new Color(248, 250, 252));
        scroll.getHorizontalScrollBar().setUnitIncrement(20);
        scroll.getVerticalScrollBar().setUnitIncrement(20);

        // Bottom hint
        JLabel hint = new JLabel("  Type in the input field to see the Trie update live");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        hint.setForeground(TEXT_MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(5, 14, 10, 14));

        card.add(top,    BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(hint,   BorderLayout.SOUTH);
        return card;
    }

    // ── TRIE LIVE POPUP ──────────────────────────────────────────────────────
    /**
     * Opens a larger floating window showing the exact same live trie view
     * that is currently visible in the side panel — same prefix, same colors,
     * updates in real time as the user keeps typing.
     */
    private void openFullTrieView() {
        final Color TRIE_GREEN       = new Color(20, 120, 90);
        final Color TRIE_GREEN_LIGHT = new Color(209, 250, 229);

        JDialog dialog = new JDialog(this, "LexiFix  —  Trie View", false);
        dialog.setSize(1000, 780);
        dialog.setMinimumSize(new Dimension(700, 500));

        // Position top-left of the screen (not centred)
        dialog.setLocation(40, 40);

        dialog.getContentPane().setBackground(BG_APP);
        dialog.setLayout(new BorderLayout(0, 0));

        // ── Header ───────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(BG_TOPBAR);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        JLabel titleLbl = new JLabel("\uD83C\uDF33  Trie Visualizer  —  Live View");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, fontSize + 4));
        titleLbl.setForeground(TRIE_GREEN);

        JLabel legendLbl = makeInfoChip(
            "Blue = prefix path  |  Green = word end  |  Sky = children",
            TRIE_GREEN, TRIE_GREEN_LIGHT);

        JButton btnClose = makePillButton("✕  Close", new Color(100, 116, 139),
                                          new Color(241, 245, 249));
        btnClose.addActionListener(e -> {
            popupTrieVisualizer = null;
            dialog.dispose();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setBackground(BG_TOPBAR);
        right.add(btnClose);

        header.add(titleLbl,  BorderLayout.WEST);
        header.add(legendLbl, BorderLayout.CENTER);
        header.add(right,     BorderLayout.EAST);

        // ── Zoom toolbar (below header) ───────────────────────────────────────
        JPanel zoomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        zoomBar.setBackground(BG_TOPBAR);
        zoomBar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        JLabel zoomIcon = new JLabel("🔍");
        zoomIcon.setFont(new Font("Segoe UI", Font.PLAIN, fontSize));
        zoomIcon.setForeground(TEXT_SUB);

        JButton btnZoomOut = makePillButton("−", TRIE_GREEN, TRIE_GREEN_LIGHT);
        btnZoomOut.setPreferredSize(new Dimension(38, 28));

        JLabel zoomLabel = makeInfoChip("100 %", TRIE_GREEN, TRIE_GREEN_LIGHT);

        JButton btnZoomIn = makePillButton("+", TRIE_GREEN, TRIE_GREEN_LIGHT);
        btnZoomIn.setPreferredSize(new Dimension(38, 28));

        JButton btnZoomReset = makePillButton("Reset", new Color(100, 116, 139),
                                               new Color(241, 245, 249));

        // Canvas: new TrieVisualizerPanel seeded with current prefix
        popupTrieVisualizer = new TrieVisualizerPanel(dictionary.getTrie());
        popupTrieVisualizer.setBackground(new Color(248, 250, 252));
        popupTrieVisualizer.update(inputField.getText().trim()
            .replaceAll("\\s+$", "")
            .replaceAll(".*\\s", ""));

        // Zoom actions
        final float[] zoom = { 1.0f };
        final float   STEP = 0.15f;
        final float   MIN  = 0.3f;
        final float   MAX  = 3.0f;

        Runnable applyZoom = () -> {
            zoom[0] = Math.max(MIN, Math.min(MAX, zoom[0]));
            popupTrieVisualizer.setScale(zoom[0]);
            zoomLabel.setText(Math.round(zoom[0] * 100) + " %");
            btnZoomOut.setEnabled(zoom[0] > MIN + 0.01f);
            btnZoomIn .setEnabled(zoom[0] < MAX - 0.01f);
        };

        btnZoomOut.addActionListener(e -> { zoom[0] -= STEP; applyZoom.run(); });
        btnZoomIn .addActionListener(e -> { zoom[0] += STEP; applyZoom.run(); });
        btnZoomReset.addActionListener(e -> { zoom[0] = 1.0f; applyZoom.run(); });

        zoomBar.add(zoomIcon);
        zoomBar.add(btnZoomOut);
        zoomBar.add(zoomLabel);
        zoomBar.add(btnZoomIn);
        zoomBar.add(Box.createHorizontalStrut(6));
        zoomBar.add(btnZoomReset);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(BG_TOPBAR);
        northPanel.add(header,  BorderLayout.NORTH);
        northPanel.add(zoomBar, BorderLayout.SOUTH);

        // ── Scroll pane ───────────────────────────────────────────────────────
        JScrollPane scroll = new JScrollPane(popupTrieVisualizer,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(new Color(248, 250, 252));
        scroll.getHorizontalScrollBar().setUnitIncrement(24);
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        // Mouse-wheel zoom (Ctrl + scroll)
        scroll.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                zoom[0] += (e.getWheelRotation() < 0) ? STEP : -STEP;
                applyZoom.run();
            }
        });

        // ── Hint footer ───────────────────────────────────────────────────────
        JLabel hint = new JLabel("  Updates live as you type  •  Ctrl + scroll to zoom");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, fontSize - 2));
        hint.setForeground(TEXT_MUTED);
        hint.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        dialog.add(northPanel, BorderLayout.NORTH);
        dialog.add(scroll,     BorderLayout.CENTER);
        dialog.add(hint,       BorderLayout.SOUTH);

        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                popupTrieVisualizer = null;
            }
        });

        dialog.setVisible(true);
    }

    // ── UNDO/REDO PANEL ──────────────────────────────────────────────────────
    private JPanel buildUndoRedoPanel() {
        JPanel card = makeCard();

        JPanel header = makeCardHeader("↩  Undo / ↪  Redo", AMBER, AMBER_LIGHT,
            "Stack (LIFO)  •  O(1) push/pop");
        undoCountLabel = makeInfoChip("Undo: 0  |  Redo: 0", AMBER, AMBER_LIGHT);

        undoStackModel = new DefaultListModel<>();
        undoStackList  = buildList(undoStackModel, AMBER, AMBER_LIGHT);
        redoStackModel = new DefaultListModel<>();
        redoStackList  = buildList(redoStackModel, GREEN, GREEN_LIGHT);

        JLabel undoLabel = makeSectionLabel("Undo Stack  (top = current state)", AMBER);
        JLabel redoLabel = makeSectionLabel("Redo Stack",                        GREEN);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        btnRow.setBackground(BG_CARD);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton u = makePillButton("↩ Undo  Ctrl+Z", AMBER, AMBER_LIGHT);
        JButton r = makePillButton("↪ Redo  Ctrl+Y", GREEN, GREEN_LIGHT);
        u.addActionListener(e -> performUndo());
        r.addActionListener(e -> performRedo());
        btnRow.add(u); btnRow.add(r);

        // Concept box
        JPanel conSep    = makeSeparator();
        JTextArea conArea = new JTextArea();
        conArea.setFont(fontSmall);
        conArea.setBackground(DIVIDER);
        conArea.setForeground(TEXT_SUB);
        conArea.setEditable(false);
        conArea.setLineWrap(true);
        conArea.setWrapStyleWord(true);
        conArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        conArea.setText(
            "STACK DATA STRUCTURE\n" +
            "LIFO: Last In, First Out\n\n" +
            "undoStack → past states\n" +
            "redoStack → undone states\n\n" +
            "★ = top of stack (current)\n\n" +
            "Type text to fill undo.\n" +
            "Press Undo to move states\n" +
            "to the redo stack."
        );

        card.add(header);
        card.add(Box.createVerticalStrut(6));
        card.add(undoCountLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(undoLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(styledScroll(undoStackList, AMBER,   160));
        card.add(Box.createVerticalStrut(10));
        card.add(redoLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(styledScroll(redoStackList, GREEN,   120));
        card.add(Box.createVerticalStrut(10));
        card.add(btnRow);
        card.add(Box.createVerticalStrut(12));
        card.add(conSep);
        card.add(Box.createVerticalStrut(10));
        card.add(styledScroll(conArea, AMBER,         140));
        return card;
    }

    // ── BOTTOM BAR ───────────────────────────────────────────────────────────
    private JPanel buildBottomBar() {
        bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(BG_TOPBAR);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));

        statusLabel = new JLabel("  Ready — Load a dictionary or start typing");
        statusLabel.setFont(fontBadge);
        statusLabel.setForeground(TEXT_SUB);

        JLabel credit = new JLabel("CSC211 Data Structures  |  LexiFix v2  |  Trie · HashMap · Stack · Graph  ");
        credit.setFont(fontSmall);
        credit.setForeground(TEXT_MUTED);

        bottomBar.add(statusLabel, BorderLayout.WEST);
        bottomBar.add(credit,      BorderLayout.EAST);
        return bottomBar;
    }

    // ── FONT REFRESH ─────────────────────────────────────────────────────────
    private void applyFontsGlobally(int delta) {
        // Recursively rescale the font of every component in the window by
        // `delta` so that ALL fonts (titles, headings, badges, mono lists,
        // buttons, hints, etc.) follow the slider — not just a hand-picked
        // subset.
        rescaleFonts(getContentPane(), delta);

        // Header rows have an explicit max height tied to the title font
        // size; grow/shrink them by the same delta so the heading text
        // never gets clipped or leaves extra dead space.
        for (JPanel row : headerRows) {
            Dimension max = row.getMaximumSize();
            row.setMaximumSize(new Dimension(max.width, Math.max(10, max.height + delta)));
            row.revalidate();
        }

        // The Trie visualizer draws its own labels with Graphics2D, so it
        // needs to be told about the change separately.
        if (trieVisualizer != null) {
            trieVisualizer.adjustFontSize(delta);
        }

        revalidate();
        repaint();
    }

    /**
     * Recursively walks the component tree and resizes every font it finds
     * by {@code delta} points (clamped to a sane minimum), while preserving
     * the original font family and style. Also grows/shrinks JList fixed
     * cell heights so rows stay readable at every font size.
     */
    private void rescaleFonts(Component comp, int delta) {
        Font f = comp.getFont();
        if (f != null) {
            int newSize = Math.max(6, f.getSize() + delta);
            comp.setFont(new Font(f.getFamily(), f.getStyle(), newSize));
        }
        if (comp instanceof JList<?>) {
            JList<?> list = (JList<?>) comp;
            int newCell = Math.max(10, list.getFixedCellHeight() + delta);
            list.setFixedCellHeight(newCell);
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                rescaleFonts(child, delta);
            }
        }
    }

    // ── CORE LOGIC ───────────────────────────────────────────────────────────
    private void onTextChanged() {
        if (!isUndoRedoAction) {
            undoRedo.push(inputField.getText());
            updateUndoRedoPanel();
        }
        debounceTimer.restart();
    }

    private void processInput() {
        String fullText = inputField.getText();
        String trimmed  = fullText.trim();
        String[] words  = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
        boolean endsWithSpace = fullText.endsWith(" ");

        String partial, completedWord;
        if (endsWithSpace) {
            completedWord = words.length > 0 ? words[words.length - 1] : "";
            partial       = "";
        } else {
            partial       = words.length > 0 ? words[words.length - 1] : "";
            completedWord = words.length > 1 ? words[words.length - 2] : "";
        }

        if (!partial.isEmpty()) {
            updateAutocomplete(partial);
            updateSpellCheck(partial);
        } else {
            autocompleteModel.clear();
            correctionModel.clear();
            spellStatusIcon.setForeground(TEXT_MUTED);
            infoArea.setText("Keep typing…");
            trieInfoLabel.setText("Waiting for input…");
            editDistanceLabel.setText("Edit distance: —");
            hashInfoLabel.setText("HashMap: —");
        }

        updatePhrasePanel(completedWord, partial);
        updateStats();

        if (endsWithSpace && words.length >= 2) {
            String w1 = words[words.length - 2];
            String w2 = words[words.length - 1];
            if (!w1.isEmpty() && !w2.isEmpty()) {
                bigramGraph.learnFromUserInput(w1, w2);
                graphInfoLabel.setText("Graph: " + bigramGraph.getVocabularySize() +
                    " nodes, " + bigramGraph.getTotalBigrams() + " edges (learned!)");
            }
        }
    }

    private void updateAutocomplete(String prefix) {
        List<WordEntry> suggestions = autocompleteEngine.suggest(prefix);
        autocompleteModel.clear();
        for (int i = 0; i < suggestions.size(); i++) {
            WordEntry e = suggestions.get(i);
            autocompleteModel.addElement((i == 0 ? "★  " : "    ") + e.getWord()
                + "  [" + e.getFrequency() + "]");
        }
        trieInfoLabel.setText("Trie: " + suggestions.size() + " result(s) for \"" + prefix + "\"");
        if (trieVisualizer != null) trieVisualizer.update(prefix);
        if (popupTrieVisualizer != null) popupTrieVisualizer.update(prefix);
    }

    private void updateSpellCheck(String word) {
        boolean wrong = spellCorrectEngine.needsCorrection(word);
        spellStatusIcon.setForeground(wrong ? RED : GREEN);
        correctionModel.clear();

        if (!wrong) {
            correctionModel.addElement("✓  \"" + word + "\" is correct");
            editDistanceLabel.setText("Edit distance: 0  (exact match)");
            hashInfoLabel.setText("HashMap: FOUND  O(1)");
            infoArea.setText("✓  \"" + word + "\" found in dictionary.\n\nHashMap lookup: O(1) average\nSpelling is correct.\n\nLevenshtein: not needed\n(word is already valid)");
        } else {
            List<WordEntry> corrections = spellCorrectEngine.suggest(word);
            SpellChecker sc = dictionary.getSpellChecker();
            int minDist = Integer.MAX_VALUE;

            StringBuilder sb = new StringBuilder();
            sb.append("✗  \"").append(word).append("\" — not in dictionary\n\n");
            sb.append("Levenshtein DP results:\n");
            sb.append("─────────────────────\n");

            for (WordEntry e : corrections) {
                int d = sc.editDistance(word, e.getWord());
                if (d < minDist) minDist = d;
                String ops = describeOps(d);
                correctionModel.addElement(
                    (corrections.indexOf(e) == 0 ? "★  " : "    ")
                    + e.getWord()
                    + "  [d=" + d + "  freq=" + e.getFrequency() + "]");
                sb.append("  ").append(e.getWord())
                  .append("  →  d=").append(d)
                  .append(" (").append(ops).append(")\n");
            }

            if (corrections.isEmpty()) {
                correctionModel.addElement("No match within edit distance 2");
                sb.append("No corrections found within\nedit distance 2.\n");
                sb.append("\nTry typing more characters.");
                minDist = -1;
            }

            String distTxt = (minDist == Integer.MAX_VALUE || minDist < 0)
                             ? ">2" : String.valueOf(minDist);
            editDistanceLabel.setText("Min edit distance: " + distTxt
                + "  (DP  O(m×n))");
            hashInfoLabel.setText("HashMap: NOT FOUND  →  running DP");
            sb.append("\nDP table: O(m×n) time\nspace: O(m×n)");
            infoArea.setText(sb.toString());
        }
    }

    /** Human-readable description of edit distance. */
    private String describeOps(int d) {
        switch (d) {
            case 0: return "exact";
            case 1: return "1 edit";
            case 2: return "2 edits";
            default: return d + " edits";
        }
    }

    private void updatePhrasePanel(String completedWord, String partial) {
        phraseModel.clear();
        if (completedWord.isEmpty()) {
            phraseModel.addElement("Type a word then Space");
            phraseModel.addElement("to see phrase suggestions");
            graphInfoLabel.setText("Graph: " + bigramGraph.getVocabularySize() +
                " nodes, " + bigramGraph.getTotalBigrams() + " edges");
            return;
        }
        List<WordEntry> phrases = bigramGraph.getPhraseSuggestions(completedWord, partial, 8);
        if (!phrases.isEmpty()) {
            phraseModel.addElement("After \"" + completedWord + "\":");
            for (int i = 0; i < phrases.size(); i++) {
                WordEntry e = phrases.get(i);
                phraseModel.addElement((i == 0 ? "★  " : "    ") + e.getWord()
                    + "  [" + e.getFrequency() + "×]");
            }
        } else {
            phraseModel.addElement("No suggestions for \"" + completedWord + "\"");
            phraseModel.addElement("Try: data, binary, linked…");
        }
        graphInfoLabel.setText("Graph: " + bigramGraph.getVocabularySize() +
            " nodes, " + bigramGraph.getTotalBigrams() + " edges");
    }

    private void updateUndoRedoPanel() {
        undoStackModel.clear();
        redoStackModel.clear();
        Object[] undoArr = undoRedo.getUndoStack().toArray();
        for (int i = undoArr.length - 1; i >= 0; i--) {
            String val = undoArr[i].toString();
            undoStackModel.addElement((i == undoArr.length - 1 ? "★  " : "    ")
                + (val.isEmpty() ? "(empty)" : "\"" + val + "\""));
        }
        Object[] redoArr = undoRedo.getRedoStack().toArray();
        for (int i = redoArr.length - 1; i >= 0; i--) {
            String val = redoArr[i].toString();
            redoStackModel.addElement("    " + (val.isEmpty() ? "(empty)" : "\"" + val + "\""));
        }
        undoCountLabel.setText("Undo: " + undoRedo.undoCount() + "  |  Redo: " + undoRedo.redoCount());
        btnUndo.setEnabled(undoRedo.canUndo());
        btnRedo.setEnabled(undoRedo.canRedo());
    }

    private void performUndo() {
        isUndoRedoAction = true;
        String prev = undoRedo.undo();
        if (prev != null) {
            inputField.setText(prev);
            inputField.setCaretPosition(prev.length());
            setStatus("Undo → \"" + prev + "\"", AMBER);
        }
        isUndoRedoAction = false;
        debounceTimer.stop();
        updateUndoRedoPanel();
        processInput();
    }

    private void performRedo() {
        isUndoRedoAction = true;
        String next = undoRedo.redo();
        if (next != null) {
            inputField.setText(next);
            inputField.setCaretPosition(next.length());
            setStatus("Redo → \"" + next + "\"", GREEN);
        }
        isUndoRedoAction = false;
        debounceTimer.stop();
        updateUndoRedoPanel();
        processInput();
    }

    private void applyWord(String word) {
        if (word == null || word.isEmpty()) return; // guard: ignore clicks on header/info rows
        String text    = inputField.getText();
        String[] words = text.trim().isEmpty() ? new String[0] : text.trim().split("\\s+");
        if (words.length > 0) words[words.length - 1] = word;
        String newText = (words.length == 0 ? word : String.join(" ", words)) + " ";
        inputField.setText(newText);
        inputField.setCaretPosition(newText.length());
        inputField.requestFocus();
        dictionary.wordSelected(word);
        updateUndoRedoPanel();
        setStatus("Applied: " + word, BLUE);
    }

    private void applyNextWord(String nextWord) {
        if (nextWord == null || nextWord.isEmpty()) return; // guard: ignore clicks on header/info rows
        String text    = inputField.getText();
        String trimmed = text.trim();
        String[] words = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
        String newText;
        if (text.endsWith(" ") || words.length == 0) {
            newText = (text.endsWith(" ") ? text : text + " ") + nextWord + " ";
        } else {
            words[words.length - 1] = nextWord;
            newText = String.join(" ", words) + " ";
        }
        inputField.setText(newText);
        inputField.setCaretPosition(newText.length());
        inputField.requestFocus();
        dictionary.wordSelected(nextWord);
        updateUndoRedoPanel();
        setStatus("Phrase: " + nextWord, ORANGE);
    }

    private void clearResults() {
        autocompleteModel.clear();
        correctionModel.clear();
        phraseModel.clear();
        spellStatusIcon.setForeground(TEXT_MUTED);
        infoArea.setText("Type a word to begin…");
        editDistanceLabel.setText("Edit distance: —");
        trieInfoLabel.setText("Waiting for input…");
        hashInfoLabel.setText("HashMap: —");
        graphInfoLabel.setText("Graph: " + bigramGraph.getVocabularySize() +
            " nodes, " + bigramGraph.getTotalBigrams() + " edges");
    }

    private void updateStats() {
        wordCountLabel.setText(dictionary.getDictionarySize() + " words");
        historyCountLabel.setText(dictionary.getSearchHistory().size() + " items");
        historyModel.clear();
        int c = 0;
        for (String h : dictionary.getSearchHistory()) {
            if (c++ >= 6) break;
            historyModel.addElement("  " + h);
        }
    }

    // ── FILE & DIALOGS ───────────────────────────────────────────────────────
    private void loadDefaultDictionary() {
        String[] paths = {
            "data/dictionary.txt",
            "dictionary.txt",
            System.getProperty("user.dir") + "/data/dictionary.txt"
        };
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    int count = dictionary.loadFromFile(f.getAbsolutePath());
                    // Defer UI refresh until the frame is fully painted so
                    // wordCountLabel and trieVisualizer are guaranteed to exist.
                    final int loaded = count;
                    SwingUtilities.invokeLater(() -> {
                        setStatus("Loaded " + loaded + " words from: " + f.getName(), GREEN);
                        updateStats();
                        if (trieVisualizer != null) trieVisualizer.setTrie(dictionary.getTrie());
                        wordCountLabel.setText(dictionary.getDictionarySize() + " words");
                    });
                } catch (IOException ignored) {}
                return;
            }
        }
        setStatus("No dictionary found — use  [Load] Load Dictionary", AMBER);
    }

    private void loadDictionaryFromFile() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter("Text files (*.txt)", "txt"));
        }
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                int count = dictionary.loadFromFile(fileChooser.getSelectedFile().getAbsolutePath());
                setStatus("Loaded " + count + " words", GREEN);
                updateStats();
                if (trieVisualizer != null) trieVisualizer.setTrie(dictionary.getTrie());
            } catch (IOException ex) {
                setStatus("Error: " + ex.getMessage(), RED);
            }
        }
    }

    private void addWordDialog() {
        // Build an inline panel so we can give richer feedback
        JTextField wordField = new JTextField(20);
        wordField.setFont(fontBody);
        JLabel feedback = new JLabel(" ");
        feedback.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        // Live validation as user types in the dialog
        wordField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { validateInput(); }
            public void removeUpdate(DocumentEvent e)  { validateInput(); }
            public void changedUpdate(DocumentEvent e) {}
            private void validateInput() {
                String w = wordField.getText().trim().toLowerCase();
                if (w.isEmpty()) {
                    feedback.setForeground(TEXT_SUB);
                    feedback.setText("Enter a word (letters only)");
                } else if (!w.matches("[a-z]+")) {
                    feedback.setForeground(RED);
                    feedback.setText("✗  Only letters a–z allowed");
                } else if (dictionary.isCorrect(w)) {
                    feedback.setForeground(AMBER);
                    feedback.setText("⚠  \"" + w + "\" already in dictionary");
                } else {
                    feedback.setForeground(GREEN);
                    feedback.setText("✓  Ready to add \"" + w + "\"");
                }
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Word to add:"));
        panel.add(Box.createVerticalStrut(6));
        panel.add(wordField);
        panel.add(Box.createVerticalStrut(4));
        panel.add(feedback);

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Add Word to Dictionary",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String word = wordField.getText().trim().toLowerCase();
            if (!word.isEmpty() && word.matches("[a-z]+")) {
                boolean alreadyExisted = dictionary.isCorrect(word);
                dictionary.addWord(word);

                // ── Real-time refresh of every panel ────────────────────
                updateStats();

                // Re-run the full input pipeline so autocomplete,
                // spell check, and trie visualizer all update immediately
                onTextChanged();

                // Also update the trie visualizer with the new word as prefix
                // so the user can see it appear in the tree right away
                if (trieVisualizer != null) {
                    trieVisualizer.setTrie(dictionary.getTrie());
                    trieVisualizer.update(word);   // zoom to the new word
                }

                // Put the new word into the input field so panels show it
                String prev = inputField.getText();
                inputField.setText(word);          // triggers onTextChanged via DocumentListener
                // restore previous text after a short delay so user isn't disrupted
                new Timer(1200, ev -> {
                    inputField.setText(prev);
                    ((Timer) ev.getSource()).stop();
                }).start();

                if (alreadyExisted) {
                    setStatus("\"" + word + "\" was already in dictionary (frequency boosted)", AMBER);
                } else {
                    setStatus("✓  Added \"" + word + "\" — now live in Trie + HashMap", GREEN);
                }
            } else if (!word.isEmpty()) {
                setStatus("Invalid word — only letters a–z allowed", RED);
            }
        }
    }

    private void learnSentenceDialog() {
        String sentence = JOptionPane.showInputDialog(this,
            "Enter a sentence to teach the graph:\n(e.g. 'binary search tree is fast')",
            "Teach Phrase", JOptionPane.PLAIN_MESSAGE);
        if (sentence != null && !sentence.trim().isEmpty()) {
            bigramGraph.learnFromText(sentence);
            setStatus("Learned phrase: " + sentence.trim(), ORANGE);
            graphInfoLabel.setText("Graph: " + bigramGraph.getVocabularySize() +
                " nodes, " + bigramGraph.getTotalBigrams() + " edges (learned!)");
        }
    }

    private void setStatus(String msg, Color color) {
        statusLabel.setText("  " + msg);
        statusLabel.setForeground(color);
    }

    private String extractWord(String item) {
        if (item == null) return "";
        // Strip leading decoration (★, spaces, arrows, checkmarks)
        String cleaned = item.replaceAll("^[★\\s→✓]+", "")
                             .replaceAll("^After[^:]*:\\s*", "");
        // Split on the stats bracket — may be separated by one or more spaces
        String[] parts = cleaned.split("\\s+\\[");
        return parts[0].trim();
    }

    // ── COMPONENT BUILDERS ───────────────────────────────────────────────────
    private JPanel makeCard() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(16, 14, 14, 14)
        ));
        return p;
    }

    /** Coloured header row: icon+title on its own line, ds-tag below it */
    private JPanel makeCardHeader(String title, Color accent, Color accentLight, String tag) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(BG_CARD);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Coloured left bar
        JPanel bar = new JPanel();
        bar.setPreferredSize(new Dimension(4, 0));
        bar.setBackground(accent);

        JLabel lbl = new JLabel("  " + title);
        lbl.setFont(fontHeading);
        lbl.setForeground(accent);

        // Title + tag stacked vertically so the title is never truncated
        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setBackground(BG_CARD);
        textCol.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        textCol.add(lbl);

        if (!tag.isEmpty()) {
            JLabel tagLbl = new JLabel("  " + tag);
            tagLbl.setFont(fontSmall);
            tagLbl.setForeground(TEXT_MUTED);
            tagLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            textCol.add(tagLbl);
        }

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            fontTitle.getSize() + (tag.isEmpty() ? 20 : 40)));

        row.add(bar,    BorderLayout.WEST);
        row.add(textCol, BorderLayout.CENTER);

        headerRows.add(row);
        return row;
    }

    private JLabel makeInfoChip(String text, Color fg, Color bg) {
        JLabel l = new JLabel("  " + text + "  ");
        l.setFont(fontBadge);
        l.setForeground(fg);
        l.setBackground(bg);
        l.setOpaque(true);
        l.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel makeChip(String text, Color fg, Color bg) {
        return makeInfoChip(text, fg, bg);
    }

    private JLabel makeSectionLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(fontSmall);
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel makeSeparator() {
        JPanel sep = new JPanel();
        sep.setBackground(DIVIDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private JList<String> buildList(DefaultListModel<String> model, Color accent, Color accentLight) {
        JList<String> list = new JList<>(model);
        list.setFont(fontMono);
        list.setBackground(BG_CARD);
        list.setForeground(TEXT_MAIN);
        list.setSelectionBackground(accentLight);
        list.setSelectionForeground(accent.darker());
        list.setFixedCellHeight(fontSize + 14);
        list.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        list.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> l, Object v, int idx, boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, v, idx, sel, foc);
                setFont(fontMono);
                if (sel) {
                    setBackground(accentLight);
                    setForeground(accent.darker());
                } else {
                    setBackground(idx % 2 == 0 ? BG_CARD : BG_ROW_ALT);
                    setForeground(idx == 0 ? accent : TEXT_MAIN);
                }
                setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
                return this;
            }
        });
        return list;
    }

    private JScrollPane styledScroll(JComponent comp, Color accent) {
        return styledScroll(comp, accent, 0);
    }

    private JScrollPane styledScroll(JComponent comp, Color accent, int fixedHeight) {
        JScrollPane sp = new JScrollPane(comp);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setBorder(new LineBorder(BORDER, 1, true));
        sp.getViewport().setBackground(BG_CARD);
        sp.setBackground(BG_CARD);
        if (fixedHeight > 0) {
            sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, fixedHeight));
            sp.setPreferredSize(new Dimension(0, fixedHeight));
        }
        // Style scrollbar
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = accent.brighter();
                trackColor = DIVIDER;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b;
            }
        });
        return sp;
    }

    private JButton makePillButton(String text, Color fg, Color bg) {
        JButton b = new JButton(text);
        b.setFont(fontBadge);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(fg.brighter(), 1, true),
            BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        Color hoverBg = fg.brighter().brighter();
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(hoverBg); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    // ── MAIN ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        // Improve rendering on Windows/Linux
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new LexiFixGUI().setVisible(true));
    }
}
