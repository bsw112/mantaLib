package com.manta.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

sealed class NetWorkException(msg: String? = null) : Throwable(msg)
data class InvalidResponseException(val invalidData: Any) : NetWorkException(invalidData.toString())
data class HttpStatusException(val statusCode: Int) : NetWorkException("[statusCode : $statusCode]")
class NullResponseException() : NetWorkException()


suspend fun <T : Any> processNetwork(
    ioDispatcher: CoroutineDispatcher,
    netWorkCall: suspend () -> Response<T>,
    dataValidator: (T) -> Boolean = { true }
): Result<T> {
    return withContext(ioDispatcher) {
        try {
            val response = netWorkCall()
            val body = response.body() ?: throw NullResponseException()
            if (response.isSuccessful) {
                if (dataValidator(body)) {
                    Result.success(body)
                } else {
                    Result.failure(InvalidResponseException(body))
                }
            } else {
                Result.failure(HttpStatusException(response.code()))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}

fun <T> ViewModel.updateUiState(
    state: MutableStateFlow<UiState<T>>,
    networkCall: suspend () -> Result<T>
) {
    viewModelScope.launch {
        state.value = UiState.loading()
        networkCall()
            .onSuccess { result ->
                state.value = UiState.success(result)
            }.onFailure { t ->
                state.value = UiState.error(t)
            }
    }
}