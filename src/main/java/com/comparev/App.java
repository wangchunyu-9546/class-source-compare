package com.comparev;

import com.comparev.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        MainView mainView = new MainView(stage);
        Scene scene = new Scene(mainView, 1180, 720);
        stage.setTitle("CompareV - Class 与源码兼容性比对");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
