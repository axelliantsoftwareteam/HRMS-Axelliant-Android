package com.axelliant.hris.model.expense

import android.net.Uri

/**
 * An attachment thumbnail: a photo picked on this device ([uri], not uploaded yet) or a file that is
 * already on the claim ([imageUrl] + [file_id]).
 */
class ImageType(
    var uri: Uri? = null,
    var imageUrl: String? = null,
    var file_id: String? = null
) {
    /** True for a photo that still has to be uploaded. */
    val isLocal: Boolean get() = uri != null
}
