#include "retargeter_final.h"
#include "lbs/lbs.h"
#include "lbs/lbs_retargeting.h"
#include "interpolation.h"
#include <android/log.h>
#include <cmath>
#include <string>
#include <sstream>

#ifndef TO_STRING
#define TO_STRING
template <typename T>
std::string to_string(T value)
{
    std::ostringstream os;
    os << value;
    return os.str();
}
#endif
extern "C"
{
    // C++ wrapper of lbs for android java which is compiled by gcc core compiler
    JNIEXPORT jobjectArray JNICALL Java_tech_silvermind_demo_retargeter_BitmapRetargeter_solveRetargeting(JNIEnv *env, jobject jobj, jobjectArray jFaces, jobjectArray jVertices, jobjectArray jMu, jfloatArray jBounds)
    {

        int faceNum = (int)(env->GetArrayLength(jFaces));
        int vertexNum = (int)(env->GetArrayLength(jVertices));
        float vertices[vertexNum][2];
        int faces[faceNum][3];
        float mu[faceNum][2];
        float bounds[4];
        float solution[vertexNum][2];

        int i = 0;
        for (; i < vertexNum; i++)
        {
            jfloatArray temp_array = (jfloatArray)env->GetObjectArrayElement(jVertices, i);
            jfloat *pVertices = env->GetFloatArrayElements(temp_array, 0);
            vertices[i][0] = pVertices[0];
            vertices[i][1] = pVertices[1];
            env->ReleaseFloatArrayElements(temp_array, pVertices, 0);
            env->DeleteLocalRef(temp_array);
        }
        env->DeleteLocalRef(jVertices);

        for (i = 0; i < faceNum; i++)
        {
            jintArray temp_array = (jintArray)env->GetObjectArrayElement(jFaces, i);
            jint *pFaces = env->GetIntArrayElements(temp_array, 0);
            faces[i][0] = pFaces[0];
            faces[i][1] = pFaces[1];
            faces[i][2] = pFaces[2];
            env->ReleaseIntArrayElements(temp_array, pFaces, 0);
            env->DeleteLocalRef(temp_array);

            jfloatArray temptemp_array = (jfloatArray)env->GetObjectArrayElement(jMu, i);
            jfloat *pMu = env->GetFloatArrayElements(temptemp_array, 0);
            mu[i][0] = pMu[0];
            mu[i][1] = pMu[1];
            env->ReleaseFloatArrayElements(temptemp_array, pMu, 0);
            env->DeleteLocalRef(temptemp_array);
        }
        env->DeleteLocalRef(jFaces);
        env->DeleteLocalRef(jMu);

        jfloat *pBounds = env->GetFloatArrayElements(jBounds, 0);
        for (i = 0; i < 4; i++)
        {
            bounds[i] = pBounds[i];
        }
        env->ReleaseFloatArrayElements(jBounds, pBounds, 0);
        env->DeleteLocalRef(jBounds);

        // Core
        lbs::solveOnRect(vertices, vertexNum, faces, faceNum, mu, bounds, solution);
        //lbs_retargeting::solveOnRect(vertices, vertexNum, faces, faceNum, mu, bounds, solution);

        // change solution to int type
        int intSolution[vertexNum][2];
        for (int i = 0; i < vertexNum; i++)
        {
            intSolution[i][0] = (int)round(solution[i][0]);
            intSolution[i][1] = (int)round(solution[i][1]);
        }

        jclass intArrayClass = env->FindClass("[I");
        if (intArrayClass == NULL)
        {
            return NULL;
        }
        jobjectArray jSolution = env->NewObjectArray((jsize)vertexNum, intArrayClass, NULL);
        if (jSolution == NULL)
        {
            return NULL;
        }
        for (i = 0; i < vertexNum; i++)
        {
            jintArray intArray = env->NewIntArray((jsize)2);
            env->SetIntArrayRegion(intArray, 0, (jsize)2, (int *)intSolution[i]);
            env->SetObjectArrayElement(jSolution, (jsize)i, intArray);
            env->DeleteLocalRef(intArray);
        }
        return jSolution;
    }

    JNIEXPORT jobjectArray JNICALL Java_tech_silvermind_demo_retargeter_BitmapRetargeter_interpolateImage(JNIEnv *env, jobject jobj, jobjectArray jFaces, jobjectArray jVerticesDomain, jobjectArray jVerticesMapping, jint maxBdX, jint maxBdY)
    {

        int faceNum = (int)(env->GetArrayLength(jFaces));
        int vertexNum = (int)(env->GetArrayLength(jVerticesDomain));
        float verticesDomain[vertexNum][2];
        float verticesMapping[vertexNum][2];
        int faces[faceNum][3];
        int maxBoundXOnDomain = (int)maxBdX;
        int maxBoundYOnDomain = (int)maxBdY;
        int i = 0;

        for (; i < vertexNum; i++)
        {
            jintArray temp_array_1 = (jintArray)env->GetObjectArrayElement(jVerticesDomain, i);
            jint *pVertices = env->GetIntArrayElements(temp_array_1, 0);
            verticesDomain[i][0] = (float)pVertices[0];
            verticesDomain[i][1] = (float)pVertices[1];
            env->ReleaseIntArrayElements(temp_array_1, pVertices, 0);
            env->DeleteLocalRef(temp_array_1);

            jintArray temp_array_2 = (jintArray)env->GetObjectArrayElement(jVerticesMapping, i);
            jint *ppVertices = env->GetIntArrayElements(temp_array_2, 0);
            verticesMapping[i][0] = (float)ppVertices[0];
            verticesMapping[i][1] = (float)ppVertices[1];
            env->ReleaseIntArrayElements(temp_array_2, ppVertices, 0);
            env->DeleteLocalRef(temp_array_2);
        }
        env->DeleteLocalRef(jVerticesMapping);
        env->DeleteLocalRef(jVerticesDomain);

        for (i = 0; i < faceNum; i++)
        {
            jintArray temp_face_array = (jintArray)env->GetObjectArrayElement(jFaces, i);
            int *pFaces = env->GetIntArrayElements(temp_face_array, 0);
            faces[i][0] = pFaces[0];
            faces[i][1] = pFaces[1];
            faces[i][2] = pFaces[2];
            env->ReleaseIntArrayElements(temp_face_array, pFaces, 0);
            env->DeleteLocalRef(temp_face_array);
        }
        env->DeleteLocalRef(jFaces);

        //__android_log_write(ANDROID_LOG_DEBUG, "debugging", ("ff: "+  to_string(maxBoundYOnDomain)+" " +  to_string(maxBoundXOnDomain)).c_str());

        // TODO: UPDATE PENDING: Multithread billinear interpolation
        int ***intepolation2dMapping_ptr = 0;
        singleFace2DIntepolation(verticesDomain, vertexNum, faces, faceNum, verticesMapping, intepolation2dMapping_ptr,
                                 maxBoundXOnDomain, maxBoundYOnDomain, _2DLinearInterpolation);
        // debug interpolation

        /*
        for (int y = 0;y < maxBoundYOnDomain + 1; y++){  
            for (int x = 0;x < maxBoundXOnDomain + 1; x++) { 
                __android_log_write(ANDROID_LOG_DEBUG, "debugging", (to_string(y)+" " +to_string(x)+": " +to_string(intepolation2dMapping_ptr[y][x][0])+" " + to_string(intepolation2dMapping_ptr[y][x][1])).c_str()); 
            }   
        }
         */

        jclass intArrayClass = env->FindClass("[I");
        if (intArrayClass == NULL)
        {
            return NULL;
        }
        jobjectArray colsMap_temp = env->NewObjectArray((jsize)(maxBoundXOnDomain + 1), intArrayClass, NULL);
        jobjectArray rowsMap = env->NewObjectArray((jsize)(maxBoundYOnDomain + 1), env->GetObjectClass(colsMap_temp), NULL);

        for (int y = 0; y < maxBoundYOnDomain + 1; y++)
        {
            jobjectArray colsMap = env->NewObjectArray((jsize)(maxBoundXOnDomain + 1), intArrayClass, NULL);
            for (int x = 0; x < maxBoundXOnDomain + 1; x++)
            {
                jintArray intArray = env->NewIntArray((jsize)2);
                env->SetIntArrayRegion(intArray, 0, 2, (jint *)intepolation2dMapping_ptr[y][x]);
                env->SetObjectArrayElement(colsMap, x, intArray);
                env->DeleteLocalRef(intArray);
            }
            env->SetObjectArrayElement(rowsMap, (jsize)y, colsMap);
            env->DeleteLocalRef(colsMap);
        }

        delete3DArray(intepolation2dMapping_ptr, maxBoundYOnDomain + 1, maxBoundXOnDomain + 1);
        return rowsMap;
    }
}
