package mobiletv.repository.remote

import com.demo.thesis.data.BuildConfig
import mobiletv.datasource.network.APIHandlerBuilder
import mobiletv.datasource.network.api.RemoteAPI


open class LoggingRepository : BaseRepository<RemoteAPI>() {


    override fun createApiService(): RemoteAPI? =
        APIHandlerBuilder.getApiService(BuildConfig.BASE_MOBILE_URL)

}