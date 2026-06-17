package com.nary.gym.appfunctions

import android.content.Context
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Service to expose AppFunctions to the system.
 * Note: Requires Android 16 (API 36) or higher.
 */
@RequiresApi(36)
@AndroidEntryPoint
class GymAppFunctionService : AppFunctionService() {

    @Inject
    lateinit var gymAppFunctions: GymAppFunctions
    
    override suspend fun executeFunction(request: ExecuteAppFunctionRequest): ExecuteAppFunctionResponse {
        return try {
            if (request.functionIdentifier == GymAppFunctionsIds.SCHEDULE_GYM_REMINDER_ID) {
                // Accessing parameters from functionParameters
                val hour = request.functionParameters.getInt("hour")
                val minute = request.functionParameters.getInt("minute")

                val appFunctionContext = object : AppFunctionContext {
                    override val context: Context = this@GymAppFunctionService
                }

                val result = gymAppFunctions.scheduleGymReminder(
                    appFunctionContext,
                    hour,
                    minute
                )

                // Public way to create AppFunctionData for the response
                val responseData = AppFunctionData.serialize(result, GymReminderResponse::class.java)
                ExecuteAppFunctionResponse.Success(responseData)
            } else {
                ExecuteAppFunctionResponse.Error(AppFunctionFunctionNotFoundException(request.functionIdentifier))
            }
        } catch (e: Exception) {
            ExecuteAppFunctionResponse.Error(AppFunctionAppUnknownException(e.message))
        }
    }
}
