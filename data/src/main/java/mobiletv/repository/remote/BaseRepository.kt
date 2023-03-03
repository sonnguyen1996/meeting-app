package mobiletv.repository.remote

abstract class BaseRepository<T> {

    protected var apiService: T? = null

    var tokenAuthenticate: String? = null

    init {
        apiService = createApiService()
    }



    protected abstract fun createApiService(): T?

}