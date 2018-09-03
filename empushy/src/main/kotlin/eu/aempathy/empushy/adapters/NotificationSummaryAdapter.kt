package eu.aempathy.empushy.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.aempathy.empushy.R
import eu.aempathy.empushy.data.EmpushyNotification
import eu.aempathy.empushy.utils.DateUtils
import eu.aempathy.empushy.utils.NotificationUtil
import kotlinx.android.synthetic.main.notification_summary_item.view.*

/**
 * Created by Kieran on 05/04/2018.
 */
class NotificationSummaryAdapter (val notificationItemList: MutableList<EmpushyNotification>, val clickListener: (EmpushyNotification) -> Unit) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // Inflate XML. Last parameter: don't immediately attach new view to the parent view group
        val view = inflater.inflate(R.layout.notification_summary_item, parent, false)
        return NotificationViewHolder(view)
    }

    override fun getItemCount() = notificationItemList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as NotificationViewHolder).bind(notificationItemList[position], clickListener)
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val TAG = javaClass.simpleName;

        fun bind(notification: EmpushyNotification, clickListener: (EmpushyNotification) -> Unit) {
            itemView.tv_notification_summary_title.text = NotificationUtil.extractUsefulText(notification)
            //itemView.tv_notification_item_place.text = DataUtils.placeCategoryName(notification.placeCategories?: ArrayList())
            itemView.tv_notification_summary_time.text = DateUtils.getDate(notification.time?:0,null)

            itemView.setOnClickListener { clickListener(notification)}
        }
    }
}