package mind.master.androidcamerax;

import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private int REQUESTED_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]
            {"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    TextureView mTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        mTextureView = findViewById(R.id.view_finder);

        if (allPermissionsGranted()) {
            startCamera(); //Start camera if permission has been granted
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUESTED_CODE_PERMISSIONS);
        }



    }

    private void startCamera() {
        CameraX.unbindAll();

        Rational aspectRatio = new Rational(mTextureView.getWidth(), mTextureView.getHeight());

        Size screen = new Size(mTextureView.getWidth(), mTextureView.getHeight()); //Size of the screen

        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();

        Preview preview = new Preview(pConfig);


        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {

            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                ViewGroup parent = (ViewGroup) mTextureView.getParent();
                parent.removeView(mTextureView);
                parent.addView(mTextureView, 0);

                mTextureView.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }
        });

        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();



        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        findViewById(R.id.imgCapture).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                File file = new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".png");
                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        String msg = "Pic captured at " + file.getAbsolutePath();
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        String msg = "Pic capture failed : " + message;
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();

                        if (cause != null) {
                            cause.printStackTrace();
                        }

                    }
                });
            }
        });
        CameraX.bindToLifecycle(this, preview, imgCap);
    }

    private void updateTransform() {
        Matrix mMatrix = new Matrix();
        float w = mTextureView.getMeasuredWidth();
        float h = mTextureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int) mTextureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }
        mMatrix.postRotate((float)rotationDgr, cX, cY);
        mTextureView.setTransform(mMatrix);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUESTED_CODE_PERMISSIONS) {
            if(allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permission not granted by the user", Toast.LENGTH_LONG).show();
                finishAndRemoveTask();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
