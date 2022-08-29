package app.simple.inure.extensions.fragments

import app.simple.inure.R
import app.simple.inure.decorations.corners.DynamicCornerEditText
import app.simple.inure.decorations.ripple.DynamicRippleImageButton
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.util.ViewUtils.gone
import app.simple.inure.util.ViewUtils.visible

open class SearchBarScopedFragment : ScopedFragment() {

    open lateinit var search: DynamicRippleImageButton
    open lateinit var title: TypeFaceTextView
    open lateinit var searchBox: DynamicCornerEditText

    protected fun searchBoxState(animate: Boolean, isVisible: Boolean) {
        if (isVisible) {
            search.setImageResource(R.drawable.ic_close)
            title.gone()
            searchBox.visible(animate)
            searchBox.showInput()
        } else {
            search.setImageResource(R.drawable.ic_search)
            title.visible(animate)
            searchBox.gone()
            searchBox.hideInput()
        }
    }
}