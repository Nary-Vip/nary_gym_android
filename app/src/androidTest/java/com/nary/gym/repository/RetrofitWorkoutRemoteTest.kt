package com.nary.gym.repository

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.remote.ApiService
import com.nary.gym.repository.remote.RetrofitWorkoutRemote
import com.nary.gym.repository.remote.WorkoutRemoteException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

@RunWith(AndroidJUnit4::class)
class RetrofitWorkoutRemoteTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val server = MockWebServer()
    private lateinit var remote: RetrofitWorkoutRemote

    @Before
    fun setUp() {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        server.useHttps(HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory())
        server.start()
        val clientCertificates = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val client = OkHttpClient.Builder()
            .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
            .build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        remote = RetrofitWorkoutRemote(api, context)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(code: Int, body: String) =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    @Test
    fun statusMapping() = runTest {
        val cases = listOf(
            404 to WorkoutRemoteException.NotFound,
            409 to WorkoutRemoteException.Conflict,
            429 to null, 500 to null, 503 to null,
        )
        for ((code, expected) in cases) {
            respond(code, """{"message":"x"}""")
            val error = runCatching { remote.delete("abc") }.exceptionOrNull()
            if (expected != null) assertEquals(expected, error) else assertTrue("$code", error is WorkoutRemoteException.Retryable)
        }
    }

    @Test
    fun validation400_isRejectedWithServerMessage() = runTest {
        respond(400, """{"errors":[{"message":"Name too short"}]}""")
        val error = runCatching { remote.delete("abc") }.exceptionOrNull()
        assertEquals(WorkoutRemoteException.Rejected("Name too short"), error)
    }

    @Test
    fun create_returnsServerId_andSendsMultipartFields() = runTest {
        val photo = File(context.cacheDir, "remote-test.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        try {
            respond(201, """{"message":"ok","workout":{"_id":"s9","workOutName":"Squat","workOutType":"LEG","workOutImage":"https://x/y.jpg"}}""")
            assertEquals("s9", remote.create("Squat", WorkOutType.LEG, photo.absolutePath))
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/workouts", request.url.encodedPath)
            val body = request.body!!.utf8()
            assertTrue(body.contains("name=\"workOutName\""))
            assertTrue(body.contains("name=\"workOutType\""))
            assertTrue(body.contains("name=\"image\""))
        } finally {
            photo.delete()
        }
    }

    @Test
    fun findIdByExactName_matchesCaseSensitively() = runTest {
        respond(200, """{"workouts":[{"_id":"a","workOutName":"squat","workOutType":"LEG","workOutImage":"/f/a.jpg"},{"_id":"b","workOutName":"Squat","workOutType":"LEG","workOutImage":"/f/b.jpg"}],"totalPages":1,"limit":100,"page":1,"totalWorkouts":2}""")
        assertEquals("b", remote.findIdByExactName("Squat"))
        assertEquals("50", server.takeRequest().url.queryParameter("limit"))
    }

    @Test
    fun findIdByExactName_looksBeyondTheFirstPage() = runTest {
        respond(200, """{"workouts":[{"_id":"a","workOutName":"Squat 2","workOutType":"LEG","workOutImage":"/f/a.jpg"}],"totalPages":2,"limit":50,"page":1,"totalWorkouts":2}""")
        respond(200, """{"workouts":[{"_id":"b","workOutName":"Squat","workOutType":"LEG","workOutImage":"/f/b.jpg"}],"totalPages":2,"limit":50,"page":2,"totalWorkouts":2}""")
        assertEquals("b", remote.findIdByExactName("Squat"))
    }

    @Test
    fun fetchAll_readsEveryPage() = runTest {
        respond(200, """{"workouts":[{"_id":"a","workOutName":"Squat","workOutType":"LEG","workOutImage":"/f/a.jpg"}],"totalPages":2,"limit":100,"page":1,"totalWorkouts":2}""")
        respond(200, """{"workouts":[{"_id":"b","workOutName":"Bench","workOutType":"CHEST","workOutImage":"/f/b.jpg"}],"totalPages":2,"limit":100,"page":2,"totalWorkouts":2}""")
        val rows = remote.fetchAll()
        assertEquals(listOf("a", "b"), rows.map { it.serverId })
        assertEquals(listOf("a", "b"), rows.map { it.id })
        assertEquals(2, server.requestCount)
        assertEquals(listOf("50", "50"), List(2) { server.takeRequest().url.queryParameter("limit") })
    }
}
