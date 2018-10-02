package eu.aempathy.empushy.adapters

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import eu.aempathy.empushy.R
import eu.aempathy.empushy.data.Feature

class FeatureExpandableListAdapter internal constructor(private val context: Context,
                                                        private val titleList: List<String>,
                                                        private val dataList: HashMap<String, List<Feature>>,
                                                        private val onClickListener: (View) -> Unit
                                    ) : BaseExpandableListAdapter() {
 
    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return this.dataList[this.titleList[listPosition]]!![expandedListPosition]
    }
 
    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }
 
    override fun getChildView(listPosition: Int, expandedListPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val feature = getChild(listPosition, expandedListPosition) as Feature
        if (convertView == null) {
            val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.feature_item, null)
        }
        val featureNameView = convertView!!.findViewById<TextView>(R.id.tv_feature_item_name)
        featureNameView.text = feature.name
        val featureDescView = convertView.findViewById<TextView>(R.id.tv_feature_item_desc)
        featureDescView.text = feature.description
        val checkBox = convertView.findViewById<CheckBox>(R.id.cb_feature_item_enabled)
        checkBox.isChecked = feature.enabled?:false
        checkBox.tag = feature.id
        //checkBox.setOnCheckedChangeListener(checkListener)
        checkBox.setOnClickListener(onClickListener)

        return convertView
    }
 
    override fun getChildrenCount(listPosition: Int): Int {
        return this.dataList[this.titleList[listPosition]]!!.size
    }
 
    override fun getGroup(listPosition: Int): Any {
        return this.titleList[listPosition]
    }
 
    override fun getGroupCount(): Int {
        return this.titleList.size
    }
 
    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }
 
    override fun getGroupView(listPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val listTitle = getGroup(listPosition) as String
        if (convertView == null) {
            val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.feature_category_item, null)
        }
        val listTitleTextView = convertView!!.findViewById<TextView>(R.id.tv_feature_item_category_name)
        listTitleTextView.setTypeface(null, Typeface.BOLD)
        listTitleTextView.text = listTitle
        return convertView
    }
 
    override fun hasStableIds(): Boolean {
        return false
    }
 
    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return false
    }
}
