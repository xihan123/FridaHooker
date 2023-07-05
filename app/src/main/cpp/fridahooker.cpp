#include <jni.h>
#include <stdlib.h>
#include <android/log.h>

extern "C"
JNIEXPORT jint JNICALL
Java_cn_xihan_fridahooker_util_Utils_execute(JNIEnv *env, jobject thiz, jstring cmd) {
    const char *ccmd = env->GetStringUTFChars(cmd, nullptr);
    int ret_code = system(ccmd);
    env->ReleaseStringUTFChars(cmd, ccmd);
//    __android_log_print(ANDROID_LOG_DEBUG, "FridaHooker", "execute cmd:%s, return code:%d", ccmd,ret_code);
    return ret_code;
}


