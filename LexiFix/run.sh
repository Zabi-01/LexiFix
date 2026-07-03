#!/bin/bash
echo "============================================"
echo "  LexiFix - Build and Run Script"
echo "============================================"

mkdir -p out

echo "Compiling..."
javac -d out -sourcepath src \
  src/Main.java \
  src/lexifix/model/TrieNode.java \
  src/lexifix/model/WordEntry.java \
  src/lexifix/engine/Trie.java \
  src/lexifix/engine/SpellChecker.java \
  src/lexifix/engine/Dictionary.java \
  src/lexifix/engine/SuggestionEngine.java \
  src/lexifix/engine/AutocompleteEngine.java \
  src/lexifix/engine/SpellCorrectEngine.java \
  src/lexifix/ui/LexiFixGUI.java

if [ $? -ne 0 ]; then
  echo "COMPILATION FAILED!"
  exit 1
fi

echo "Starting LexiFix..."
java -cp out Main
