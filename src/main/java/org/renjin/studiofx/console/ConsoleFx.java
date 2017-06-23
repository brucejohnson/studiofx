/*
 * NMRFx Processor : A Program for Processing NMR Data 
 * Copyright (C) 2004-2017 One Moon Scientific, Inc., Westfield, N.J., USA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renjin.studiofx.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.S;
import javafx.scene.input.KeyCombination;
import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.PopupWindow;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileSystemException;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.Paragraph;
import static org.fxmisc.richtext.TwoDimensional.Bias.Forward;
import org.fxmisc.wellbehaved.event.EventHandlerHelper;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.EventPattern.keyTyped;
import org.renjin.script.RenjinScriptEngine;
import org.renjin.script.RenjinScriptEngineFactory;
import org.renjin.studiofx.FXMLController;
import org.renjin.studiofx.StudioSession;

/**
 *
 * @author brucejohnson
 */
public class ConsoleFx implements RichConsole, Runnable {

    static Clipboard clipBoard = Clipboard.getSystemClipboard();
    public static RenjinScriptEngine renjinEngine = null;

    InlineCssTextArea outputArea;

    protected final List<String> history = new ArrayList<>();
    protected int historyPointer = 0;
    String prompt = "> ";
    boolean renjinMode = false;
    private OutputStream outPipe;
    private InputStream inPipe;
    private InputStream in;
    private PrintStream out;
    private NameCompletion nameCompletion;
    final int SHOW_AMBIG_MAX = 10;
    CommandList commandList;
    Map<String, Function> interpreters = new HashMap<>();
    Function function = null;

    public ConsoleFx() {
        this(null, null);
    }

    public ConsoleFx(InputStream cin, OutputStream cout) {
        outPipe = cout;
        if (outPipe == null) {
            outPipe = new PipedOutputStream();
            try {
                in = new PipedInputStream((PipedOutputStream) outPipe);
            } catch (IOException e) {
                print("Console internal	error (1)...", Color.RED);
            }
        }

        inPipe = cin;
        if (inPipe == null) {
            PipedOutputStream pout = new PipedOutputStream();
            out = new PrintStream(pout);
            try {
                inPipe = new ConsoleFx.BlockingPipedInputStream(pout);
            } catch (IOException e) {
                print("Console internal error: " + e);
            }
        }
        // Start the inpipe watcher
        new Thread(this).start();

    }

    public void initInterpreter(StudioSession session) {
        Repl interpreter = null;
        try {
            interpreter = new Repl(this, session);
        } catch (FileSystemException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        new Thread(interpreter).start();
    }

    private void print(String s) {
        runOnFxThread(() -> outputArea.appendText(s));
    }

    static String cssColor(Color color) {
        int red = (int) (color.getRed() * 255);
        int green = (int) (color.getGreen() * 255);
        int blue = (int) (color.getBlue() * 255);
        return "rgb(" + red + ", " + green + ", " + blue + ")";
    }

    private void print(String s, Color color) {
        runOnFxThread(() -> {
//            consoleArea.appendText(s);
//            Set<String> style = Collections.singleton("-fx-fill: " + cssColor(color) + ";");

//            Set<String> style = Collections.singleton("keyword");
//            ReadOnlyStyledDocument sDoc = ReadOnlyStyledDocument.fromString(s, style);
//            System.out.println("style " + sDoc.getStyleOfChar(2));
//            consoleArea.append(sDoc);
            int start = outputArea.getLength();
            outputArea.appendText(s);
            int end = outputArea.getLength();
            outputArea.setStyle(start, end, "-fx-fill: " + cssColor(color));
        });
    }

    public InputStream getInputStream() {
        return in;
    }

    public Reader getIn() {
        return new InputStreamReader(in);
    }

    public PrintStream getOut() {
        return out;
    }

    public PrintStream getErr() {
        return out;
    }

    private void inPipeWatcher() throws IOException {
        byte[] ba = new byte[256]; // arbitrary blocking factor
        int read;
        while ((read = inPipe.read(ba)) != -1) {
            print(new String(ba, 0, read));
            //text.repaint();
        }

        //  println("Console: Input     closed...");
    }

    public void run() {
        try {
            inPipeWatcher();
        } catch (IOException e) {
            print("Console: I/O Error: " + e + "\n", Color.RED);
        }
    }

    @Override
    public void print(Object o, java.awt.Color color) {
    }

    @Override
    public void setNameCompletion(NameCompletion nc) {
        nameCompletion = nc;
        commandList = new CommandList(this);
    }

    @Override
    public void setWaitFeedback(boolean on) {
    }

    @Override
    public void println(Object o) {
    }

    @Override
    public void print(Object o) {
    }

    @Override
    public void error(Object o) {
    }

    @Override
    public int getCharactersPerLine() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    class ConsoleOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            runOnFxThread(() -> outputArea.appendText(String.valueOf((char) b)));
        }

    }

    public static void runOnFxThread(final Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    public void banner() {
        String banner = "Welcome";
        runOnFxThread(() -> outputArea.appendText(banner + "\n"));
    }

    public void prompt() {
        runOnFxThread(() -> outputArea.appendText(prompt));
        runOnFxThread(() -> outputArea.positionCaret(outputArea.getLength()));
    }

    public void save() {
        System.out.println("save");
    }

    public void typed(KeyEvent keyEvent) {
        if (keyEvent.isShortcutDown()) {
            return;
        }
        String keyString = keyEvent.getCharacter();
        if ((keyString != null) && (keyString.length() > 0)) {
            char keyChar = keyString.charAt(0);
            if (!Character.isISOControl(keyChar)) {
                int caretPosition = outputArea.getCaretPosition();
                int nParagraphs = outputArea.getParagraphs().size();
                outputArea.insertText(caretPosition, keyString);
            }
        }
        PopupWindow popup = outputArea.getPopupWindow();
        if ((popup != null) && popup.isShowing()) {
            checkCommand();
        }
    }

    public void delete() {
        int nChar = outputArea.getLength();
        int col = outputArea.getCaretColumn();
        int pos = outputArea.getCaretPosition();
        if (col > prompt.length()) {
            outputArea.deleteText(pos - 1, pos);
        }
    }

    public void historyUp() {
        historyPointer--;
        if (historyPointer < 0) {
            historyPointer = 0;
        }
        getHistory();
    }

    public void historyDown() {
        historyPointer++;
        if (historyPointer > history.size()) {
            historyPointer = history.size();
        }
        getHistory();
    }

    public void getHistory() {
        int nParagraphs = outputArea.getParagraphs().size();
        Paragraph para = outputArea.getParagraph(nParagraphs - 1);
        int nChar = para.length();
        para.delete(prompt.length(), nChar);

        String historyString = "";
        if (historyPointer < history.size()) {
            historyString = history.get(historyPointer);
        }
        int nChars = outputArea.getLength();
        int paraStart = nChars - para.length();

        outputArea.replaceText(paraStart + prompt.length(), nChars, historyString);

    }

    public void paste(KeyEvent event) {
        String string = clipBoard.getString();
        if (string != null) {
            outputArea.appendText(string);
        }
    }

    public void copy(KeyEvent event) {
        String text = outputArea.getSelectedText();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipBoard.setContent(content);
    }

    public static RenjinScriptEngine getRenjin() {
        if (renjinEngine == null) {
            RenjinScriptEngineFactory renjinFactory = new RenjinScriptEngineFactory();
            renjinEngine = renjinFactory.getScriptEngine();
        }
        return renjinEngine;
    }

    private String getCurrentLine() {
        int nParagraphs = outputArea.getParagraphs().size();
        Paragraph para = outputArea.getParagraph(nParagraphs - 1);
        String command = para.toString();
        if (command.startsWith(prompt)) {
            command = command.substring(prompt.length());
        }
        command = command.trim();
        command = command.replace("\0", "");
        return command;
    }

    void complete(String command) {
        String part = getCurrentLine();
        outputArea.appendText(command.substring(part.length()) + "()");
        int length = outputArea.getLength();
        outputArea.positionCaret(length - 1);
    }

    private void checkCommand() {
        String part = getCurrentLine();
        if (nameCompletion == null) {
            return;
        }

        int i = part.length() - 1;

        // Character.isJavaIdentifierPart()  How convenient for us!!
        while (i >= 0
                && (Character.isJavaIdentifierPart(part.charAt(i))
                || part.charAt(i) == '.')) {
            i--;
        }

        part = part.substring(i + 1);

        if (part.length() < 2) // reasonable completion length
        {
            return;
        }

        //System.out.println("completing part: "+part);
        // no completion
        String[] complete = nameCompletion.completeName(part);
        if (complete.length == 0) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            return;
        }

        // Found one completion (possibly what we already have)
        if (complete.length == 1 && !complete.equals(part)) {
            complete(complete[0]);
            PopupWindow popup = outputArea.getPopupWindow();
            if ((popup != null) && popup.isShowing()) {
                popup.hide();
            }
            return;
        }

        String commonPrefix = StringUtils.getCommonPrefix(complete);
        if (commonPrefix.length() > part.length()) {
            outputArea.appendText(commonPrefix.substring(part.length()));
        }

        commandList.setContent(complete);
        commandList.show(outputArea);
    }

    public void enter() {
        String command = getCurrentLine();
        if (command.length() > 0) {
            history.add(command);
            historyPointer = history.size();
        }
        outputArea.appendText("\n");
        command += '\n';
        acceptLine(command);
    }

    public void caret(Integer oldValue, Integer newValue) {
        int pos = outputArea.getCaretPosition();
        int col = outputArea.getCaretColumn();
        int startPar = outputArea.offsetToPosition(pos, Forward).getMajor();
        int nParagraphs = outputArea.getParagraphs().size();
        if (startPar != (nParagraphs - 1)) {
            outputArea.positionCaret(oldValue);
        } else if (col < prompt.length()) {
            outputArea.positionCaret(pos + (prompt.length() - col));
        }

    }

    public void setInterpreter(String name) {
        function = interpreters.get(name);
    }

    private void acceptLine(String line) {
        StringBuilder buf = new StringBuilder();
        int lineLength = line.length();
        for (int i = 0; i < lineLength; i++) {
            char c = line.charAt(i);
            buf.append(c);
        }
        line = buf.toString();
        if (line.trim().startsWith("interp(\"") && line.trim().endsWith("\")")) {
            String cmd = line.trim().substring(8);
            String interpName = cmd.substring(0, cmd.length() - 2);
            System.out.println("interp " + interpName);
            if (interpName.equals("renjin")) {
                function = null;
            } else {
                function = interpreters.get(interpName);
            }
            return;
        }
        if (outPipe == null) {
            print("Console internal   error: cannot output ...", Color.RED);
        } else {
            try {
                if (function != null) {
                    if (function != null) {
                        function.apply(line);
                        getOut().print("> ");
                    }

                } else {
                    outPipe.write(line.getBytes());
                    outPipe.flush();
                }
            } catch (IOException e) {
                outPipe = null;
                throw new RuntimeException("Console pipe broken...", e);
            }
        }
    }

    public void setOutputArea(InlineCssTextArea outputArea) {
        this.outputArea = outputArea;
    }

    public void addInterpreter(String name, Function<String, String> function) {
        interpreters.put(name, function);
        this.function = function;

    }

    public void addHandler() {
        // this.interpreter = interpreter;
        outputArea.setEditable(false);
        //interpreter.setOut(new ConsoleOutputStream());
        //interpreter.setErr(new ConsoleOutputStream());

        EventHandler<? super KeyEvent> ctrlS = EventHandlerHelper
                .on(keyPressed(S, CONTROL_DOWN)).act(event -> save())
                .create();

        EventHandlerHelper.install(outputArea.onKeyPressedProperty(), ctrlS);

        EventHandler<? super KeyEvent> enter = EventHandlerHelper
                .on(keyPressed(ENTER)).act(event -> enter())
                .create();

        EventHandlerHelper.install(outputArea.onKeyPressedProperty(), enter);
        EventHandler<? super KeyEvent> backSpace = EventHandlerHelper
                .on(keyPressed(KeyCode.BACK_SPACE)).act(event -> delete())
                .create();

        EventHandler<? super KeyEvent> delete = EventHandlerHelper
                .on(keyPressed(KeyCode.DELETE)).act(event -> delete())
                .create();

        EventHandlerHelper.install(outputArea.onKeyPressedProperty(), delete);
        EventHandlerHelper.install(outputArea.onKeyPressedProperty(), backSpace);

        EventHandler<? super KeyEvent> tab = EventHandlerHelper
                .on(keyPressed(KeyCode.TAB)).act(event -> checkCommand())
                .create();
        EventHandlerHelper.install(outputArea.onKeyPressedProperty(), tab);

        EventHandler<? super KeyEvent> paste = EventHandlerHelper
                .on(keyPressed(KeyCode.V, KeyCombination.SHORTCUT_DOWN)).act(event -> paste(event))
                .create();

        EventHandlerHelper.install(outputArea.onKeyPressedProperty(), paste);

        EventHandler<? super KeyEvent> copy = EventHandlerHelper
                .on(keyPressed(KeyCode.C, KeyCombination.SHORTCUT_DOWN)).act(event -> copy(event))
                .create();

        EventHandlerHelper.install(outputArea.onKeyPressedProperty(), copy);

        EventHandler<? super KeyEvent> historyUp = EventHandlerHelper
                .on(keyPressed(KeyCode.UP)).act(event -> historyUp())
                .create();

        EventHandlerHelper.install(outputArea.onKeyPressedProperty(), historyUp);

        EventHandler<? super KeyEvent> historyDown = EventHandlerHelper
                .on(keyPressed(KeyCode.DOWN)).act(event -> historyDown())
                .create();

        EventHandlerHelper.install(outputArea.onKeyPressedProperty(), historyDown);

        EventHandler<? super KeyEvent> charTyped = EventHandlerHelper
                .on(keyTyped()).act(event -> typed(event))
                .create();
        EventHandlerHelper.install(outputArea.onKeyTypedProperty(), charTyped);
        outputArea.caretPositionProperty().addListener((ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) -> {
            caret(oldValue, newValue);
        });

    }

    public static class BlockingPipedInputStream extends PipedInputStream {

        boolean closed;

        public BlockingPipedInputStream(PipedOutputStream pout)
                throws IOException {
            super(pout);
        }

        public synchronized int read() throws IOException {
            if (closed) {
                throw new IOException("stream closed");
            }

            while (super.in < 0) {    // While no data */
                notifyAll();    // Notify any writers to wake up
                try {
                    wait(750);
                } catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
            }
            // This is what the superclass does.
            int ret = buffer[super.out++] & 0xFF;
            if (super.out >= buffer.length) {
                super.out = 0;
            }
            if (super.in == super.out) {
                super.in = -1;
                /* now empty */
            }
            return ret;
        }

        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

}
