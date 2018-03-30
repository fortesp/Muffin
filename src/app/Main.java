package Application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
/*
    Muffin Project v0.1
    Author: Pedro Fortes (c) 2017
    https://github.com/fortesp
 */
public class Main extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception{

        Parent root = FXMLLoader.load(getClass().getResource("Application.fxml"));
        primaryStage.setTitle("Muffin v0.1");
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root, 700, 470));
        primaryStage.show();
    }

    public static void main(String[] args) {

        launch(args);
    }


}
