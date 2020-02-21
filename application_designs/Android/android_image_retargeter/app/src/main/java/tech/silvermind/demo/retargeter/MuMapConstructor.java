package tech.silvermind.demo.retargeter;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * Created by edward on 5/19/16.
 */
public class MuMapConstructor {

    public static final int MODEL_MONO_MU = 2;
    public static final int MODEL_STRONG_MU = 0;
    public static final int MODEL_WEAK_MU = 1;
    private static int MODEL = MODEL_MONO_MU;

    /*
    @IntDef({MODEL_MONO_MU, MODEL_STRONG_MU, MODEL_WEAK_MU})
    public @interface MuModel {
    }
    */

    public static void setModel(final int MODEL) {
        MuMapConstructor.MODEL = MODEL;
    }

    public static int getModel() {
        return MODEL;
    }

    // FIXME: Update pending:
    public static float[][] constructMuOnFace(int[][] faceDomain, int[][] vertexDomain, boolean[] faceSelectedBoolean, ArrayList<ArrayList<Point>> objectSet, int bitmapSourceWidth, int bitmapSourceHeight, int bitmapOutputWidth) {
        float[][] mu = null;
        // default
        float minPortionNonObjectRegionPreserve = 0.2f;
        // program pattern follows Matlab version as similar s possible
        //int[] RY = new int[bitmapOutputHeight];
        int[] RX = new int[bitmapSourceWidth];
        int[] RY = new int[bitmapSourceHeight];
        switch (MODEL) {
            /***
             *                    MODEL_STRONG_MU
             *
             *  Remarks:
             *  RX,RY are used to indicate objects' locations here
             *  RY_STRONG_MU indicates objects' region size
             *  */
            case MODEL_STRONG_MU:
                mu = new float[faceSelectedBoolean.length][2];
                int RY_STRONG_MU = 0;
                for (ArrayList<Point> object : objectSet) {
                    int objectXMin = object.get(0).x;
                    int objectXMax = object.get(0).x;
                    int objectYMin = object.get(0).y;
                    int objectYMax = object.get(0).y;
                    for (Point point : object) {
                        if (objectXMax < point.x) objectXMax = point.x;
                        if (objectXMin > point.x) objectXMin = point.x;
                        if (objectYMax < point.y) objectYMax = point.y;
                        if (objectYMin > point.y) objectYMin = point.y;
                    }
                    int objectWidth = objectXMax - objectXMin;
                    //int objectHeight = objectYMax = objectYMin;
                    // TODO: Here is different from Matlab version to let object width contributes on all R for easier constraint implementation of F constraints
                    RY_STRONG_MU += objectWidth;
                    for (int y = objectYMin; y <= objectYMax; y++) {
                        RY[y] = 1;
                    }
                    for (int x = objectXMin; x <= objectXMax; x++) {
                        RX[x] = 1;
                    }
                }
                if (RY_STRONG_MU > bitmapOutputWidth * (1 - minPortionNonObjectRegionPreserve)) {
                    //TODO: Case that objects break out of deformed domain
                    // objectScaleRatio is same in deformed width and height
                    float objectRescaleRatio = bitmapOutputWidth * (1 - minPortionNonObjectRegionPreserve) / RY_STRONG_MU;
                    float nonObjectScaleRatioWidth = minPortionNonObjectRegionPreserve * bitmapOutputWidth / (bitmapSourceWidth - RY_STRONG_MU);

                    // get object region total height
                    int objectRegionHeight = 0;
                    for (int one : RY) {
                        objectRegionHeight += one;
                    }
                    float nonObjectScaleRatioHeight = (bitmapSourceHeight - objectRegionHeight * objectRescaleRatio) / (bitmapSourceHeight - objectRegionHeight);

                    for (int f = 0; f < faceDomain.length; f++) {
                        int v1Index = faceDomain[f][0];
                        int v2Index = faceDomain[f][1];
                        int v3Index = faceDomain[f][2];
                        int[] v1 = vertexDomain[v1Index];
                        int[] v2 = vertexDomain[v2Index];
                        int[] v3 = vertexDomain[v3Index];
                        if (RX[v1[0]] == 1 && RX[v2[0]] == 1 && RX[v3[0]] == 1) {
                            if (RY[v1[1]] == 1 && RY[v2[1]] == 1 && RY[v3[1]] == 1) {
                                mu[f][0] = 0;
                            } else {
                                mu[f][0] = (objectRescaleRatio - nonObjectScaleRatioHeight) / (objectRescaleRatio + nonObjectScaleRatioHeight);
                            }
                        } else {
                            mu[f][0] = (nonObjectScaleRatioWidth - nonObjectScaleRatioHeight) / (nonObjectScaleRatioWidth + nonObjectScaleRatioHeight);
                        }
                    }


                } else {
                    //TODO: Case that objects stays in deformed domain
                    //float widthRatio = (float) RY[0] / bitmapSourceWidth;
                    float widthRatio = (float) (bitmapOutputWidth - RY_STRONG_MU) / (bitmapSourceWidth - RY_STRONG_MU);
                    float backgroundMu = (widthRatio - 1) / (widthRatio + 1);
                    for (int f = 0; f < faceDomain.length; f++) {
                        int v1Index = faceDomain[f][0];
                        int v2Index = faceDomain[f][1];
                        int v3Index = faceDomain[f][2];
                        int v1X = vertexDomain[v1Index][0];
                        int v2X = vertexDomain[v2Index][0];
                        int v3X = vertexDomain[v3Index][0];
                        if (RX[v1X] == 1 || RX[v2X] == 1 || RX[v3X] == 1) {
                            mu[f][0] = 0;
                        } else {
                            mu[f][0] = backgroundMu;
                        }
                    }
                }
                break;
            // FIXME: Update pending:
            /***
             *                    MODEL_WEAK_MU
             *  */
            case MODEL_WEAK_MU:
                int[] RY_WEAK_MU = new int[bitmapSourceHeight];
                mu = new float[faceSelectedBoolean.length][2];
                for (int f = 0; f < faceSelectedBoolean.length; f++) {
                    if (!faceSelectedBoolean[f]) continue;
                    int[] v1 = {vertexDomain[faceDomain[f][0]][0], vertexDomain[faceDomain[f][0]][1]};
                    int[] v2 = {vertexDomain[faceDomain[f][1]][0], vertexDomain[faceDomain[f][1]][1]};
                    int[] v3 = {vertexDomain[faceDomain[f][2]][0], vertexDomain[faceDomain[f][2]][1]};
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
                            RY_WEAK_MU[y] += Math.abs(edgeX1 - edgeX2);
                        }
                    } else if (upperYVertex[1] == middleYVertex[1]) {
                        for (int y = bottomYVertex[1] + 1; y < middleYVertex[1]; y++) {
                            int edgeX1 = (y - bottomYVertex[1]) * (upperYVertex[0] - bottomYVertex[0])
                                    / (upperYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                            int edgeX2 = (y - bottomYVertex[1]) * (middleYVertex[0] - bottomYVertex[0])
                                    / (middleYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                            RY_WEAK_MU[y] += Math.abs(edgeX1 - edgeX2);
                        }
                    } else {
                        for (int y = bottomYVertex[1] + 1; y <= middleYVertex[1]; y++) {
                            int edgeX1 = (y - bottomYVertex[1]) * (upperYVertex[0] - bottomYVertex[0])
                                    / (upperYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                            int edgeX2 = (y - bottomYVertex[1]) * (middleYVertex[0] - bottomYVertex[0])
                                    / (middleYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                            RY_WEAK_MU[y] += Math.abs(edgeX1 - edgeX2);
                        }
                        for (int y = middleYVertex[1] + 1; y <= upperYVertex[1]; y++) {
                            int edgeX1 = (y - bottomYVertex[1]) * (upperYVertex[0] - bottomYVertex[0])
                                    / (upperYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                            int edgeX2 = (y - middleYVertex[1]) * (upperYVertex[0] - middleYVertex[0])
                                    / (upperYVertex[1] - middleYVertex[1]) + middleYVertex[0];
                            RY_WEAK_MU[y] += Math.abs(edgeX1 - edgeX2);
                        }
                    }
                }
                int RY_WEAK_MU_MAX = RY_WEAK_MU[0];
                for (int i = 0; i < bitmapSourceHeight; i++) {
                    if (RY_WEAK_MU_MAX < RY_WEAK_MU[i]) {
                        RY_WEAK_MU_MAX = RY_WEAK_MU[i];
                    }
                }
                if (RY_WEAK_MU_MAX > bitmapOutputWidth * (1 - minPortionNonObjectRegionPreserve)) {
                    //TODO: Case that objects break out of deformed domain
                    // objectScaleRatio is same in deformed width and height
                    // get object region total height
                    int objectRegionHeight = 0;
                    for (int one : RY) {
                        if(one != 0){
                            objectRegionHeight += 1;
                        }
                    }
                    float objectRescaleRatio = bitmapOutputWidth * (1 - minPortionNonObjectRegionPreserve) / RY_WEAK_MU_MAX;

                    for (int f = 0; f < faceDomain.length; f++) {
                        int v1Index = faceDomain[f][0];
                        int v2Index = faceDomain[f][1];
                        int v3Index = faceDomain[f][2];
                        int[] v1 = vertexDomain[v1Index];
                        int[] v2 = vertexDomain[v2Index];
                        int[] v3 = vertexDomain[v3Index];

                        float mu1RatioWidth = 1;
                        float mu2RatioWidth = 1;
                        float mu3RatioWidth = 1;
                        float mu1RatioHeight = 1;
                        float mu2RatioHeight = 1;
                        float mu3RatioHeight = 1;

                        if (faceSelectedBoolean[f]) {
                            // in object region
                        } else {
                            mu1RatioWidth = (bitmapOutputWidth - RY_WEAK_MU[v1[1]] * objectRescaleRatio) / (bitmapSourceWidth - RY_WEAK_MU[v1[1]]);
                            mu2RatioWidth = (bitmapOutputWidth - RY_WEAK_MU[v2[1]] * objectRescaleRatio) / (bitmapSourceWidth - RY_WEAK_MU[v2[1]]);
                            mu3RatioWidth = (bitmapOutputWidth - RY_WEAK_MU[v3[1]] * objectRescaleRatio) / (bitmapSourceWidth - RY_WEAK_MU[v3[1]]);
                            if(RY[v1[1]] != 0 && RY[v2[1]] != 0 && RY[v3[1]] != 0){
                                // around object region horizontally
                                mu1RatioHeight = objectRescaleRatio;
                                mu2RatioHeight = objectRescaleRatio;
                                mu3RatioHeight = objectRescaleRatio;
                            }else{
                                mu1RatioHeight = (bitmapSourceHeight - objectRegionHeight*objectRescaleRatio) / (bitmapSourceHeight - objectRegionHeight);
                                mu2RatioHeight = (bitmapSourceHeight - objectRegionHeight*objectRescaleRatio) / (bitmapSourceHeight - objectRegionHeight);
                                mu3RatioHeight = (bitmapSourceHeight - objectRegionHeight*objectRescaleRatio) / (bitmapSourceHeight - objectRegionHeight);
                            }
                        }
                        float mu1 = (mu1RatioWidth - mu1RatioHeight) / (mu1RatioWidth + mu1RatioHeight);
                        float mu2 = (mu2RatioWidth - mu2RatioHeight) / (mu2RatioWidth + mu2RatioHeight);
                        float mu3 = (mu3RatioWidth - mu3RatioHeight) / (mu3RatioWidth + mu3RatioHeight);
                        mu[f][0] = (mu1 + mu2 + mu3) / 3;
                    }
                    // use mono mu scheme
                } else {
                    //TODO: Case that objects stays in deformed domain
                    for (int f = 0; f < faceDomain.length; f++) {
                        if (!faceSelectedBoolean[f]) {
                            int v1Index = faceDomain[f][0];
                            int v2Index = faceDomain[f][1];
                            int v3Index = faceDomain[f][2];
                            float mu1RatioWidth = (float) (bitmapOutputWidth - RY_WEAK_MU[vertexDomain[v1Index][1]]) / (bitmapSourceWidth - RY_WEAK_MU[vertexDomain[v1Index][1]]);
                            float mu2RatioWidth = (float) (bitmapOutputWidth - RY_WEAK_MU[vertexDomain[v2Index][1]]) / (bitmapSourceWidth - RY_WEAK_MU[vertexDomain[v2Index][1]]);
                            float mu3RatioWidth = (float) (bitmapOutputWidth - RY_WEAK_MU[vertexDomain[v3Index][1]]) / (bitmapSourceWidth - RY_WEAK_MU[vertexDomain[v3Index][1]]);

                            float mu1 = (mu1RatioWidth - 1) / (mu1RatioWidth + 1);
                            float mu2 = (mu2RatioWidth - 1) / (mu2RatioWidth + 1);
                            float mu3 = (mu3RatioWidth - 1) / (mu3RatioWidth + 1);
                            mu[f][0] = (mu1 + mu2 + mu3) / 3;
                        }
                    }
                }
                break;
            /***
             *                    MODEL_MONO_MU
             *  */
            case MODEL_MONO_MU:
                mu = new float[faceSelectedBoolean.length][2];
                float ratio = (float) (bitmapOutputWidth + 1) / (bitmapSourceWidth + 1);
                float bkgdMu = (ratio - 1) / (ratio + 1);
                for (int i = 0; i < faceSelectedBoolean.length; i++) {
                    if (!faceSelectedBoolean[i]) {
                        mu[i][0] = bkgdMu;
                        mu[i][1] = 0;
                    }
                }
                break;
        }
        for (float[] value : mu) {
            //Log.d("debugging", String.valueOf(value[0]));
        }
        return mu;
    }

    @Deprecated
    public static float[][] constructMuOnFace(boolean[] faceSelectedBoolean, int bitmapSourceWidth, int bitmapOutputWidth) {

        float[][] mu = null;
        switch (MODEL) {
            case MODEL_MONO_MU:
                mu = new float[faceSelectedBoolean.length][2];
                float ratio = (float) (bitmapOutputWidth + 1) / (bitmapSourceWidth + 1);
                float bkgdMu = (ratio - 1) / (ratio + 1);
                for (int i = 0; i < faceSelectedBoolean.length; i++) {
                    if (!faceSelectedBoolean[i]) {
                        mu[i][0] = bkgdMu;
                        mu[i][1] = 0;
                    }
                }
                break;
            case MODEL_STRONG_MU:
                mu = new float[faceSelectedBoolean.length][2];
                break;
        }
        return mu;
    }


}
