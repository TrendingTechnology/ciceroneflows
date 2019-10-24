package ru.aradxxx.ciceronetabssample.di;

import ru.aradxxx.ciceronetabssample.tabfragment.Tab1;
import ru.aradxxx.ciceronetabssample.tabfragment.Tab2;
import ru.aradxxx.ciceronetabssample.tabfragment.Tab3;
import ru.aradxxx.ciceronetabssample.tabfragment.TabContainerFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public interface FragmentBindingModule {
    @ContributesAndroidInjector
    Tab1 provideTab1Fragment();

    @ContributesAndroidInjector
    Tab2 provideTab2Fragment();

    @ContributesAndroidInjector
    Tab3 provideTab3Fragment();

    @ContributesAndroidInjector
    TabContainerFragment provideTabContainerFragment();
}