package com.ss.challengetask.di

import com.ss.challengetask.datastore.TimerDataStore
import com.ss.challengetask.datastore.TimerPreference
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@InstallIn(ViewModelComponent::class)
@Module
abstract class UserPreferenceModule {

    @Binds
    abstract fun bindUserPreferences(impl: TimerDataStore): TimerPreference
}