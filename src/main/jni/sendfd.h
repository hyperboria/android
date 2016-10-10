#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

jint Java_berlin_meshnet_cjdns_FileDescriptorSender_sendfd(JNIEnv *env, jobject thiz, jstring path, jint file_descriptor);

#ifdef __cplusplus
}
#endif
