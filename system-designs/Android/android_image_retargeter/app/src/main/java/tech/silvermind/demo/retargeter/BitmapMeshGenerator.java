package tech.silvermind.demo.retargeter;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat6;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Subdiv2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tech.silvermind.Jdt.triangulator.DelaunayTriangulator;

import static org.opencv.imgproc.Imgproc.cvtColor;

/**
 * Created by edward on 4/2/16.
 */
public class BitmapMeshGenerator {
    static {
        System.loadLibrary("opencv_java3");
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    public interface BitmapMeshResultCallback {
        void onBitmapMeshResult(BitmapMeshGenerator.Mesh mesh);
    }

    private int bitmapWidth;
    private int bitmapHeight;
    final int expectedVertexNum = 450;

    public class Mesh {
        public int[][] faces;
        public int[][] vertices;
    }

    private BitmapMeshResultCallback callback;

    public void generateMesh(final BitmapMeshResultCallback callback, final Bitmap bitmap) {
        this.callback = callback;
        new generateMeshThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap);
    }

    private static final boolean REGULAR_MESH = true;

    private class generateMeshThread extends AsyncTask<Bitmap, Void, Mesh> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected BitmapMeshGenerator.Mesh doInBackground(Bitmap... params) {

            long time = System.currentTimeMillis();
            Mesh mesh = new Mesh();
            bitmapWidth = params[0].getWidth();
            bitmapHeight = params[0].getHeight();

            //ArrayList<int[]> selectedEdgeVertexList = new ArrayList<>();
            ArrayList<int[]> selectedEdgeVertexList = new ArrayList<>();
            if (!REGULAR_MESH) {
                /** Canny Edge Detector */
                Mat rgbImage = new Mat(bitmapHeight, bitmapWidth, CvType.CV_8UC3);
                Mat blurImage = new Mat(bitmapHeight, bitmapWidth, CvType.CV_8UC1);
                Mat grayImage = new Mat();
                Mat cannyImage = new Mat();
                //Mat dst = new Mat(bitmapHeight, bitmapWidth, CvType.CV_8UC3);
                //Mat detected_edges = new Mat(bitmapHeight, bitmapWidth, CvType.CV_8UC1);

                //int edgeThresh = 1;
                final int lowThreshold = 50;
                final int max_lowThreshold = 100;
                //int ratio = 3;
                int kernel_size = 3;
                Utils.bitmapToMat(params[0], rgbImage);
                cvtColor(rgbImage, grayImage, Imgproc.COLOR_BGR2GRAY);

                Imgproc.blur(grayImage, blurImage, new Size(4, 4));
                //blurImage = doSegmentationOnMat(rgbImage);
                Imgproc.Canny(blurImage, cannyImage, lowThreshold, max_lowThreshold, kernel_size, true);
                //Canny(detected_edges, detected_edges, lowThreshold, lowThreshold * ratio, kernel_size);

                ArrayList<int[]> edgeVertexList = new ArrayList<>();
                for (int y = 0; y < cannyImage.rows(); y++) {
                    for (int x = 0; x < cannyImage.cols(); x++) {
                        if (cannyImage.get(y, x)[0] != 0) {
                            edgeVertexList.add(new int[]{x, y});
                        }
                    }
                }
                if (edgeVertexList.size() < expectedVertexNum) {
                    selectedEdgeVertexList = edgeVertexList;
                } else {
                    int gap = Math.round((float) edgeVertexList.size() / 250);
                    for (int i = 0; i < edgeVertexList.size(); i += Math.round(gap)) {
                        selectedEdgeVertexList.add(edgeVertexList.get(i));
                    }
                }
                // add corners and boundaries to avoid errors
                int numberOfPointsOnBoundaries = 3;
                for (int i = 0; i < numberOfPointsOnBoundaries; i++) {
                    // left
                    selectedEdgeVertexList.add(new int[]{0, Math.round((bitmapHeight - 1) * (float) i / (numberOfPointsOnBoundaries - 1))});
                    // right
                    selectedEdgeVertexList.add(new int[]{bitmapWidth - 1, Math.round((bitmapHeight - 1) * (float) i / (numberOfPointsOnBoundaries - 1))});
                    // bottom
                    selectedEdgeVertexList.add(new int[]{Math.round((bitmapWidth - 1) * (float) i / (numberOfPointsOnBoundaries - 1)), bitmapHeight - 1});
                    // top
                    selectedEdgeVertexList.add(new int[]{Math.round((bitmapWidth - 1) * (float) i / (numberOfPointsOnBoundaries - 1)), 0});
                }

                /*
                selectedEdgeVertexList.add(new int[]{0, 0});
                selectedEdgeVertexList.add(new int[]{0, bitmapHeight - 1});
                selectedEdgeVertexList.add(new int[]{bitmapWidth - 1, 0});
                selectedEdgeVertexList.add(new int[]{bitmapWidth - 1, bitmapHeight - 1});
                selectedEdgeVertexList.add(new int[]{0, (int) ((float) (bitmapHeight - 1)) / 2});
                selectedEdgeVertexList.add(new int[]{bitmapWidth - 1, (int) ((float) (bitmapHeight - 1)) / 2});
                selectedEdgeVertexList.add(new int[]{(int) ((float) (bitmapWidth - 1)) / 2, 0});
                selectedEdgeVertexList.add(new int[]{(int) ((float) (bitmapWidth - 1)) / 2, bitmapHeight - 1});
                */
            } else {
                int gap = Math.round((float) Math.sqrt(((float) bitmapHeight * bitmapWidth) / expectedVertexNum));
                int y = 0;
                for (; y < bitmapHeight; y += gap) {
                    int x = 0;
                    for (; x < bitmapWidth; x += gap) {
                        selectedEdgeVertexList.add(new int[]{x, y});
                    }
                    if (x != bitmapWidth - 1) {
                        selectedEdgeVertexList.add(new int[]{bitmapWidth - 1, y});
                    }
                }
                if (y != bitmapHeight - 1) {
                    int x = 0;
                    for (; x < bitmapWidth; x += gap) {
                        selectedEdgeVertexList.add(new int[]{x, bitmapHeight - 1});
                    }
                    if (x != bitmapWidth - 1) {
                        selectedEdgeVertexList.add(new int[]{bitmapWidth - 1, bitmapHeight - 1});
                    }
                }
                // add corners
            }

            //FIXME: JZY3D's Delaunay Triangulation sometimes returns null
            /*
            DelaunayTriangulation delaunayTriangulation = new DelaunayTriangulation();
            for (int[] v : selectedEdgeVertexList) {
                delaunayTriangulation.insertPoint(new Point(v[0], v[1]));
            }

            getJZY3DTriangulation(mesh, delaunayTriangulation);
            return mesh;
            */

            //FIXME: OpenCV's Delaunay Triangulation has bugs - some vertices are out bounded.
            Subdiv2D subdiv2D = new Subdiv2D(new Rect(0, 0, bitmapWidth, bitmapHeight));
            for (int[] v : selectedEdgeVertexList) {
                //Log.d("debugging","vertex (x,y):" + String.valueOf(v[0])+" "+String.valueOf(v[1]));
                subdiv2D.insert(new Point(v[0], v[1]));
            }

            getSubdiv2DVertexFace(mesh, subdiv2D);

            //TODO: Currently using JDT
            //getJDT(mesh, selectedEdgeVertexList);
            Log.d("debugging", "Generate mesh time: " + String.valueOf((float) (System.currentTimeMillis() - time) / 1000) + "s");

            return mesh;
        }

        @Override
        protected void onPostExecute(BitmapMeshGenerator.Mesh m) {
            callback.onBitmapMeshResult(m);
        }
    }

    private Mat doSegmentationOnMat(Mat input) {
        Mat output = new Mat(bitmapHeight, bitmapWidth, CvType.CV_8UC3);
        Imgproc.pyrMeanShiftFiltering(input, output, 20, 45, 3, new TermCriteria(1, 10, 0));
        return output;
    }

    /*
    private void getJdtTriangulation(Mesh mesh, Vector<Triangle> triangleList) {

        int currentVertexIndex = 0;
        int[][] tempVertexIndexMap = new int[bitmapHeight][bitmapWidth];
        for (int[] row : tempVertexIndexMap) {
            Arrays.fill(row, -1);
        }
        List<int[]> vertexList = new ArrayList<>();
        List<int[]> faceList = new ArrayList<>();
        for (int tri = 0; tri < triangleList.size(); tri++) {
            int[] vertexIndexInFace = new int[3];
            Triangle triangle = triangleList.get(tri);
            int[] faceVertices = new int[]{
                    triangle.a.x,
                    triangle.a.y,
                    triangle.b.x,
                    triangle.b.y,
                    triangle.c.x,
                    triangle.c.y
            };
            //Log.d("debugging", "faceVertices:" + Arrays.toString(faceVertices));

            /*{
                        Log.d("debugging", String.valueOf(faceVertices[m + 1])+" "+String.valueOf(faceVertices[m]));
                    }
            for (int m = 0; m < 6; m +=2){
                if (faceVertices[m + 1] < 0){
                    validFace = false;
                    break;
                    //faceVertices[m + 1] = 0;
                }else if (faceVertices[m + 1]>= bitmapHeight){
                    validFace = false;
                    break;
                    //faceVertices[m + 1] = bitmapHeight - 1;
                }
                if (faceVertices[m] < 0){
                    validFace = false;
                    break;
                    //faceVertices[m] = 0;
                }else if (faceVertices[m] >= bitmapWidth){
                    validFace = false;
                    break;
                    //faceVertices[m] = bitmapWidth - 1;
                }
            }

            for (int m = 0; m < 6; m += 2) {
                if (tempVertexIndexMap[faceVertices[m + 1]][faceVertices[m]] == -1) {
                    vertexList.add(new int[]{faceVertices[m], faceVertices[m + 1]});
                    tempVertexIndexMap[faceVertices[m + 1]][faceVertices[m]] = currentVertexIndex;
                    vertexIndexInFace[m / 2] = currentVertexIndex;
                    currentVertexIndex++;
                } else {
                    vertexIndexInFace[m / 2] = tempVertexIndexMap[faceVertices[m + 1]][faceVertices[m]];
                }
            }
            faceList.add(vertexIndexInFace);
        }
        mesh.vertices = vertexList.toArray(new int[vertexList.size()][2]);
        mesh.faces = faceList.toArray(new int[faceList.size()][3]);
    }
    */

    //FIXME: JZY3D & OpenCV 's functions
    /*
    private Point[] vertexList2Points(List<int[]> list){
        Point[] points = new Point[list.size()];
        for(int i=0; i<list.size();i++ ){
            points[i] = new Point(list.get(i)[0], list.get(i)[1]);
        }
        return points;
    }
    private void getJZY3DTriangulation(Mesh mesh, DelaunayTriangulation delaunayTriangulation){
        int currentVertexIndex = 0;
        int[][] tempVertexIndexMap = new int[bitmapHeight][bitmapWidth];
        for (int[] row: tempVertexIndexMap){
            Arrays.fill(row,-1);
        }
        List<int[]> vertexList = new ArrayList<>();
        List<int[]> faceList = new ArrayList<>();

        Iterator<Triangle> triangleList = delaunayTriangulation.trianglesIterator();
        int f = -1;
        while (triangleList.hasNext()){
            f++;
            boolean validFace = true;
            int[] vertexIndexInFace = new int[3];
            Triangle triangle = triangleList.next();
            if (triangle.getA() == null) continue;
            int[] faceVertices = new int[]{
                    (int) Math.round(triangle.getA().getX()),
                    (int) Math.round(triangle.getA().getY()),
                    (int) Math.round(triangle.getB().getX()),
                    (int) Math.round(triangle.getB().getY()),
                    (int) Math.round(triangle.getC().getX()),
                    (int) Math.round(triangle.getC().getY())
            };
            //Log.d("debugging", "faceVertices:" + Arrays.toString(faceVertices));

            for (int m = 0; m < 6; m +=2){
                if (faceVertices[m + 1] < 0){
                    validFace = false;
                    break;
                    //faceVertices[m + 1] = 0;
                }else if (faceVertices[m + 1]>= bitmapHeight){
                    validFace = false;
                    break;
                    //faceVertices[m + 1] = bitmapHeight - 1;
                }
                if (faceVertices[m] < 0){
                    validFace = false;
                    break;
                    //faceVertices[m] = 0;
                }else if (faceVertices[m] >= bitmapWidth){
                    validFace = false;
                    break;
                    //faceVertices[m] = bitmapWidth - 1;
                }
            }

            if(validFace){
                for (int m = 0; m < 6; m +=2){
                    if (tempVertexIndexMap[ faceVertices[m + 1]][faceVertices[m]] == -1){
                        vertexList.add(new int[]{faceVertices[m], faceVertices[m + 1]});
                        tempVertexIndexMap[faceVertices[m + 1]][faceVertices[m]] = currentVertexIndex;
                        vertexIndexInFace[m/2] = currentVertexIndex;
                        currentVertexIndex++;
                    }else{
                        vertexIndexInFace[m/2] = tempVertexIndexMap[faceVertices[m + 1]][faceVertices[m]];
                    }
                }
                faceList.add(vertexIndexInFace);
            }
        }
        mesh.vertices = vertexList.toArray(new int[vertexList.size()][2]);
        mesh.faces = faceList.toArray(new int[faceList.size()][3]);
    }
*/

    private void getJDT(Mesh mesh, ArrayList<int[]> selectedEdgeVertexList) {
        // JDT Delaunay Triangulation
        DelaunayTriangulator delaunayTriangulator = new DelaunayTriangulator();
        DelaunayTriangulator.DelaunayRestult result = DelaunayTriangulator.getResultContainer();
        delaunayTriangulator.triangulte(selectedEdgeVertexList, bitmapWidth, bitmapHeight, result);
        mesh.vertices = result.vertices;
        mesh.faces = result.faces;

    }

    private void getSubdiv2DVertexFace(Mesh mesh, Subdiv2D subdiv2D) {
        int currentVertexIndex = 0;
        int[][] tempVertexIndexMap = new int[bitmapHeight][bitmapWidth];
        for (int[] row : tempVertexIndexMap) {
            Arrays.fill(row, -1);
        }
        List<int[]> vertexList = new ArrayList<>();
        List<int[]> faceList = new ArrayList<>();

        MatOfFloat6 triangleList = new MatOfFloat6();
        subdiv2D.getTriangleList(triangleList);

        for (int f = 0; f < triangleList.rows(); f++) {
            boolean validFace = true;
            int[] vertexIndexInFace = new int[3];
            float[] faceVertices = new float[6];
            triangleList.get(f, 0, faceVertices);
            //Log.d("debugging", "faceVertices:" + Arrays.toString(faceVertices));

            for (int m = 0; m < 6; m += 2) {
                if (Math.round(faceVertices[m + 1]) < 0) {
                    validFace = false;
                    break;
                    //faceVertices[m + 1] = 0;
                } else if (Math.round(faceVertices[m + 1]) >= bitmapHeight) {
                    validFace = false;
                    break;
                    //faceVertices[m + 1] = bitmapHeight - 1;
                }
                if (Math.round(faceVertices[m]) < 0) {
                    validFace = false;
                    break;
                    //faceVertices[m] = 0;
                } else if (Math.round(faceVertices[m]) >= bitmapWidth) {
                    validFace = false;
                    break;
                    //faceVertices[m] = bitmapWidth - 1;
                }
            }

            if (validFace) {
                for (int m = 0; m < 6; m += 2) {
                    if (tempVertexIndexMap[Math.round(faceVertices[m + 1])][Math.round(faceVertices[m])] == -1) {
                        vertexList.add(new int[]{Math.round(faceVertices[m]), Math.round(faceVertices[m + 1])});
                        tempVertexIndexMap[Math.round(faceVertices[m + 1])][Math.round(faceVertices[m])] = currentVertexIndex;
                        vertexIndexInFace[m / 2] = currentVertexIndex;
                        currentVertexIndex++;
                    } else {
                        vertexIndexInFace[m / 2] = tempVertexIndexMap[Math.round(faceVertices[m + 1])][Math.round(faceVertices[m])];
                    }
                }
                faceList.add(vertexIndexInFace);
            }
        }
        mesh.vertices = vertexList.toArray(new int[vertexList.size()][2]);
        mesh.faces = faceList.toArray(new int[faceList.size()][3]);
    }
}
