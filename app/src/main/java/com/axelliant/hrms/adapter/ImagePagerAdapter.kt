package com.axelliant.hrms.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.axelliant.hrms.R
import com.axelliant.hrms.extention.setUrlImage
import com.axelliant.hrms.model.expense.Attachments


class ImagePagerAdapter(private val context: Context, private val images: List<Attachments>) :
    PagerAdapter() {
    override fun getCount(): Int {
        return images.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.image_detail_row, container, false)

        val imageView = view.findViewById<ImageView>(R.id.imageView)

        imageView.setUrlImage(
            images[position].file_url, context
        )


        container.addView(view)
        return view
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}