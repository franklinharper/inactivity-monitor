package com.franklinharper.inactivitymonitor.dependencyinjection

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.franklinharper.inactivitymonitor.*
import com.franklinharper.inactivitymonitor.settings.SystemSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.bipi.tressence.file.FileLoggerTree
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object ApplicationModule {

  @Singleton
  @Provides
  fun provideSharedPreferences(@ApplicationContext application: Context): SharedPreferences {
    return PreferenceManager.getDefaultSharedPreferences(application)
  }

  @Singleton
  @Provides
  fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager {
    return context.getSystemService(NotificationManager::class.java)!!
  }

  @Singleton
  @Provides
  fun provideSystemSettings() = SystemSettings()

  @Singleton
  @Provides
  fun provideMovementRecognitionSubscriber() = MovementRecognitionSubscriber()

  @Singleton
  @Provides
  fun provideRemoteDb() = RemoteDb()

  @Singleton
  @Provides
  fun provideFileLoggerTree(@ApplicationContext application: Context): FileLoggerTree {
    val logDir = File(application.filesDir, "logs")
    if (!logDir.exists()) {
      logDir.mkdir()
    }
    return FileLoggerTree.Builder()
      .withFileName("file%g.log")
      .withDir(logDir)
      .withSizeLimit(50_000)
      .withFileLimit(3)
      .withMinPriority(Log.DEBUG)
      .appendToFile(true)
      .build()
  }

  @Singleton
  @Provides
  fun provideEventRepository(localDb: LocalDb, remoteDb: RemoteDb): EventRepository {
    return DbEventRepository(localDb, remoteDb)
  }
}