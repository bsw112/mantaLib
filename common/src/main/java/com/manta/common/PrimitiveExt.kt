package com.manta.common

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

fun <T> List<T>?.toSafe() : List<T> {
    return this ?: emptyList()
}

fun Boolean?.toSafe() : Boolean {
    return this ?: false
}

fun Int?.toSafe() : Int{
    return this ?: 0
}

fun String?.toSafe() : String{
    return this ?: ""
}

@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
inline fun <T> sdkAndUp(sdkVersion: Int, onSdk: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= sdkVersion) {
        onSdk()
    } else null
}
