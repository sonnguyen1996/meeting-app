package thesis.di

import com.demo.thesis.data.BuildConfig
import dagger.Module
import dagger.Provides
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import thesis.datasource.network.api.RemoteAPI
import java.util.*
import java.util.concurrent.TimeUnit

@Module
class NetworkModule {

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)

    private val retrofitBuilder = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())

    @Provides
    fun provideMeetingApiService(service: OkHttpClient): RemoteAPI {
        val headers = getMeetingToken()
        httpClient.addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            for (headerKey in headers.keys) {
                val headerValue = headers[headerKey]
                if (!headerValue.isNullOrEmpty()) {
                    builder.addHeader(headerKey, headerValue)
                }
            }
            val headerRequest = builder.build()
            chain.proceed(headerRequest)
        }
        if (BuildConfig.DEBUG) {
           httpClient.addInterceptor(
                HttpLoggingInterceptor().setLevel(
                    HttpLoggingInterceptor.Level.BODY
                )
            )
        }

        val client = httpClient.build()
        // create retrofit client with defaults
        val retrofit = retrofitBuilder
            .baseUrl(BuildConfig.BASE_MOBILE_URL)
            .client(client)
            .build()

        // create provided service and return
        return retrofit.create(RemoteAPI::class.java)
    }

    private fun getMeetingToken(): Map<String, String> {
        val headMap: MutableMap<String, String> =
            LinkedHashMap()
        headMap["Authorization"] = BuildConfig.AUTH_TOKEN
        return headMap
    }
}