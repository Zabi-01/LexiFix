<div align="center">

# 🚀 LexiFix

### *A Premium, DSA-Powered Desktop Text Assistant & Interactive Trie Visualizer*

[![Language](https://img.shields.io/badge/Language-Java-orange.svg?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![GUI](https://img.shields.io/badge/GUI-Swing-blue.svg?style=for-the-badge)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![DSA](https://img.shields.io/badge/DSA-Trie%20%7C%20Graph%20%7C%20Stack%20%7C%20HashMap-red.svg?style=for-the-badge)](https://github.com/Zabi-01/LexiFix)
[![License](https://img.shields.io/badge/Academic%20Project-CSC211-green.svg?style=for-the-badge)](https://github.com/Zabi-01/LexiFix)

---

<p align="center">
  <strong>LexiFix</strong> is a high-performance, real-time typing companion that demonstrates the practical application of core <strong>Data Structures & Algorithms (DSA)</strong> in text processing. Built with a responsive, modern light-themed desktop GUI, it provides live prefix matching, spelling suggestions, phrase predictions, state history management, and an interactive tree visualizer.
</p>

</div>

---

## 🎨 Graphical User Interface (GUI) Showcase

The LexiFix interface is split into dynamic, color-coded modular dashboards designed for absolute clarity:

### 🖥️ Main Editor Workspace
*The central workspace containing the real-time editor, dynamic metrics, and modular analysis widgets.*
<p align="center">
  <img src="assets/main_workspace.png" alt="Main Workspace" width="95%" style="border-radius: 8px; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);"/>
</p>

<br/>

### 🌳 Live Trie Visualizer & Dialog Panel
*An interactive canvas rendering the prefix tree (Trie) representation of the lexicon. It shows active search paths in blue, terminal words in green, and supports panning and zooming.*
<p align="center">
  <img src="assets/trie_visualizer.png" alt="Trie Visualizer" width="95%" style="border-radius: 8px; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);"/>
</p>

<br/>

### 🔍 Features Breakdown & Suggestions
*Real-time panels for Autocomplete suggestions, spell correction alternatives, and predictive word suggestions.*

<table width="100%">
  <tr>
    <td width="33%" align="center"><strong>Trie Autocomplete</strong></td>
    <td width="33%" align="center"><strong>Spell Corrections</strong></td>
    <td width="33%" align="center"><strong>Bigram Graph Predictions</strong></td>
  </tr>
  <tr>
    <td><img src="assets/autocomplete.png" alt="Autocomplete" style="border-radius: 6px; border: 1px solid #e2e8f0;"/></td>
    <td><img src="assets/spell_check.png" alt="Spell Corrections" style="border-radius: 6px; border: 1px solid #e2e8f0;"/></td>
    <td><img src="assets/phrase_suggestions.png" alt="Phrase Suggestions" style="border-radius: 6px; border: 1px solid #e2e8f0;"/></td>
  </tr>
  <tr>
    <td>Retrieves words starting with the typed prefix in <code>O(L)</code> time.</td>
    <td>Computes Levenshtein edit distance and checks the dictionary using a <code>HashMap</code>.</td>
    <td>Predicts the next logical word by analyzing transition frequencies in an Adjacency Graph.</td>
  </tr>
</table>

---

## 🧠 Core Algorithms & Complexity Analysis

LexiFix implements optimized data structures to perform instant checks and predictions as you type:

| Engine / Component | Underlying Data Structure | Key Algorithm | Time Complexity | Space Complexity |
| :--- | :--- | :--- | :---: | :---: |
| **Autocomplete** | `Trie` (Prefix Tree) | DFS Prefix Retrieval | $\mathcal{O}(L)$ | $\mathcal{O}(V \times \Sigma)$ |
| **Dictionary Validation** | `HashMap` | Hash-bucket search | $\mathcal{O}(1)$ | $\mathcal{O}(N)$ |
| **Spell Correction** | Custom Dynamic Array | Levenshtein Distance (DP) | $\mathcal{O}(M \times N)$ | $\mathcal{O}(M \times N)$ |
| **Phrase Suggestion** | Directed Weighted Graph | Adjacency Map traversal | $\mathcal{O}(1)$ (neighbor fetch) | $\mathcal{O}(V + E)$ |
| **Undo / Redo** | Double `Stack` | LIFO State caching | $\mathcal{O}(1)$ | $\mathcal{O}(S)$ |

*Where:*
* $L$ = Length of the active prefix query
* $M, N$ = Lengths of compared words
* $V, E$ = Vertices (words) and Edges (transitions) in the phrase graph
* $S$ = Size of the history cache stack
* $\Sigma$ = Alphabet size

---

## 📂 Repository Directory Layout

```
LexiFix/
├── .gitignore
├── README.md
└── LexiFix/
    ├── data/
    │   └── dictionary.txt          # Lexicon database containing default vocabulary
    ├── src/
    │   ├── Main.java               # Application bootstrap class
    │   └── lexifix/
    │       ├── model/
    │       │   ├── TrieNode.java   # Trie node containing character links & flags
    │       │   └── WordEntry.java  # Word wrapper carrying frequency metrics
    │       ├── engine/
    │       │   ├── Trie.java               # Custom prefix tree implementation
    │       │   ├── Dictionary.java         # Lexicon file loader and parser
    │       │   ├── AutocompleteEngine.java # Prefix search processor
    │       │   ├── SpellChecker.java       # Direct verification engine
    │       │   ├── SpellCorrectEngine.java # Dynamic Programming corrector
    │       │   ├── SuggestionEngine.java   # Word recommendation interface
    │       │   ├── BigramGraph.java        # Adjacency Graph for phrase predictions
    │       │   └── UndoRedoManager.java    # State cache double-stack manager
    │       └── ui/
    │           ├── LexiFixGUI.java         # Main Java Swing window application
    │           └── TrieVisualizerPanel.java# Live graphics panel rendering the Trie
    ├── run.bat                     # Windows CLI compiler and launch script
    └── run.sh                      # Unix/macOS CLI compiler and launch script
```

---

## 🚀 How to Build & Run Locally

### Prerequisites
* **Java Development Kit (JDK) 11 or higher** must be installed on your machine and available in your environment variables.

### 💻 Windows Command Line
Double-click the `run.bat` file in the `LexiFix` folder or launch it from PowerShell/CMD:
```cmd
cd LexiFix
run.bat
```

### 🍎 macOS & Linux Terminal
Give execution permissions to the script, then execute it:
```bash
cd LexiFix
chmod +x run.sh
./run.sh
```

---

## ✨ Design & Optimization Highlights

* **🎨 Harmonic Color Palette:** Sleek, low-contrast, light-themed panels built with curated HSL colors to ensure visual balance.
* **⚡ Debounced Inputs:** Integrated `javax.swing.Timer` debounces input queries (120ms) to ensure the interface updates smoothly without lag during quick typing.
* **📏 Dynamic DPI Scaling:** An interactive font slider ($10\text{px} - 28\text{px}$) that programmatically resizes layouts, buttons, card panels, and scrollbars on the fly.
* **⌨️ Keyboard Hotkeys:** Full compatibility with standard productivity bindings: `Ctrl + Z` for undo states and `Ctrl + Y` for redo states.

---

## 🎓 Academic Credit
Developed as a semester project for the **CSC211 Data Structures and Algorithms** course.
