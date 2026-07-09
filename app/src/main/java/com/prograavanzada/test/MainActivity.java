package com.prograavanzada.test;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private PreviewView viewFinder;
    private MyGLSurfaceView glSurfaceView;
    private ExecutorService cameraExecutor;
    private HandLandmarker handLandmarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.glSurfaceView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (checkPermissions()) {
            setupMediaPipe();
            startCamera();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMediaPipe();
            startCamera();
        }
    }

    private void setupMediaPipe() {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("hand_landmarker.task") // Requiere el archivo del modelo en assets
                    .build();

            HandLandmarker.HandLandmarkerOptions options = HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener(this::onHandTrackingResult)
                    .setErrorListener(e -> Log.e("HandTracking", "Error: " + e.getMessage()))
                    .build();

            handLandmarker = HandLandmarker.createFromOptions(this, options);
        } catch (Exception e) {
            Log.e("HandTracking", "Error inicializando MediaPipe: " + e.getMessage());
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);

            } catch (Exception e) {
                Log.e("Camera", "Fallo al iniciar la cámara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(ImageProxy imageProxy) {
        if (handLandmarker != null) {
            long timestampMs = imageProxy.getImageInfo().getTimestamp() / 1000000;
            Bitmap bitmap = imageProxy.toBitmap(); // Requiere API nivel 21+, asegurado por CameraX

            if (bitmap != null) {
                MPImage mpImage = new BitmapImageBuilder(bitmap).build();
                handLandmarker.detectAsync(mpImage, timestampMs);
            }
        }
        imageProxy.close(); // IMPORTANTÍSIMO: Cerrar el proxy para recibir el siguiente frame
    }

    private void onHandTrackingResult(HandLandmarkerResult result, MPImage mpImage) {
        if (result.landmarks().size() > 0) {
            // Extraer los 21 puntos de la primera mano detectada
            float[][] landmarksArray = new float[21][2];
            for (int i = 0; i < 21; i++) {
                landmarksArray[i][0] = result.landmarks().get(0).get(i).x(); // Normalizado 0 a 1
                landmarksArray[i][1] = result.landmarks().get(0).get(i).y(); // Normalizado 0 a 1
            }

            // Enviar datos al renderizador
            glSurfaceView.getRenderer().updateHandLandmarks(landmarksArray);
        } else {
            // Si no hay mano, enviamos null para borrar las líneas
            glSurfaceView.getRenderer().updateHandLandmarks(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (handLandmarker != null) {
            handLandmarker.close();
        }
    }
}