#include <android/log.h>
#include <jni.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <errno.h>

#include "sendfd.h"

jint Java_berlin_meshnet_cjdns_FileDescriptorSender_sendfd(JNIEnv *env, jobject thiz, jstring path, jint file_descriptor)
{
    int fd, len, err, rval;
    const char *pipe_path = env->GetStringUTFChars(path, 0);
    struct sockaddr_un un;
    char buf[256];

#ifndef NDEBUG
    __android_log_print(ANDROID_LOG_DEBUG, "FileDescriptorSender", "sendfd() called with [%s] [%d]", pipe_path, file_descriptor);
#endif

    if ((fd = socket(AF_UNIX, SOCK_STREAM, 0)) < 0) {
#ifndef NDEBUG
        char *errstr = strerror(errno);
        __android_log_print(ANDROID_LOG_DEBUG, "FileDescriptorSender", "sendfd socket() failed [%s]", errstr);
#endif
        return (jint)-1;
    }

    memset(&un, 0, sizeof(un));
    un.sun_family = AF_UNIX;
    strcpy(un.sun_path, pipe_path);
    if (connect(fd, (struct sockaddr *)&un, sizeof(struct sockaddr_un)) < 0) {
#ifndef NDEBUG
        char *errstr = strerror(errno);
        __android_log_print(ANDROID_LOG_DEBUG, "FileDescriptorSender", "sendfd connect() failed [%s]", errstr);
#endif
        close(fd);
        return (jint)-1;
    }

    struct msghdr msg;
    struct iovec iov[1];

    union {
        struct cmsghdr cm;
        char control[CMSG_SPACE(sizeof(int))];
    } control_un;
    struct cmsghdr *cmptr;

    msg.msg_control = control_un.control;
    msg.msg_controllen = sizeof(control_un.control);

    cmptr = CMSG_FIRSTHDR(&msg);
    cmptr->cmsg_len = CMSG_LEN(sizeof(int));
    cmptr->cmsg_level = SOL_SOCKET;
    cmptr->cmsg_type = SCM_RIGHTS;
    *((int *) CMSG_DATA(cmptr)) = file_descriptor;

    msg.msg_name = NULL;
    msg.msg_namelen = 0;

    iov[0].iov_base = buf;
    iov[0].iov_len = sizeof(buf);
    msg.msg_iov = iov;
    msg.msg_iovlen = 1;

    int r;
    if ((r = sendmsg(fd, &msg, MSG_NOSIGNAL)) < 0) {
#ifndef NDEBUG
        char *errstr = strerror(errno);
        __android_log_print(ANDROID_LOG_DEBUG, "FileDescriptorSender", "sendfd sendmsg() failed [%s]", errstr);
#endif
        close(fd);
        return (jint)-1;
    }

    close(fd);
    return (jint)0;
}
