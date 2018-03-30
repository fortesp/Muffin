package Application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;
/*
    Muffin Project v0.1
    Author: Pedro Fortes (c) 2017
    https://github.com/fortesp
 */
public class Controller {

    static List<MFile> fileList = new ArrayList<>();
    static List<List<MFile>> resultList = new ArrayList<List<MFile>>();
    static int[] resultSpace = new int[3];

    static double SIMILARITY_THRESHOLD = 0.75;
    static int SIZE_THRESHOLD = 100;
    static String LOOKUPDIR = "D:\\";
    static String BACKUPDIR = "bkp\\";
    static String EXTENSIONS = "mp3 wav mp4 jpg ogg wma pls avi mkv mov mpg mpeg";

    @FXML
    public TextField txtDir;
    public ProgressBar progressBar;
    public Label lblProgress;
    public Button btCheck;
    public ListView listView;
    public Label lblTotal;
    public Button btClean;
    public Label lblStatus;
    public Button btOpen;

    int totalProcessed = 0;
    Task progressWorker;

    public void initialize(){
    }

    @FXML
    public void readDirectoryAction() throws InterruptedException {

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        reset();

        LOOKUPDIR = txtDir.getText();

        // Check the directory
        if(!(LOOKUPDIR.length() > 0) || !new File(LOOKUPDIR).exists()) {
            showMessage("'" + LOOKUPDIR + "' directory does not exist.");
            return;
        }

        btCheck.setDisable(true);
        btClean.setDisable(true);

        // Read directory recursively
        lblStatus.setText("Reading directory...");
        Thread.currentThread().sleep(100);
        getFilesIn(LOOKUPDIR);
        // --

        // Start the progress bar
        progressWorker = progressWorker();
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(progressWorker.progressProperty());

        new Thread(progressWorker).start();
        // --

        lblStatus.setText("Comparing files, this may take some time...");

        // Main process
        new Thread(() -> {
            try {
                    // Start processing...
                    processList();
                    // --

                    Platform.runLater(() -> {
                            lblTotal.setText("Report:\n\n"
                                    + "Total files: " + fileList.size() + "\n"
                                    + "Total duplicated files: " + listView.getItems().size() + "\n"
                                    + "Total exact files: " + resultList.get(0).size() + "\n"
                                    + "Total similar files: " + resultList.get(1).size() + "\n"
                                    + "Total garbage files: " + resultList.get(2).size() + "\n\n"
                                    + "Total wasted space: \n" + computeSpace(resultSpace[0] + resultSpace[1] + resultSpace[2]) + "\n"
                                    + "Total wasted garbage files space: \n" + computeSpace(resultSpace[2]) + "\n"
                            );

                            btClean.setDisable(false);
                            btCheck.setDisable(false);
                            lblStatus.setText("Finished.");
                    });


                } catch (IOException e) {
            }
        }).start();


    }

    @FXML
    public void openFileDialog() {

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select directory");

        File file = chooser.showDialog(btOpen.getScene().getWindow());

        txtDir.setText(file.getAbsolutePath());
    }

    private String computeSpace(int v) {

        String[] units = {"b", "Kb", "Mb", "Gb"};

        int o = 0;
        for(String unit : units) {

            if(v / 1024 == 0) break;

            v = v / 1024;
            o++;
        }

        return v + units[o];
    }

    @FXML
    public void removeAction() {

        List<MFile> list = listView.getSelectionModel().getSelectedItems();

        if(list.isEmpty()) return;

        // Are you sure confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("The duplicated files will be moved to + " + getBackupDir() + " directory. Are you sure you want to proceed?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() != ButtonType.OK)  return;
        // --

        try {
            File f = new File(getBackupDir());
            if (!f.exists()) {
                System.out.println("Creating directory: " + getBackupDir());
                f.mkdir();
            }
        } catch(SecurityException e){
            System.out.println("Error creating backup directory. Nothing to do.");
            System.exit(1);
        }

        for(MFile file : list) {

            try {

                Path p1 = Paths.get(file.toString());
                Path p2 = Paths.get(getBackupDir() + file.getName());

                Files.move(p1, p2, StandardCopyOption.REPLACE_EXISTING);

                System.out.println(file + " moved to " + getBackupDir());

            } catch(IOException e) {

                System.out.println("Something went wront moving a file. Processed aborted.");
                System.exit(1);
            }

        }

        listView.getItems().removeAll(list);
    }

    public Task progressWorker() {

        int listSize = fileList.size();

        progressBar.progressProperty().addListener(observable -> {
            lblProgress.setText(((totalProcessed * 100) / listSize) + "%");
            if (progressBar.getProgress() >= 1) {
                progressBar.setStyle("-fx-accent: forestgreen;");
            }
        });

        return new Task() {
            @Override
            protected Object call() throws Exception {

                while(totalProcessed < listSize) {
                    Thread.sleep(100);
                    updateProgress(totalProcessed, listSize);

                }
                return true;
            }
        };
    }

    private void processList() throws IOException {

        int listSize = fileList.size();

        resultList.add(0, new ArrayList<>()); // Exact
        resultList.add(1, new ArrayList<>()); // Similar
        resultList.add(2, new ArrayList<>()); // Garbage files

        long start = System.currentTimeMillis();

        for (int i = 0; i < listSize; i++) {

            MFile f1 = fileList.get(i);


            // Garbage files
            if (!EXTENSIONS.contains(f1.getExtension()) || f1.length() == 0) {
                resultList.get(2).add(f1);
                resultSpace[2] += f1.length();
                totalProcessed++;
                continue;
            }
            // --

            for (int j = i + 1; j < listSize; j++) {

                MFile f2 = fileList.get(j);

              //  if(f2.getParentMFile() != null) continue;

                double srate = getSimilarity(f1.getName(), f2.getName());
                if (srate > SIMILARITY_THRESHOLD && Math.abs(f1.length() - f2.length()) <= SIZE_THRESHOLD) {

                    f1.addLinkedFile(f2);

                    f2.setParentMFile(f1);

                    int opt = (f1.length() == f2.length() && f1.contentEquals(f2))?0:1;

                    if(!resultList.get(opt).contains(f1)) {
                        resultList.get(opt).add(f1);
                        resultSpace[opt] += f1.length();
                    }

                }
            }

            totalProcessed++;
        }

        Platform.runLater(() -> {
            listView.getItems().addAll(resultList.get(0));
            listView.getItems().addAll(resultList.get(1));
            listView.getItems().addAll(resultList.get(2));
        });

        long end = System.currentTimeMillis();

    }

    private void getFilesIn(String sDir) {

        File[] files = new MFile(sDir).listFiles();

        for (File file : files) {
            if (file.isDirectory() && !getBackupDir().contains(file.getAbsolutePath())) {
                getFilesIn(file.getAbsolutePath());
            } else {

                fileList.add(new MFile(file));
            }
        }
    }

    private void reset() {

        resultList.clear();
        fileList.clear();

        resultSpace[0] = 0;
        resultSpace[1] = 0;
        resultSpace[2] = 0;

        lblTotal.setText("");
        lblStatus.setText("");
        btCheck.setDisable(false);
        btClean.setDisable(false);
        listView.getItems().clear();
        totalProcessed = 0;
    }
    private String getBackupDir() {

        return LOOKUPDIR + "\\" + BACKUPDIR;
    }

    private void showMessage(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText("");
        alert.setContentText(message);

        alert.showAndWait();
    }

    // Levenshtein Distance
    // Source: http://stackoverflow.com/questions/955110/similarity-string-comparison-in-java
    private double getSimilarity(String s1, String s2) {

        if (s1 == s2) return 1.0;

        String longer = s1, shorter = s2;

        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0; /* both strings are zero length */
        }
        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) / (double) longerLength;
    }
}
