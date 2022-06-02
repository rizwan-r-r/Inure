package app.simple.inure.ui.panels

import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import app.simple.inure.R
import app.simple.inure.adapters.home.AdapterFrequentlyUsed
import app.simple.inure.constants.BundleConstants
import app.simple.inure.decorations.overscroll.CustomVerticalRecyclerView
import app.simple.inure.dialogs.menus.AppsMenu
import app.simple.inure.extensions.fragments.ScopedFragment
import app.simple.inure.interfaces.adapters.AppsAdapterCallbacks
import app.simple.inure.ui.app.AppInfo
import app.simple.inure.util.FragmentHelper
import app.simple.inure.viewmodels.panels.HomeViewModel

class MostUsed : ScopedFragment() {

    private lateinit var recyclerView: CustomVerticalRecyclerView
    private lateinit var adapterFrequentlyUsed: AdapterFrequentlyUsed

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_most_used, container, false)

        recyclerView = view.findViewById(R.id.most_used_recycler_view)
        adapterFrequentlyUsed = AdapterFrequentlyUsed()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showLoader()

        homeViewModel.frequentlyUsed.observe(viewLifecycleOwner) {
            postponeEnterTransition()
            hideLoader()

            adapterFrequentlyUsed.apps = it
            recyclerView.adapter = adapterFrequentlyUsed

            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }

            adapterFrequentlyUsed.setOnItemClickListener(object : AppsAdapterCallbacks {
                override fun onAppClicked(packageInfo: PackageInfo, icon: ImageView) {
                    openAppInfo(packageInfo, icon)
                }

                override fun onAppLongPressed(packageInfo: PackageInfo, icon: ImageView) {
                    AppsMenu.newInstance(packageInfo)
                        .show(childFragmentManager, "apps_menu")
                }

                override fun onSearchPressed(view: View) {
                    clearTransitions()
                    FragmentHelper.openFragment(requireActivity().supportFragmentManager,
                                                Search.newInstance(true),
                                                "search")
                }

                override fun onSettingsPressed(view: View) {
                    clearExitTransition()
                    FragmentHelper.openFragment(parentFragmentManager, Preferences.newInstance(), "prefs_screen")
                }
            })
        }
    }

    private fun openAppInfo(packageInfo: PackageInfo, icon: ImageView) {
        FragmentHelper.openFragment(requireActivity().supportFragmentManager,
                                    AppInfo.newInstance(packageInfo, icon.transitionName),
                                    icon, "app_info")
    }

    companion object {
        fun newInstance(loader: Boolean = false): MostUsed {
            val args = Bundle()
            val fragment = MostUsed()
            args.putBoolean(BundleConstants.loading, loader)
            fragment.arguments = args
            return fragment
        }
    }
}