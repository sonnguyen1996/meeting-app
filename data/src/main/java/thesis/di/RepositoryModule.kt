package thesis.di

import com.demo.domain.domain.repository.IMeetingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import thesis.repository.remote.MeetingRepository

@InstallIn(SingletonComponent::class)
@Module
abstract class RepositoryModule {
    @Binds
    abstract fun provideMeetingRepository(repository: MeetingRepository): IMeetingRepository
}