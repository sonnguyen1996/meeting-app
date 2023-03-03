package com.example.fpt.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.demothesisfpteduvn.R
import com.example.fpt.ui.metting.ultils.MeetingMenuItem

class LeaveOptionListAdapter(context: Context, resource: Int, objects: List<MeetingMenuItem>) :
    ArrayAdapter<MeetingMenuItem?>(context, resource, objects) {
    internal val context: Context
    private val OptionsList: List<MeetingMenuItem>

    init {
        OptionsList = objects
        this.context = context
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView: View = inflater.inflate(R.layout.leave_options_list_layout, parent, false)
        val itemIcon = rowView.findViewById<ImageView>(R.id.iv_item_icon)
        val itemName = rowView.findViewById<TextView>(R.id.tv_item_name)
        val itemDesc = rowView.findViewById<TextView>(R.id.tv_item_desc)
        val Options: MeetingMenuItem = OptionsList[position]
        itemDesc.text = Options.itemDescription
        itemName.text = Options.itemName
        itemIcon.setImageDrawable(Options.itemIcon)
        return rowView
    }
}