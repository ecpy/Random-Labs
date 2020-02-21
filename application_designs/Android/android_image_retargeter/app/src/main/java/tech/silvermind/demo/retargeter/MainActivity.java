package tech.silvermind.demo.retargeter;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import tech.silvermind.dirchooser.DirectoryChooserConfig;
import tech.silvermind.dirchooser.DirectoryChooserFragment;

public class MainActivity extends AppCompatActivity implements DirectoryChooserFragment.OnFragmentInteractionListener, tech.silvermind.demo.retargeter.ColorPickerDialog.OnColorChangedListener, ImageProcessingView.SaveRetargetedRawBitmapCallback {

    /**
     * Constants
     */
    private static final String WARNING_NO_IMAGE_IMPORTED = "No image is imported.";
    private static final String WARNING_FAIL_SAVE_IMAGE = "Failed to save image. Please choose another SD card.";
    private static final String MESSAGE_STARTUP = "Press me to load a sample or \ntake image from camera or gallery.";
    private static final int STATE_NO_IMAGE_IMPORTED = 1;
    private static final int STATE_PROCESSING_IMAGE = 2;

    /**
     * TextView
     */
    public final static int LONG_PRESS_TIME_ON_TEXTVIEW = 500;
    private final Handler handlerOnTextView = new Handler();
    private TextView textView;

    /**
     * PopupWindow
     */
    PopupWindow popupWindowAbout;

    /**
     * Toolbar
     **/
    private Toolbar toolbar;
    private MenuItem menuItem_TakeAPhoto;
    private MenuItem menuItem_ImportFromGallery;
    private ActionBar actionBar;

    /**
     * Others
     */
    private int STATE = STATE_NO_IMAGE_IMPORTED;

    /**
     * ImageView
     */
    private ImageProcessingView ipv;
    private int ImageProcessingViewHeight = 0;
    private int ImageProcessingViewWidth = 0;

    /**
     * File location
     */
    private DirectoryChooserFragment dirChooser;

    /**
     * ColorPickerDialog
     */
    private ColorPickerDialog colorPickerDialog;

    /**
     * Algorithm values
     */
    private Bitmap rawBitmap;
    private int rawBitmapWidth;
    private int rawBitmapHeight;
    private boolean IMAGE_ROTATED_TO_PROCESS = false;

    /**
     * Memory
     */
    private static int MEMORY_SIZE = 0;
    private static int MAX_BITMAP_PIXELS = 0;

    /**
     * User Preference
     */
    private static SharedPreferences preferences;
    private static SharedPreferences.Editor preferenceEditor;
    private static final String USER_PREFERENCE = "0";
    // save location
    private static final String PREF_SAVE_LOCATION = "1";
    private static String PREF_SAVE_LOCATION_NULL;
    private static String PREF_SAVE_LOCATION_VALUE;
    // color
    private static final String PREF_COLOR = "2";
    private static int PREF_COLOR_NULL = R.color.Orange;
    private static int PREF_COLOR_VALUE;
    // model
    private static final String PREF_MODEL = "3";
    private static int PREF_MODEL_NULL = 0;
    private static int PREF_MODEL_VALUE;
    // mesh
    private static final String PREF_MESH = "4";
    private static boolean PREF_MESH_NULL = false;
    private static boolean PREF_MESH_VALUE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MEMORY_SIZE = ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryClass();
        MAX_BITMAP_PIXELS = calculateMaxBitmapPixels();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ipv = (ImageProcessingView) findViewById(R.id.imageProcessingView);
        //ipv.setupInkColor(getResources().getColor(R.color.DefaultColor));
        loadUserSetting();
        ViewTreeObserver viewTreeObserver = ipv.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    ImageProcessingViewWidth = ipv.getWidth();
                    ImageProcessingViewHeight = ipv.getHeight();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        ipv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        ipv.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            });
        }
        final Runnable runnableOnTextView = new Runnable() {
            @Override
            public void run() {
                ipv.setVisibility(View.VISIBLE);
                rawBitmap = getSampleImage();
                ipv.setImageBitmapSource(rawBitmap);
                STATE = STATE_PROCESSING_IMAGE;
                textView.setVisibility(View.GONE);
            }
        };
        textView = (TextView) findViewById(R.id.textView);
        textView.setText(MESSAGE_STARTUP);
        textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, final MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handlerOnTextView.postDelayed(runnableOnTextView, LONG_PRESS_TIME_ON_TEXTVIEW);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        handlerOnTextView.removeCallbacks(runnableOnTextView);
                        break;
                    case MotionEvent.ACTION_UP:
                        handlerOnTextView.removeCallbacks(runnableOnTextView);
                        break;
                }
                return false;
            }
        });
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        menuItem_TakeAPhoto = (MenuItem) findViewById(R.id.take_a_photo);
        menuItem_ImportFromGallery = (MenuItem) findViewById(R.id.import_from_gallery);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        ipv.setVisibility(View.INVISIBLE);
        loadAds();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //menu.getItem(4).setEnabled(false);
        //menu.getItem(5).setEnabled(false);
        //menu.getItem(4).setVisible(false);
        //menu.getItem(5).setVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    private void loadUserSetting() {
        preferences = getSharedPreferences(USER_PREFERENCE, MODE_PRIVATE);
        preferenceEditor = preferences.edit();
        PREF_SAVE_LOCATION_NULL = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).getAbsolutePath();
        PREF_SAVE_LOCATION_VALUE = preferences.getString(PREF_SAVE_LOCATION, PREF_SAVE_LOCATION_NULL);
        // set save location to current menu

        PREF_COLOR_VALUE = preferences.getInt(PREF_COLOR, PREF_COLOR_NULL);
        ipv.setupInkColor(PREF_COLOR_VALUE);

        PREF_MODEL_VALUE = preferences.getInt(PREF_MODEL, PREF_MODEL_NULL);
        MuMapConstructor.setModel(PREF_MODEL_VALUE);
    }

    private void hideNavigationBar() {

        //TODO: 11-4-2016 FIX: Hide Navigation Bar
        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                //| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            // TODO: The system bars are visible. Make any desired
                            // adjustments to your UI, such as showing the action bar or
                            // other navigational controls.

                            //decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                            //decorView.setSystemUiVisibility(decorView.getSystemUiVisibility());
                            //getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                            decorView.setSystemUiVisibility(flags);
                            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        } else {
                            // TODO: The system bars are NOT visible. Make any desired
                            // adjustments to your UI, such as hiding the action bar or
                            // other navigational controls.
                        }
                    }
                });
        decorView.setSystemUiVisibility(flags);
    }

    private int calculateMaxBitmapPixels() {
        float percentageEachBitmapInUse = 0.15f;
        float eachPixelMemory = (float) 8;

        return (int) (percentageEachBitmapInUse * MEMORY_SIZE * 1024 * 1024 / eachPixelMemory);
    }

    private Bitmap getSampleImage() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), R.drawable.sample, options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        options.inSampleSize = calculateInSampleSize(options, ImageProcessingViewHeight * imageWidth / imageHeight, ImageProcessingViewHeight);
        options.inJustDecodeBounds = false;
        return Bitmap.createScaledBitmap(decodeSampledBitmapFromResource(getResources(), R.drawable.sample, ImageProcessingViewHeight * imageWidth / imageHeight, ImageProcessingViewHeight),
                ImageProcessingViewHeight * imageWidth / imageHeight, ImageProcessingViewHeight, true);
        //decodeSampledBitmapFromResource(getResources(), R.drawable.sample, ImageProcessingViewHeight * imageWidth / imageHeight, ImageProcessingViewHeight);
    }

    private static boolean isMenuLoaded = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_buttons, menu);
        PREF_MESH_VALUE = preferences.getBoolean(PREF_MESH, PREF_MESH_NULL);
        ipv.SHOW_MESH = PREF_MESH_VALUE;
        MenuItem showMesh = menu.findItem(R.id.mesh);
        if (PREF_MESH_VALUE) {
            showMesh.setTitle(R.string.hide_mesh);
        } else {
            showMesh.setTitle(R.string.show_mesh);
        }
        isMenuLoaded = true;
        return super.onCreateOptionsMenu(menu);
    }

    private void lockScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void unlockScreen() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void showWarning(final String string) {
        //textView.setText(string);
        lockScreen();
        final Animation in = new AlphaAnimation(0f, 1f) {{
            setDuration(250);
        }};
        final Animation out = new AlphaAnimation(1f, 0f) {{
            setDuration(250);
        }};
        out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                textView.setText(string);
                textView.startAnimation(in);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        in.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!textView.getText().toString().equals(WARNING_NO_IMAGE_IMPORTED)) {
                    //textView.setEnabled(true);
                    //(findViewById(R.id.bt_go)).setEnabled(true);
                    //(findViewById(R.id.save_file)).setEnabled(true);
                    unlockScreen();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        textView.startAnimation(out);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                out.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        textView.setText(MESSAGE_STARTUP);
                        textView.startAnimation(in);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                textView.startAnimation(out);
            }
        }, 1500);
        /*
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(MESSAGE_STARTUP);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isMenuLocked) return super.onOptionsItemSelected(item);
        switch (STATE) {
            case STATE_NO_IMAGE_IMPORTED:
                switch (item.getItemId()) {
                    case R.id.share:
                        showWarning(WARNING_NO_IMAGE_IMPORTED);
                        return super.onOptionsItemSelected(item);
                    case R.id.take_a_photo:
                        photoFromCamera();
                        return super.onOptionsItemSelected(item);
                    case R.id.import_from_gallery:
                        importFromGallery();
                        return super.onOptionsItemSelected(item);
                    case R.id.save_file:
                        showWarning(WARNING_NO_IMAGE_IMPORTED);
                        return super.onOptionsItemSelected(item);
                    case R.id.erase:
                        showWarning(WARNING_NO_IMAGE_IMPORTED);
                        return super.onOptionsItemSelected(item);
                    /*
                    case R.id.ink:
                        showWarning(WARNING_NO_IMAGE_IMPORTED);
                        return super.onOptionsItemSelected(item);
                        */
                    case R.id.save_location:
                        showSaveLocation();
                        return super.onOptionsItemSelected(item);
                    case R.id.brushColor:
                        showColorPickerDialog();
                        return super.onOptionsItemSelected(item);
                    case R.id.mesh:
                        showWarning(WARNING_NO_IMAGE_IMPORTED);
                        return super.onOptionsItemSelected(item);
                    case R.id.selectModel:
                        showModelSelector();
                        return super.onOptionsItemSelected(item);
                    /*
                    case R.id.about:
                        showAbout();
                        return super.onOptionsItemSelected(item);
                        */
                    //TODO: 11-4-2016 FIX: Hiding Navigation Bar scheme dispose, exit button not required
                    /*
                    case R.id.exit:
                        exit();
                        return super.onOptionsItemSelected(item);
                        */
                    default:
                        return super.onOptionsItemSelected(item);
                }
            case STATE_PROCESSING_IMAGE:
                switch (item.getItemId()) {
                    case R.id.share:
                        share();
                        return super.onOptionsItemSelected(item);
                    case R.id.take_a_photo:
                        photoFromCamera();
                        return super.onOptionsItemSelected(item);
                    case R.id.import_from_gallery:
                        importFromGallery();
                        return super.onOptionsItemSelected(item);
                    case R.id.save_file:
                        saveProcessedPhoto();
                        return super.onOptionsItemSelected(item);
                    case R.id.erase:
                        ipv.reset();
                        //ipv.setMode(ImageProcessingView.MODE_ERASE);
                        return super.onOptionsItemSelected(item);
                    /*
                    case R.id.ink:
                        //ipv.setMode(ImageProcessingView.MODE_INK);
                        textView.setText(WARNING_NO_IMAGE_IMPORTED);
                        return super.onOptionsItemSelected(item);
                        */
                    case R.id.save_location:
                        showSaveLocation();
                        return super.onOptionsItemSelected(item);
                    case R.id.mesh:
                        if (!ipv.SHOW_MESH) {
                            item.setTitle(R.string.hide_mesh);
                            ipv.showMesh(true);
                            PREF_MESH_VALUE = true;
                            preferenceEditor.putBoolean(PREF_MESH, PREF_MESH_VALUE);
                            preferenceEditor.commit();
                        } else {
                            item.setTitle(R.string.show_mesh);
                            ipv.showMesh(false);
                            PREF_MESH_VALUE = false;
                            preferenceEditor.putBoolean(PREF_MESH, PREF_MESH_VALUE);
                            preferenceEditor.commit();
                        }
                        return super.onOptionsItemSelected(item);
                    case R.id.brushColor:
                        showColorPickerDialog();
                        return super.onOptionsItemSelected(item);
                    case R.id.selectModel:
                        showModelSelector();
                        return super.onOptionsItemSelected(item);
                    /*
                    case R.id.about:
                        showAbout();
                        return super.onOptionsItemSelected(item);
                        */
                    //TODO: 11-4-2016 FIX: Hiding Navigation Bar scheme dispose, exit button not required
                    /*
                    case R.id.exit:
                        exit();
                        return super.onOptionsItemSelected(item);
                        */
                    default:
                        return super.onOptionsItemSelected(item);
                }
        }
        return super.onOptionsItemSelected(item);
    }


    private void share() {
        if (isMenuLoaded) {
            ACTION = ACTION_SHARE;
            saveProcessedPhoto();
        }
    }

    private void saveProcessedPhoto() {
        switch (ACTION) {
            case ACTION_SAVE:
                if (isExternalStorageAllowed) {
                    lockScreen();
                    lockMenu();
                    //if (Environment.getExternalStorageState() == Environment.) {
                    //Toast.makeText(getApplicationContext(), "Saving image.", Toast.LENGTH_SHORT).show();
                    ipv.showProgressDialog();
                    ipv.saveRetargetedRawBitmapOnThread(rawBitmap, this);
                } else {
                    unlockMenu();
                    unlockScreen();
                    isExternalStorageAllowed = checkPermission_6_0(this);
                    if (!isExternalStorageAllowed) {
                        Toast.makeText(getApplicationContext(), WARNING_SAVE_IMAGE_FAILED, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveProcessedPhoto();
                }
                break;
            case ACTION_SHARE:
                lockScreen();
                lockMenu();
                ipv.showProgressDialog();
                ipv.saveRetargetedRawBitmapOnThread(rawBitmap, this);
                break;
        }
    }

    private static final String WARNING_SAVE_IMAGE_FAILED = "Failed to save image.\nPlease choose another SD Card.";

    private static boolean checkPermission_6_0(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return true;
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION);
        return false;
    }

    private static boolean isExternalStorageAllowed = true;
    private static int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 7070;
    private static final int ACTION_SHARE = 3;
    private static final int ACTION_SAVE = 4;
    private static int ACTION = ACTION_SAVE;
    private static Uri shareCacheUri = null;

    private void lockMenu() {
        isMenuLocked = true;
    }

    private void unlockMenu() {
        isMenuLocked = false;
    }

    private boolean isMenuLocked = false;


    private void showModelSelector() {
        new MaterialDialog.Builder(this).title("Choose Model").items(R.array.models).itemsCallbackSingleChoice(MuMapConstructor.getModel(), new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                MuMapConstructor.setModel(which);
                // write to preference
                PREF_MODEL_VALUE = which;
                preferenceEditor.putInt(PREF_MODEL, PREF_MODEL_VALUE);
                preferenceEditor.commit();
                return false;
            }
        }).positiveText("confirm").show();

    }

    private void exit() {
        //System.exit(0);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private static final int RequestCode_GALLERY = 1;
    private static final int RequestCode_CAMERA = 2;

    private void importFromGallery() {
        startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                RequestCode_GALLERY);
    }

    private OutputStream out;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCode_CAMERA:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "No image is imported.", Toast.LENGTH_LONG).show();
                    return;
                }
                ipv.setVisibility(View.VISIBLE);
                textView.setVisibility(View.INVISIBLE);

                rawBitmap = getBitmapFromPath();
                rawBitmapWidth = rawBitmap.getWidth();
                rawBitmapHeight = rawBitmap.getHeight();
                if ((float) rawBitmapWidth / rawBitmapHeight > (float) ImageProcessingViewWidth / ImageProcessingViewHeight) {
                    IMAGE_ROTATED_TO_PROCESS = true;
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    rawBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmapWidth, rawBitmapHeight, matrix, true);
                    Toast.makeText(getApplicationContext(), "Processed image will be rotated back when save.", Toast.LENGTH_LONG).show();
                }

                if (rawBitmap == null) {
                    Toast.makeText(getApplicationContext(), "No image is imported.", Toast.LENGTH_LONG).show();
                    break;
                } else {
                    ipv.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.INVISIBLE);
                }
                ipv.setImageBitmapSource(compressImgSize2FitIPV(rawBitmap));
                STATE = STATE_PROCESSING_IMAGE;
                break;
            case RequestCode_GALLERY:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "No image is imported.", Toast.LENGTH_LONG).show();
                    return;
                }
                InputStream stream;
                try {
                    stream = getContentResolver().openInputStream(
                            data.getData());
                    ipv.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.INVISIBLE);

                    // FIXME: first calculate suitable size and then ensure import .png format
                    rawBitmap = BitmapFactory.decodeStream(stream);
                    rawBitmapWidth = rawBitmap.getWidth();
                    rawBitmapHeight = rawBitmap.getHeight();
                    if ((float) rawBitmapWidth / rawBitmapHeight > (ImageProcessingViewWidth - 2 * ipv.EDGE_MAX_MOVE_PADDING) / ImageProcessingViewHeight) {
                        IMAGE_ROTATED_TO_PROCESS = true;
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        rawBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmapWidth, rawBitmapHeight, matrix, true);
                        Toast.makeText(getApplicationContext(), "Image will be rotated back when save.", Toast.LENGTH_SHORT).show();
                    }
                    ipv.setImageBitmapSource(compressImgSize2FitIPV(rawBitmap));
                    STATE = STATE_PROCESSING_IMAGE;
                    stream.close();
                } catch (FileNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "Image is not imported.", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;

            case REQUEST_CODE_SHARE:
                // delete cache
                deleteCache();
                unlockMenu();
                unlockScreen();
                ipv.hideProgressDialog();
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    public void deleteCache() {
        try {
            File dir = getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
        }
    }

    private static final int REQUEST_CODE_SHARE = 9090;

    private void photoFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(getApplicationContext(), "Error in importing camera image.", Toast.LENGTH_LONG).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, RequestCode_CAMERA);
            }
        }
    }

    private final static int SAVE_BITMAP_MAX_PIXEL_SIZE = 2400 * 1600;

    private Bitmap getBitmapFromPath() {
        //BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        //bmOptions.inJustDecodeBounds = false;
        // TODO: UPDATE: Decode bitmap to suitable size to avoid memory issue
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, option);
        double resizeRatio = Math.sqrt((double) SAVE_BITMAP_MAX_PIXEL_SIZE / (option.outWidth * option.outHeight));
        int autoHeight = (int) (resizeRatio * option.outHeight);
        int autoWidth = (int) (resizeRatio * option.outWidth);
        // Calculate inSampleSize
        option.inSampleSize = calculateInSampleSize(option, autoWidth, autoHeight);
        // Decode bitmap with inSampleSize set
        option.inJustDecodeBounds = false;
        return Bitmap.createScaledBitmap(BitmapFactory.decodeFile(mCurrentPhotoPath, option),
                (int) (resizeRatio * option.outWidth), (int) (resizeRatio * option.outHeight), false);
    }

    private String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "TEMP";
        File storageDir = getExternalFilesDir(null);
        File image = new File(storageDir, imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap compressImgSize2FitIPV(Bitmap bitmap) {
        int imageHeight = bitmap.getHeight();
        int imageWidth = bitmap.getWidth();

        // TODO: prevent height or width == 0 even after getting bitmap
        while (ImageProcessingViewHeight == 0) ;
        return Bitmap.createScaledBitmap(bitmap,
                ImageProcessingViewHeight * imageWidth / imageHeight, ImageProcessingViewHeight, true);

    }

    private void showColorPickerDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            colorPickerDialog = new ColorPickerDialog(this, this, PREF_COLOR_VALUE, (int) (ImageProcessingViewHeight * 0.2), (int) (ImageProcessingViewHeight * 0.15),
                    actionBar.getHeight() / 2, R.style.ColorPickerTheme_v21);
        } else {
            colorPickerDialog = new ColorPickerDialog(this, this, PREF_COLOR_VALUE, (int) (ImageProcessingViewHeight * 0.2), (int) (ImageProcessingViewHeight * 0.15),
                    actionBar.getHeight() / 2, R.style.ColorPickerTheme);
        }

        ipv.setInkColor(PREF_COLOR_VALUE);
        //Log.d("debugging", String.valueOf(actionBar.getHeight()));
        colorPickerDialog.show();
    }

    private void showSaveLocation() {
        if (dirChooser == null) {
            dirChooser = DirectoryChooserFragment.newInstance(DirectoryChooserConfig.builder().initialDirectory(PREF_SAVE_LOCATION_VALUE)
                    .newDirectoryName(getApplicationContext().getString(getApplicationContext().getApplicationInfo().labelRes))
                    .build());
            dirChooser.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme_DialogTheme);
        }
        dirChooser.show(getFragmentManager(), null);
    }

    private void showAbout() {
        View view = ((LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popup_about, null);
        if (popupWindowAbout == null) {
            popupWindowAbout = new PopupWindow(view, 750, 260, true);
            popupWindowAbout.setOutsideTouchable(true);
            popupWindowAbout.setAnimationStyle(R.style.AnimationFade);
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    popupWindowAbout.dismiss();
                    return false;
                }
            });
        }
        popupWindowAbout.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    @Override
    public void onSelectDirectory(String location) {
        PREF_SAVE_LOCATION_VALUE = location;
        preferenceEditor.putString(PREF_SAVE_LOCATION, PREF_SAVE_LOCATION_VALUE);
        preferenceEditor.commit();
        dirChooser.dismiss();
    }

    @Override
    public void onCancelChooser() {
        dirChooser.dismiss();
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                          int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    @Override
    public void colorChanged(int color) {
        ipv.setInkColor(color);
        PREF_COLOR_VALUE = color;
        preferenceEditor.putInt(PREF_COLOR, PREF_COLOR_VALUE);
        preferenceEditor.commit();
        colorPickerDialog.dismiss();
    }

    String cacheFileName = "9p845y1034895y9q348hf34hf.jpg";
    private static final String WARNING_NO_APPROPRIATE_INTENT = "No share application found.";
    private static final String MESSAGE_IMAGE_SAVED = "Image is saved.";

    public static File getRemovableStorage() {
        final String value = System.getenv("SECONDARY_STORAGE");
        if (!TextUtils.isEmpty(value)) {
            final String[] paths = value.split(":");
            for (String path : paths) {
                File file = new File(path);
                if (file.isDirectory()) {
                    return file;
                }
            }
        }
        return null;
    }

    private static boolean isOnRemovableSDCARDPath(String location) {
        final File microSD = getRemovableStorage();
        if (microSD != null) {
            if (("/storage" + location).startsWith(microSD.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onProcessedResult(Bitmap bitmap) {
        switch (ACTION) {
            case ACTION_SHARE:
                try {
                    File cacheDirectory = new File(getApplicationContext().getCacheDir().getPath() + "/images/");
                    cacheDirectory.mkdirs();
                    shareCacheUri = Uri.parse(getApplicationContext().getCacheDir().getPath() + "/images/" + cacheFileName);
                    // save to cache
                    out = new FileOutputStream(shareCacheUri.getPath());
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    File file = new File(shareCacheUri.getPath());
                    shareCacheUri = FileProvider.getUriForFile(getApplicationContext(), "tech.silvermind.demo.retargeter.fileprovider", file);

                    // share CacheUri
                    Intent send = ImageAPIParser.parse(this, getPackageManager(), shareCacheUri);
                    if (send == null) {
                        Toast.makeText(getApplicationContext(), WARNING_NO_APPROPRIATE_INTENT, Toast.LENGTH_LONG).show();
                        break;
                    }
                    startActivityForResult(send, REQUEST_CODE_SHARE);

                } catch (Exception e) {
                    unlockMenu();
                    unlockScreen();
                    ipv.hideProgressDialog();
                    e.printStackTrace();
                }
                ACTION = ACTION_SAVE;
                break;
            case ACTION_SAVE:
                //FIXME: UPDATE PENDING: custom saving location　
                try {
                    if (isOnRemovableSDCARDPath(PREF_SAVE_LOCATION_VALUE)) {
                        MediaFile mediaFile = new MediaFile(getContentResolver(), new File(PREF_SAVE_LOCATION_VALUE + "/result_" +
                                new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + ".png"));
                        out = mediaFile.write();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        Toast.makeText(getApplicationContext(), MESSAGE_IMAGE_SAVED, Toast.LENGTH_LONG).show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                unlockMenu();
                                unlockScreen();
                                ipv.hideProgressDialog();
                            }
                        }, 2000);
                        out.close();
                        break;
                    }

                    out = new FileOutputStream(PREF_SAVE_LOCATION_VALUE + "/result_" +
                            new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + ".png");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    Toast.makeText(getApplicationContext(), MESSAGE_IMAGE_SAVED, Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            unlockMenu();
                            unlockScreen();
                            ipv.hideProgressDialog();
                        }
                    }, 2000);
                    out.close();
                } catch (Exception e) {
                    unlockMenu();
                    unlockScreen();
                    ipv.hideProgressDialog();
                    Toast.makeText(getApplicationContext(), WARNING_SAVE_IMAGE_FAILED, Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showSaveLocation();
                        }
                    }, 1000);
                    e.printStackTrace();
                }
                break;
        }


    }

    /*
    private void saveProcessedPhoto() {
        ipv.saveRetargetedRawBitmapOnThread(rawBitmap, this);
    }
    */

    /**
     * Admob
     */
    private static AdView mAdView;
    private static InterstitialAd fullScreenAdView;
    private boolean isFullScreenAdLoaded = false;
    private void loadAds(){
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest1 = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest1);
        fullScreenAdView = new InterstitialAd(this);
        fullScreenAdView.setAdUnitId(getString(R.string.ads_segma_retargeter_1));
        AdRequest adRequest2 = new AdRequest.Builder().build();
        fullScreenAdView.loadAd(adRequest2);
        fullScreenAdView.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
            }
            @Override
            public void onAdLoaded() {
                //showFullScreenAd();
            }
        });
    }
    public static void showFullScreenAd() {
        if (fullScreenAdView.isLoaded()) {
            fullScreenAdView.show();
        }
    }

    /**
     * Activity cycles
     */
    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

}
