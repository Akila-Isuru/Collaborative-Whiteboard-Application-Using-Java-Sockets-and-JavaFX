package org.example.whiteboard;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML; // FXML භාවිතය සඳහා
import javafx.fxml.FXMLLoader; // FXML Load කිරීම සඳහා
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Locale;

public class WhiteboardClient extends Application {
    private Socket socket;
    private PrintWriter out;
    private GraphicsContext gc;

    // වත්මන් ඇඳීමේ සැකසුම්
    private Color currentColor = Color.BLACK;
    private double currentSize = 5.0;
    private double lastX, lastY;

    // --- FXML අංග ---
    // FXML ගොනුවේ ඇති fx:id සමඟ ගලපා (@FXML) මෙහි ප්‍රකාශ කළ යුතුය
    @FXML private Label statusLabel;
    @FXML private Canvas canvas;
    @FXML private ColorPicker colorPicker;
    @FXML private Slider sizeSlider;
    @FXML private TextField usernameField;
    // --- FXML අංග අවසන් ---

    @Override
    public void start(Stage primaryStage) throws Exception {
        // FXML ගොනුව Load කිරීම
        FXMLLoader loader = new FXMLLoader(getClass().getResource("WhiteboardClient.fxml"));
        // Controller එක ලෙස මේ class එක භාවිතා කරන ලෙස loader එකට පැවසීම
        // FXML ගොනුවේ fx:controller="WhiteboardClient" ඇති නිසා මෙය අත්‍යවශ්‍ය නොවේ, නමුත් හොඳ පුරුද්දකි
        loader.setController(this);

        Parent root = loader.load();

        // FXML Load වූ පසු initialize() ක්‍රමය ස්වයංක්‍රීයව කැඳවනු ලැබේ

        Scene scene = new Scene(root);
        primaryStage.setTitle("සරල වයිට්බෝඩ් - Client (FXML)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // FXML Load වූ පසු කැඳවනු ලබන ක්‍රමය. මෙහිදී UI අංග සකසනු ලැබේ.
    @FXML
    public void initialize() {
        // GraphicsContext එක ලබා ගැනීම
        gc = canvas.getGraphicsContext2D();

        // --- UI අංග සඳහා මුල් සැකසුම් සහ සිදුවීම් සකස් කිරීම ---

        // වර්ණ තෝරකය
        colorPicker.setValue(currentColor);
        colorPicker.setOnAction(e -> {
            currentColor = colorPicker.getValue();
            gc.setStroke(currentColor);
        });

        // ප්‍රමාණ ස්ලයිඩරය
        sizeSlider.setValue(currentSize);
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentSize = newVal.doubleValue();
            gc.setLineWidth(currentSize);
        });

        // ඇඳීමේ ඛණ්ඩාංක සකස් කිරීම
        gc.setLineWidth(currentSize);
        gc.setStroke(currentColor);

        // කැන්වසය මත මූසික සිදුවීම් සකස් කිරීම
        canvas.setOnMousePressed(e -> {
            lastX = e.getX();
            lastY = e.getY();
        });

        canvas.setOnMouseDragged(e -> {
            // තමන්ගේ කැන්වසය මත ඇඳීම
            drawLocally(lastX, lastY, e.getX(), e.getY(), currentColor, currentSize);

            // සර්වර් එකට දත්ත යැවීම
            sendDrawData(lastX, lastY, e.getX(), e.getY(), currentColor, currentSize);

            lastX = e.getX();
            lastY = e.getY();
        });

        // සර්වර් එකට සම්බන්ධ වීම වෙනම ත්‍රෙඩ් එකක ආරම්භ කිරීම
        new Thread(this::connectToServer).start();
    }

    // Clear බොත්තම සඳහා ක්‍රමය (FXML එකෙන් කැඳවනු ලැබේ)
    @FXML
    private void handleClearButton() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    // තමන්ගේ කැන්වසය මත ඇඳීම
    private void drawLocally(double oldX, double oldY, double newX, double newY, Color color, double size) {
        gc.setStroke(color);
        gc.setLineWidth(size);
        gc.strokeLine(oldX, oldY, newX, newY);
    }

    // සර්වර් එකට ඇඳීමේ දත්ත යැවීම
    private void sendDrawData(double oldX, double oldY, double newX, double newY, Color color, double size) {
        if (out != null) {
            // සරල පෙළ පණිවිඩය: "DRAW:oldX,oldY,newX,newY,colorHex,size"
            String colorHex = color.toString().substring(2, 8);
            String message = String.format(Locale.US, "DRAW:%.2f,%.2f,%.2f,%.2f,%s,%.1f",
                    oldX, oldY, newX, newY, colorHex, size);
            out.println(message);
        }
    }

    // සර්වර් එකට සම්බන්ධ වී දත්ත ලබා ගැනීම
    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Platform.runLater(() -> statusLabel.setText("✅ සර්වර් එකට සම්බන්ධයි. පරිශීලක නාමය: " + usernameField.getText()));

            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                if (serverResponse.startsWith("DRAW:")) {
                    processDrawData(serverResponse.substring(5));
                }
            }

        } catch (IOException e) {
            Platform.runLater(() -> statusLabel.setText("❌ සම්බන්ධතා දෝෂය: සර්වර් එක සොයා ගත නොහැක."));
            System.err.println("Client දෝෂය: " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                // වසා දැමීමේ දෝෂය
            }
            Platform.runLater(() -> statusLabel.setText("❌ සම්බන්ධතාවය විසන්ධි විය."));
        }
    }

    // ලැබුණු දත්ත විශ්ලේෂණය කර කැන්වසය මත ඇඳීම
    private void processDrawData(String data) {
        try {
            String[] parts = data.split(",");
            if (parts.length == 6) {
                double oldX = Double.parseDouble(parts[0]);
                double oldY = Double.parseDouble(parts[1]);
                double newX = Double.parseDouble(parts[2]);
                double newY = Double.parseDouble(parts[3]);

                Color color = Color.web("#" + parts[4]);
                double size = Double.parseDouble(parts[5]);

                Platform.runLater(() -> {
                    drawLocally(oldX, oldY, newX, newY, color, size);
                });
            }
        } catch (Exception e) {
            System.err.println("ලැබුණු දත්ත හසුරුවීමේ දෝෂය: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}