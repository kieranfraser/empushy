package eu.aempathy.empushy.adapters

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.aempathy.empushy.R
import eu.aempathy.empushy.data.AppSummaryItem
import eu.aempathy.empushy.dialogs.AppSummaryDialog
import kotlinx.android.synthetic.main.app_summary_item.view.*

/**
 * Created by Kieran on 05/04/2018.
 */
class HomeAdapter (val appSummaryItemList: MutableList<AppSummaryItem>, val longClickListener: (AppSummaryItem) -> Boolean) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // Inflate XML. Last parameter: don't immediately attach new view to the parent view group
        val view = inflater.inflate(R.layout.app_summary_item, parent, false)
        return NotificationViewHolder(view)
    }

    override fun getItemCount() = appSummaryItemList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as NotificationViewHolder).bind(appSummaryItemList[position], longClickListener)
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val TAG = javaClass.simpleName;

        fun bind(appItem: AppSummaryItem, longClickListener: (AppSummaryItem) -> Boolean) {
            val context = itemView.context
            itemView.tv_app_summary_item_app.text = appItem.appName
            itemView.tv_app_summary_item_now.text = appItem.active?.size.toString() + " for now";
            try {
                val icon = context?.packageManager?.getApplicationIcon(appItem.app?.trim())
                itemView.iv_app_summary_item_icon!!.setImageDrawable(icon)
            } catch (e: Exception) {
                Log.d("AppSummaryAdapter", e.toString())
            }
            itemView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {

                    val dialog = AppSummaryDialog.newInstance(appItem)
                    dialog.show((context as Activity).fragmentManager, "")
                }
            })
            //itemView.setOnLongClickListener{ clickListener(appItem) }


            //Picasso.get().load("http://i.imgur.com/DvpvklR.png").into(itemView.iv_notification_item_app_icon)

            itemView.setOnLongClickListener { longClickListener(appItem)}
        }
    }

    fun removeAt(position: Int) {
        appSummaryItemList.removeAt(position)
        notifyItemRemoved(position)
    }
}