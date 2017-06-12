/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renjin.studiofx.console;

import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.controlsfx.control.PopOver;
import org.fxmisc.richtext.PopupAlignment;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.wellbehaved.event.EventHandlerHelper;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;

/**
 *
 * @author Bruce Johnson
 */
public class CommandList {

    PopOver popOver;
    ListView<String> listView;
    ConsoleFx consoleFx;

    public CommandList(ConsoleFx consoleFx) {
        this.consoleFx = consoleFx;
        init(consoleFx.outputArea);
    }

    public PopOver getPopOver(StyledTextArea textArea) {
        return popOver;
    }

    public void show(StyledTextArea textArea) {
        popOver.show(textArea);
        textArea.requestLayout();
    }

    private void enter() {
        String cmd = (String) listView.getSelectionModel().getSelectedItem();
        consoleFx.complete(cmd);
        popOver.hide();
    }

    private void init(StyledTextArea textArea) {
        popOver = new PopOver();
        textArea.setPopupWindow(popOver);
        textArea.setPopupAlignment(PopupAlignment.CARET_BOTTOM);

        listView = new ListView();
        popOver.setContentNode(listView);
        EventHandler<? super KeyEvent> enter = EventHandlerHelper
                .on(keyPressed(KeyCode.ENTER)).act(event -> enter())
                .create();

        EventHandlerHelper.install(listView.onKeyPressedProperty(), enter);

        EventHandler<? super KeyEvent> leave = EventHandlerHelper
                .on(keyPressed(KeyCode.ESCAPE)).act(event -> popOver.hide())
                .create();

        EventHandlerHelper.install(listView.onKeyPressedProperty(), leave);
    }

    public void setContent(String[] commands) {
        listView.getItems().setAll(commands);
        listView.getSelectionModel().select(0);
    }

}
