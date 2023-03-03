package mobiletv.datasource.network

import com.demo.thesis.data.BuildConfig
import mobiletv.datasource.network.api.RemoteAPI
import mobiletv.datasource.network.provider.RetrofitService
import java.util.*

class APIHandlerBuilder {
    companion object {
        private var clientAPIService: RemoteAPI? = null

        private var authenToken = BuildConfig.AUTH_TOKEN

        @Synchronized
        fun getApiService(url: String): RemoteAPI? {
            if (clientAPIService == null) {
                clientAPIService = initApiService(url)
            }
            return clientAPIService
        }

        private fun initApiService(url: String): RemoteAPI {
            return RetrofitService.create(RemoteAPI::class.java, url, getMeetingToken())
        }

        private fun getMeetingToken(): Map<String, String> {
            val headMap: MutableMap<String, String> =
                LinkedHashMap()
            headMap["Authorization"] = authenToken
            return headMap
        }
    }


}

