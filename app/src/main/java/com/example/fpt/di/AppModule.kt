package com.example.fpt.di


import com.demo.domain.domain.usecase.MeetingInteraction
import com.demo.domain.domain.usecase.meeting_interface.MeetingUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun provideMovieUseCase(meetingUserCase: MeetingInteraction): MeetingUseCase
}