#include "retargeter_final.h"
#include "lbs.h"
#include "interpolation.h"
#include <cmath>
extern "C"
{  
    // C++ wrapper of lbs for android java which is compiled by gcc core compiler
    JNIEXPORT jobjectArray JNICALL Java_com_example_root_retargeter_BitmapRetargeter_solveRetargeting
    (JNIEnv* env, jobject jobj, jobjectArray jVertices, jobjectArray jFaces, jobjectArray jMu, jfloatArray jBounds){
        
        int faceNum = (int) (env->GetArrayLength(jFaces));
        int vertexNum = (int) (env->GetArrayLength(jVertices));
        float vertices[vertexNum][2];
        int faces[faceNum][3];
        float mu[faceNum][2]; 
        float bounds[4];
        float solution[vertexNum][2];
        int i = 0;  
        
        for (;i<vertexNum; i++){ 
            jfloatArray temp_array = (jfloatArray) env -> GetObjectArrayElement(jVertices, i); 
            jfloat *pVertices =  env->GetFloatArrayElements(temp_array, 0);
            env->DeleteLocalRef(temp_array);
            vertices[i][0] = pVertices[0];
            vertices[i][1] = pVertices[1];
        }  
        
        for (i = 0;i<faceNum; i++){
            jintArray temp_array = (jintArray) env -> GetObjectArrayElement(jFaces, i);  
            jint *pFaces =  env->GetIntArrayElements(temp_array, 0); 
            env->DeleteLocalRef(temp_array); 
            faces[i][0] = pFaces[0];
            faces[i][1] = pFaces[1];
            faces[i][2] = pFaces[2];
            
            jfloatArray temptemp_array = (jfloatArray) env -> GetObjectArrayElement(jMu, i); 
            jfloat *pMu =  env->GetFloatArrayElements(temptemp_array, 0); 
            env->DeleteLocalRef(temptemp_array); 
            mu[i][0] = pMu[0];
            mu[i][1] = pMu[1];
        }  
        
        jfloat *pBounds = env->GetFloatArrayElements(jBounds, 0);
        for (i = 0;i<4; i++){
            bounds[i] = pBounds[i]; 
        }    
        
        // Core
        lbs::solveOnRect(vertices, vertexNum, faces, faceNum, mu, bounds, solution);
        
        
        
        int maxBoundXOnDomain = (int) round(bounds[1] - bounds[0]);
        int maxBoundYOnDomain = (int) round(bounds[3] - bounds[2]); 
        int* intepolation2dMapping_ptr = 0;  
        
        // change solution to use fixed-y schemes
        for (int i = 0; i < vertexNum; i++){
            solution[i][1] = vertices[i][1];
        }
        singleFace2DIntepolation(solution, vertexNum, faces, faceNum, vertices, intepolation2dMapping_ptr,
                maxBoundXOnDomain, maxBoundYOnDomain, _2DLinearInterpolation);  
        
        jclass intArrayClass = env->FindClass("[I"); 
        if (intArrayClass == NULL) {
            return NULL;  
        }           
        jintArray intArray = env->NewIntArray((jsize) 2);  
        jobjectArray colsMap = env->NewObjectArray((jsize) (maxBoundXOnDomain + 1), intArrayClass, NULL);
        jobjectArray rowsMap = env->NewObjectArray((jsize) (maxBoundYOnDomain + 1), env->GetObjectClass(colsMap), NULL); 
        for (int y = 0;y < maxBoundYOnDomain + 1; y++){
        for (int x = 0;x < maxBoundXOnDomain + 1; x++) {
            env->SetIntArrayRegion(intArray, 0, (jsize) 2, (jint*)intepolation2dMapping_ptr + 2*(maxBoundXOnDomain + 1)*y + 2*maxBoundXOnDomain); 
            env->SetObjectArrayElement(colsMap, (jsize) x, intArray); 
            env->SetObjectArrayElement(rowsMap, (jsize) y, colsMap); 
        }  
        }
        env->DeleteLocalRef(intArray);
        env->DeleteLocalRef(colsMap); 
        
        return rowsMap;
        
        /*
        jclass floatArrayClass = env->FindClass("[F");
        if (floatArrayClass == NULL) {
            return NULL;  
        } 
        jobjectArray jSolution = env->NewObjectArray((jsize) vertexNum, floatArrayClass, NULL);
        if (jSolution == NULL) {
            return NULL;  
        } 
        for (i = 0;i < vertexNum; i++) {
            jfloatArray floatArray = env->NewFloatArray((jsize) 2); 
            env->SetFloatArrayRegion(floatArray, 0, (jsize) 2, (jfloat*) solution[i]);
            env->SetObjectArrayElement(jSolution, (jsize) i, floatArray);
            env->DeleteLocalRef(floatArray); 
        }  
        return jSolution;  
         * */
    }
}



