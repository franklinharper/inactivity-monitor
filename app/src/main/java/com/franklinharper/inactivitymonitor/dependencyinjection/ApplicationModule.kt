package com.franklinharper.inactivitymonitor.dependencyinjection

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
object AppModule {

//  @Provides
//  fun provideAnalyticsService(
//    // Potential dependencies of this type
//  ): AnalyticsService {
//    return Retrofit.Builder()
//      .baseUrl("https://example.com")
//      .build()
//      .create(AnalyticsService::class.java)
//  }
}