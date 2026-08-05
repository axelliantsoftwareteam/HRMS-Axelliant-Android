package com.axelliant.hris.core.contracts.designsystem

enum class DesignSystemFamily {
    MATERIAL,
    FLUENT
}

enum class ComponentRole {
    BUTTON,
    TEXT_FIELD,
    BOTTOM_SHEET,
    CARD,
    TOP_BAR,
    BOTTOM_NAVIGATION,
    DIALOG,
    LIST_ITEM,
    PROGRESS
}

data class DesignComponentSpec(
    val role: ComponentRole,
    val family: DesignSystemFamily,
    val componentName: String
)

interface DesignSystemContract {
    val defaultFamily: DesignSystemFamily
    fun componentFor(role: ComponentRole): DesignComponentSpec
}
