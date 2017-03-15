// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

#ifdef __cplusplus
#include <cstdlib>
#include <cstddef>
#include <cstdbool>
#include <cstdint>
#else
#include <stdlib.h>
#include <stddef.h>
#include <stdbool.h>
#include <stdint.h>
#endif

#ifdef _CRTDBG_MAP_ALLOC
#include <crtdbg.h>
#endif


#include "testrunnerswitcher.h"
#include "umock_c.h"
#include "umock_c_negative_tests.h"
#include "umocktypes_charptr.h"
#include "umocktypes_stdint.h"
#include "umock_c_prod.h"

#define NN_EXPORT

#include <nn.h>
#include <pair.h>
static TEST_MUTEX_HANDLE g_testByTest;
static TEST_MUTEX_HANDLE g_dllByDll;

#define ENABLE_MOCKS
#include <jni.h>
#include "java_nanomsg.h"


//=============================================================================
//Globals
//=============================================================================

static JNIEnv* global_env = (JNIEnv*)0x42;

//=============================================================================
//MOCKS
//=============================================================================

//JNIEnv function mocks
MOCK_FUNCTION_WITH_CODE(JNICALL, jclass, FindClass, JNIEnv*, env, const char*, name)
jclass clazz = (jclass)0x42;
MOCK_FUNCTION_END(clazz)

MOCK_FUNCTION_WITH_CODE(JNICALL, jclass, GetObjectClass, JNIEnv*, env, jobject, obj);
jclass clazz = (jclass)0x42;
MOCK_FUNCTION_END(clazz)

MOCK_FUNCTION_WITH_CODE(JNICALL, jmethodID, GetMethodID, JNIEnv*, env, jclass, clazz, const char*, name, const char*, sig);
jmethodID methodID = (jmethodID)0x42;
MOCK_FUNCTION_END(methodID)

MOCK_FUNCTION_WITH_CODE(JNICALL, jstring, NewStringUTF, JNIEnv*, env, const char*, utf);
jstring jstr = (jstring)utf;
MOCK_FUNCTION_END(jstr)

MOCK_FUNCTION_WITH_CODE(JNICALL, const char *, GetStringUTFChars, JNIEnv*, env, jstring, string, jboolean*, isCopy);
const char *stringChars = "Test";
MOCK_FUNCTION_END(stringChars)

MOCK_FUNCTION_WITH_CODE(JNICALL, jsize, GetArrayLength, JNIEnv*, env, jarray, arr);
jsize size = sizeof(arr);
MOCK_FUNCTION_END(size)

MOCKABLE_FUNCTION(JNICALL, jbyteArray, NewByteArray, JNIEnv*, env, jsize, len);
jbyteArray my_NewByteArray(JNIEnv* env, jsize len)
{
    return (jbyteArray)malloc(1);
}

MOCK_FUNCTION_WITH_CODE(JNICALL, void, GetByteArrayRegion, JNIEnv*, env, jbyteArray, arr, jsize, start, jsize, len, jbyte*, buf);
MOCK_FUNCTION_END()

MOCKABLE_FUNCTION(JNICALL, void, DeleteLocalRef, JNIEnv*, env, jobject, obj);
void my_DeleteLocalRef(JNIEnv* env, jobject obj)
{
    free((void*)obj);
}

MOCK_FUNCTION_WITH_CODE(JNICALL, jthrowable, ExceptionOccurred, JNIEnv*, env);
MOCK_FUNCTION_END(NULL)

MOCK_FUNCTION_WITH_CODE(JNICALL, void, ExceptionClear, JNIEnv*, env);
MOCK_FUNCTION_END()

struct JNINativeInterface_ env = {
    0, 0, 0, 0,

    NULL, NULL, FindClass, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, ExceptionOccurred, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NewStringUTF, NULL, GetStringUTFChars, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
};

#undef ENABLE_MOCKS

//Nanomsg mocks
MOCK_FUNCTION_WITH_CODE(, int, nn_bind, int, s, const char *, addr)
MOCK_FUNCTION_END(0)

MOCK_FUNCTION_WITH_CODE(, int, nn_close, int, s)
MOCK_FUNCTION_END(0)

MOCK_FUNCTION_WITH_CODE(, int, nn_errno)
MOCK_FUNCTION_END(0)

MOCK_FUNCTION_WITH_CODE(, int, nn_freemsg, void *, msg)
MOCK_FUNCTION_END(0)

MOCK_FUNCTION_WITH_CODE(, int, nn_recv, int, s, void *, buf, size_t, len, int, flags)
MOCK_FUNCTION_END(0)

MOCK_FUNCTION_WITH_CODE(, int, nn_send, int, s, const void *, buf, size_t, len, int, flags)
MOCK_FUNCTION_END(0)

MOCK_FUNCTION_WITH_CODE(, int, nn_shutdown, int, s, int, how)
MOCK_FUNCTION_END(0)

MOCK_FUNCTION_WITH_CODE(, int, nn_socket, int, domain, int, protocol)
MOCK_FUNCTION_END(0)

MOCK_FUNCTION_WITH_CODE(, const char *, nn_strerror, int, errnum)
MOCK_FUNCTION_END(0)

MOCK_FUNCTION_WITH_CODE(, const char *, nn_symbol, int, i, int*, value)
MOCK_FUNCTION_END(0)

DEFINE_ENUM_STRINGS(UMOCK_C_ERROR_CODE, UMOCK_C_ERROR_CODE_VALUES)

static
void
on_umock_c_error(
    UMOCK_C_ERROR_CODE error_code
) {
    char temp_str[256];
    (void)snprintf(temp_str, sizeof(temp_str), "umock_c reported error :%s", ENUM_TO_STRING(UMOCK_C_ERROR_CODE, error_code));
    ASSERT_FAIL(temp_str);
}

BEGIN_TEST_SUITE(JavaNanomsg_UnitTests)

TEST_SUITE_INITIALIZE(TestClassInitialize)
{
    int result;
    TEST_INITIALIZE_MEMORY_DEBUG(g_dllByDll);
    g_testByTest = TEST_MUTEX_CREATE();

    result = umock_c_init(on_umock_c_error);
    ASSERT_ARE_EQUAL(int, 0, result);
    //umock_c_init(on_umock_c_error);
    umocktypes_charptr_register_types();
    umocktypes_stdint_register_types();

    REGISTER_UMOCK_ALIAS_TYPE(jint, int32_t);
    REGISTER_UMOCK_ALIAS_TYPE(jclass, void*);
    REGISTER_UMOCK_ALIAS_TYPE(jmethodID, void*);
    REGISTER_UMOCK_ALIAS_TYPE(jobject, void*);
    REGISTER_UMOCK_ALIAS_TYPE(jsize, int);
    REGISTER_UMOCK_ALIAS_TYPE(jthrowable, int);
    REGISTER_UMOCK_ALIAS_TYPE(jbyteArray, void*);
    REGISTER_UMOCK_ALIAS_TYPE(jsize, int);
    REGISTER_UMOCK_ALIAS_TYPE(const jbyte*, void*);
    REGISTER_UMOCK_ALIAS_TYPE(jarray, void*);
    REGISTER_UMOCK_ALIAS_TYPE(JNIEnv*, void*);
  
    REGISTER_UMOCK_ALIAS_TYPE(const char*, char*);

    REGISTER_UMOCK_ALIAS_TYPE(void**, void*);
    REGISTER_UMOCK_ALIAS_TYPE(va_list, void*);

#ifdef __cplusplus

    global_env = new JNIEnv();
    ((JNIEnv*)(global_env))->functions = new JNINativeInterface_(env);

#else

    global_env = (void*)((JNIEnv*)malloc(sizeof(JNIEnv)));
    *((JNIEnv*)(*global_env)) = malloc(sizeof(struct JNINativeInterface_));
    *(struct JNINativeInterface_*)(*((JNIEnv*)(*global_env))) = env;
#endif
   
}

TEST_SUITE_CLEANUP(TestClassCleanup)
{
    umock_c_deinit();

    TEST_MUTEX_DESTROY(g_testByTest);
    TEST_DEINITIALIZE_MEMORY_DEBUG(g_dllByDll);
}

TEST_FUNCTION_INITIALIZE(TestMethodInitialize)
{
    if (TEST_MUTEX_ACQUIRE(g_testByTest))
    {
        ASSERT_FAIL("our mutex is ABANDONED. Failure in test framework");
    }

    umock_c_reset_all_calls();
}

TEST_FUNCTION_CLEANUP(TestMethodCleanup)
{
    global_env = NULL;
    TEST_MUTEX_RELEASE(g_testByTest);
}

TEST_FUNCTION(Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1errno_success)
{
    //Arrange
    umock_c_reset_all_calls();

    jobject jObject = (jobject)0x42;
    jint err = (jint)21;
    jclass clazz = (jclass)0x42;

    STRICT_EXPECTED_CALL(nn_errno())
        .SetReturn(EAGAIN);
  
    //Act
    jint result = Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1errno(global_env, jObject);

    //Assert
    ASSERT_ARE_EQUAL(int32_t, EAGAIN, result);
}

TEST_FUNCTION(Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1strerror_success)
{
    //Arrange
    umock_c_reset_all_calls();

    jobject jObject = (jobject)0x42;
    jint err = (jint)21;
    jclass clazz = (jclass)0x42;
    const char* error = "Error";

    STRICT_EXPECTED_CALL(nn_strerror(err))
        .SetReturn(error);
    STRICT_EXPECTED_CALL(NewStringUTF(IGNORED_PTR_ARG, error))
        .IgnoreArgument(1);
    STRICT_EXPECTED_CALL(ExceptionOccurred(IGNORED_PTR_ARG))
        .IgnoreArgument(1);

    //Act
    jstring result = Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1strerror(global_env, jObject, err);
}

TEST_FUNCTION(Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1strerror_should_return_empty)
{
    //Arrange
    umock_c_reset_all_calls();

    jobject jObject = (jobject)0x42;
    jint err = (jint)21;
    jclass clazz = (jclass)0x42;
    const char* error = "Error";

    STRICT_EXPECTED_CALL(nn_strerror(err))
        .SetReturn(0);
    STRICT_EXPECTED_CALL(NewStringUTF(IGNORED_PTR_ARG, error))
        .IgnoreArgument(1);
    STRICT_EXPECTED_CALL(ExceptionOccurred(IGNORED_PTR_ARG))
        .IgnoreArgument(1);

    //Act
    jstring result = Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1strerror(global_env, jObject, err);
}

TEST_FUNCTION(Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1socket_success)
{
    //Arrange
    umock_c_reset_all_calls();

    jobject jObject = (jobject)0x42;
    jint domain = (jint)AF_SP;
    jint protocol = (jint)NN_PAIR;
    jint socket = (jint)1;
    STRICT_EXPECTED_CALL(nn_socket(domain, protocol))
        .SetReturn(socket);

    //Act
    jint result = Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1socket(global_env, jObject, domain, protocol);

    //Assert
    ASSERT_ARE_EQUAL(int32_t, socket, result);
}

TEST_FUNCTION(Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1close_success)
{
    //Arrange
    umock_c_reset_all_calls();

    jobject jObject = (jobject)0x42;
    jint socket = (jint)1;
    jint expectedResult = (jint)0;
    STRICT_EXPECTED_CALL(nn_close(socket))
        .SetReturn(expectedResult);

    //Act
    jint result = Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1close(global_env, jObject, socket);

    //Assert
    ASSERT_ARE_EQUAL(int32_t, expectedResult, result);
}

TEST_FUNCTION(Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1bind_success)
{
    //Arrange
    umock_c_reset_all_calls();

    jobject jObject = (jobject)0x42;
    jint socket = (jint)1;
    char* address = "control_id";
    jint endpointId = (jint)0;
    STRICT_EXPECTED_CALL(nn_bind(socket, address))
        .SetReturn(endpointId);

    //Act
    jint result = Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1bind(global_env, jObject, socket, (jstring)address);

    //Assert
    ASSERT_ARE_EQUAL(int32_t, endpointId, result);
}

TEST_FUNCTION(Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1shutdown_success)
{
    //Arrange
    umock_c_reset_all_calls();

    jobject jObject = (jobject)0x42;
    jint socket = (jint)1;
    jint endpoint = (jint)0;
    jint expectedResult = (jint)0;
    STRICT_EXPECTED_CALL(nn_shutdown(socket, endpoint))
        .SetReturn(expectedResult);

    //Act
    jint result = Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1shutdown(global_env, jObject, socket, endpoint);

    //Assert
    ASSERT_ARE_EQUAL(int32_t, expectedResult, result);
}

TEST_FUNCTION(Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1send_success)
{
    //Arrange
    umock_c_reset_all_calls();

    jobject jObject = (jobject)0x42;
    jint socket = (jint)1;
    jbyteArray buffer = (jbyteArray)0x42;
    jint flags = (jint)1;
    jint expectedResult = (jint)5;
    STRICT_EXPECTED_CALL(nn_send(socket, buffer, 5, flags))
        .SetReturn(expectedResult);

    //Act
    jint result = Java_com_microsoft_azure_gateway_remote_NanomsgLibrary_nn_1send(global_env, jObject, socket, buffer,  flags);

    //Assert
    // ASSERT_ARE_EQUAL(int32_t, expectedResult, result);
}


END_TEST_SUITE(JavaNanomsg_UnitTests);
