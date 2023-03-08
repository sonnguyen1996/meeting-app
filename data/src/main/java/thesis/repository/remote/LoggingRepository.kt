package thesis.repository.remote

import com.demo.thesis.data.BuildConfig
import thesis.datasource.network.APIHandlerBuilder
import thesis.datasource.network.api.RemoteAPI
import javax.inject.Singleton


@Singleton
open class LoggingRepository : BaseRepository<RemoteAPI>() {


    override fun createApiService(): RemoteAPI? =
        APIHandlerBuilder.getApiService(BuildConfig.BASE_MOBILE_URL)

}