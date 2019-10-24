package ru.aradxxx.ciceronetabssample.di;

import android.app.Application;
import android.content.Context;

import ru.aradxxx.ciceronetabssample.MainActivity;

import androidx.annotation.NonNull;
import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public interface ActivityBindingModule {
    @ContributesAndroidInjector(modules = {
            AppActivityModule.class,
            FragmentBindingModule.class
    })
    MainActivity bindAppActivity();

    @Module()
    abstract class AppActivityModule {
        @Binds
        public abstract Context bindContext(@NonNull Application application);
    }
}