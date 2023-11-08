package com.example.text2speech;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private MaterialButton inputImageBtn;
    private MaterialButton changeLocaleBtn;
    private MaterialButton recognizeTextBtn;
    private MaterialButton startStopContinueSpeechBtn;
    private ShapeableImageView imageIv;
    private EditText recognizedTextEt;
    private TextToSpeech textToSpeech;

    private static final String TAG = "MAIN_TAG";

    private Uri imageUri = null;
    private String[] cameraPermissions;
    private String[] storagePermissions;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;

    private boolean isSpeaking = false;
    private boolean isPaused = false;
    private int lastPosition = 0;
    private String spokenText;
    private FloatingActionButton howToUseBtn;

    private int checkClick = 0;

    private com.google.mlkit.vision.text.TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputImageBtn = findViewById(R.id.inputImageBtn);
        changeLocaleBtn = findViewById(R.id.changeLocaleBtn);
        recognizeTextBtn = findViewById(R.id.recognizeTextBtn);
        startStopContinueSpeechBtn = findViewById(R.id.startStopContinueSpeechBtn);
        imageIv = findViewById(R.id.imageIv);
        recognizedTextEt = findViewById(R.id.recognizedTextEt);
        howToUseBtn = findViewById(R.id.learnToUseBtn);

        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};



        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);



        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputImageDialog();
            }
        });

        changeLocaleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLocaleDialog();
            }
        });

        recognizeTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageUri == null) {
                    Toast.makeText(MainActivity.this, "Pick an image first...", Toast.LENGTH_SHORT).show();
                } else {
                    checkClick = 1;
                    recognizedTextFromImage();
                }
            }
        });

        howToUseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                instructions();
            }
        });

        startStopContinueSpeechBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isSpeaking) {
                    // Start or Continue speaking
                    startSpeech();
                } else {
                    if (isPaused) {
                        // Continue from where it was paused
                        continueSpeech();
                    } else {
                        // Stop speaking
                        stopSpeech();
                    }
                }
            }
        });
    }

    // ALL OF THE FUNCTIONS
    private void changeLocale(Locale locale) {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(locale);
                } else {
                    Log.e("TAG", "Text-to-Speech initialization failed.");
                }
            }
        });
    }

    private void instructions() {
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.dialog_icon_id)
                .setTitle("How to use the app: ")
                .setMessage("1. Select or take an image. The image will replace the image icon\n" +
                        "2. Select the language in the image\n" +
                        "3. Press recognize text\n\n" +
                        "You can always click this button again to check these instructions")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLocaleDialog() {
        if (changeLocaleBtn != null) {
            PopupMenu popupMenu = new PopupMenu(this, changeLocaleBtn);

            popupMenu.getMenu().add(Menu.NONE, 1, 1, "ENGLISH");
            popupMenu.getMenu().add(Menu.NONE, 2, 2, "GERMAN");
            popupMenu.getMenu().add(Menu.NONE, 3, 3, "ITALIAN");
            popupMenu.getMenu().add(Menu.NONE, 4, 4, "FRENCH");
            popupMenu.getMenu().add(Menu.NONE, 5, 5, "FRENCH CANADIAN");
            popupMenu.getMenu().add(Menu.NONE, 6, 6, "CANADIAN");

            popupMenu.show();

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    int id = menuItem.getItemId();
                    if (id == 1) {
                        changeLocale(Locale.US);
                    } else if (id == 2) {
                        changeLocale(Locale.GERMAN);
                    } else if (id == 3) {
                        changeLocale(Locale.ITALIAN);
                    } else if (id == 4) {
                        changeLocale(Locale.FRENCH);
                    } else if (id == 5) {
                        changeLocale(Locale.CANADA_FRENCH);
                    } else if (id == 6) {
                        changeLocale(Locale.FRENCH);
                    }
                    return true;
                }
            });
        } else {
            Log.e("TAG", "changeLocaleBtn is null");
        }
    }

    private void showInputImageDialog() {
        PopupMenu popupMenu = new PopupMenu(this, inputImageBtn);

        popupMenu.getMenu().add(Menu.NONE, 1, 1, "CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "GALLERY");

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == 1) {
                    Log.d(TAG, "onMenuItemClick: Camera Clicked...");
                    if (checkCameraPermissions()) {
                        pickImageCamera();
                    } else {
                        requestCameraPermissions();
                    }
                } else if (id == 2) {
                    Log.d(TAG, "onMenuItemClick: Gallery Clicked");
                    if (checkStoragePermission()) {
                        pickImageGallery();
                    } else {
                        requestStoragePermission();
                    }
                }
                return true;
            }
        });
    }

    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri " + imageUri);
                        imageIv.setImageURI(imageUri);
                    } else {
                        Log.d(TAG, "onActivityResult: cancelled");
                        Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickImageCamera() {
        Log.d(TAG, "pickImageCamera: ");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "onActivityResult: imageUri " + imageUri);
                        imageIv.setImageURI(imageUri);
                    } else {
                        Log.d(TAG, "onActivityResult: cancelled");
                        Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermissions() {
        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return cameraResult && storageResult;
    }

    private void requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void recognizedTextFromImage() {
        Log.d(TAG, "recognizedTextFromImage: ");
        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);

            textRecognizer.process(inputImage)
                    .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            String recognizedText = text.getText();
                            recognizedTextEt.setText(recognizedText);

                            // Speak the recognized text
                            spokenText = recognizedText;
                            speakRecognizedText(recognizedText);
                        }
                    })
                    .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "onFailure: ", e);
                            Toast.makeText(MainActivity.this, "Failed recognizing text due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "recognizedTextFromImage: ", e);
            Toast.makeText(MainActivity.this, "Failed Preparing Image due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void speakRecognizedText(String text) {
        // Check if Text-to-Speech is ready
        if (textToSpeech != null && textToSpeech.getEngines().size() > 0) {
            // Use a handler to post delayed speech synthesis
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Speak the recognized text out loud
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, text);
                    isSpeaking = true;
                    isPaused = false;
                    startStopContinueSpeechBtn.setText("Stop");
                }
            }, 100); // Delay before starting speech
        }
    }

    private void stopSpeech() {
        if (textToSpeech != null && isSpeaking) {
            textToSpeech.stop();
            isSpeaking = false;
            isPaused = false;
            startStopContinueSpeechBtn.setText("Continue");
        }
    }

    private void continueSpeech() {
        if (textToSpeech != null && !isPaused) {
            isPaused = true;
            lastPosition = spokenText.length() - lastPosition;
            String text = spokenText.substring(lastPosition);
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, text);
            isSpeaking = true;
            startStopContinueSpeechBtn.setText("Stop");
        }
    }

    private void startSpeech() {
        if (textToSpeech != null) {
            String text = recognizedTextEt.getText().toString();
            if (!text.isEmpty()) {
                spokenText = text;
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, text);
                isSpeaking = true;
                isPaused = false;
                startStopContinueSpeechBtn.setText("Stop");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
        System.exit(0);
    }
}