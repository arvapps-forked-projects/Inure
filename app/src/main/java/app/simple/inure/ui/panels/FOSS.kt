package app.simple.inure.ui.panels

import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.R
import app.simple.inure.adapters.home.AdapterFOSS
import app.simple.inure.constants.BottomMenuConstants
import app.simple.inure.decorations.overscroll.CustomVerticalRecyclerView
import app.simple.inure.dialogs.menus.AppsMenu
import app.simple.inure.extensions.fragments.ScopedFragment
import app.simple.inure.interfaces.adapters.AdapterCallbacks
import app.simple.inure.viewmodels.panels.HomeViewModel

class FOSS : ScopedFragment() {

    private lateinit var recyclerView: CustomVerticalRecyclerView
    private var adapterFOSS: AdapterFOSS? = null
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_foss, container, false)

        recyclerView = view.findViewById(R.id.foss_recycler_view)
        adapterFOSS = AdapterFOSS()

        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel.getFossApps().observe(viewLifecycleOwner) {
            hideLoader()
            postponeEnterTransition()

            adapterFOSS?.apps = it
            recyclerView.adapter = adapterFOSS

            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }

            adapterFOSS?.setOnItemClickListener(object : AdapterCallbacks {
                override fun onAppClicked(packageInfo: PackageInfo, icon: ImageView) {
                    openAppInfo(packageInfo, icon)
                }

                override fun onAppLongPressed(packageInfo: PackageInfo, icon: ImageView) {
                    AppsMenu.newInstance(packageInfo)
                        .show(childFragmentManager, "apps_menu")
                }
            })

            bottomRightCornerMenu?.initBottomMenuWithRecyclerView(BottomMenuConstants.getUninstalledBottomMenuItems(), recyclerView) { id, _ ->
                when (id) {
                    R.drawable.ic_settings -> {
                        openFragmentSlide(Preferences.newInstance(), "prefs_screen")
                    }
                    R.drawable.ic_search -> {
                        openFragmentSlide(Search.newInstance(true), "search")
                    }
                    R.drawable.ic_refresh -> {
                        showLoader(manualOverride = true)
                        homeViewModel.refreshPackageData()
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance(): FOSS {
            val args = Bundle()
            val fragment = FOSS()
            fragment.arguments = args
            return fragment
        }
    }
}