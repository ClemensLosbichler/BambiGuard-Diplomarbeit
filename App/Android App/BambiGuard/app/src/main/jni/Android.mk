LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
OPENCVROOT := C:\Users\Clemens\OpenCV-android-sdk
include $(OPENCVROOT)\sdk\native\jni\OpenCV.mk

LOCAL_SRC_FILES := bambiGuardDetector.cpp
LOCAL_LDLIBS += -llog
LOCAL_MODULE := bambiGuardDetector

include $(BUILD_SHARED_LIBRARY)