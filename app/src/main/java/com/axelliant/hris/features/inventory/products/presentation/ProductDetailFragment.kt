package com.axelliant.hris.features.inventory.products.presentation

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentProductDetailBinding
import com.axelliant.hris.databinding.ItemProductDetailInfoTileBinding
import com.axelliant.hris.databinding.ItemProductDetailTabBinding
import com.axelliant.hris.databinding.ItemProductVendorAssociatedBinding
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductDetailResponse
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

@AndroidEntryPoint
class ProductDetailFragment : Fragment() {
    private val viewModel: ProductDetailViewModel by viewModels()
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val infoAdapter = ProductDetailInfoAdapter()
    private val technicalSpecsAdapter = ProductDetailInfoAdapter()
    private val vendorAdapter = ProductDetailVendorAdapter()
    private val tabsAdapter = ProductDetailTabsAdapter(::selectDetailTab)
    private var hasBoundArgs = false
    private var selectedDetailTab = ProductDetailTab.ProductDescription
    private var detailSectionExpanded = true
    private var descriptionExpanded = false
    private var currentProduct: ProductDetailUi? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActions()
        setupRecyclerViews()
        observeProduct()

        val productFromList = requireArguments().toProductDetailUi()
        if (productFromList.name.isNotBlank()) {
            hasBoundArgs = true
            bindProduct(productFromList)
        } else {
            arguments?.getString(ARG_PRODUCT_ID)?.let(viewModel::loadProduct)
        }
    }

    private fun setupActions() {
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.searchButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.moreButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.detailSectionHeader.setOnClickListener {
            detailSectionExpanded = !detailSectionExpanded
            updateSectionContentVisibility()
        }
    }

    private fun setupRecyclerViews() {
        binding.productInfoRecyclerView.apply {
            adapter = infoAdapter
            layoutManager = GridLayoutManager(requireContext(), PRODUCT_INFO_SPAN_COUNT)
            addItemDecorationIfMissing(
                ProductDetailGridSpacingDecoration(resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp))
            )
        }
        binding.detailTabsRecyclerView.apply {
            adapter = tabsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            addItemDecorationIfMissing(
                ProductDetailHorizontalSpacingDecoration(resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp))
            )
        }
        binding.technicalSpecsRecyclerView.apply {
            adapter = technicalSpecsAdapter
            layoutManager = GridLayoutManager(requireContext(), PRODUCT_INFO_SPAN_COUNT)
            addItemDecorationIfMissing(
                ProductDetailGridSpacingDecoration(resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp))
            )
        }
        binding.vendorsRecyclerView.apply {
            adapter = vendorAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeProduct() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productState.collect { state ->
                    if (hasBoundArgs) return@collect

                    binding.progress.isVisible = state is UiState.Loading
                    binding.errorText.isVisible = state is UiState.Error || state is UiState.Unauthorized
                    binding.contentScroll.isVisible = state is UiState.Success || state is UiState.Empty || state is UiState.Idle
                    when (state) {
                        is UiState.Success -> bindProduct(state.data.toProductDetailUi())
                        UiState.Empty -> bindProduct(ProductDetailUi.empty())
                        is UiState.Error -> binding.errorText.text = state.message
                        UiState.Unauthorized -> binding.errorText.text = "Session expired."
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun bindProduct(product: ProductDetailUi) {
        currentProduct = product
        binding.progress.isVisible = false
        binding.errorText.isVisible = false
        binding.contentScroll.isVisible = true

        binding.productImageLabel.text = product.thumbnailLabel.ifBlank { product.name.take(2).uppercase(Locale.US) }
        binding.productImageFrame.setBackgroundResource(
            if (product.brandThumbnail) R.drawable.bg_product_thumb_cisco else R.drawable.bg_product_detail_image
        )
        binding.productImageLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_on_primary))
        binding.statusBadge.text = product.status.ifBlank { getString(R.string.product_filter_active) }
        binding.productName.text = product.name.ifBlank { "-" }
        binding.productPrice.text = product.price.ifBlank { "$0.00" }
        binding.stockBadge.text = product.availability.ifBlank { ProductAvailability.InStock.label }

        val stockIsAvailable = product.availability.equals(ProductAvailability.InStock.label, ignoreCase = true)
        binding.stockBadge.setTextColor(
            ContextCompat.getColor(requireContext(), if (stockIsAvailable) R.color.ds_success else R.color.ds_error)
        )
        binding.stockBadge.setBackgroundResource(if (stockIsAvailable) R.drawable.bg_status_success else R.drawable.bg_status_error)

        val statusIsActive = product.status.equals(ProductStatusFilter.Active.name, ignoreCase = true) ||
            product.status.equals(getString(R.string.product_filter_active), ignoreCase = true)
        binding.statusBadge.setTextColor(
            ContextCompat.getColor(requireContext(), if (statusIsActive) R.color.ds_success else R.color.ds_error)
        )
        binding.statusBadge.setBackgroundResource(if (statusIsActive) R.drawable.bg_status_success else R.drawable.bg_status_error)

        infoAdapter.submitList(product.infoTiles())
        renderSelectedTab()
    }

    private fun ProductDetailUi.infoTiles(): List<ProductDetailInfoTileUi> {
        return listOf(
            ProductDetailInfoTileUi(R.drawable.ic_detail_box, getString(R.string.axe_part_no), sku),
            ProductDetailInfoTileUi(R.drawable.ic_detail_building, getString(R.string.mfg_name), manufacturer),
            ProductDetailInfoTileUi(R.drawable.ic_detail_tag, getString(R.string.mfg_part_no), manufacturerPartNumber),
            ProductDetailInfoTileUi(R.drawable.ic_detail_globe, getString(R.string.country_of_origin), countryOfOrigin)
        )
    }

    private fun detailTabs(): List<ProductDetailTabUi> {
        return listOf(
            ProductDetailTabUi(ProductDetailTab.ProductDescription, getString(R.string.product_description)),
            ProductDetailTabUi(ProductDetailTab.TechnicalSpecifications, getString(R.string.technical_specifications)),
            ProductDetailTabUi(ProductDetailTab.VendorAssociated, getString(R.string.vendor_associated)),
            ProductDetailTabUi(ProductDetailTab.ComplianceCertification, getString(R.string.compliance_certification)),
            ProductDetailTabUi(ProductDetailTab.CompetitorPricing, getString(R.string.competitor_pricing))
        )
    }

    private fun selectDetailTab(tab: ProductDetailTab) {
        if (selectedDetailTab == tab) return
        selectedDetailTab = tab
        detailSectionExpanded = true
        renderSelectedTab()
    }

    private fun renderSelectedTab() {
        val product = currentProduct ?: return
        tabsAdapter.submitList(detailTabs(), selectedDetailTab)
        binding.detailSectionTitle.text = when (selectedDetailTab) {
            ProductDetailTab.ProductDescription -> getString(R.string.product_description)
            ProductDetailTab.TechnicalSpecifications -> getString(R.string.technical_specifications)
            ProductDetailTab.VendorAssociated -> getString(R.string.vendor_associated)
            ProductDetailTab.ComplianceCertification -> getString(R.string.compliance_certification)
            ProductDetailTab.CompetitorPricing -> getString(R.string.competitor_pricing)
        }
        binding.detailSectionMeta.isVisible = selectedDetailTab == ProductDetailTab.VendorAssociated
        binding.detailSectionChevron.isVisible = true

        when (selectedDetailTab) {
            ProductDetailTab.ProductDescription -> bindDescription(
                product.description.ifBlank { buildFallbackDescription(product) }
            )
            ProductDetailTab.TechnicalSpecifications -> technicalSpecsAdapter.submitList(product.technicalSpecs())
            ProductDetailTab.VendorAssociated -> {
                val vendors = product.vendors.ifEmpty { product.fallbackVendorList() }
                binding.detailSectionMeta.text = getString(
                    if (vendors.size == 1) R.string.vendor_count_single else R.string.vendor_count_plural,
                    vendors.size
                )
                vendorAdapter.submitList(vendors)
            }
            ProductDetailTab.ComplianceCertification,
            ProductDetailTab.CompetitorPricing -> {
                binding.readMoreText.isVisible = false
                binding.productDescription.maxLines = Int.MAX_VALUE
                binding.productDescription.text = "-"
            }
        }
        updateSectionContentVisibility()
    }

    private fun updateSectionContentVisibility() {
        val showingDescription = selectedDetailTab == ProductDetailTab.ProductDescription ||
            selectedDetailTab == ProductDetailTab.ComplianceCertification ||
            selectedDetailTab == ProductDetailTab.CompetitorPricing
        binding.productDescription.isVisible = detailSectionExpanded && showingDescription
        binding.technicalSpecsRecyclerView.isVisible =
            detailSectionExpanded && selectedDetailTab == ProductDetailTab.TechnicalSpecifications
        binding.vendorsRecyclerView.isVisible =
            detailSectionExpanded && selectedDetailTab == ProductDetailTab.VendorAssociated
        binding.readMoreText.isVisible =
            detailSectionExpanded &&
                selectedDetailTab == ProductDetailTab.ProductDescription &&
                !descriptionExpanded &&
                binding.productDescription.lineCount > DESCRIPTION_COLLAPSED_LINES
        binding.detailSectionChevron.rotation = if (detailSectionExpanded) 0f else 180f
    }

    private fun ProductDetailUi.technicalSpecs(): List<ProductDetailInfoTileUi> {
        return listOf(
            ProductDetailInfoTileUi(R.drawable.ic_detail_screen, getString(R.string.screen_size), screenSize),
            ProductDetailInfoTileUi(R.drawable.ic_detail_ruler, getString(R.string.dimensions), dimensions),
            ProductDetailInfoTileUi(R.drawable.ic_detail_scale, getString(R.string.unit_of_measure), unitOfMeasure),
            ProductDetailInfoTileUi(R.drawable.ic_detail_weight, getString(R.string.weight), weight),
            ProductDetailInfoTileUi(R.drawable.ic_detail_shield, getString(R.string.warranty_months), warrantyPeriod),
            ProductDetailInfoTileUi(R.drawable.ic_detail_calendar, getString(R.string.validity_period_days), validityPeriod)
        )
    }

    private fun ProductDetailUi.fallbackVendorList(): List<ProductVendorUi> {
        if (vendor.isBlank()) return emptyList()
        return listOf(
            ProductVendorUi(
                name = vendor,
                sku = sku,
                availability = availability,
                unit = "-",
                costPrice = "-",
                listPrice = price,
                status = status.ifBlank { getString(R.string.product_filter_active) }
            )
        )
    }

    private fun bindDescription(description: String) {
        descriptionExpanded = false
        binding.productDescription.maxLines = DESCRIPTION_COLLAPSED_LINES
        binding.productDescription.text = description
        binding.readMoreText.isVisible = true
        binding.readMoreText.setOnClickListener {
            descriptionExpanded = true
            binding.productDescription.maxLines = Int.MAX_VALUE
            updateSectionContentVisibility()
        }
        binding.productDescription.post {
            if (binding.productDescription.lineCount <= DESCRIPTION_COLLAPSED_LINES) {
                binding.readMoreText.isVisible = false
            } else {
                updateSectionContentVisibility()
            }
        }
    }

    private fun RecyclerView.addItemDecorationIfMissing(decoration: RecyclerView.ItemDecoration) {
        if (itemDecorationCount == 0) {
            addItemDecoration(decoration)
        }
    }

    private fun buildFallbackDescription(product: ProductDetailUi): String {
        return "${product.name.ifBlank { getString(R.string.products) }} is available in the internal product catalog. " +
            "Review the product identifiers, manufacturer information, vendor association, and compliance details before adding it to assets."
    }

    private fun Bundle.toProductDetailUi(): ProductDetailUi {
        return ProductDetailUi(
            id = getString(ARG_PRODUCT_ID).orEmpty(),
            name = getString(ARG_PRODUCT_NAME).orEmpty(),
            sku = getString(ARG_PRODUCT_SKU).orEmpty(),
            manufacturer = getString(ARG_PRODUCT_MANUFACTURER).orEmpty(),
            manufacturerPartNumber = getString(ARG_PRODUCT_MFG_PART).orEmpty(),
            vendor = getString(ARG_PRODUCT_VENDOR).orEmpty(),
            category = getString(ARG_PRODUCT_CATEGORY).orEmpty(),
            description = getString(ARG_PRODUCT_DESCRIPTION).orEmpty(),
            countryOfOrigin = getString(ARG_PRODUCT_COUNTRY).orEmpty(),
            screenSize = getString(ARG_PRODUCT_SCREEN_SIZE).orEmpty(),
            dimensions = getString(ARG_PRODUCT_DIMENSIONS).orEmpty(),
            unitOfMeasure = getString(ARG_PRODUCT_UNIT_OF_MEASURE).orEmpty(),
            weight = getString(ARG_PRODUCT_WEIGHT).orEmpty(),
            warrantyPeriod = getString(ARG_PRODUCT_WARRANTY).orEmpty(),
            validityPeriod = getString(ARG_PRODUCT_VALIDITY).orEmpty(),
            vendors = getVendorArgs(),
            price = getString(ARG_PRODUCT_PRICE).orEmpty(),
            availability = getString(ARG_PRODUCT_AVAILABILITY).orEmpty(),
            status = getString(ARG_PRODUCT_STATUS).orEmpty(),
            thumbnailLabel = getString(ARG_PRODUCT_THUMBNAIL).orEmpty(),
            brandThumbnail = getBoolean(ARG_PRODUCT_BRAND_THUMBNAIL, false)
        )
    }

    private fun ProductDetailResponse.toProductDetailUi(): ProductDetailUi {
        return ProductDetailUi(
            id = id.toString(),
            name = title,
            sku = sku.orEmpty(),
            manufacturer = manufacturerName ?: brand.orEmpty(),
            manufacturerPartNumber = manufacturerPartNumber.orEmpty(),
            vendor = vendors.firstOrNull()?.name.orEmpty(),
            category = category.orEmpty(),
            description = description.orEmpty(),
            countryOfOrigin = countryOfOrigin.orEmpty(),
            screenSize = technicalSpecifications["Screen Size"].orEmpty(),
            dimensions = technicalSpecifications["Dimensions"].orEmpty(),
            unitOfMeasure = technicalSpecifications["Unit Of Measure"].orEmpty(),
            weight = technicalSpecifications["Weight"].orEmpty(),
            warrantyPeriod = technicalSpecifications["Warranty (Months)"].orEmpty(),
            validityPeriod = technicalSpecifications["Validity Period (Days)"].orEmpty(),
            vendors = vendors.map {
                ProductVendorUi(
                    name = it.name,
                    sku = it.sku.orEmpty(),
                    availability = availabilityStatus.orEmpty(),
                    unit = "-",
                    costPrice = "-",
                    listPrice = (price ?: 0.0).formatCurrency(),
                    status = availabilityStatus.orEmpty()
                )
            },
            price = (price ?: 0.0).formatCurrency(),
            availability = availabilityStatus.orEmpty(),
            status = availabilityStatus.orEmpty(),
            thumbnailLabel = brand?.take(3)?.uppercase(Locale.US).orEmpty(),
            brandThumbnail = brand.equals("Cisco", ignoreCase = true)
        )
    }

    private fun Double.formatCurrency(): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(this)
    }

    private fun Bundle.getVendorArgs(): List<ProductVendorUi> {
        val names = getStringArrayList(ARG_VENDOR_NAMES).orEmpty()
        val skus = getStringArrayList(ARG_VENDOR_SKUS).orEmpty()
        val availabilities = getStringArrayList(ARG_VENDOR_AVAILABILITIES).orEmpty()
        val units = getStringArrayList(ARG_VENDOR_UNITS).orEmpty()
        val costPrices = getStringArrayList(ARG_VENDOR_COST_PRICES).orEmpty()
        val listPrices = getStringArrayList(ARG_VENDOR_LIST_PRICES).orEmpty()
        val statuses = getStringArrayList(ARG_VENDOR_STATUSES).orEmpty()
        return names.mapIndexed { index, name ->
            ProductVendorUi(
                name = name,
                sku = skus.getOrNull(index).orEmpty(),
                availability = availabilities.getOrNull(index).orEmpty(),
                unit = units.getOrNull(index).orEmpty(),
                costPrice = costPrices.getOrNull(index).orEmpty(),
                listPrice = listPrices.getOrNull(index).orEmpty(),
                status = statuses.getOrNull(index).orEmpty()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val ARG_PRODUCT_ID = "productId"
        const val ARG_PRODUCT_NAME = "productName"
        const val ARG_PRODUCT_SKU = "productSku"
        const val ARG_PRODUCT_MANUFACTURER = "productManufacturer"
        const val ARG_PRODUCT_MFG_PART = "productManufacturerPartNumber"
        const val ARG_PRODUCT_VENDOR = "productVendor"
        const val ARG_PRODUCT_CATEGORY = "productCategory"
        const val ARG_PRODUCT_DESCRIPTION = "productDescription"
        const val ARG_PRODUCT_COUNTRY = "productCountryOfOrigin"
        const val ARG_PRODUCT_SCREEN_SIZE = "productScreenSize"
        const val ARG_PRODUCT_DIMENSIONS = "productDimensions"
        const val ARG_PRODUCT_UNIT_OF_MEASURE = "productUnitOfMeasure"
        const val ARG_PRODUCT_WEIGHT = "productWeight"
        const val ARG_PRODUCT_WARRANTY = "productWarrantyPeriod"
        const val ARG_PRODUCT_VALIDITY = "productValidityPeriod"
        const val ARG_VENDOR_NAMES = "productVendorNames"
        const val ARG_VENDOR_SKUS = "productVendorSkus"
        const val ARG_VENDOR_AVAILABILITIES = "productVendorAvailabilities"
        const val ARG_VENDOR_UNITS = "productVendorUnits"
        const val ARG_VENDOR_COST_PRICES = "productVendorCostPrices"
        const val ARG_VENDOR_LIST_PRICES = "productVendorListPrices"
        const val ARG_VENDOR_STATUSES = "productVendorStatuses"
        const val ARG_PRODUCT_PRICE = "productPrice"
        const val ARG_PRODUCT_AVAILABILITY = "productAvailability"
        const val ARG_PRODUCT_STATUS = "productStatus"
        const val ARG_PRODUCT_THUMBNAIL = "productThumbnailLabel"
        const val ARG_PRODUCT_BRAND_THUMBNAIL = "productBrandThumbnail"
        const val PRODUCT_INFO_SPAN_COUNT = 2
        const val DESCRIPTION_COLLAPSED_LINES = 3
    }
}

private data class ProductDetailUi(
    val id: String,
    val name: String,
    val sku: String,
    val manufacturer: String,
    val manufacturerPartNumber: String,
    val vendor: String,
    val category: String,
    val description: String,
    val countryOfOrigin: String,
    val screenSize: String,
    val dimensions: String,
    val unitOfMeasure: String,
    val weight: String,
    val warrantyPeriod: String,
    val validityPeriod: String,
    val vendors: List<ProductVendorUi>,
    val price: String,
    val availability: String,
    val status: String,
    val thumbnailLabel: String,
    val brandThumbnail: Boolean
) {
    companion object {
        fun empty() = ProductDetailUi(
            id = "",
            name = "",
            sku = "",
            manufacturer = "",
            manufacturerPartNumber = "",
            vendor = "",
            category = "",
            description = "",
            countryOfOrigin = "",
            screenSize = "",
            dimensions = "",
            unitOfMeasure = "",
            weight = "",
            warrantyPeriod = "",
            validityPeriod = "",
            vendors = emptyList(),
            price = "",
            availability = "",
            status = "",
            thumbnailLabel = "",
            brandThumbnail = false
        )
    }
}

private data class ProductDetailInfoTileUi(
    val iconRes: Int,
    val label: String,
    val value: String
)

private data class ProductDetailTabUi(
    val tab: ProductDetailTab,
    val title: String
)

private enum class ProductDetailTab {
    ProductDescription,
    TechnicalSpecifications,
    VendorAssociated,
    ComplianceCertification,
    CompetitorPricing
}

private class ProductDetailInfoAdapter : RecyclerView.Adapter<ProductDetailInfoAdapter.InfoViewHolder>() {
    private val items = mutableListOf<ProductDetailInfoTileUi>()

    fun submitList(newItems: List<ProductDetailInfoTileUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
        val binding = ItemProductDetailInfoTileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InfoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class InfoViewHolder(
        private val binding: ItemProductDetailInfoTileBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductDetailInfoTileUi) {
            binding.tileIcon.setImageResource(item.iconRes)
            binding.tileLabel.text = item.label
            binding.tileValue.text = item.value.ifBlank { "-" }
        }
    }
}

private class ProductDetailTabsAdapter(
    private val onTabClick: (ProductDetailTab) -> Unit
) : RecyclerView.Adapter<ProductDetailTabsAdapter.TabViewHolder>() {
    private val items = mutableListOf<ProductDetailTabUi>()
    private var selectedTab = ProductDetailTab.ProductDescription

    fun submitList(newItems: List<ProductDetailTabUi>, selected: ProductDetailTab) {
        items.clear()
        items.addAll(newItems)
        selectedTab = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val binding = ItemProductDetailTabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.bind(items[position], selectedTab == items[position].tab, onTabClick)
    }

    override fun getItemCount(): Int = items.size

    class TabViewHolder(
        private val binding: ItemProductDetailTabBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: ProductDetailTabUi,
            selected: Boolean,
            onTabClick: (ProductDetailTab) -> Unit
        ) {
            binding.tabText.text = item.title
            binding.tabText.setBackgroundResource(
                if (selected) R.drawable.bg_product_detail_tab_selected else R.drawable.bg_product_detail_tab_unselected
            )
            binding.tabText.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (selected) R.color.ds_on_primary else R.color.ds_text_primary
                )
            )
            binding.root.setOnClickListener { onTabClick(item.tab) }
        }
    }
}

private class ProductDetailVendorAdapter : RecyclerView.Adapter<ProductDetailVendorAdapter.VendorViewHolder>() {
    private val items = mutableListOf<ProductVendorUi>()

    fun submitList(newItems: List<ProductVendorUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VendorViewHolder {
        val binding = ItemProductVendorAssociatedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VendorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VendorViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VendorViewHolder(
        private val binding: ItemProductVendorAssociatedBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductVendorUi) {
            val active = item.status.equals("Active", ignoreCase = true) ||
                item.status.equals("Available", ignoreCase = true)
            val inStock = item.availability.equals(ProductAvailability.InStock.label, ignoreCase = true)
            binding.vendorName.text = item.name.ifBlank { "-" }
            binding.vendorSku.text = binding.root.context.getString(
                R.string.sku_part_no,
                item.sku.ifBlank { "-" }
            )
            binding.vendorStatus.text = item.status.ifBlank { "-" }
            binding.vendorStatus.setBackgroundResource(
                if (active) R.drawable.bg_status_success else R.drawable.bg_status_error
            )
            binding.vendorStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, if (active) R.color.ds_success else R.color.ds_error)
            )
            binding.vendorAvailability.text = item.availability.ifBlank { "-" }
            binding.vendorAvailability.setBackgroundResource(
                if (inStock) R.drawable.bg_status_success else R.drawable.bg_status_error
            )
            binding.vendorAvailability.setTextColor(
                ContextCompat.getColor(binding.root.context, if (inStock) R.color.ds_success else R.color.ds_error)
            )
            binding.vendorUnit.text = item.unit.ifBlank { "-" }
            binding.vendorCostPrice.text = item.costPrice.ifBlank { "-" }
            binding.vendorListPrice.text = item.listPrice.ifBlank { "-" }
        }
    }
}

private class ProductDetailGridSpacingDecoration(
    private val spacing: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view).takeIf { it != RecyclerView.NO_POSITION } ?: return
        val column = position % 2
        outRect.left = if (column == 0) 0 else spacing / 2
        outRect.right = if (column == 0) spacing / 2 else 0
        outRect.bottom = spacing
    }
}

private class ProductDetailHorizontalSpacingDecoration(
    private val spacing: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION && position < (parent.adapter?.itemCount ?: 0) - 1) {
            outRect.right = spacing
        }
    }
}
