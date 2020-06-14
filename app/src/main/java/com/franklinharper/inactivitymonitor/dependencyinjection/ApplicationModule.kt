package com.franklinharper.inactivitymonitor.dependencyinjection

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

@Module
@InstallIn(ApplicationComponent::class)
object ApplicationModule {

  @Provides
  fun provideSharedPreferences(@ApplicationContext application: Context): SharedPreferences {
    return PreferenceManager.getDefaultSharedPreferences(application)
  }

  @Provides
  fun provideSystemSettings() = SystemSettings()

  @Provides
  fun provideMovementRecognitionSubscriber() = MovementRecognitionSubscriber()

  @Provides
  fun provideRemoteDb() = RemoteDb()

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

  @Provides
  fun provideEventRepository(localDb: LocalDb, remoteDb: RemoteDb): EventRepository {
    return DbEventRepository(localDb, remoteDb)
  }
}