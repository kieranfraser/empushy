package eu.aempathy.empushy.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.*
import eu.aempathy.empushy.R
import eu.aempathy.empushy.adapters.NotificationSummaryAdapter
import eu.aempathy.empushy.data.AppSummaryItem
import eu.aempathy.empushy.data.EmpushyNotification

/**
 * Created by Kieran on 12/04/2018.
 */
class AppSummaryDialog: DialogFragment(){

    val TAG = javaClass.simpleName;

    companion object {
        fun newInstance(summaryItem: AppSummaryItem): AppSummaryDialog {
            val dialog = AppSummaryDialog()

            // Supply num input as an argument.
            val args = Bundle()
            args.putSerializable("summaryItem", summaryItem)
            /*args.putSerializable("user", user)
            args.putSerializable("notification", notification)*/
            dialog.setArguments(args)

            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the Builder class for convenient dialog construction
        val builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.dialog_app_summary_detail, null)
        builder.setView(view)
        val summaryItem = getArguments().getSerializable("summaryItem") as AppSummaryItem;

        val recyclerView: RecyclerView = view.findViewById(R.id.rv_summary_dialog_notifications)
        val iconView: ImageView = view.findViewById(R.id.iv_app_summary_icon)
        val titleView: TextView = view.findViewById(R.id.tv_app_summary_app)


        val radioGroup: RadioGroup = view.findViewById(R.id.rg_app_summary_detail)
        val rbNow: RadioButton = view.findViewById(R.id.rb_app_summary_detail_now)

        try {
            val icon = context.packageManager.getApplicationIcon(summaryItem.app?.trim())
            iconView.setImageDrawable(icon)
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
        }
        titleView.text = summaryItem.appName

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.hasFixedSize()

        if(rbNow.isChecked)
            recyclerView.adapter = NotificationSummaryAdapter(summaryItem.active?.toMutableList()?: mutableListOf(), { notification : EmpushyNotification -> notificationItemClicked() })
        else
            recyclerView.adapter = NotificationSummaryAdapter(summaryItem.hidden?.toMutableList()?: mutableListOf(), { notification : EmpushyNotification -> notificationItemClicked() })
        // Create the AlertDialog object and return it
        radioGroup.setOnCheckedChangeListener(object: RadioGroup.OnCheckedChangeListener{
            override fun onCheckedChanged(p0: RadioGroup?, p1: Int) {
                Log.d(TAG, "Changed checked")
                if(rbNow.isChecked)
                    recyclerView.adapter = NotificationSummaryAdapter(summaryItem.active?.toMutableList()?: mutableListOf(), { notification : EmpushyNotification -> notificationItemClicked() })
                else
                    recyclerView.adapter = NotificationSummaryAdapter(summaryItem.hidden?.toMutableList()?: mutableListOf(), { notification : EmpushyNotification -> notificationItemClicked() })
            }
        })

        val btOkay: Button = view.findViewById(R.id.bt_dialog_app_summary_detail)
        btOkay.setOnClickListener(View.OnClickListener {
            this@AppSummaryDialog.dismissAllowingStateLoss() })


        return builder.create()
    }

    fun notificationItemClicked(){
        this@AppSummaryDialog.dismissAllowingStateLoss()
    }

    /*@OnClick(R.id.rv_summary_dialog_notifications,R.id.iv_app_summary_icon,R.id.tv_app_summary_item_app)
    fun toggleDialog(view: View) {
        this@AppSummaryDialog.dismissAllowingStateLoss()
    }*/
}