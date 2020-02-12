package tech.silvermind.demo.retargeter;

import android.app.ProgressDialog;
import android.content.Context;
//import android.graphics.AvoidXfermode;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;



import java.util.ArrayList;
 
/**
 * Created by edward on 2/24/16.
 */
public class ImageProcessingView extends ImageView implements BitmapMeshGenerator.BitmapMeshResultCallback, BitmapRetargeter.RetargetedBitmapResultCallbacks {

    private ProgressDialog progressDialog;// = new ProgressDialog(getContext(), R.style.ProgressDialogTheme);
    private int screenWidth;
    private int screenHeight;
    private int viewWidth;
    private int viewHeight;
    private int bitmapOutputWidthLeft = 0;
    private int bitmapOutputWidthRight = 0;
    private int bitmapOutputHeightTop = 0;
    private int bitmapOutputHeightDown = 0;
    private int previousBitmapOutputWidthLeft = 0;
    private int previousBitmapOutputWidthRight = 0;

    private float bitmapOutputW2HRatio = 0;

    public void hideProgressDialog(){
        //if (progressDialog.isShowing()) progressDialog.dismiss();
        progressDialog.dismiss();
    }

    public ImageProcessingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getScreenInfo(context);
    }

    public ImageProcessingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getScreenInfo(context);
    }

    public ImageProcessingView(Context context, int maskPaintColor) {
        super(context);
        getScreenInfo(context);
        setupInkColor(maskPaintColor);
    }

    public ImageProcessingView(Context context, Bitmap bitmap) {
        super(context);
        getScreenInfo(context);
        setImageBitmapSource(bitmap);
    }

    public ImageProcessingView(Context context) {
        super(context);
        getScreenInfo(context);
    }

    private void getScreenInfo(Context context) {
        if(!isInEditMode()){
            this.context = context;
            DisplayMetrics metrics = new DisplayMetrics();
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
            screenHeight = metrics.heightPixels;
            screenWidth = metrics.widthPixels;
            progressDialog = new ProgressDialog(context, R.style.ProgressDialogTheme);
        }
    }

    private static Context context;
    private static BitmapMeshGenerator meshGenerator = new BitmapMeshGenerator();
    BitmapMeshGenerator.Mesh mesh;
    private static ConvexHullCreator convexHullCreator;

    public void setImageBitmapSource(Bitmap bitmap) {
        bitmapRetargeter = new BitmapRetargeter(context, this);
        convexHullCreator = new ConvexHullCreator(bitmap.getWidth(), bitmap.getHeight());
        showProgressDialog();
        setClickable(false);
        setImageBitmap(bitmap);
        // TODO: it is better to display before generated mesh
        bitmapSource = bitmap;
        bitmapOutputW2HRatio = ((float) bitmap.getWidth()) / bitmap.getHeight();
        bitmapOutputWidthLeft = Math.round((viewWidth - bitmapOutputW2HRatio * viewHeight) / 2);
        bitmapOutputWidthRight = viewWidth - bitmapOutputWidthLeft - 1;
        previousBitmapOutputWidthLeft = bitmapOutputWidthLeft;
        previousBitmapOutputWidthRight = bitmapOutputWidthRight;
        bitmapOutputHeightTop = 0;
        bitmapOutputHeightDown = viewHeight - 1;
        bitmapMaskSource = Bitmap.createBitmap(bitmapOutputWidthRight - bitmapOutputWidthLeft + 1, viewHeight, Bitmap.Config.ARGB_8888);
        bitmapMeshSource = Bitmap.createBitmap(bitmapOutputWidthRight - bitmapOutputWidthLeft + 1, viewHeight, Bitmap.Config.ARGB_8888);
        bitmapMaskCache = bitmapMaskSource;
        bitmapMeshCache = bitmapMeshSource;
        STATUS = STATUS_COMPUTING;
        meshGenerator.generateMesh(this, bitmapSource);
        //meshDomain = new MeshDomain(bitmapOutputWidthRight - bitmapOutputWidthLeft + 1, viewHeight, bitmapOutputWidthLeft, 0);
        maskCanvas = new Canvas(bitmapMaskSource);
        meshCanvas = new Canvas(bitmapMeshSource);
        inverseMap = new Rect2DMap(bitmapOutputWidthRight - bitmapOutputWidthLeft + 1, viewHeight, bitmapOutputWidthLeft, 0, true);
        movingEdgePath.rewind();
        movingEdgePath.addRect(bitmapOutputWidthLeft, bitmapOutputHeightTop, bitmapOutputWidthRight, bitmapOutputHeightDown, Path.Direction.CW);
        progressDialog.dismiss();
        setClickable(true);
        //invalidate();
    }

    @Override
    public void onBitmapMeshResult(BitmapMeshGenerator.Mesh mesh) {
        verticesMapping = mesh.vertices;
        meshDomain = new MeshDomain(bitmapOutputWidthRight - bitmapOutputWidthLeft + 1, viewHeight);
        this.mesh = mesh;
        meshDomain.setFaceVertex(mesh.vertices, mesh.faces);
        STATUS = STATUS_MOVING;
        setClickable(true);
        progressDialog.dismiss();
        reset();
        drawSelectedFaces(false);
        invalidate();
    }

    private boolean isOnMeasured = false;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (viewWidth != 0 && !isOnMeasured) {
            if (bitmapSource != null) {
                bitmapOutputW2HRatio = ((float) bitmapSource.getWidth()) / bitmapSource.getHeight();
                bitmapOutputWidthLeft = Math.round((viewWidth - bitmapOutputW2HRatio * viewHeight) / 2);
                bitmapOutputWidthRight = viewWidth - bitmapOutputWidthLeft - 1;
                bitmapOutputHeightTop = 0;
                bitmapOutputHeightDown = viewHeight - 1;
            }
            isOnMeasured = true;
        }
    }

    private int TOUCH_PRE_X = 0;
    private int TOUCH_PRE_Y = 0;
    private Path movingEdgePath = new Path();

    private Paint movingEdgePaint = new Paint() {
        {
            setStyle(Paint.Style.STROKE);
            setColor(getResources().getColor(R.color.WhiteSmoke));
            setAlpha(0x70);
            setStrokeJoin(Paint.Join.ROUND);
            setStrokeCap(Paint.Cap.ROUND);
            setStrokeWidth(10);
        }
    };

    private Rect2DMap inverseMap;
    private MeshDomain meshDomain;
    private Bitmap bitmapSource;

    private volatile Bitmap bitmapMaskSource;
    private volatile Bitmap bitmapMaskCache;
    private volatile Bitmap bitmapMeshSource;
    private volatile Bitmap bitmapMeshCache;

    public static boolean SHOW_MESH = true;

    public void showMesh(boolean show) {
        SHOW_MESH = show;
        invalidate();
    }

    private Canvas maskCanvas;
    private Canvas meshCanvas;
    private int MODE = MODE_INK;
    public final static int MODE_ERASE = 1;
    public final static int MODE_INK = 0;
    private Path maskPath = new Path();
    private static Xfermode inkMode = new Xfermode();
    //private static AvoidXfermode inkMode = new AvoidXfermode(0, 0, AvoidXfermode.Mode.TARGET);
    private PorterDuffXfermode clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private final static float paintStrokeWidthInInkMode = 40;
    private final static float paintStrokeWidthInEraseMode = 240;
    private static int inkAlpha = 50;
    private Paint maskPaint = new Paint() {
        {
            //default setting
            setAntiAlias(true);
            setColor(maskPaintColor);
            setAlpha(inkAlpha);
            setStyle(Paint.Style.STROKE);
            setStrokeJoin(Paint.Join.ROUND);
            setStrokeCap(Paint.Cap.ROUND);
            setStrokeWidth(paintStrokeWidthInInkMode);
            setXfermode(inkMode);
        }
    };
    private int maskPaintColor = getResources().getColor(R.color.Orange);

    public void setupInkColor(int color) {
        maskPaint.setColor(color);
        maskPaint.setAlpha(inkAlpha);
    }

    public void setInkColor(int color) {
        if (maskCanvas != null) {
            color = Color.argb(inkAlpha, Color.red(color), Color.green(color), Color.blue(color));
            resetColorOnMaskBitmapSource(color);
            maskPaint.setColor(color);
            if (MODE == MODE_INK) {
                maskPaint.setXfermode(inkMode);
            } else {
                maskPaint.setXfermode(clearMode);
            }
        }
    }

    private void resetColorOnMaskBitmapSource(int color) {
        // TODO: as AvoidXfermode is depreciated, I write a function to replace it
        // TODO: FIXED: Below code enables fast color update
        int[] pixels = new int[bitmapMaskCache.getHeight() * bitmapMaskCache.getWidth()];
        bitmapMaskCache.getPixels(pixels, 0, bitmapMaskCache.getWidth(), 0, 0, bitmapMaskCache.getWidth(), bitmapMaskCache.getHeight());
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] != 0) {// && pixels[i] != meshSelectedColor && pixels[i] != meshUnselectedColor){
                pixels[i] = color;
            }
        }
        bitmapMaskCache.setPixels(pixels, 0, bitmapMaskCache.getWidth(), 0, 0, bitmapMaskCache.getWidth(), bitmapMaskCache.getHeight());
        invalidate();
    }

    private static final float TOUCH_MOVE_TOLERANCE = 5;
    private static final float TOUCH_IMAGE_EDGE_TOLERANCE = 10;
    public static final float EDGE_MAX_MOVE_PADDING = 150;
    public static final float EDGE_MIN_MOVE_PADDING = 250;

    private int ACTION = ACTION_TOUCH_NOTHING;
    private static final int ACTION_TOUCH_IMAGE_CONTENT = 21;
    private static final int ACTION_TOUCH_OUT_OF_IMAGE_CONTENT = 22;
    private static final int ACTION_START_MOVE_IMAGE_EDGE = 11;
    private static final int ACTION_MOVE_IMAGE_EDGE = 12;
    private static final int ACTION_FINISH_MOVE_IMAGE_EDGE = 13;
    private static final int ACTION_TOUCH_OUT_OF_VIEW = 1;
    private static final int ACTION_TOUCH_NOTHING = 0;

    private int[] getRectFromDirty(int x1, int y1, int x2, int y2, int radius) {
        int x_min;
        int y_min;
        int x_max;
        int y_max;
        if (x1 <= x2) {
            x_min = x1;
            x_max = x2;
        } else {
            x_min = x2;
            x_max = x1;
        }
        if (y1 <= y2) {
            y_min = y1;
            y_max = y2;
        } else {
            y_min = y2;
            y_max = y1;
        }
        x_min -= radius;
        x_max += radius;
        y_min -= radius;
        y_max += radius;
        return new int[]{x_min, x_max, y_min, y_max};
    }

    private void TOUCH_DOWN(int x, int y) {
        TOUCH_PRE_X = x;
        TOUCH_PRE_Y = y;
        // as initial touch down action, firstly identify which area user is touching on.
        // Assumed touch sensor coordinate starts (0,0) from left top pixel
        if (y < 0 || y >= viewHeight || x < 0 || x >= viewWidth) {
            ACTION = ACTION_TOUCH_OUT_OF_VIEW;
            return;
        }
        // now, the touch location is in the view, then identify the action is outside of the bitmap or not.
        boolean left = x < bitmapOutputWidthLeft + TOUCH_IMAGE_EDGE_TOLERANCE;
        if (left) SIDE = SIDE_LEFT;
        boolean right = x > bitmapOutputWidthRight - TOUCH_IMAGE_EDGE_TOLERANCE;
        if (right) SIDE = SIDE_RIGHT;
        if (left || right) {
            movingEdgePath.rewind();
            movingEdgePath.addRect(bitmapOutputWidthLeft, bitmapOutputHeightTop, bitmapOutputWidthRight, bitmapOutputHeightDown, Path.Direction.CW);
            // define current action
            ACTION = ACTION_START_MOVE_IMAGE_EDGE;
            invalidate();
        } else {
            ACTION = ACTION_TOUCH_IMAGE_CONTENT;
            //finishAddingObject = true;
            invalidate();
            // touching on the bitmap
            // get the source bitmap coordinate of current touch location
            int[] REF_COORD = inverseMap.getValues(x, y);
            if (MODE == MODE_INK) {
                maskPaint.setXfermode(inkMode);
                maskPaint.setStrokeWidth(paintStrokeWidthInInkMode);

                // select neighbourhood mesh around the touch point.
                meshDomain.selectFaces(REF_COORD[0], REF_COORD[1]);
            } else {
                maskPaint.setXfermode(clearMode);
                maskPaint.setStrokeWidth(paintStrokeWidthInEraseMode);

                // unselect neighbourhood mesh around the touch point.
                meshDomain.unSelectFaces(REF_COORD[0], REF_COORD[1]);
            }
            // mask path
            maskPath.reset();
            maskPath.moveTo(REF_COORD[0], REF_COORD[1]);
            ObjectPointsCounter.prepareNewObject();
            convexHullCreator.addPoint(REF_COORD[0], REF_COORD[1]);
            bitmapRetargeter.drawPartOfBitmapFromFullInverseMapping(bitmapMaskSource, bitmapMaskCache, inverseMap,
                    getRectFromDirty(x, y, x, y, (int) (paintStrokeWidthInEraseMode) / 4));
        }
    }

    private final static int SIDE_LEFT = 1;
    private final static int SIDE_RIGHT = -1;
    private final static int SIDE_NONE = 0;
    private static int SIDE = SIDE_NONE;

    private void TOUCH_MOVE(int x, int y) {
        // TODO: Here is added to ensure every pts can be captured by mesh
        if (ACTION == ACTION_TOUCH_IMAGE_CONTENT) {
            if (x < bitmapOutputWidthLeft || x > bitmapOutputWidthRight || y < bitmapOutputHeightTop || y > bitmapOutputHeightDown) {
                // out of bounds
            } else {
                int[] REF_COORD = inverseMap.getValues(x, y);
                // else still touching the content
                if (MODE == MODE_INK) {
                    // select neighbourhood mesh around the touch point.
                    meshDomain.selectFaces(REF_COORD[0], REF_COORD[1]);
                } else {
                    // unselect neighbourhood mesh around the touch point.
                    meshDomain.unSelectFaces(REF_COORD[0], REF_COORD[1]);
                }
            }
        }

        float dx = Math.abs(x - TOUCH_PRE_X);
        float dy = Math.abs(y - TOUCH_PRE_Y);
        if (dx >= TOUCH_MOVE_TOLERANCE || dy >= TOUCH_MOVE_TOLERANCE) {
            // variables
            int[] REF_COORD;
            int[] PRE_REF_COORD;
            final int PREVIOUS_ACTION = ACTION;
            switch (PREVIOUS_ACTION) {
                case ACTION_START_MOVE_IMAGE_EDGE:
                    ACTION = ACTION_MOVE_IMAGE_EDGE;
                case ACTION_MOVE_IMAGE_EDGE:
                    // on the left side
                    if (x < bitmapOutputWidthLeft + TOUCH_IMAGE_EDGE_TOLERANCE) {
                        if (SIDE != SIDE_LEFT) break;
                        int bitmapOutputWidthLeftTemp = bitmapOutputWidthLeft + x - TOUCH_PRE_X;
                        int bitmapOutputWidthRightTemp = bitmapOutputWidthRight - x + TOUCH_PRE_X;
                        if (bitmapOutputWidthRightTemp < viewWidth - EDGE_MAX_MOVE_PADDING && bitmapOutputWidthRightTemp > (viewWidth + EDGE_MIN_MOVE_PADDING) / 2) {
                            bitmapOutputWidthLeft = bitmapOutputWidthLeftTemp;
                            bitmapOutputWidthRight = bitmapOutputWidthRightTemp;
                        }
                    } else {
                        if (SIDE != SIDE_RIGHT) break;
                        // on the right side
                        int bitmapOutputWidthLeftTemp = bitmapOutputWidthLeft - x + TOUCH_PRE_X;
                        int bitmapOutputWidthRightTemp = bitmapOutputWidthRight + x - TOUCH_PRE_X;
                        if (bitmapOutputWidthRightTemp <= viewWidth - EDGE_MAX_MOVE_PADDING && bitmapOutputWidthRightTemp >= (viewWidth + EDGE_MIN_MOVE_PADDING) / 2) {
                            bitmapOutputWidthLeft = bitmapOutputWidthLeftTemp;
                            bitmapOutputWidthRight = bitmapOutputWidthRightTemp;
                        }
                    }
                    movingEdgePath.rewind();
                    movingEdgePath.addRect(bitmapOutputWidthLeft, bitmapOutputHeightTop, bitmapOutputWidthRight, bitmapOutputHeightDown, Path.Direction.CW);
                    invalidate();
                    break;
                case ACTION_TOUCH_IMAGE_CONTENT:
                    // check if the touch location is out of view
                    if (y < 0 || y >= viewHeight || x < 0 || x >= viewWidth) {
                        ACTION = ACTION_TOUCH_OUT_OF_VIEW;
                        break; // do nothing
                    }
                    // check if out of content
                    if (x < bitmapOutputWidthLeft + TOUCH_IMAGE_EDGE_TOLERANCE || x > bitmapOutputWidthRight - TOUCH_IMAGE_EDGE_TOLERANCE) {
                        ACTION = ACTION_TOUCH_OUT_OF_IMAGE_CONTENT;
                        break; // do nothing
                    }
                    // else still touching the content
                    REF_COORD = inverseMap.getValues(x, y);
                    PRE_REF_COORD = inverseMap.getValues(TOUCH_PRE_X, TOUCH_PRE_Y);
                    // mask path
                    if (MODE == MODE_INK) {
                        maskPaint.setXfermode(inkMode);
                        // select neighbourhood mesh around the touch point.
                        meshDomain.selectFaces(REF_COORD[0], REF_COORD[1]);
                    } else {
                        maskPaint.setXfermode(clearMode);
                        // unselect neighbourhood mesh around the touch point.
                        meshDomain.unSelectFaces(REF_COORD[0], REF_COORD[1]);
                    }
                    convexHullCreator.addPoint(REF_COORD[0], REF_COORD[1]);
                    maskPath.quadTo(PRE_REF_COORD[0], PRE_REF_COORD[1], (REF_COORD[0] + PRE_REF_COORD[0]) / 2, (REF_COORD[1] + PRE_REF_COORD[1]) / 2);
                    maskCanvas.drawPath(maskPath, maskPaint);
                    bitmapRetargeter.drawPartOfBitmapFromFullInverseMapping(bitmapMaskSource, bitmapMaskCache, inverseMap,
                            getRectFromDirty(TOUCH_PRE_X, TOUCH_PRE_Y, x, y, (int) (paintStrokeWidthInEraseMode / 4)));
                    break;
                case ACTION_TOUCH_OUT_OF_IMAGE_CONTENT:
                    // check if the touch location is back to the view
                    if (y < 0 || y >= viewHeight || x < 0 || x >= viewWidth) {
                        // out of view
                        ACTION = ACTION_TOUCH_OUT_OF_VIEW;
                        break; // do nothing
                    }
                    // check if still out of content
                    if (x < bitmapOutputWidthLeft + TOUCH_IMAGE_EDGE_TOLERANCE || x > bitmapOutputWidthRight - TOUCH_IMAGE_EDGE_TOLERANCE) {
                        ACTION = ACTION_TOUCH_OUT_OF_IMAGE_CONTENT;
                        break; // do nothing
                    } else {
                        // back into the content
                        ACTION = ACTION_TOUCH_IMAGE_CONTENT;
                        REF_COORD = inverseMap.getValues(x, y);
                        // mask path
                        maskPath.reset();
                        maskPath.moveTo(REF_COORD[0], REF_COORD[1]);
                        break;
                    }
                case ACTION_TOUCH_OUT_OF_VIEW:
                    // check if the touch location is in the view
                    if (y < 0 || y >= viewHeight || x < 0 || x >= viewWidth) {
                        // still out of view
                        break; // do nothing
                    }
                    // check if still out of content
                    if (x < bitmapOutputWidthLeft + TOUCH_IMAGE_EDGE_TOLERANCE || x > bitmapOutputWidthRight - TOUCH_IMAGE_EDGE_TOLERANCE) {
                        ACTION = ACTION_TOUCH_OUT_OF_IMAGE_CONTENT;
                        break; // do nothing
                    } else {
                        // back into the content
                        ACTION = ACTION_TOUCH_IMAGE_CONTENT;
                        REF_COORD = inverseMap.getValues(x, y);
                        // mask path
                        maskPath.reset();
                        maskPath.moveTo(REF_COORD[0], REF_COORD[1]);
                        break;
                    }
                default:
                    break;
            }
            TOUCH_PRE_X = x;
            TOUCH_PRE_Y = y;
        }
    }

    private BitmapRetargeter bitmapRetargeter;

    private void TOUCH_UP(int x, int y) {
        //finishAddingObject = true;
        int PREVIOUS_ACTION = ACTION;
        if (PREVIOUS_ACTION == ACTION_MOVE_IMAGE_EDGE || PREVIOUS_ACTION == ACTION_START_MOVE_IMAGE_EDGE) {
            ACTION = ACTION_FINISH_MOVE_IMAGE_EDGE; // for action passing to onDraw
            SIDE = SIDE_NONE;
            STATUS = STATUS_COMPUTING;
            MainActivity.showFullScreenAd();
            showProgressDialog();
            drawSelectedFaces(true);
            // create new retargeted bitmap and retargeted static mask cache from bitmap source and mask

            float[][] mu = MuMapConstructor.constructMuOnFace(mesh.faces, mesh.vertices, meshDomain.getFaceSelectedBoolean(),
                    ObjectPointsCounter.getObjectSet(), bitmapSource.getWidth(), bitmapSource.getHeight(), bitmapOutputWidthRight - bitmapOutputWidthLeft + 1);
            bitmapRetargeter.retargetBitmap(new Bitmap[]{bitmapSource, bitmapMaskSource, bitmapMeshSource},
                    mesh, mu, new float[]{bitmapOutputWidthLeft, bitmapOutputWidthRight,
                            bitmapOutputHeightTop, bitmapOutputHeightDown});
            return;
        }
        //maskPath.reset();
        //movingEdgePath.rewind();
        if (PREVIOUS_ACTION == ACTION_TOUCH_IMAGE_CONTENT) {
            // TODO: get static object convex hull;
            setClickable(false);
            showProgressDialog();
            ArrayList<Point> pointSet = new ArrayList<>();
            ArrayList<Integer> pointIndexSet = new ArrayList<>();
            convexHullCreator.getPointIndexInConvexHull(mesh.vertices, pointIndexSet, pointSet);
            meshDomain.selectFaces(pointIndexSet);
            ObjectPointsCounter.addPointSet(pointSet);
            convexHullCreator.reset();
            drawSelectedFaces(true);
            progressDialog.dismiss();
            setClickable(true);
            bitmapRetargeter.makeBitmapFromFullInverseMapping(new Bitmap[]{bitmapMeshSource}, inverseMap.getFull2DMapStartFromZero());
            ACTION = ACTION_TOUCH_NOTHING;
        }
        invalidate();
    }

    /*
    private float[][] constructMuFromSelectedFace() {
        boolean[] faceSelectedBoolean = meshDomain.getFaceSelectedBoolean();
        float[][] mu = new float[mesh.faces.length][2];
        float ratio = (float) (bitmapOutputWidthRight - bitmapOutputWidthLeft + 1) /
                (previousBitmapOutputWidthRight - previousBitmapOutputWidthLeft + 1);
        float bkgdMu = (ratio - 1) / (ratio + 1);
        for (int i = 0; i < mesh.faces.length; i++) {
            if (!faceSelectedBoolean[i]) {
                mu[i][0] = bkgdMu;
                mu[i][1] = 0;
            }
        }
        return mu;
    }
    */

    public void reset() {
        //MODE_ERASE
        maskCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        meshCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        drawSelectedFaces(false);
        bitmapRetargeter.makeBitmapFromFullInverseMapping(new Bitmap[]{bitmapMaskSource, bitmapMeshSource}, inverseMap.getFull2DMapStartFromZero());
        ObjectPointsCounter.reset();
        meshDomain.unSelectAllFaces();
        invalidate();
    }

    public void setMode(int MODE) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(movingEdgePath, movingEdgePaint); // draw border for anytime
        drawStaticMaskOnUiThread(canvas);
        /*
        Log.d("debugging",String.valueOf(ACTION));
        private int ACTION = ACTION_TOUCH_NOTHING;
        private static final int ACTION_TOUCH_IMAGE_CONTENT = 21;
        private static final int ACTION_TOUCH_OUT_OF_IMAGE_CONTENT = 22;
        private static final int ACTION_START_MOVE_IMAGE_EDGE = 11;
        private static final int ACTION_MOVE_IMAGE_EDGE = 12;
        private static final int ACTION_FINISH_MOVE_IMAGE_EDGE = 13;
        private static final int ACTION_TOUCH_OUT_OF_VIEW = 1;
        private static final int ACTION_TOUCH_NOTHING = 0;
        */
    }

    //TODO: debug paint
    private int meshUnselectedColor = Color.GREEN;
    private int meshSelectedColor = Color.RED;
    private int debugPaintWidth = 2;
    private Paint drawMeshPaint = new Paint() {{
        setStrokeWidth(debugPaintWidth);
        setColor(meshUnselectedColor);
    }};

    private final static int STATUS_COMPUTING = 1;
    private final static int STATUS_MOVING = 2;
    private final static int STATUS_SAVING_IMAGE = 3;
    private static int STATUS = STATUS_MOVING;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (STATUS == STATUS_COMPUTING) return false;
        int x = Math.round(event.getX());
        int y = Math.round(event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                TOUCH_DOWN(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                TOUCH_MOVE(x, y);
                break;
            case MotionEvent.ACTION_UP:
                TOUCH_UP(x, y);
                break;
        }
        //drawSelectedFaces(false);
        invalidate();
        return true;
    }
    private void drawSelectedFaces(boolean allFace) {
        //if (!SHOW_MESH) return;
        if (mesh != null) {
            for (int[] f : mesh.faces) {
                drawMeshPaint.setColor(meshUnselectedColor);
                meshCanvas.drawLines(new float[]{mesh.vertices[f[0]][0], mesh.vertices[f[0]][1],
                        mesh.vertices[f[1]][0], mesh.vertices[f[1]][1]}, drawMeshPaint);
                meshCanvas.drawLines(new float[]{mesh.vertices[f[1]][0], mesh.vertices[f[1]][1],
                        mesh.vertices[f[2]][0], mesh.vertices[f[2]][1]}, drawMeshPaint);
                meshCanvas.drawLines(new float[]{mesh.vertices[f[0]][0], mesh.vertices[f[0]][1],
                        mesh.vertices[f[2]][0], mesh.vertices[f[2]][1]}, drawMeshPaint);
            }
            if (allFace) {
                for (int i = 0; i < mesh.faces.length; i++) {
                    int[] f = mesh.faces[i];
                    if (meshDomain.isSelected(i)) {
                        drawMeshPaint.setColor(meshSelectedColor);
                        meshCanvas.drawLines(new float[]{mesh.vertices[f[0]][0], mesh.vertices[f[0]][1],
                                mesh.vertices[f[1]][0], mesh.vertices[f[1]][1]}, drawMeshPaint);
                        meshCanvas.drawLines(new float[]{mesh.vertices[f[1]][0], mesh.vertices[f[1]][1],
                                mesh.vertices[f[2]][0], mesh.vertices[f[2]][1]}, drawMeshPaint);
                        meshCanvas.drawLines(new float[]{mesh.vertices[f[0]][0], mesh.vertices[f[0]][1],
                                mesh.vertices[f[2]][0], mesh.vertices[f[2]][1]}, drawMeshPaint);
                    }
                }
            }
        }
    }

    private void drawStaticMaskOnUiThread(Canvas canvas) {
        //setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        if (bitmapMaskCache != null)
            canvas.drawBitmap(bitmapMaskCache, previousBitmapOutputWidthLeft, 0, null);
        if (SHOW_MESH && bitmapMeshCache != null)
            canvas.drawBitmap(bitmapMeshCache, previousBitmapOutputWidthLeft, 0, null);
    }


    public void showProgressDialog() {
        // TODO: 10-4-2016 FIX: start dialog in UI Thread and hiding Navigation Bar scheme disposed
        //setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
        }
    }

    //TODO: Image Processing Callbacks - start
    static int[][] verticesMapping;

    @Override
    public void onRetargetedBitmapResult(Bitmap[] retargetedBitmaps, int[][][] fullInverseMap, int[][] verticesMapping) {
        // return new retargeted bitmap and retargeted static mask cache from bitmap source and mask
        setImageBitmap(retargetedBitmaps[0]);
        bitmapMaskCache = retargetedBitmaps[1];
        bitmapMeshCache = retargetedBitmaps[2];
        this.verticesMapping = verticesMapping;
        inverseMap.renew(new int[]{bitmapOutputWidthLeft, 0}, fullInverseMap);
        previousBitmapOutputWidthLeft = bitmapOutputWidthLeft;
        previousBitmapOutputWidthRight = bitmapOutputWidthRight;
        STATUS = STATUS_MOVING;
        progressDialog.dismiss();
        invalidate();
    }

    @Override
    public void onInterpolatedBitmapResult(Bitmap[] retargetedBitmap, int[][][] fullInverseMap) {
        switch (STATUS) {
            case STATUS_SAVING_IMAGE:
                saveRetargetedRawBitmapCallback.onProcessedResult(retargetedBitmap[0]);
                progressDialog.dismiss();
                break;
            default:
                break;
        }
        STATUS = STATUS_MOVING;
    }

    @Override
    public void onFullInverseMappingBitmapResult(Bitmap[] bitmaps) {
        if (bitmaps.length == 1) {
            bitmapMeshCache = bitmaps[0];
            invalidate();
            return;
        }
        bitmapMaskCache = bitmaps[0];
        bitmapMeshCache = bitmaps[1];
        invalidate();
    }

    //private static boolean finishAddingObject = true;
    @Override
    public void onPartInverseMappingBitmapResult() {
        //drawSelectedFaces(true);
        invalidate();
    }
    //TODO: Image Processing Callbacks - end

    public interface SaveRetargetedRawBitmapCallback {
        void onProcessedResult(Bitmap bitmap);
    }

    private SaveRetargetedRawBitmapCallback saveRetargetedRawBitmapCallback;

    private final static int SAVE_BITMAP_MAX_PIXEL_SIZE = 2400 * 1600;

    public void saveRetargetedRawBitmapOnThread(Bitmap bitmap, SaveRetargetedRawBitmapCallback callback) {

        /*
        // TODO: UPDATE: Resize bitmap to avoid memory issue
        double resizeRatio = Math.sqrt((double)SAVE_BITMAP_MAX_PIXEL_SIZE/(bitmap.getWidth()*bitmap.getHeight()));
        if (resizeRatio < 1){
            bitmap = Bitmap.createScaledBitmap(bitmap, (int)(resizeRatio*bitmap.getWidth()), (int)(resizeRatio*bitmap.getHeight()), false);
        }
        */

        saveRetargetedRawBitmapCallback = callback;
        showProgressDialog();
        STATUS = STATUS_SAVING_IMAGE;
        int rawBitmapHeight = bitmap.getHeight();
        int scaledBitmapWidth = previousBitmapOutputWidthRight - previousBitmapOutputWidthLeft + 1;
        float scaleRatio = (float) rawBitmapHeight / bitmapSource.getHeight();
        int rawRetargetedWidth = Math.round(scaleRatio * scaledBitmapWidth);
        // change verticesMapping ratio back to raw one
        int[][] rawVerticesMapping = new int[verticesMapping.length][2];
        for (int i = 0; i < verticesMapping.length; i++) {
            rawVerticesMapping[i][0] = Math.round(scaleRatio * verticesMapping[i][0]);
            rawVerticesMapping[i][1] = Math.round(scaleRatio * verticesMapping[i][1]);
        }
        int[][] rawVertices = new int[mesh.vertices.length][2];
        for (int i = 0; i < rawVertices.length; i++) {
            rawVertices[i][0] = Math.round(scaleRatio * mesh.vertices[i][0]);
            rawVertices[i][1] = Math.round(scaleRatio * mesh.vertices[i][1]);
        }

        bitmapRetargeter.interpolateFromRetargetedMapping(new Bitmap[]{bitmap}, mesh.faces, rawVerticesMapping, rawVertices,
                rawRetargetedWidth - 1, rawBitmapHeight - 1);
    }
}
