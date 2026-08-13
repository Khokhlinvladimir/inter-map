package org.osmdroid.samplefragments.ui

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import org.osmdroid.R

/**
 * base in this http://www.androidhive.info/2013/07/android-expandable-list-view-tutorial/
 *
 *
 * created on 1/1/2017.
 *
 * @author Alex O'Ree
 */
class ExpandableListAdapter(
    private val _context: Context, // header titles
    private val _listDataHeader: MutableList<String?>,
    // child data in format of header title, child title
    private val _listDataChild: HashMap<String?, MutableList<String?>?>
) : BaseExpandableListAdapter() {

    override fun getChild(groupPosition: Int, childPosititon: Int): Any? {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))!!
            .get(childPosititon)
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getChildView(
        groupPosition: Int, childPosition: Int,
        isLastChild: Boolean, convertView: View?, parent: ViewGroup?
    ): View {
        val childText = getChild(groupPosition, childPosition) as String?

        val childView = convertView ?: run {
            val infalInflater = this._context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            infalInflater.inflate(R.layout.list_item, null)
        }

        val txtListChild = childView
            .findViewById<TextView>(R.id.lblListItem)

        txtListChild.setText(childText)
        return childView
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        val strings = this._listDataChild.get(this._listDataHeader.get(groupPosition))
        if (strings != null) return strings.size
        return 0
    }

    override fun getGroup(groupPosition: Int): Any? {
        return this._listDataHeader.get(groupPosition)
    }

    override fun getGroupCount(): Int {
        return this._listDataHeader.size
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getGroupView(
        groupPosition: Int, isExpanded: Boolean,
        convertView: View?, parent: ViewGroup?
    ): View {
        val headerTitle = getGroup(groupPosition) as String?
        val groupView = convertView ?: run {
            val infalInflater = this._context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            infalInflater.inflate(R.layout.list_group, null)
        }

        val lblListHeader = groupView
            .findViewById<TextView>(R.id.lblListHeader)
        lblListHeader.setTypeface(null, Typeface.BOLD)
        lblListHeader.setText(headerTitle)

        return groupView
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }
}
