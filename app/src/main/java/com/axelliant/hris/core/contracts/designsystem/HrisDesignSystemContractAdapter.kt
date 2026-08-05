package com.axelliant.hris.core.contracts.designsystem

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HrisDesignSystemContractAdapter @Inject constructor() : DesignSystemContract {
    override val defaultFamily: DesignSystemFamily = DesignSystemFamily.FLUENT

    override fun componentFor(role: ComponentRole): DesignComponentSpec {
        val componentName = when (role) {
            ComponentRole.BUTTON -> "RoundedButton / Fluent AppButton"
            ComponentRole.TEXT_FIELD -> "Material TextInputLayout"
            ComponentRole.BOTTOM_SHEET -> "Material BottomSheetDialogFragment"
            ComponentRole.CARD -> "MaterialCardView / Compose Fluent card"
            ComponentRole.TOP_BAR -> "HRIS toolbar layout"
            ComponentRole.BOTTOM_NAVIGATION -> "Material BottomNavigationView"
            ComponentRole.DIALOG -> "Material AlertDialog"
            ComponentRole.LIST_ITEM -> "RecyclerView row layout"
            ComponentRole.PROGRESS -> "HRIS loading dialog"
        }
        return DesignComponentSpec(role, defaultFamily, componentName)
    }
}
