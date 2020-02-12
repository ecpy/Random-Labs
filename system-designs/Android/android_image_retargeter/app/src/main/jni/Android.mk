LOCAL_PATH := $(call my-dir) 
include $(CLEAR_VARS)
LOCAL_MODULE    := retargeter
LOCAL_LDLIBS := -llog
LOCAL_SRC_FILES := retargeter_final.cpp  
LOCAL_C_INCLUDES:= $(LOCAL_PATH)/lbs
LOCAL_C_INCLUDES+= $(LOCAL_PATH)
LOCAL_SRC_FILES := retargeter_final.cpp 
include $(BUILD_SHARED_LIBRARY)

