package tech.silvermind.demo.retargeter;

import android.support.annotation.NonNull;

import org.opencv.core.MatOfFloat6;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Subdiv2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by edward on 5/7/16.
 */
public class ConvexHullCreator {
    private Subdiv2D subdiv2D;
    private boolean[][] pointsInConvexHull;
    private int domainWidth;
    private int domainHeight;

    public ConvexHullCreator(int domainWidth, int domainHeight) {
        this.domainHeight = domainHeight;
        this.domainWidth = domainWidth;
        subdiv2D = new Subdiv2D(new Rect(0, 0, domainWidth, domainHeight));
        pointsInConvexHull = new boolean[domainHeight][domainWidth];
    }

    public void reset() {
        subdiv2D = new Subdiv2D(new Rect(0, 0, domainWidth, domainHeight));
        pointsInConvexHull = new boolean[domainHeight][domainWidth];
    }

    public void addPoint(int x, int y) {
        subdiv2D.insert(new Point(x, y));
    }

    public void getPointIndexInConvexHull(int[][] pointSet, @NonNull ArrayList<Integer> indexResult, @NonNull ArrayList<android.graphics.Point> pointResult) {
        findPointsInConvexHull(subdiv2D);
        //ArrayList<Integer> is = new ArrayList<>();
        for (int i = 0; i < pointSet.length; i++) {
            if (pointsInConvexHull[pointSet[i][1]][pointSet[i][0]]) {
                indexResult.add(new Integer(i));
                pointResult.add(new android.graphics.Point(pointSet[i][0], pointSet[i][1]));
            }
            //is.add(new Integer(i));
        }
        //return convertIntegers(is);
    }


    private static int[] convertIntegers(List<Integer> integers) {
        int[] ret = new int[integers.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = integers.get(i).intValue();
        }
        return ret;
    }

    private void findPointsInConvexHull(Subdiv2D subdiv2D) {
        int currentVertexIndex = 0;
        int[][] tempVertexIndexMap = new int[domainHeight][domainWidth];
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
                } else if (Math.round(faceVertices[m + 1]) >= domainHeight) {
                    validFace = false;
                    break;
                    //faceVertices[m + 1] = bitmapHeight - 1;
                }
                if (Math.round(faceVertices[m]) < 0) {
                    validFace = false;
                    break;
                    //faceVertices[m] = 0;
                } else if (Math.round(faceVertices[m]) >= domainWidth) {
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
        int[][] vertices = vertexList.toArray(new int[vertexList.size()][2]);
        int[][] faces = faceList.toArray(new int[faceList.size()][3]);

        for (int f = 0; f < faces.length; f++) {
            int[] v1 = {vertices[faces[f][0]][0], vertices[faces[f][0]][1]};
            int[] v2 = {vertices[faces[f][1]][0], vertices[faces[f][1]][1]};
            int[] v3 = {vertices[faces[f][2]][0], vertices[faces[f][2]][1]};

            // find upper, middle, lower vertex
            int[] upperYVertex = new int[2];
            int[] middleYVertex = new int[2];
            int[] bottomYVertex = new int[2];

            if (v1[1] >= v2[1] && v1[1] >= v3[1]) {
                upperYVertex = v1;
                if (v2[1] >= v3[1]) {
                    middleYVertex = v2;
                    bottomYVertex = v3;
                } else {
                    middleYVertex = v3;
                    bottomYVertex = v2;
                }
            }
            if (v2[1] >= v3[1] && v2[1] >= v1[1]) {
                upperYVertex = v2;
                if (v1[1] >= v3[1]) {
                    middleYVertex = v1;
                    bottomYVertex = v3;
                } else {
                    middleYVertex = v3;
                    bottomYVertex = v1;
                }
            }
            if (v3[1] >= v1[1] && v3[1] >= v2[1]) {
                upperYVertex = v3;
                if (v2[1] >= v1[1]) {
                    middleYVertex = v2;
                    bottomYVertex = v1;
                } else {
                    middleYVertex = v1;
                    bottomYVertex = v2;
                }
            }
            // assume triangle vertices are not collinear
            if (bottomYVertex[1] == middleYVertex[1]) {
                for (int y = middleYVertex[1]; y <= upperYVertex[1]; y++) {
                    int edgeX1 = (y - bottomYVertex[1]) * (upperYVertex[0] - bottomYVertex[0])
                            / (upperYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    int edgeX2 = (y - middleYVertex[1]) * (upperYVertex[0] - middleYVertex[0])
                            / (upperYVertex[1] - middleYVertex[1]) + middleYVertex[0];
                    addFaceToMap(edgeX1, edgeX2, y, f);
                }
            } else if (upperYVertex[1] == middleYVertex[1]) {
                for (int y = bottomYVertex[1] + 1; y < middleYVertex[1]; y++) {
                    int edgeX1 = (y - bottomYVertex[1]) * (upperYVertex[0] - bottomYVertex[0])
                            / (upperYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    int edgeX2 = (y - bottomYVertex[1]) * (middleYVertex[0] - bottomYVertex[0])
                            / (middleYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    addFaceToMap(edgeX1, edgeX2, y, f);
                }
            } else {
                for (int y = bottomYVertex[1] + 1; y <= middleYVertex[1]; y++) {
                    int edgeX1 = (y - bottomYVertex[1]) * (upperYVertex[0] - bottomYVertex[0])
                            / (upperYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    int edgeX2 = (y - bottomYVertex[1]) * (middleYVertex[0] - bottomYVertex[0])
                            / (middleYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    addFaceToMap(edgeX1, edgeX2, y, f);
                }
                for (int y = middleYVertex[1] + 1; y <= upperYVertex[1]; y++) {
                    int edgeX1 = (y - bottomYVertex[1]) * (upperYVertex[0] - bottomYVertex[0])
                            / (upperYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    int edgeX2 = (y - middleYVertex[1]) * (upperYVertex[0] - middleYVertex[0])
                            / (upperYVertex[1] - middleYVertex[1]) + middleYVertex[0];
                    addFaceToMap(edgeX1, edgeX2, y, f);
                }
            }
        }
    }

    private void addFaceToMap(int edgeX1, int edgeX2, int y, int f) {
        int max = Math.max(edgeX1, edgeX2);
        int min = Math.min(edgeX1, edgeX2);
        if (y >= domainHeight || y < 0) return;
        for (int x = min; x <= max; x++) {
            // check valid point
            if (x >= domainWidth || x < 0) continue;
            pointsInConvexHull[y][x] = true;
        }
    }

}
