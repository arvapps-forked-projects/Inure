package app.simple.inure.adapters.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.simple.inure.R
import app.simple.inure.decorations.ripple.DynamicRippleLegendLinearLayout
import app.simple.inure.decorations.theme.ThemeIcon
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.preferences.AppearancePreferences

class AdapterTags(private val tags: ArrayList<String>) : RecyclerView.Adapter<AdapterTags.Holder>() {

    private var callback: TagsCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return when (viewType) {
            TYPE_TAG -> {
                Holder(LayoutInflater.from(parent.context)
                           .inflate(R.layout.adapter_tags, parent, false))
            }
            TYPE_ADD -> {
                Holder(LayoutInflater.from(parent.context)
                           .inflate(R.layout.adapter_tags_add, parent, false))
            }
            else -> {
                throw IllegalStateException("Unexpected value: $viewType")
            }
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if (tags.size == 0) {
            holder.container.setRippleColor(AppearancePreferences.getAccentColor())

            holder.container.setOnClickListener {
                callback?.onAddClicked()
            }
        } else {
            holder.tag.text = tags[position]

            holder.container.setOnClickListener {
                callback?.onTagClicked(tags[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return if (tags.size == 0) {
            1
        } else {
            tags.size.plus(1)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (tags.size == 0) {
            TYPE_ADD
        } else {
            if (position == tags.size) {
                TYPE_ADD
            } else {
                TYPE_TAG
            }
        }
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tag: TypeFaceTextView = itemView.findViewById(R.id.tag_label)
        val icon: ThemeIcon = itemView.findViewById(R.id.tag_icon)
        val container: DynamicRippleLegendLinearLayout = itemView.findViewById(R.id.container)
    }

    fun addTag(tag: String) {
        tags.add(tag)
        notifyItemInserted(tags.size)
    }

    fun removeTag(tag: String) {
        val index = tags.indexOf(tag)
        tags.remove(tag)
        notifyItemRemoved(index)
    }

    fun setOnTagCallbackListener(callback: TagsCallback) {
        this.callback = callback
    }

    companion object {
        private const val TAG = "AdapterTags"
        private const val TYPE_TAG = 0
        private const val TYPE_ADD = 1

        interface TagsCallback {
            fun onTagClicked(tag: String)
            fun onAddClicked()
        }
    }
}