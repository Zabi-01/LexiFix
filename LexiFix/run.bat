@echo off
echo ============================================
echo   LexiFix v2 - Build and Run
echo ============================================
if not exist "out" mkdir out

javac -d out -sourcepath src ^
  src/lexifix/model/TrieNode.java ^
  src/lexifix/model/WordEntry.java ^
  src/lexifix/engine/Trie.java ^
  src/lexifix/engine/SpellChecker.java ^
  src/lexifix/engine/Dictionary.java ^
  src/lexifix/engine/SuggestionEngine.java ^
  src/lexifix/engine/AutocompleteEngine.java ^
  src/lexifix/engine/SpellCorrectEngine.java ^
  src/lexifix/engine/UndoRedoManager.java ^
  src/lexifix/engine/BigramGraph.java ^
  src/lexifix/ui/LexiFixGUI.java ^
  src/Main.java

if %errorlevel% neq 0 ( echo FAILED! & pause & exit /b 1 )
echo Launching LexiFix...
java -cp out Main
pause
