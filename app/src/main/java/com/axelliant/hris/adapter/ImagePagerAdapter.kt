package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.axelliant.hris.R
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.model.expense.Attachments
import com.bumptech.glide.Glide


class ImagePagerAdapter(private val context: Context, private val images: List<Attachments>) :
    PagerAdapter() {
    override fun getCount(): Int {
        return images.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.image_detail_row, container, false)

        val imageView = view.findViewById<ImageView>(R.id.imageView)
        Glide.with(context)
            .load(images[position].file_url) // image url
            .placeholder(R.drawable.ic_place_holder) // any placeholder to load at start
            .error(R.drawable.ic_place_holder)  // any image in case of error
            .centerCrop()
            .into(imageView)


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