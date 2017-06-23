package org.renjin.studiofx;

import de.codecentric.centerdevice.MenuToolkit;
import de.codecentric.centerdevice.dialogs.about.AboutStageBuilder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.renjin.studiofx.console.ConsoleFx;

public class MainApp extends Application {

    public static ArrayList<Stage> stages = new ArrayList<>();
    private static String version = null;
    private static String appName = "Renjin StudioFx";
    private ConsoleFx console;
    private WorkspacePane workspaceTab;
    private FilesPane filesTab;
    private static MenuBar mainMenuBar = null;
    Boolean isMac = null;
    static MainApp mainApp = null;

    @Override
    public void start(Stage stage) throws Exception {
        mainApp = this;
        stages.add(stage);
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Scene.fxml"));
        Platform.setImplicitExit(true);
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");

        stage.setTitle("Renjin StudioFX");
        stage.setScene(scene);
        stage.show();
        if (mainMenuBar == null) {
            mainMenuBar = makeMenuBar(appName);
        }

    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application. main() serves only as fallback in case the
     * application can not be launched through deployment artifacts, e.g., in IDEs with limited FX support. NetBeans
     * ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    private ConsoleFx getConsole() {
        return console;
    }

    public static Stage getMainStage() {
        return stages.get(0);
    }

    public static void removeStage(Stage stage) {
        synchronized (stages) {
            stages.remove(stage);
            if (stages.isEmpty()) {
                Platform.exit();
                System.exit(0);
            }
        }
    }

    public static void registerStage(Stage stage, FXMLController controller) {
        if (!stages.contains(stage)) {
            stages.add(stage);
        }
        stage.setOnCloseRequest(e -> {
            removeStage(stage);
        });

    }

    public static boolean isMac() {
        return SystemUtils.IS_OS_MAC;
    }

    public static MenuBar getMenuBar() {
        return mainApp.makeMenuBar(appName);
    }

    Stage makeAbout(String appName) {
        AboutStageBuilder aboutStageBuilder = AboutStageBuilder.start("About " + appName)
                .withAppName(appName).withCloseOnFocusLoss().withHtml("<i>Interactive Studio for Renjin</i>")
                .withVersionString("Version " + getVersion()).withCopyright("Copyright \u00A9 " + Calendar
                .getInstance().get(Calendar.YEAR));
        Image image = new Image(MainApp.class.getResourceAsStream("/images/studiofx.png"));
        aboutStageBuilder = aboutStageBuilder.withImage(image);
        return aboutStageBuilder.build();
    }

    MenuBar makeMenuBar(String appName) {
        MenuToolkit tk = null;
        if (isMac()) {
            tk = MenuToolkit.toolkit();
        }
        MenuBar menuBar = new MenuBar();

        // Application Menu
        // TBD: services menu
        Menu appMenu = new Menu(appName); // Name for appMenu can't be set at
        // Runtime
        MenuItem aboutItem = null;
        Stage aboutStage = makeAbout(appName);
        if (tk != null) {
            aboutItem = tk.createAboutMenuItem(appName, aboutStage);
        } else {
            aboutItem = new MenuItem("About...");
            aboutItem.setOnAction(e -> aboutStage.show());
        }
        MenuItem prefsItem = new MenuItem("Preferences...");
        MenuItem quitItem;
        //prefsItem.setOnAction(e -> showPreferences(e));
        if (tk != null) {
            quitItem = tk.createQuitMenuItem(appName);
            appMenu.getItems().addAll(aboutItem, new SeparatorMenuItem(), prefsItem, new SeparatorMenuItem(),
                    tk.createHideMenuItem(appName), tk.createHideOthersMenuItem(), tk.createUnhideAllMenuItem(),
                    new SeparatorMenuItem(), quitItem);
        } else {
            quitItem = new MenuItem("Quit");
            quitItem.setOnAction(e -> Platform.exit());
        }
        Menu helpMenu = new Menu("Help");

        if (tk != null) {
            Menu windowMenu = new Menu("Window");
            windowMenu.getItems().addAll(tk.createMinimizeMenuItem(), tk.createZoomMenuItem(), tk.createCycleWindowsItem(),
                    new SeparatorMenuItem(), tk.createBringAllToFrontItem());
            menuBar.getMenus().addAll(appMenu, windowMenu);
            tk.autoAddWindowMenuItems(windowMenu);
            tk.setGlobalMenuBar(menuBar);
        } else {
            menuBar.getMenus().addAll(helpMenu);
            helpMenu.getItems().add(0, aboutItem);
        }

        return menuBar;
    }

    public static String getVersion() {
        if (version == null) {
            String cp = System.getProperty("java.class.path");
            // processorgui-10.1.2.jar
            String jarPattern = ".*studiofx-([0-9\\.\\-abcr]+)\\.jar.*";
            Pattern pat = Pattern.compile(jarPattern);
            Matcher match = pat.matcher(cp);
            version = "0.0.0";
            if (match.matches()) {
                version = match.group(1);
            }
        }
        return version;
    }

}
