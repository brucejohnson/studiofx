package org.renjin.studiofx;

import java.io.PrintWriter;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextArea;
import org.renjin.studiofx.console.ConsoleFx;

public class FXMLController implements Initializable {

    @FXML
    private VBox topBar;
    @FXML
    InlineCssTextArea outputArea;
    ConsoleFx consoleFx;
    private StudioSession session;
    Stage stage;

    @FXML
    private void handleButtonAction(ActionEvent event) {
        System.out.println("You clicked me!");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        stage = MainApp.getMainStage();
        MainApp.registerStage(stage, this);
        System.out.println("initialize");
        consoleFx = new ConsoleFx();
        consoleFx.setOutputArea(outputArea);
        consoleFx.addHandler();
        outputArea.setEditable(true);
        if (!MainApp.isMac()) {
            MenuBar menuBar = MainApp.getMenuBar();
            topBar.getChildren().add(0, menuBar);
        }

        this.session = new StudioSession();
        session.setStdOut(new PrintWriter(consoleFx.getOut()));
        consoleFx.initInterpreter(session);

    }
}
