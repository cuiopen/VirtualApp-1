package io.virtualapp.home;

import android.app.Activity;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;

import io.virtualapp.R;
import io.virtualapp.VApp;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.AppRepository;
import io.virtualapp.sys.Installd;

/**
 * @author Lody
 */
class HomePresenterImpl implements HomeContract.HomePresenter {

    private HomeContract.HomeView mView;
    private Activity mActivity;
    private AppRepository mRepo;

    HomePresenterImpl(HomeContract.HomeView view) {
        mView = view;
        mActivity = view.getActivity();
        mRepo = new AppRepository(mActivity);
        mView.setPresenter(this);
    }

    @Override
    public void start() {
        dataChanged();
    }

    @Override
    public void launchApp(AppData data) {
        try {
            if (data instanceof PackageAppData) {
                PackageAppData appData = (PackageAppData) data;
                appData.isFirstOpen = false;
                LoadingActivity.launch(mActivity, appData.packageName, 0);
            } else if (data instanceof MultiplePackageAppData) {
                MultiplePackageAppData multipleData = (MultiplePackageAppData) data;
                multipleData.isFirstOpen = false;
                LoadingActivity.launch(mActivity, multipleData.appInfo.packageName, ((MultiplePackageAppData) data).userId);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dataChanged() {
        mView.showLoading();
        mRepo.getVirtualApps().done(mView::loadFinish).fail(mView::loadError);
    }

    @Override
    public void addApp(AppInfoLite info) {
        Installd.addApp(info, new Installd.UpdateListener() {
            @Override
            public void update(AppData model) {
                mView.refreshLauncherItem(model);
            }

            @Override
            public void fail(String msg) {
                Toast.makeText(VApp.getApp(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void deleteApp(AppData data) {
        try {
            mView.removeAppToLauncher(data);
            if (data instanceof PackageAppData) {
                mRepo.removeVirtualApp(((PackageAppData) data).packageName, 0);
            } else {
                MultiplePackageAppData appData = (MultiplePackageAppData) data;
                mRepo.removeVirtualApp(appData.appInfo.packageName, appData.userId);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createShortcut(AppData data) {
        boolean result = false;
        if (data instanceof PackageAppData) {
            result = VirtualCore.get().createShortcut(0, ((PackageAppData) data).packageName, null);
        } else if (data instanceof MultiplePackageAppData) {
            MultiplePackageAppData appData = (MultiplePackageAppData) data;
            result = VirtualCore.get().createShortcut(appData.userId, appData.appInfo.packageName, null);
        }
        if (result) {
            Toast.makeText(mActivity, R.string.create_shortcut_success, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void clearApp(AppData data) {
        if (data instanceof PackageAppData) {
            VirtualCore.get().clearPackage(((PackageAppData) data).packageName);
        } else {
            MultiplePackageAppData appData = (MultiplePackageAppData) data;
            VirtualCore.get().clearPackageAsUser(appData.userId, appData.appInfo.packageName);
        }
    }

    @Override
    public void killApp(AppData data) {
        if (data instanceof PackageAppData) {
            VirtualCore.get().killApp(((PackageAppData) data).packageName, 0);
        } else {
            MultiplePackageAppData appData = (MultiplePackageAppData) data;
            VirtualCore.get().killApp(((MultiplePackageAppData) data).appInfo.packageName, appData.userId);
        }
    }
}
