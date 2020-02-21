package tech.silvermind.demo.retargeter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;

import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;

import tech.silvermind.demo.retargter.ScriptC_interpolation;

/**
 * Created by edward on 4/10/16.
 */
public class BitmapRetargeter {
    static {
        System.loadLibrary("retargeter");
        System.loadLibrary("opencv_java3");
    }

    public BitmapRetargeter(Context context, RetargetedBitmapResultCallbacks callbacks) {
        BitmapRetargeter.callbacks = callbacks;
        createRenderScript(context);
    }

    private void createRenderScript(Context context) {
        renderScript = RenderScript.create(context);
        interpolationScript = new ScriptC_interpolation(renderScript);
    }

    private static RetargetedBitmapResultCallbacks callbacks;

    private native static int[][] solveRetargeting(int[][] faces, float[][] vertices, float[][] mu, float[] bounds);
    private native static int[][][] interpolateImage(int[][] faces, int[][] verticesDomain, int[][] verticesMapping, int maxBoundXOnDomain, int maxBoundYOnDomain);

    public interface RetargetedBitmapResultCallbacks {
        void onRetargetedBitmapResult(Bitmap[] retargetedBitmap, int[][][] fullInverseMap, int[][] verticesMapping);

        void onInterpolatedBitmapResult(Bitmap[] retargetedBitmap, int[][][] fullInverseMap);

        // TODO: Depreciated. Slow method to update mesh.
        void onFullInverseMappingBitmapResult(Bitmap[] bitmaps);

        // TODO: assume the output is always referenced to bitmap input; This method allows efficient update of mesh
        void onPartInverseMappingBitmapResult();
    }

    public void retargetBitmap(Bitmap[] bitmaps, BitmapMeshGenerator.Mesh mesh, float[][] mu, float[] bounds) {
        float[][] vertices = new float[mesh.vertices.length][2];
        for (int i = 0; i < mesh.vertices.length; i++) {
            vertices[i][0] = mesh.vertices[i][0];
            vertices[i][1] = mesh.vertices[i][1];
        }
        new RetargetBitmapOnThread().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                new Object[]{mesh.faces, vertices, mu, bounds, bitmaps});
    }

    public void interpolateFromRetargetedMapping(Bitmap[] bitmaps, int[][] faces, int[][] domainVertices, int[][] mappingVertices, int maxBoundXnDomain, int maxBoundYOnDomain) {
        new InterpolateBitmapOnThread().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                new Object[]{faces, domainVertices, mappingVertices, maxBoundXnDomain, maxBoundYOnDomain, bitmaps});
    }

    // TODO: Depreciated. Slow method to update mesh.
    public void makeBitmapFromFullInverseMapping(Bitmap[] bitmaps, int[][][] inverseMapping) {
        //if (isOnThreadMakingBitmap) return;
        new MakeBitmapFromFullInverseMappingOnThread().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                new Object[]{bitmaps, inverseMapping});
    }

    // TODO: assume the output is always referenced to bitmap input; This method allows efficient update of mesh
    synchronized public void drawPartOfBitmapFromFullInverseMapping(final Bitmap source, final Bitmap output, final tech.silvermind.demo.retargeter.Rect2DMap inverseMapping, final int[] dirty) {
        //if (isOnThreadMakingBitmap) return;
        //new DrawPartOfBitmapFromFullInverseMappingOnThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Object[]{source, output, inverseMapping, dirty});
        new DrawPartOfBitmapFromFullInverseMappingOnThread().execute(new Object[]{source, output, inverseMapping, dirty});
    }

    //private static boolean isOnThreadMakingBitmap = false;

    private static RenderScript renderScript; // renderscript tools class
    private static Allocation bitmapSourceAllocation;
    private static Allocation bitmapOutputAllocation;
    private static ScriptC_interpolation interpolationScript;

    private static class RetargetBitmapOnThread extends AsyncTask<Object[], Void, Object[]> {
        @Override
        protected Object[] doInBackground(Object[]... params) {
            float[][] v = (float[][]) params[0][1];
            float[] bds = (float[]) params[0][3];
            int[][] f = (int[][]) params[0][0];
            float[][] mu = (float[][]) params[0][2];
            Bitmap[] bitmaps = (Bitmap[]) params[0][4];
            // FIXME: Debug: write files to check data validity - start

            FileOutputStream outputStream;
            String filename;
            File file;
            // v
            filename = "v.m";
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);

            try {
                outputStream = new FileOutputStream(file, false);
                outputStream.write("v = [ ...\n".getBytes());
                for (int i = 0; i < v.length; i++) {
                    outputStream.write((String.valueOf(v[i][0]) + ", " + String.valueOf(v[i][1]) + ";\n").getBytes());
                }
                outputStream.write("];".getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // mu
            filename = "mu.m";
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            try {
                outputStream = new FileOutputStream(file, false);
                outputStream.write("mu = [ ...\n".getBytes());
                for (int i = 0; i < mu.length; i++) {
                    outputStream.write((String.valueOf(mu[i][0]) + ", " + String.valueOf(mu[i][1]) + ";\n").getBytes());
                }
                outputStream.write("];".getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // bds
            filename = "bd.m";
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            try {
                outputStream = new FileOutputStream(file, false);
                outputStream.write("bds = [ ".getBytes());
                outputStream.write((String.valueOf(bds[0]) + ",").getBytes());
                outputStream.write((String.valueOf(bds[1]) + ",").getBytes());
                outputStream.write((String.valueOf(bds[2]) + ",").getBytes());
                outputStream.write((String.valueOf(bds[3])).getBytes());
                outputStream.write("];".getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // face
            filename = "f.m";
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            try {
                outputStream = new FileOutputStream(file, false);
                outputStream.write("f = [ ...\n".getBytes());
                for (int i = 0; i < f.length; i++) {
                    outputStream.write((String.valueOf(f[i][0]) + ", " + String.valueOf(f[i][1]) + ", " + String.valueOf(f[i][2]) + ";\n").getBytes());
                }
                outputStream.write("];".getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


            // FIXME: Debug: write files to check data validity - end

            // FIXME: solve retargeting by lbs written by Eigen which has bugs - different solutions in different os

            long time = System.currentTimeMillis();
            int[][] verticesMapping = solveRetargeting(
                    f, // face
                    v, // vertices
                    mu, // mu
                    bds // bounds
            );
            Log.d("debugging", "Retargeting time: " + String.valueOf((float) (System.currentTimeMillis() - time) / 1000) + "s");

            // FIXME: Debug: write files to check data validity - start

            // v
            filename = "map.m";
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);

            try {
                outputStream = new FileOutputStream(file, false);
                outputStream.write("map = [ ...\n".getBytes());
                for (int i = 0; i < verticesMapping.length; i++) {
                    outputStream.write((String.valueOf(verticesMapping[i][0]) + ", " + String.valueOf(verticesMapping[i][1]) + ";\n").getBytes());
                }
                outputStream.write("];".getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // FIXME: Debug: write files to check data validity - end

            /*
            // use fixed-y scheme
            for (int i = 0;i<verticesMapping.length;i++){
                verticesMapping[i][1] = (int) v[i][1];
            }
            */
            // interpolate map
            int[][] verticesDomain = new int[v.length][2];
            for (int i = 0; i < v.length; i++) {
                verticesDomain[i][0] = (int) v[i][0];
                verticesDomain[i][1] = (int) v[i][1];
            }

            time = System.currentTimeMillis();
            int[][][] inverse2dInterpolationMapping = interpolateImage(
                    (int[][]) params[0][0], // face
                    verticesMapping, // verticesDomain
                    verticesDomain, // verticesMapping
                    (int) (bds[1] - bds[0]), // maBoundXOnDomain
                    (int) (bds[3] - bds[2])  // maBoundYOnDomain
            );
            Log.d("debugging", "Making inverse map time: " + String.valueOf((float) (System.currentTimeMillis() - time) / 1000) + "s");

            time = System.currentTimeMillis();

            /*
            // TODO: DEPRECIATED: OpenCV interpolation
            Bitmap[] interpolatedBitmaps = new Bitmap[bitmaps.length];

            Mat[] movingMats = new Mat[2];
            Mat[] targetMats = new Mat[2];

            for (int i = 0; i < bitmaps.length; i++) {
                //int width = (int) (bds[1] - bds[0] + 1);
                //int height = (int) (bds[3] - bds[2] + 1);
                int width = inverse2dInterpolationMapping[0].length;
                int height = inverse2dInterpolationMapping.length;
                interpolatedBitmaps[i] = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                movingMats[i] = new Mat(bitmaps[0].getHeight(), bitmaps[0].getWidth(), CvType.CV_8UC4);
                targetMats[i] = new Mat(height, width, CvType.CV_8UC4);
                Utils.bitmapToMat(bitmaps[i], movingMats[i]);
            }

            for (int y = 0; y < inverse2dInterpolationMapping.length; y += 1) {
                for (int x = 0; x < inverse2dInterpolationMapping[0].length; x += 1) {
                    for (int b = 0; b < bitmaps.length; b++) {
                        double[] color = movingMats[b].get(inverse2dInterpolationMapping[y][x][1], inverse2dInterpolationMapping[y][x][0]);
                        targetMats[b].put(y, x, color);
                    }
                }
            }

            for (int i = 0;i < bitmaps.length; i++){
                Utils.matToBitmap(targetMats[i], interpolatedBitmaps[i]);
            }
            */
            // TODO: DEPRECIATED: OpenCV Fast interpolation
            /*
            Bitmap[] interpolatedBitmaps = new Bitmap[bitmaps.length];

            Mat[] movingMats = new Mat[2];
            Mat[] targetMats = new Mat[2];
            for (int i = 0; i < bitmaps.length; i++) {
                int width = inverse2dInterpolationMapping[0].length;
                int height = inverse2dInterpolationMapping.length;
                interpolatedBitmaps[i] = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                movingMats[i] = new Mat(bitmaps[0].getHeight(), bitmaps[0].getWidth(), CvType.CV_8UC4);
                targetMats[i] = new Mat(height, width, CvType.CV_8UC4);
                Utils.bitmapToMat(bitmaps[i], movingMats[i]);
            }
            int frequency = 5;
            for (int y = 0; y < inverse2dInterpolationMapping.length; y += frequency) {
                for (int x = 0; x < inverse2dInterpolationMapping[0].length; x += frequency) {
                    for (int b = 0; b < bitmaps.length; b++) {
                        double[] color = movingMats[b].get(inverse2dInterpolationMapping[y][x][1], inverse2dInterpolationMapping[y][x][0]);
                        if (x + frequency - 1 > inverse2dInterpolationMapping[0].length || y + frequency - 1 > inverse2dInterpolationMapping.length) {
                            for (int i = 0; x + i < inverse2dInterpolationMapping[0].length; i++) {
                                for (int j = 0; y + j < inverse2dInterpolationMapping.length; j++) {
                                    targetMats[b].put(y + j, x + i, color);
                                }
                            }
                        }
                        for (int i = 0; i < frequency; i++) {
                            for (int j = 0; j < frequency; j++) {
                                targetMats[b].put(y + j, x + i, color);
                            }
                        }
                    }
                }
            }
            for (int b = 0;b < bitmaps.length; b++){
                Utils.matToBitmap(targetMats[b], interpolatedBitmaps[b]);
            }
            */
            // TODO: UPDATE: 20160506: SuperFast Renderscript interpolation
            int domainWidth = inverse2dInterpolationMapping[0].length;
            int domainHeight = inverse2dInterpolationMapping.length;
            //Log.d("debugging", String.valueOf(bds[1] - bds[0] + 1) +" "+ String.valueOf(bds[3] - bds[2] + 1));
            //Log.d("debugging", String.valueOf(domainWidth) +" "+ String.valueOf(domainHeight));

            int[] inverseMapping = new int[domainHeight * domainWidth];
            int[] domainIndex = new int[domainHeight * domainWidth];
            for (int y = 0; y < domainHeight; y++) {
                for (int x = 0; x < domainWidth; x++) {
                    inverseMapping[domainWidth * y + x] = bitmaps[0].getWidth() * inverse2dInterpolationMapping[y][x][1] + inverse2dInterpolationMapping[y][x][0];
                    //inverseMapping[domainWidth * y + x] = (int)(bitmaps[0].getWidth()*y + x*(float)bitmaps[0].getWidth()/domainWidth);
                    domainIndex[domainWidth * y + x] = domainWidth * y + x;
                }
            }
            Allocation inverseMappingAllocation = Allocation.createSized(renderScript,
                    Element.I32(renderScript), domainHeight * domainWidth, Allocation.USAGE_SCRIPT);
            Allocation domainIndexAllocation = Allocation.createSized(renderScript,
                    Element.I32(renderScript), domainHeight * domainWidth, Allocation.USAGE_SCRIPT);
            inverseMappingAllocation.copyFrom(inverseMapping);
            domainIndexAllocation.copyFrom(domainIndex);
            interpolationScript.set_mapIndexes(inverseMappingAllocation);
            interpolationScript.set_domainIndexes(domainIndexAllocation);
            interpolationScript.set_sourceWidth(bitmaps[0].getWidth());
            interpolationScript.set_sourceHeight(bitmaps[0].getHeight());
            interpolationScript.set_targetWidth(domainWidth);
            interpolationScript.set_targetHeight(domainHeight);
            Bitmap[] interpolatedBitmaps = new Bitmap[bitmaps.length];
            for (int b = 0; b < bitmaps.length; b++) {
                interpolatedBitmaps[b] = Bitmap.createBitmap(domainWidth, domainHeight, Bitmap.Config.ARGB_8888);
                bitmapSourceAllocation = Allocation.createFromBitmap(renderScript, bitmaps[b],
                        Allocation.MipmapControl.MIPMAP_NONE,
                        Allocation.USAGE_SCRIPT);
                bitmapOutputAllocation = Allocation.createFromBitmap(renderScript, interpolatedBitmaps[b],
                        Allocation.MipmapControl.MIPMAP_NONE,
                        Allocation.USAGE_SCRIPT);
                interpolationScript.set_bitmapSource(bitmapSourceAllocation);
                interpolationScript.set_bitmapOutput(bitmapOutputAllocation);
                interpolationScript.set_script(interpolationScript);
                interpolationScript.invoke_filter();
                bitmapOutputAllocation.copyTo(interpolatedBitmaps[b]);
            }

            Log.d("debugging", "Interpolation time: " + String.valueOf((float) (System.currentTimeMillis() - time) / 1000) + "s");
            return new Object[]{interpolatedBitmaps, inverse2dInterpolationMapping, verticesMapping};
        }

        @Override
        protected void onPostExecute(Object[] objects) {
            callbacks.onRetargetedBitmapResult((Bitmap[]) objects[0], (int[][][]) objects[1], (int[][]) objects[2]);
        }
    }


    private static class InterpolateBitmapOnThread extends AsyncTask<Object[], Void, Object[]> {
        @Override
        protected Object[] doInBackground(Object[]... params) {
            Bitmap[] bitmaps = (Bitmap[]) params[0][5];

            int[][][] inverse2dInterpolationMapping = interpolateImage(
                    (int[][]) params[0][0], // face
                    (int[][]) params[0][1], // domainVertices
                    (int[][]) params[0][2], // mappingVertices
                    (int) params[0][3], // maBoundXOnDomain
                    (int) params[0][4] // maBoundYOnDomain
            );

            /*
            // interpolate image
            Bitmap[] bitmaps = (Bitmap[]) params[0][5];
            Bitmap[] interpolatedBitmaps = new Bitmap[bitmaps.length];
            for (int i = 0; i < bitmaps.length; i++) {
                Bitmap bitmap = bitmaps[i];
                Mat movingMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC3);
                Utils.bitmapToMat(bitmap, movingMat);
                Mat targetMat = new Mat((int) (((float[]) params[0][3])[3] - ((float[]) params[0][3])[2]) + 1,
                        (int) (((float[]) params[0][3])[1] - ((float[]) params[0][3])[0]) + 1, CvType.CV_8UC3);

                for (int y = 0; y < inverse2dInterpolationMapping.length; y++) {
                    for (int x = 0; x < inverse2dInterpolationMapping[0].length; x++) {
                        targetMat.put(y, x,
                                movingMat.get(inverse2dInterpolationMapping[y][x][1], inverse2dInterpolationMapping[y][x][0])
                        );
                    }
                }
                interpolatedBitmaps[i] = Bitmap.createBitmap(
                        (int) params[0][3] + 1,
                        (int) params[0][4] + 1,
                        Bitmap.Config.ARGB_8888
                );
                Utils.matToBitmap(targetMat, interpolatedBitmaps[i]);
            }
            */

            // TODO: UPDATE: 20160506: SuperFast Renderscript interpolation
            Bitmap[] interpolatedBitmaps = new Bitmap[bitmaps.length];
            int domainWidth = inverse2dInterpolationMapping[0].length;
            int domainHeight = inverse2dInterpolationMapping.length;
            //Log.d("debugging", String.valueOf(bds[1] - bds[0] + 1) +" "+ String.valueOf(bds[3] - bds[2] + 1));
            //Log.d("debugging", String.valueOf(domainWidth) +" "+ String.valueOf(domainHeight));

            int[] inverseMapping = new int[domainHeight * domainWidth];
            int[] domainIndex = new int[domainHeight * domainWidth];
            for (int y = 0; y < domainHeight; y++) {
                for (int x = 0; x < domainWidth; x++) {
                    inverseMapping[domainWidth * y + x] = bitmaps[0].getWidth() * inverse2dInterpolationMapping[y][x][1] + inverse2dInterpolationMapping[y][x][0];
                    //inverseMapping[domainWidth * y + x] = (int)(bitmaps[0].getWidth()*y + x*(float)bitmaps[0].getWidth()/domainWidth);
                    domainIndex[domainWidth * y + x] = domainWidth * y + x;
                }
            }
            Allocation inverseMappingAllocation = Allocation.createSized(renderScript,
                    Element.I32(renderScript), domainHeight * domainWidth, Allocation.USAGE_SCRIPT);
            Allocation domainIndexAllocation = Allocation.createSized(renderScript,
                    Element.I32(renderScript), domainHeight * domainWidth, Allocation.USAGE_SCRIPT);
            inverseMappingAllocation.copyFrom(inverseMapping);
            domainIndexAllocation.copyFrom(domainIndex);
            interpolationScript.set_mapIndexes(inverseMappingAllocation);
            interpolationScript.set_domainIndexes(domainIndexAllocation);
            interpolationScript.set_sourceWidth(bitmaps[0].getWidth());
            interpolationScript.set_sourceHeight(bitmaps[0].getHeight());
            interpolationScript.set_targetWidth(domainWidth);
            interpolationScript.set_targetHeight(domainHeight);
            for (int b = 0; b < bitmaps.length; b++) {
                interpolatedBitmaps[b] = Bitmap.createBitmap(domainWidth, domainHeight, Bitmap.Config.ARGB_8888);
                bitmapSourceAllocation = Allocation.createFromBitmap(renderScript, bitmaps[b],
                        Allocation.MipmapControl.MIPMAP_NONE,
                        Allocation.USAGE_SCRIPT);
                bitmapOutputAllocation = Allocation.createFromBitmap(renderScript, interpolatedBitmaps[b],
                        Allocation.MipmapControl.MIPMAP_NONE,
                        Allocation.USAGE_SCRIPT);
                interpolationScript.set_bitmapSource(bitmapSourceAllocation);
                interpolationScript.set_bitmapOutput(bitmapOutputAllocation);
                interpolationScript.set_script(interpolationScript);
                interpolationScript.invoke_filter();
                bitmapOutputAllocation.copyTo(interpolatedBitmaps[b]);
            }

            return new Object[]{interpolatedBitmaps, inverse2dInterpolationMapping};
        }

        @Override
        protected void onPostExecute(Object[] objects) {
            callbacks.onInterpolatedBitmapResult((Bitmap[]) objects[0], (int[][][]) objects[1]);
        }
    }

    // TODO: DEPRECIATED 20160506
    private static class MakeBitmapFromFullInverseMappingOnThread extends AsyncTask<Object[], Void, Bitmap[]> {
        @Override
        protected void onPreExecute() {
            //isOnThreadMakingBitmap = true;
        }
        private static final boolean OPENCV_INTERPOLATION = true;
        @Override
        protected Bitmap[] doInBackground(Object[]... params) {
            long time = System.currentTimeMillis();

            Bitmap[] bitmaps = (Bitmap[]) params[0][0];
            int[][][] inverse2dInterpolationMapping = (int[][][]) params[0][1];
            Bitmap[] interpolatedBitmaps = new Bitmap[bitmaps.length];

            /*
            if (OPENCV_INTERPOLATION) {
                Mat[] movingMats = new Mat[bitmaps.length];
                Mat[] targetMats = new Mat[bitmaps.length];

                for (int b = 0; b < bitmaps.length; b++) {
                    int width = inverse2dInterpolationMapping[0].length;
                    int height = inverse2dInterpolationMapping.length;
                    interpolatedBitmaps[b] = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    movingMats[b] = new Mat(bitmaps[0].getHeight(), bitmaps[0].getWidth(), CvType.CV_8UC4);
                    targetMats[b] = new Mat(height, width, CvType.CV_8UC4);
                    Utils.bitmapToMat(bitmaps[b], movingMats[b]);
                }

                // TODO: OpenCV interpolation
                int frequency = 1;
                for (int y = 0; y < inverse2dInterpolationMapping.length; y += frequency) {
                    for (int x = 0; x < inverse2dInterpolationMapping[0].length; x += frequency) {
                        for (int b = 0; b < bitmaps.length; b++) {
                            double[] color = movingMats[b].get(inverse2dInterpolationMapping[y][x][1], inverse2dInterpolationMapping[y][x][0]);
                            if (x + frequency - 1 > inverse2dInterpolationMapping[0].length || y + frequency - 1 > inverse2dInterpolationMapping.length) {
                                for (int i = 0; x + i < inverse2dInterpolationMapping[0].length; i++) {
                                    for (int j = 0; y + j < inverse2dInterpolationMapping.length; j++) {
                                        targetMats[b].put(y + j, x + i, color);
                                    }
                                }
                            }
                            for (int i = 0; i < frequency; i++) {
                                for (int j = 0; j < frequency; j++) {
                                    targetMats[b].put(y + j, x + i, color);
                                }
                            }
                        }
                    }
                }

                for (int b = 0; b < bitmaps.length; b++) {
                    Utils.matToBitmap(targetMats[b], interpolatedBitmaps[b]);
                }
            } else {
                // TODO: Java interpolation
                for (int b = 0; b < bitmaps.length; b++) {
                    int[] pixels = new int[inverse2dInterpolationMapping.length * inverse2dInterpolationMapping[0].length];
                    int frequency = 10;
                    for (int y = 0; y < inverse2dInterpolationMapping.length; y += frequency) {
                        for (int x = 0; x < inverse2dInterpolationMapping[0].length; x += frequency) {
                            int color = bitmaps[b].getPixel(inverse2dInterpolationMapping[y][x][0], inverse2dInterpolationMapping[y][x][1]);
                            if (x + frequency - 1 > inverse2dInterpolationMapping[0].length || y + frequency - 1 > inverse2dInterpolationMapping.length) {
                                for (int i = 0; x + i < inverse2dInterpolationMapping[0].length; i++) {
                                    for (int j = 0; y + j < inverse2dInterpolationMapping.length; j++) {
                                        pixels[(y + j) * inverse2dInterpolationMapping[0].length + x + i] = color;
                                    }
                                }
                            }
                            for (int i = 0; i < frequency; i++) {
                                for (int j = 0; j < frequency; j++) {
                                    pixels[(y + j) * inverse2dInterpolationMapping[0].length + x + i] = color;
                                }
                            }
                        }
                    }

                    interpolatedBitmaps[b].setPixels(pixels, 0, inverse2dInterpolationMapping[0].length, 0, 0, inverse2dInterpolationMapping[0].length, inverse2dInterpolationMapping.length);
                }
            }
            */
            // TODO: UPDATE: 20160506: SuperFast Renderscript interpolation
            int domainWidth = inverse2dInterpolationMapping[0].length;
            int domainHeight = inverse2dInterpolationMapping.length;

            int[] inverseMapping = new int[domainHeight * domainWidth];
            int[] domainIndex = new int[domainHeight * domainWidth];
            for (int y = 0; y < domainHeight; y++) {
                for (int x = 0; x < domainWidth; x++) {
                    inverseMapping[domainWidth * y + x] = bitmaps[0].getWidth() * inverse2dInterpolationMapping[y][x][1] + inverse2dInterpolationMapping[y][x][0];
                    //inverseMapping[domainWidth * y + x] = (int)(bitmaps[0].getWidth()*y + x*(float)bitmaps[0].getWidth()/domainWidth);
                    domainIndex[domainWidth * y + x] = domainWidth * y + x;
                }
            }
            Allocation inverseMappingAllocation = Allocation.createSized(renderScript,
                    Element.I32(renderScript), domainHeight * domainWidth, Allocation.USAGE_SCRIPT);
            Allocation domainIndexAllocation = Allocation.createSized(renderScript,
                    Element.I32(renderScript), domainHeight * domainWidth, Allocation.USAGE_SCRIPT);
            inverseMappingAllocation.copyFrom(inverseMapping);
            domainIndexAllocation.copyFrom(domainIndex);
            interpolationScript.set_mapIndexes(inverseMappingAllocation);
            interpolationScript.set_domainIndexes(domainIndexAllocation);
            interpolationScript.set_sourceWidth(bitmaps[0].getWidth());
            interpolationScript.set_sourceHeight(bitmaps[0].getHeight());
            interpolationScript.set_targetWidth(domainWidth);
            interpolationScript.set_targetHeight(domainHeight);
            for (int b = 0; b < bitmaps.length; b++) {
                interpolatedBitmaps[b] = Bitmap.createBitmap(domainWidth, domainHeight, Bitmap.Config.ARGB_8888);
                bitmapSourceAllocation = Allocation.createFromBitmap(renderScript, bitmaps[b],
                        Allocation.MipmapControl.MIPMAP_NONE,
                        Allocation.USAGE_SCRIPT);
                bitmapOutputAllocation = Allocation.createFromBitmap(renderScript, interpolatedBitmaps[b],
                        Allocation.MipmapControl.MIPMAP_NONE,
                        Allocation.USAGE_SCRIPT);
                interpolationScript.set_bitmapSource(bitmapSourceAllocation);
                interpolationScript.set_bitmapOutput(bitmapOutputAllocation);
                interpolationScript.set_script(interpolationScript);
                interpolationScript.invoke_filter();
                bitmapOutputAllocation.copyTo(interpolatedBitmaps[b]);
            }

            Log.d("debugging", "Mesh Bitmap Making time: " + String.valueOf((float) (System.currentTimeMillis() - time) / 1000) + "s");
            return interpolatedBitmaps;
        }

        @Override
        protected void onPostExecute(Bitmap[] bitmaps) {
            callbacks.onFullInverseMappingBitmapResult(bitmaps);
            //isOnThreadMakingBitmap = false;
        }
    }


    private static class DrawPartOfBitmapFromFullInverseMappingOnThread extends AsyncTask<Object[], Void, Void> {

        private static final Object lock = new Object();

        @Override
        protected void onPreExecute() {
            //isOnThreadMakingBitmap = true;
        }

        private static final boolean OPENCV_INTERPOLATION = false;

        @Override
        protected Void doInBackground(Object[]... params) {
            Bitmap source = (Bitmap) params[0][0];
            Bitmap output = (Bitmap) params[0][1];
            Rect2DMap inverseMapping = (Rect2DMap) params[0][2];
            int[] dirty = (int[]) params[0][3];
            int[] outputTopLeft = inverseMapping.getDomainLeftTop();

            if (dirty[0] < outputTopLeft[0]) {
                dirty[0] = outputTopLeft[0];
            }
            if (dirty[1] >= outputTopLeft[0] + output.getWidth()) {
                dirty[1] = outputTopLeft[0] + output.getWidth() - 1;
            }
            if (dirty[2] < outputTopLeft[1]) {
                dirty[2] = outputTopLeft[1];
            }
            if (dirty[3] >= outputTopLeft[1] + output.getHeight()) {
                dirty[3] = outputTopLeft[1] + output.getHeight() - 1;
            }

            long time = System.currentTimeMillis();

            // TODO: here implements interpolation by OpenCV
            // interpolate image

            synchronized (lock) {
                if (OPENCV_INTERPOLATION) {
                    Mat movingMat;
                    Mat targetMat;

                    movingMat = new Mat(source.getHeight(), source.getWidth(), CvType.CV_8UC4);
                    targetMat = new Mat(output.getHeight(), output.getWidth(), CvType.CV_8UC4);
                    Utils.bitmapToMat(source, movingMat);
                    Utils.bitmapToMat(output, targetMat);

                    int frequency = 1;
                    for (int y = dirty[2]; y < dirty[3] + 1; y += frequency) {
                        for (int x = dirty[0]; x < dirty[1] + 1; x += frequency) {
                            double[] color = movingMat.get(inverseMapping.getFull2DMapStartFromZero()[y][x][1], inverseMapping.getFull2DMapStartFromZero()[y][x][0]);
                            if (x + frequency - 1 > source.getWidth() || y + frequency - 1 > source.getHeight()) {
                                for (int i = 0; x + i < source.getWidth(); i++) {
                                    for (int j = 0; y + j < source.getHeight(); j++) {
                                        targetMat.put(y + j, x + i, color);
                                    }
                                }
                            }
                            for (int i = 0; i < frequency; i++) {
                                for (int j = 0; j < frequency; j++) {
                                    targetMat.put(y + j, x + i, color);
                                }
                            }
                        }
                    }
                } else {
                    // TODO: UDPATE: 20160506: android partial interpolation
                    int[] dirtyInArrayCoordinate = dirty;
                    dirtyInArrayCoordinate[0] -= outputTopLeft[0];
                    dirtyInArrayCoordinate[1] -= outputTopLeft[0];
                    dirtyInArrayCoordinate[2] -= outputTopLeft[1];
                    dirtyInArrayCoordinate[3] -= outputTopLeft[1];

                    int frequency = 1;
                    for (int y = dirtyInArrayCoordinate[2]; y < dirtyInArrayCoordinate[3] + 1; y += frequency) {
                        for (int x = dirtyInArrayCoordinate[0]; x < dirtyInArrayCoordinate[1] + 1; x += frequency) {
                            int color = source.getPixel(inverseMapping.getFull2DMapStartFromZero()[y][x][0], inverseMapping.getFull2DMapStartFromZero()[y][x][1]);
                            if (x + frequency - 1 > output.getWidth() || y + frequency - 1 > output.getHeight()) {
                                for (int i = 0; x + i < output.getWidth(); i++) {
                                    for (int j = 0; y + j < output.getHeight(); j++) {
                                        output.setPixel(x + i, y + j, color);
                                    }
                                }
                            }
                            for (int i = 0; i < frequency; i++) {
                                for (int j = 0; j < frequency; j++) {
                                    output.setPixel(x + i, y + j, color);
                                }
                            }
                        }
                    }

                    // TODO: UPDATE: 20160507: SuperFast Renderscript interpolation
                    // FIXME: Currently contains memory bugs, but is it a good way to create whole bitmap and then crop?
                    /*
                    dirty[0] -= outputTopLeft[0];
                    dirty[1] -= outputTopLeft[0];
                    dirty[2] -= outputTopLeft[1];
                    dirty[3] -= outputTopLeft[1];

                    int domainWidth = output.getWidth();
                    int domainHeight = output.getHeight();
                    int sourceWidth = source.getWidth();
                    int sourceHeight = source.getHeight();

                    int dirtyWidth = dirty[1] - dirty[0] + 1;
                    int dirtyHeight = dirty[3] - dirty[2] + 1;
                    int[] inverseMappingIndex = new int[dirtyWidth * dirtyHeight];
                    int[] domainIndex = new int[dirtyWidth * dirtyHeight];
                    int currentIndexInDirty = 0;
                    for (int current_y = dirty[2]; current_y <= dirty[3]; current_y++) {
                        for (int current_x = dirty[0]; current_x <= dirty[1]; current_x++) {
                            inverseMappingIndex[currentIndexInDirty] =
                                    sourceWidth * inverseMapping.getFull2DMapStartFromZero()[current_y][current_x][1]
                                            + inverseMapping.getFull2DMapStartFromZero()[current_y][current_x][0];
                            //inverseMapping[domainWidth * y + x] = (int)(bitmaps[0].getWidth()*y + x*(float)bitmaps[0].getWidth()/domainWidth);
                            domainIndex[currentIndexInDirty] = domainWidth * current_y + current_x;
                            currentIndexInDirty++;
                        }
                    }
                    Allocation inverseMappingAllocation = Allocation.createSized(renderScript,
                            Element.I32(renderScript), currentIndexInDirty, Allocation.USAGE_SCRIPT);
                    Allocation domainIndexAllocation = Allocation.createSized(renderScript,
                            Element.I32(renderScript), currentIndexInDirty, Allocation.USAGE_SCRIPT);
                    inverseMappingAllocation.copyFrom(inverseMappingIndex);
                    domainIndexAllocation.copyFrom(domainIndex);
                    interpolationScript.set_mapIndexes(inverseMappingAllocation);
                    interpolationScript.set_domainIndexes(domainIndexAllocation);
                    interpolationScript.set_sourceWidth(sourceWidth);
                    interpolationScript.set_sourceHeight(sourceHeight);
                    interpolationScript.set_targetWidth(domainWidth);
                    interpolationScript.set_targetHeight(domainHeight);

                    Bitmap bitmapWaitToCrop = Bitmap.createBitmap(domainWidth, domainHeight, Bitmap.Config.ARGB_8888);
                    bitmapSourceAllocation = Allocation.createFromBitmap(renderScript, source,
                            Allocation.MipmapControl.MIPMAP_NONE,
                            Allocation.USAGE_SCRIPT);
                    bitmapOutputAllocation = Allocation.createFromBitmap(renderScript, bitmapWaitToCrop,
                            Allocation.MipmapControl.MIPMAP_NONE,
                            Allocation.USAGE_SCRIPT);
                    interpolationScript.set_bitmapSource(bitmapSourceAllocation);
                    interpolationScript.set_bitmapOutput(bitmapOutputAllocation);
                    interpolationScript.set_script(interpolationScript);
                    interpolationScript.invoke_filter();
                    bitmapOutputAllocation.copyTo(bitmapWaitToCrop);
                    new Canvas(output).drawBitmap(Bitmap.createBitmap(bitmapWaitToCrop, dirty[0], dirty[2], dirty[1] - dirty[0] + 1, dirty[3] - dirty[2] + 1),
                            dirty[0], dirty[2], null);
                    */
                }
            }

            //Log.d("debugging", "Mesh Bitmap Making time: " + String.valueOf((float) (System.currentTimeMillis() - time) / 1000) + "s");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            callbacks.onPartInverseMappingBitmapResult();
            //isOnThreadMakingBitmap = false;
        }
    }

// FIXME: Debug: write files to check data validity

            /*
            Log.d("debugging", "size1:" + String.valueOf(inverse2dInterpolationMapping.length));
            Log.d("debugging", "size2:" + String.valueOf(inverse2dInterpolationMapping[0].length));
            Log.d("debugging", "size3:" + String.valueOf(inverse2dInterpolationMapping[0][0].length));

            int x_min = inverse2dInterpolationMapping[0][0][0];
            for (int j = 0;j< inverse2dInterpolationMapping[0].length;j++){
                for (int i = 0;i < inverse2dInterpolationMapping.length;i++){
                    //Log.d("debugging","hihi: "  +  String.valueOf(inverse2dInterpolationMapping[i][j][0]));
                    if (x_min > inverse2dInterpolationMapping[i][j][0]){
                        x_min = inverse2dInterpolationMapping[i][j][0];
                    }
                }
            }
            Log.d("debugging", "min = " + String.valueOf(x_min));

            // a
            filename = "a.m";
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            Log.d("debugging","Oh shit, writing");
            try {
                Log.d("debugging","i: "  +  "hi");
                outputStream = new FileOutputStream(file, false);
                String fileString = "sol_x = [ ...\n";
                for (int i = 0;i< inverse2dInterpolationMapping.length;i++){
                    Log.d("debugging","i: "  +  String.valueOf(i));
                    for (int j = 0;j<inverse2dInterpolationMapping[0].length - 1;j++){
                        fileString += String.valueOf(inverse2dInterpolationMapping[i][j][0]) + ", ";
                    }
                    fileString += String.valueOf(inverse2dInterpolationMapping[i][inverse2dInterpolationMapping[0].length - 1][0]) + ";\n";
                }
                outputStream.write(fileString.getBytes());
                outputStream.write("sol_x = [ ...\n".getBytes());
                for (int i = 0;i< inverse2dInterpolationMapping.length;i++){
                    for (int j = 0;j<inverse2dInterpolationMapping[0].length - 1;j++){
                        outputStream.write((
                                String.valueOf(inverse2dInterpolationMapping[i][j][0]) + ", ").getBytes());
                    }
                    outputStream.write((
                            String.valueOf(inverse2dInterpolationMapping[i][inverse2dInterpolationMapping[0].length - 1][0]) + ";\n").getBytes());
                }

                outputStream.write("];".getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.d("debugging","shit");
                e.printStackTrace();
            }

            // b
            filename = "b.m";
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            try {
                outputStream = new FileOutputStream(file, false);
                String fileString = "sol_y = [ ...\n";
                for (int i = 0;i< inverse2dInterpolationMapping.length;i++){
                    Log.d("debugging","i: "  +  String.valueOf(i));
                    for (int j = 0;j<inverse2dInterpolationMapping[1].length - 1;j++){
                        fileString += String.valueOf(inverse2dInterpolationMapping[i][j][1]) + ", ";
                    }
                    fileString += String.valueOf(inverse2dInterpolationMapping[i][inverse2dInterpolationMapping[1].length - 1][1]) + ";\n";
                }
                outputStream.write(fileString.getBytes());
                outputStream.write("sol_y = [ ...\n".getBytes());
                for (int i = 0;i< inverse2dInterpolationMapping.length;i++){
                    for (int j = 0;j<inverse2dInterpolationMapping[1].length - 1;j++){
                        outputStream.write((
                                String.valueOf(inverse2dInterpolationMapping[i][j][1]) + ", ").getBytes());
                    }
                    outputStream.write((
                            String.valueOf(inverse2dInterpolationMapping[i][inverse2dInterpolationMapping[1].length - 1][1]) + ";\n").getBytes());
                }
                outputStream.write("];".getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.d("debugging","shit");
                e.printStackTrace();
            }
            */


}
