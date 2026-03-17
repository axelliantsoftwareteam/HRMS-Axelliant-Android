package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import com.axelliant.hris.adapter.ImagePagerAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.FragmentImageDetailBinding
import com.axelliant.hris.model.expense.Attachments
import com.axelliant.hris.navigation.AppNavigator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ImageDetailFragment : BaseFragment() {
    private var mCurrentPosition = 0

    private var _binding: FragmentImageDetailBinding? = null
    private val binding get() = _binding

    private var listOfAttachments:List<Attachments> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentImageDetailBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments != null) {
            val parsedData = arguments?.getString("images", "")
            listOfAttachments =    Gson().fromJson(parsedData, object : TypeToken<List<Attachments>>() {}.type)

         }



        binding?.ivBack?.setOnClickListener{
            AppNavigator.moveBackToPreviousFragment()
        }

        val adapter = ImagePagerAdapter(requireContext(), listOfAttachments)
        binding?.viewPager?.adapter = adapter
        binding?.tvTitle?.text = "0".plus(0 + 1).plus("/0").plus(listOfAttachments?.size)


        binding?.viewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // This method will be invoked when the current page is scrolled
            }

            override fun onPageSelected(position: Int) {
                // This method will be invoked when a new page becomes selected
                mCurrentPosition = position
                binding?.tvTitle?.text = "0".plus(position + 1).plus("/0").plus(listOfAttachments?.size)
            }

            override fun onPageScrollStateChanged(state: Int) {
                // This method will be invoked when the scroll state changes
            }
        })


    }


}
