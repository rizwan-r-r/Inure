package app.simple.inure.popups.bootmanager

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import app.simple.inure.R
import app.simple.inure.constants.SortConstant
import app.simple.inure.decorations.ripple.DynamicRippleTextView
import app.simple.inure.extensions.popup.BasePopupWindow
import app.simple.inure.extensions.popup.PopupLinearLayout
import app.simple.inure.preferences.BootManagerPreferences

class PopupBootManagerAppsCategory(view: View) : BasePopupWindow() {

    private val system: DynamicRippleTextView
    private val user: DynamicRippleTextView
    private val both: DynamicRippleTextView

    init {
        val contentView = LayoutInflater.from(view.context).inflate(R.layout.popup_apps_category, PopupLinearLayout(view.context))

        system = contentView.findViewById(R.id.popup_category_system)
        user = contentView.findViewById(R.id.popup_category_user)
        both = contentView.findViewById(R.id.popup_category_both)

        system.onClick(SortConstant.SYSTEM)
        user.onClick(SortConstant.USER)
        both.onClick(SortConstant.BOTH)

        when (BootManagerPreferences.getAppsCategory()) {
            SortConstant.USER -> user.isSelected = true
            SortConstant.SYSTEM -> system.isSelected = true
            SortConstant.BOTH -> both.isSelected = true
        }

        init(contentView, view)
    }

    private fun TextView.onClick(category: String) {
        this.setOnClickListener {
            BootManagerPreferences.setAppsCategory(category)
            dismiss()
        }
    }
}