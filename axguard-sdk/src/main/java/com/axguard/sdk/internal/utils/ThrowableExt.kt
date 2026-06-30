package com.axguard.sdk.internal.utils

import com.axguard.sdk.api.models.threats.SecurityCheckErrorKind
import java.io.IOException

internal fun Throwable.toErrorKind(): @SecurityCheckErrorKind Int = when (this) {
    is IOException -> SecurityCheckErrorKind.IO
    is SecurityException -> SecurityCheckErrorKind.SECURITY
    is InterruptedException -> SecurityCheckErrorKind.INTERRUPTED
    else -> SecurityCheckErrorKind.INTERNAL
}
