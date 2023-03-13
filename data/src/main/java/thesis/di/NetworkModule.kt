package thesis.di

import com.demo.thesis.data.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import thesis.datasource.network.api.RemoteAPI
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val httpClient = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
        return httpClient
            .addInterceptor(provideLoggingInterceptor())
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                builder.addHeader("Authorization", BuildConfig.AUTH_TOKEN)
                val headerRequest = builder.build()
                chain.proceed(headerRequest)
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            if (BuildConfig.DEBUG) {
                level = HttpLoggingInterceptor.Level.BODY
            }
        }
    }

    @Singleton
    @Provides
    fun provideMeetingApiService(client: OkHttpClient): RemoteAPI {
        val retrofitBuilder =
            Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
        // create retrofit client with defaults
        val retrofit = retrofitBuilder
            .baseUrl(BuildConfig.BASE_MOBILE_URL)
            .client(client)
            .build()

        // create provided service and return
        return retrofit.create(RemoteAPI::class.java)
    }
}