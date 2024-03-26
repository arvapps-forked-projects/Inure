package app.simple.inure.viewmodels.panels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.R
import app.simple.inure.constants.PreferencesSearchConstants
import app.simple.inure.extensions.viewmodels.WrappedViewModel
import app.simple.inure.models.PreferenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreferencesViewModel(application: Application) : WrappedViewModel(application) {

    var keyword: String? = null
        set(value) {
            field = value
            loadPreferencesSearchData()
        }

    private val preferences: MutableLiveData<ArrayList<Pair<Int, Int>>> by lazy {
        MutableLiveData<ArrayList<Pair<Int, Int>>>().also {
            loadPreferencesData()
        }
    }

    private val preferencesSearchData: MutableLiveData<ArrayList<PreferenceModel>> by lazy {
        MutableLiveData<ArrayList<PreferenceModel>>()
    }

    fun getPreferences(): LiveData<ArrayList<Pair<Int, Int>>> {
        return preferences
    }

    fun getPreferencesSearchData(): LiveData<ArrayList<PreferenceModel>> {
        return preferencesSearchData
    }

    private fun loadPreferencesData() {
        viewModelScope.launch(Dispatchers.Default) {
            val list = arrayListOf(
                    Pair(R.drawable.ic_appearance, R.string.appearance),
                    Pair(R.drawable.ic_behaviour, R.string.behavior),
                    Pair(R.drawable.ic_app_settings, R.string.configuration),
                    Pair(R.drawable.ic_formatting, R.string.formatting),
                    Pair(R.drawable.ic_accessibility, R.string.accessibility),
                    Pair(0, 0), // Divider
                    Pair(R.drawable.ic_terminal_black, R.string.terminal),
                    Pair(R.drawable.ic_shell, R.string.shell),
                    Pair(0, 0), // Divider
                    // Pair(R.drawable.ic_layouts, R.string.layouts),
                    // Pair(R.drawable.ic_radiation_nuclear, R.string.trackers),
                    // Pair(0, 0), // Divider
                    Pair(R.drawable.ic_data_object, R.string.development),
                    Pair(R.drawable.ic_audio_placeholder, R.string.about)
            )

            preferences.postValue(list)
        }
    }

    private fun loadPreferencesSearchData() {
        viewModelScope.launch(Dispatchers.Default) {
            val list = arrayListOf<PreferenceModel>()

            if (keyword.isNullOrEmpty()) {
                loadPreferencesData()
            } else {
                val context = context

                for (prefs in PreferencesSearchConstants.preferencesStructureData) {
                    if (context.getString(prefs.title).lowercase().contains(keyword!!.lowercase()) ||
                        context.getString(prefs.description).lowercase().contains(keyword!!.lowercase()) ||
                        context.getString(prefs.category).lowercase().contains(keyword!!.lowercase()) ||
                        context.getString(prefs.type).lowercase().contains(keyword!!.lowercase()) ||
                        context.getString(prefs.panel).lowercase().contains(keyword!!.lowercase())) {

                        list.add(prefs)
                    }
                }
            }

            preferencesSearchData.postValue(list)
        }
    }
}
