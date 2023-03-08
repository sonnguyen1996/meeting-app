package thesis.datasource.network.provider

import com.demo.thesis.data.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class RetrofitService {

    companion object {

        private val httpClient = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)

        /**
         * Valid instance of [retrofit2.Retrofit.Builder] for reuse across
         * retrofit instances.
         *
         */
        private val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())

        /**
         * Create an implementation of the API endpoints defined by the `service` interface.
         *
         * @param service      valid retrofit service definition
         * @param baseUrl      valid service base url
         * @param authProvider valid authentication provider
         * @param headers      valid http headers to apply on every request
         * @return an object of type S from the `service` creation
         */
        fun <S> create(
            service: Class<S>?,
            baseUrl: String,
            headers: Map<String, String>
        ): S {
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
                .baseUrl(baseUrl)
                .client(client)
                .build()

            // create provided service and return
            return retrofit.create(service)
        }
    }
}