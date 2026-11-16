import javafx.application.Application;
import javafx.stage.Stage;
import com.gitpulse.StartingWindow.SplashScreen;
public class GitPulse extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setFullScreen(true);
        SplashScreen splash = new SplashScreen();
        splash.show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}