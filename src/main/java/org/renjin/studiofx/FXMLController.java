package org.renjin.studiofx;

import java.io.PrintWriter;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.InlineCssTextArea;
import org.jfxplot.PlotManager;
import org.jfxplot.StageManager;
import org.renjin.parser.RParser;
import org.renjin.sexp.SEXP;
import org.renjin.studiofx.console.ConsoleFx;

public class FXMLController implements Initializable {

    @FXML
    private VBox topBar;
    @FXML
    InlineCssTextArea consoleArea;
    @FXML
    BorderPane plotPane;
    @FXML
    CodeArea codeArea;
    ConsoleFx consoleFx;
    private StudioSession session;
    Stage stage;
    RCodeAreaHighlighter highlighter;
    PlotManager plotManager;
    StageManager stageManager;

    static String initialCode = "import(org.jfxplot.PlotApp)\n"
            + "import(org.jfxplot.PlotManager)\n"
            + "import(org.jfxplot.StageManager)\n"
            + "import(org.jfxplot.GraphicsState)\n"
            + "library(org.jfxplot.plot)\n"
            + "PlotApp$setLaunched()\n"
            + "StageManager$setLaunched()\n";

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
        consoleFx.setOutputArea(consoleArea);
        consoleFx.addHandler();
        consoleArea.setEditable(true);
        if (!MainApp.isMac()) {
            MenuBar menuBar = MainApp.getMenuBar();
            topBar.getChildren().add(0, menuBar);
        }
        highlighter = new RCodeAreaHighlighter(codeArea);

        this.session = new StudioSession();
        session.setStdOut(new PrintWriter(consoleFx.getOut()));
        consoleFx.initInterpreter(session);
//        PlotApp.setLaunched();
//        PlotManager plotManager = PlotApp.getManager();
        plotManager = new PlotManager();
        stageManager = new StageManager();
        plotManager.setStudioMode(plotPane);
        SEXP expr = RParser.parseInlineSource(initialCode);
        session.getTopLevelContext().evaluate(expr);

    }

    @FXML
    public void execCode() {
        String text = codeArea.getText() + "\n";
        SEXP expr = RParser.parseInlineSource(text);
        session.getTopLevelContext().evaluate(expr);
    }
}
