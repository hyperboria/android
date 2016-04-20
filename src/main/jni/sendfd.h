#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

jint Java_berlin_meshnet_cjdns_Cjdroute_sendfd(JNIEnv *env, jobject thiz, jstring path, jint tun_fd);

#ifdef __cplusplus
}
#endif
