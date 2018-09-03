package eu.aempathy.empushy.adapters

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import eu.aempathy.empushy.R
import eu.aempathy.empushy.data.AppSummaryItem
import eu.aempathy.empushy.dialogs.AppSummaryDialog
import kotlinx.android.synthetic.main.app_summary_item.view.*


class AppSummaryAdapter(context: Context, var summaryItems: ArrayList<AppSummaryItem>,
                        val clickListener: (AppSummaryItem) -> Boolean) : BaseAdapter() {
    var context: Context? = context

    override fun getCount(): Int {
        return summaryItems.size
    }

    override fun getItem(position: Int): Any {
        return summaryItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = this.summaryItems[position]

        val inflator = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemView = inflator.inflate(R.layout.app_summary_item, null)
        itemView.tv_app_summary_item_app.text = summaryItems[position].appName
        itemView.tv_app_summary_item_now.text = (summaryItems[position].active?.size.toString() + " for now");
        itemView.tv_app_summary_item_later.text = (summaryItems[position].hidden?.size.toString() + " for later");
        try {
            val icon = context?.packageManager?.getApplicationIcon(summaryItems[position].app?.trim())
            itemView.iv_app_summary_item_icon!!.setImageDrawable(icon)
        } catch (e: Exception) {
            Log.d("AppSummaryAdapter", e.toString())
        }
        itemView.setOnLongClickListener{ clickListener(summaryItems[position]) }

        /*itemView.setOnLongClickListener(View.OnLongClickListener {

            val builder = AlertDialog.Builder(context);
            builder.setTitle("Remove Notifications");

            builder.setPositiveButton("Remove"){dialog, which ->
                // remove notification
                // start task to remove all notifications in active and hidden lists
            }
            builder.setNegativeButton("Cancel"){dialog,which ->
                dialog.cancel()
            }
            builder.show();

            return@OnLongClickListener true
        })*/

        itemView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {

                val dialog = AppSummaryDialog.newInstance(summaryItems[position])
                dialog.show((context as Activity).fragmentManager, "")
            }
        })

        return itemView
    }
}

/*
class AppSummaryAdapter(internal var context: Context, var summaryItems: ArrayList<AppSummaryItem>) : BaseAdapter() {
    private lateinit var layoutInflater: LayoutInflater

    override fun getCount(): Int {
        return summaryItems.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {

        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater


        val holder = Holder()
        val rowView: View

        rowView = layoutInflater.inflate(R.layout.app_summary_item, parent)
        holder.tv = rowView.findViewById(R.id.tv_app_summary_item_app)
        holder.tv = rowView.findViewById(R.id.tv_app_summary_item_now)
        holder.img = rowView.findViewById(R.id.iv_app_summary_item_icon)

        holder.tv?.setText(summaryItems[position].app)
        try {
            val icon = context.packageManager.getApplicationIcon(summaryItems[position].app?.trim())
            holder.img!!.setImageDrawable(icon)
        } catch (e: Exception) {
            Log.d("AppSummaryAdapter", e.toString())
        }



        rowView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {

                Toast.makeText(context, "You Clicked " + summaryItems[position], Toast.LENGTH_LONG).show()

            }
        })

        return rowView
    }

    inner class Holder {
        internal var tv: TextView? = null
        internal var img: ImageView? = null
    }

}*/
