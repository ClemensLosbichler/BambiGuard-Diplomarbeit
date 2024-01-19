#include <jni.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgcodecs.hpp>

#include "BambiGuard.h"
#include "at_ac_szybbs_bambiguard_model_BambiGuardDetector.h"

string detectBambisString(Mat image)
{
    __android_log_print(ANDROID_LOG_DEBUG, "kek", "JNI JNI function detectBambisString called");

	BambiGuardRecognition recognition = BambiGuardRecognition(25);

    __android_log_print(ANDROID_LOG_DEBUG, "kek", "JNI BambiGuardRecognition initialized");

    vector<Point> bambis = recognition.detectBambisInImage(image);
    string output = "";

    __android_log_print(ANDROID_LOG_DEBUG, "kek", "JNI bambis recognized");

    for (Point bambi : bambis)
        output += to_string(bambi.x) + "|" + to_string(bambi.y) + " ";

    __android_log_print(ANDROID_LOG_DEBUG, "kek", "JNI bambis converted to string");

    return output;
}

string matriceToString(Mat mat)
{
    string matString = "";

    for (int row = 0; row < mat.rows; ++row) {
        for (int col = 0; col < mat.cols; ++col) {
            matString += to_string(mat.at<unsigned char>(col, row)) + ", ";
        }
        matString += " ";
    }

    return matString;
}

extern "C" {
JNIEXPORT jstring JNICALL Java_at_ac_szybbs_bambiguard_model_BambiGuardDetector_detectBambisInImage
(JNIEnv * env, jobject obj, jint width, jint height, jbyteArray byteArrayParam)
{
    __android_log_print(ANDROID_LOG_DEBUG, "kek", "JNI method called");

    jbyte * byteArray = env->GetByteArrayElements(byteArrayParam, NULL);

    Mat mat = Mat(height, width, CV_8UC3, byteArray);

    __android_log_print(ANDROID_LOG_DEBUG, "kek", "mat builded");

    /*__android_log_print(ANDROID_LOG_DEBUG, "kek", "matrice %s", matriceToString(mat).c_str());*/

    return env->NewStringUTF("fuck u!");

    cvtColor(mat, mat, COLOR_BGR2GRAY);

    __android_log_print(ANDROID_LOG_DEBUG, "kek", "color converted to gray");

    string output = detectBambisString(mat);

    __android_log_print(ANDROID_LOG_DEBUG, "kek", "JNI detected bambis in matrix");

    env->ReleaseByteArrayElements(byteArrayParam, byteArray, JNI_ABORT);

    return env->NewStringUTF(output.c_str());
}
}