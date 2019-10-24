package ru.aradxxx.ciceronetabs;

import android.content.Intent;
import android.os.Bundle;

import ru.aradxxx.ciceronetabs.command.SwitchTab;
import ru.aradxxx.ciceronetabs.command.TBack;
import ru.aradxxx.ciceronetabs.command.TBackTo;
import ru.aradxxx.ciceronetabs.command.TForward;
import ru.aradxxx.ciceronetabs.command.TReplace;
import ru.aradxxx.ciceronetabs.command.TSwitchTab;

import java.util.LinkedList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import ru.terrakok.cicerone.Navigator;
import ru.terrakok.cicerone.android.support.SupportAppScreen;
import ru.terrakok.cicerone.commands.Back;
import ru.terrakok.cicerone.commands.BackTo;
import ru.terrakok.cicerone.commands.Command;
import ru.terrakok.cicerone.commands.Forward;
import ru.terrakok.cicerone.commands.Replace;

/**
 * Реализация навигатора для управления экранами в табах.
 */
public final class TabNavigator<R extends TabRouter> implements Navigator {
    @NonNull
    private final AppCompatActivity activity;
    @NonNull
    private final FragmentManager activityFM;
    @NonNull
    private final FragmentManager fragmentManager;
    private final int containerId;
    private final TabCicerone<R> tabCicerone;
    private LinkedList<String> localStackCopy;

    public TabNavigator(@NonNull AppCompatActivity activity,
            @NonNull TabCicerone<R> tabCicerone,
            int containerId) {
        this(activity, tabCicerone, activity.getSupportFragmentManager(), containerId);
    }

    public TabNavigator(@NonNull AppCompatActivity activity,
            @NonNull TabCicerone<R> tabCicerone,
            @NonNull FragmentManager fragmentManager,
            int containerId) {
        this.activity = activity;
        this.tabCicerone = tabCicerone;
        this.activityFM = activity.getSupportFragmentManager();
        this.fragmentManager = fragmentManager;
        this.containerId = containerId;
    }

    /**
     * Override this method to setup fragment transaction {@link FragmentTransaction}.
     * For example: setCustomAnimations(...), addSharedElement(...) or setReorderingAllowed(...)
     *
     * @param command             current navigation command. Will be only {@link Forward} or {@link Replace}
     * @param currentFragment     current fragment in container
     *                            (for {@link Replace} command it will be screen previous in new chain, NOT replaced
     *                            screen)
     * @param nextFragment        next screen fragment
     * @param fragmentTransaction fragment transaction
     */
    protected static void setupFragmentTransaction(Command command,
            Fragment currentFragment,
            Fragment nextFragment,
            FragmentTransaction fragmentTransaction) {
        //Override this method to setup fragment transaction {@link FragmentTransaction}.
        //For example: setCustomAnimations(...), addSharedElement(...) or setReorderingAllowed(...)
    }

    /**
     * Override this method to create option for start activity
     *
     * @param command        current navigation command. Will be only {@link Forward} or {@link Replace}
     * @param activityIntent activity intent
     * @return transition options
     */
    protected static Bundle createStartActivityOptions(Command command, Intent activityIntent) {
        return null;
    }

    /**
     * Called when there is no activity to open {@code screenKey}.
     *
     * @param screen         screen
     * @param activityIntent intent passed to start Activity for the {@code screenKey}
     */
    protected static void unexistingActivity(SupportAppScreen screen, Intent activityIntent) {
        // Do nothing by default
    }

    protected static void errorWhileCreatingScreen(SupportAppScreen screen) {
        throw new RuntimeException("Can't create a screen: " + screen.getScreenKey());
    }

    /**
     * Creates Fragment matching {@code screenKey}.
     *
     * @param screen screen
     * @return instantiated fragment for the passed screen
     */
    protected static Fragment createFragment(SupportAppScreen screen) {
        Fragment fragment = screen.getFragment();

        if (fragment == null) {
            errorWhileCreatingScreen(screen);
        }
        return fragment;
    }

    @Override
    public void applyCommands(Command[] commands) {
        fragmentManager.executePendingTransactions();

        //copy stack before apply commands
        copyStackToLocal();

        for (Command command : commands) {
            applyCommand(command);
        }
    }

    /**
     * Perform transition described by the navigation command
     *
     * @param command the navigation command to apply
     */
    protected void applyCommand(Command command) {
        if (command instanceof TSwitchTab) {
            tSwitchTab((TSwitchTab) command);
        } else if (command instanceof TForward) {
            tForward((TForward) command);
        } else if (command instanceof TReplace) {
            tReplace((TReplace) command);
        } else if (command instanceof TBackTo) {
            tBackTo((TBackTo) command);
        } else if (command instanceof TBack) {
            tBack((TBack) command);
        } else if (command instanceof SwitchTab) {
            switchTab((SwitchTab) command);
        } else if (command instanceof Forward) {
            activityForward((Forward) command);
        } else if (command instanceof Replace) {
            activityReplace((Replace) command);
        } else if (command instanceof BackTo) {
            backTo((BackTo) command);
        } else if (command instanceof Back) {
            fragmentBack();
        }
    }

    protected void tSwitchTab(@NonNull TSwitchTab command) {
        tabCicerone.tabsContainerRouter().intSwitchTab(command.getScreen());
    }

    protected void tForward(TForward command) {
        tabCicerone.router(command.tabTag()).navigateTo(command.getScreen());
    }

    protected void tBack(TBack command) {
        tabCicerone.router(command.tabTag()).exit();
    }

    protected void tBackTo(TBackTo command) {
        tabCicerone.router(command.tabTag()).backTo(command.getScreen());
    }

    protected void tReplace(TReplace command) {
        tabCicerone.router(command.tabTag()).replaceScreen(command.getScreen());
    }

    protected void activityForward(Forward command) {
        SupportAppScreen screen = (SupportAppScreen) command.getScreen();
        Intent activityIntent = screen.getActivityIntent(activity);

        // Start activity
        if (activityIntent != null) {
            Bundle options = createStartActivityOptions(command, activityIntent);
            checkAndStartActivity(screen, activityIntent, options);
        } else {
            fragmentForward(command);
        }
    }

    protected void fragmentForward(Forward command) {
        SupportAppScreen screen = (SupportAppScreen) command.getScreen();
        Fragment fragment = createFragment(screen);

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        setupFragmentTransaction(
                command,
                fragmentManager.findFragmentById(containerId),
                fragment,
                fragmentTransaction
        );

        fragmentTransaction
                .replace(containerId, fragment, screen.getScreenKey())
                .addToBackStack(screen.getScreenKey())
                .commit();
        localStackCopy.add(screen.getScreenKey());
    }

    protected void fragmentBack() {
        if (!isActivity() && activityFM.getBackStackEntryCount() != 0) {
            tabCicerone.activityRouter().exit();
            return;
        }
        if (!localStackCopy.isEmpty()) {
            fragmentManager.popBackStack();
            localStackCopy.removeLast();
        } else {
            activityBack();
        }
    }

    protected void activityBack() {
        activity.finish();
    }

    protected void activityReplace(Replace command) {
        SupportAppScreen screen = (SupportAppScreen) command.getScreen();
        Intent activityIntent = screen.getActivityIntent(activity);

        // Replace activity
        if (activityIntent != null) {
            Bundle options = createStartActivityOptions(command, activityIntent);
            checkAndStartActivity(screen, activityIntent, options);
            activity.finish();
        } else {
            fragmentReplace(command);
        }
    }

    protected void fragmentReplace(Replace command) {
        SupportAppScreen screen = (SupportAppScreen) command.getScreen();
        Fragment fragment = createFragment(screen);

        if (!localStackCopy.isEmpty()) {
            fragmentManager.popBackStack();
            localStackCopy.removeLast();

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            setupFragmentTransaction(
                    command,
                    fragmentManager.findFragmentById(containerId),
                    fragment,
                    fragmentTransaction
            );

            fragmentTransaction
                    .replace(containerId, fragment, screen.getScreenKey())
                    .addToBackStack(screen.getScreenKey())
                    .commit();
            localStackCopy.add(screen.getScreenKey());
        } else {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            setupFragmentTransaction(
                    command,
                    fragmentManager.findFragmentById(containerId),
                    fragment,
                    fragmentTransaction
            );

            fragmentTransaction
                    .replace(containerId, fragment, screen.getScreenKey())
                    .commit();
        }
    }

    /**
     * Performs {@link BackTo} command transition
     */
    protected void backTo(BackTo command) {
        if (command.getScreen() == null) {
            backToRoot();
        } else {
            String key = command.getScreen().getScreenKey();
            int index = localStackCopy.indexOf(key);
            int size = localStackCopy.size();

            if (index != -1) {
                for (int i = 1; i < size - index; i++) {
                    localStackCopy.removeLast();
                }
                fragmentManager.popBackStack(key, 0);
            } else {
                backToUnexisting((SupportAppScreen) command.getScreen());
            }
        }
    }

    /**
     * Called when we tried to fragmentBack to some specific screen (via {@link BackTo} command),
     * but didn't found it.
     *
     * @param screen screen
     */
    protected void backToUnexisting(SupportAppScreen screen) {
        backToRoot();
    }

    protected void switchTab(@NonNull SwitchTab switchTab) {
        String tabTag = switchTab.getScreen().getScreenKey();
        Fragment newFragment = fragmentManager.findFragmentByTag(tabTag);
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (newFragment == null) {
            if (!(switchTab.getScreen() instanceof SupportAppScreen)) {
                throw new RuntimeException("Can't create a tab screen: " + tabTag);
            }

            SupportAppScreen supportAppScreen = (SupportAppScreen) switchTab.getScreen();
            Fragment fragment = supportAppScreen.getFragment();

            if (fragment == null) {
                throw new RuntimeException("Can't create a tab screen: " + tabTag);
            }
            transaction.add(containerId, fragment, tabTag);
        }

        /*
        Скрываем остальные табфрагменты
         */
        for (String tag : tabCicerone.tabTags()) {
            if (!tag.isEmpty() && !tag.equals(tabTag)) {
                detachIfExists(transaction, tag);
            }
        }

        if (newFragment != null) {
            transaction.attach(newFragment);
        }
        transaction.commit();
    }

    private void copyStackToLocal() {
        localStackCopy = new LinkedList<>();

        final int stackSize = fragmentManager.getBackStackEntryCount();
        for (int i = 0; i < stackSize; i++) {
            localStackCopy.add(fragmentManager.getBackStackEntryAt(i).getName());
        }
    }

    private void backToRoot() {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        localStackCopy.clear();
    }

    private void checkAndStartActivity(SupportAppScreen screen, Intent activityIntent, Bundle options) {
        // Check if we can start activity
        if (activityIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(activityIntent, options);
        } else {
            unexistingActivity(screen, activityIntent);
        }
    }

    private void detachIfExists(@NonNull FragmentTransaction transaction, @NonNull String... tags) {
        for (String tag : tags) {
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment != null) {
                transaction.detach(fragment);
            }
        }
    }

    private boolean isActivity() {
        return activityFM.equals(fragmentManager);
    }
}

